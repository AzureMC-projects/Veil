const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('veil', {
  navigate: url => ipcRenderer.send('browser:navigate', url),
  back: () => ipcRenderer.send('browser:back'),
  forward: () => ipcRenderer.send('browser:forward'),
  reload: () => ipcRenderer.send('browser:reload'),
  stop: () => ipcRenderer.send('browser:stop'),
  newTab: () => ipcRenderer.send('browser:new-tab'),
  activateTab: id => ipcRenderer.send('browser:activate-tab', id),
  closeTab: id => ipcRenderer.send('browser:close-tab', id),
  toggleProtection: enabled => ipcRenderer.send('browser:toggle-protection', enabled),
  clearData: () => ipcRenderer.send('browser:clear-data'),
  openExternal: url => ipcRenderer.send('browser:open-external', url),
  getSettings: () => ipcRenderer.invoke('browser:get-settings'),
  setSetting: (key, value) => ipcRenderer.invoke('browser:set-setting', key, value),
  onState: callback => ipcRenderer.on('browser:state', (_event, state) => callback(state)),
  onToast: callback => ipcRenderer.on('browser:toast', (_event, message) => callback(message))
});
