const { app, BrowserWindow, BrowserView, ipcMain, session, shell } = require('electron');
const path = require('node:path');

const CHROME_HEIGHT = 126;
const TRACKER_HOSTS = [
  'doubleclick.net','google-analytics.com','googletagmanager.com','googlesyndication.com',
  'adservice.google.com','connect.facebook.net','facebook.net','analytics.twitter.com',
  'ads-twitter.com','scorecardresearch.com','hotjar.com','segment.io','segment.com',
  'amplitude.com','mixpanel.com','clarity.ms','fullstory.com','taboola.com','outbrain.com',
  'criteo.com','adnxs.com','adsrvr.org','quantserve.com','newrelic.com','bugsnag.com',
  'sentry.io','branch.io','appsflyer.com','adjust.com','matomo.cloud'
];

let win;
let tabs = [];
let activeTabId = null;
let nextTabId = 1;
let protectionEnabled = true;
let blockedCount = 0;
let settings = { blockTrackers: true, blockThirdPartyCookies: true, doNotTrack: true };
const browserSession = () => session.defaultSession;

function isTracker(url) {
  try {
    const host = new URL(url).hostname.toLowerCase();
    return TRACKER_HOSTS.some(domain => host === domain || host.endsWith('.' + domain));
  } catch { return false; }
}

function configurePrivacy() {
  const ses = browserSession();
  ses.webRequest.onBeforeRequest((details, callback) => {
    if (!settings.blockTrackers || details.resourceType === 'mainFrame') return callback({});
    if (!isTracker(details.url)) return callback({});
    blockedCount += 1;
    broadcastState();
    callback({ cancel: true });
  });
  ses.webRequest.onBeforeSendHeaders((details, callback) => {
    const headers = { ...details.requestHeaders };
    if (settings.doNotTrack) headers.DNT = '1';
    callback({ requestHeaders: headers });
  });
  ses.webRequest.onHeadersReceived((details, callback) => {
    const responseHeaders = { ...details.responseHeaders };
    if (settings.blockThirdPartyCookies) {
      for (const key of Object.keys(responseHeaders)) {
        if (key.toLowerCase() === 'set-cookie' && details.resourceType !== 'mainFrame') delete responseHeaders[key];
      }
    }
    callback({ responseHeaders });
  });
  ses.setPermissionRequestHandler((_webContents, _permission, callback) => callback(false));
  ses.setPermissionCheckHandler(() => false);
}

function activeTab() { return tabs.find(t => t.id === activeTabId); }

function broadcastState() {
  if (!win || win.isDestroyed()) return;
  const current = activeTab();
  win.webContents.send('browser:state', {
    tabs: tabs.map(t => ({ id: t.id, title: t.title || 'New tab', url: t.url || '', loading: t.loading })),
    activeTabId, blockedCount, protectionEnabled: settings.blockTrackers,
    canGoBack: !!current && current.view.webContents.navigationHistory.canGoBack(),
    canGoForward: !!current && current.view.webContents.navigationHistory.canGoForward(),
    url: current?.url || '', title: current?.title || 'Veil'
  });
}

function layoutViews() {
  if (!win || win.isDestroyed()) return;
  const { width, height } = win.getContentBounds();
  for (const tab of tabs) {
    tab.view.setBounds({ x: 0, y: CHROME_HEIGHT, width: Math.max(0, width), height: Math.max(0, height - CHROME_HEIGHT) });
    tab.view.setAutoResize({ width: true, height: true });
  }
}

function safeUrl(input) {
  const value = String(input || '').trim();
  if (!value) return 'https://duckduckgo.com/';
  if (/^(about:blank|https?:\/\/)/i.test(value)) return value;
  if (/^(javascript|file|data|vbscript):/i.test(value)) return 'https://duckduckgo.com/?q=' + encodeURIComponent(value);
  if (/^(localhost|127\.0\.0\.1)(:\d+)?([/].*)?$/i.test(value) || /^([\w-]+\.)+[a-z]{2,}(:\d+)?([/].*)?$/i.test(value)) return 'https://' + value;
  return 'https://duckduckgo.com/?q=' + encodeURIComponent(value);
}

function createTab(url = 'https://duckduckgo.com/') {
  const view = new BrowserView({ webPreferences: {
    nodeIntegration: false, contextIsolation: true, sandbox: true,
    webSecurity: true, allowRunningInsecureContent: false,
    safeDialogs: true, spellcheck: true
  }});
  const tab = { id: nextTabId++, view, title: 'New tab', url: '', loading: true };
  tabs.push(tab);
  win.addBrowserView(view);
  activeTabId = tab.id;
  view.webContents.on('will-navigate', (event, target) => { try { const protocol = new URL(target).protocol; if (!['https:', 'http:'].includes(protocol)) event.preventDefault(); } catch { event.preventDefault(); } });
  view.webContents.setWindowOpenHandler(({ url: target }) => {
    createTab(target);
    return { action: 'deny' };
  });
  view.webContents.on('page-title-updated', (_event, title) => { tab.title = title || 'New tab'; broadcastState(); });
  view.webContents.on('did-navigate', (_event, url) => { tab.url = url; tab.loading = false; broadcastState(); });
  view.webContents.on('did-navigate-in-page', (_event, url) => { tab.url = url; broadcastState(); });
  view.webContents.on('did-start-loading', () => { tab.loading = true; broadcastState(); });
  view.webContents.on('did-stop-loading', () => { tab.loading = false; broadcastState(); });
  view.webContents.on('render-process-gone', () => { tab.title = 'Page crashed'; broadcastState(); });
  layoutViews();
  tabs.filter(t => t.id !== activeTabId).forEach(t => t.view.setBounds({ x: -10000, y: -10000, width: 1, height: 1 }));
  view.webContents.loadURL(safeUrl(url)).catch(() => {});
  broadcastState();
  return tab;
}

function activateTab(id) {
  const chosen = tabs.find(t => t.id === Number(id));
  if (!chosen) return;
  activeTabId = chosen.id;
  tabs.forEach(t => t.view.setBounds(t.id === chosen.id
    ? { x: 0, y: CHROME_HEIGHT, width: win.getContentBounds().width, height: Math.max(0, win.getContentBounds().height - CHROME_HEIGHT) }
    : { x: -10000, y: -10000, width: 1, height: 1 }));
  broadcastState();
}

function closeTab(id) {
  const index = tabs.findIndex(t => t.id === Number(id));
  if (index < 0) return;
  const [tab] = tabs.splice(index, 1);
  win.removeBrowserView(tab.view);
  tab.view.webContents.close();
  if (!tabs.length) createTab();
  else if (activeTabId === tab.id) activateTab(tabs[Math.max(0, index - 1)].id);
  broadcastState();
}

function createWindow() {
  win = new BrowserWindow({
    width: 1440, height: 920, minWidth: 760, minHeight: 560,
    backgroundColor: '#0b100e',
    title: 'Veil',
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: true }
  });
  win.loadFile('index.html');
  win.on('resize', layoutViews);
  win.webContents.on('did-finish-load', () => { if (!tabs.length) createTab(); else { layoutViews(); broadcastState(); } });
  win.on('closed', () => { win = null; tabs = []; activeTabId = null; });
}

app.whenReady().then(() => {
  app.setAppUserModelId('com.veil.browser');
  configurePrivacy();
  createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

ipcMain.on('browser:navigate', (_e, url) => activeTab()?.view.webContents.loadURL(safeUrl(url)).catch(() => {}));
ipcMain.on('browser:back', () => { const w = activeTab()?.view.webContents; if (w?.navigationHistory.canGoBack()) w.navigationHistory.goBack(); });
ipcMain.on('browser:forward', () => { const w = activeTab()?.view.webContents; if (w?.navigationHistory.canGoForward()) w.navigationHistory.goForward(); });
ipcMain.on('browser:reload', () => activeTab()?.view.webContents.reload());
ipcMain.on('browser:stop', () => activeTab()?.view.webContents.stop());
ipcMain.on('browser:new-tab', () => createTab());
ipcMain.on('browser:activate-tab', (_e, id) => activateTab(id));
ipcMain.on('browser:close-tab', (_e, id) => closeTab(id));
ipcMain.on('browser:toggle-protection', (_e, enabled) => { settings.blockTrackers = !!enabled; broadcastState(); });
ipcMain.on('browser:clear-data', async () => {
  await browserSession().clearStorageData({ storages: ['appcache','cookies','filesystem','indexdb','localstorage','shadercache','websql','serviceworkers','cachestorage'] });
  await browserSession().clearCache();
  broadcastState();
  win?.webContents.send('browser:toast', 'Browsing data cleared');
});
ipcMain.on('browser:open-external', (_e, url) => {
  try { const parsed = new URL(url); if (['https:', 'http:'].includes(parsed.protocol)) shell.openExternal(parsed.href); } catch {}
});
ipcMain.handle('browser:get-settings', () => ({ ...settings, blockedCount }));
ipcMain.handle('browser:set-setting', (_e, key, value) => {
  if (!['blockTrackers','blockThirdPartyCookies','doNotTrack'].includes(key)) return false;
  settings[key] = !!value;
  broadcastState();
  return true;
});
