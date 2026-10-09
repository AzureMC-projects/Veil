# Veil Browser

**A privacy-focused desktop browser. No C++ application code.**

Veil is an Electron desktop browser built with JavaScript, HTML, and CSS. Electron includes Chromium so Veil opens real websites in browser views rather than simulating a browser in a webpage.

## Features in v1

- Desktop window with tabs, address/search bar, back, forward, reload, and keyboard shortcuts.
- DuckDuckGo search when text is entered instead of a web address.
- Built-in blocking for requests to a curated set of known tracking and advertising hosts.
- Optional Do Not Track request header.
- Optional removal of `Set-Cookie` response headers from embedded/subresource requests.
- Location, camera, microphone, and notification permissions denied by default.
- Clear browsing cookies, site storage, and cache from Privacy settings.
- Secure renderer defaults: context isolation, sandboxing, no Node integration in page content, and web security enabled.
- GitHub Actions workflow to package Windows, macOS, and Linux builds.

## Run from source

Install Node.js 22 or newer, then run:

```sh
npm install
npm start
```

## Build an installer

```sh
npm run dist
```

Platform-specific builds are also available with `npm run dist:win`, `npm run dist:mac`, and `npm run dist:linux`. Build output is written to `release/`. GitHub Actions builds on Windows, macOS, and Linux and uploads installers as workflow artifacts.

## Privacy scope and limitations

Veil v1 is not a claim of perfect anonymity. Its tracker blocklist is built in and is not automatically updated; it cannot block every tracker. The subresource cookie control removes cookie-setting response headers for non-main-frame requests, which can break some sites and is not equivalent to Chromium's full third-party-cookie partitioning. Do Not Track is advisory and websites may ignore it. Site permissions are denied by default and there is no per-site permission allowlist yet. Browser fingerprinting, IP address visibility, malicious websites, and all tracking techniques are not fully prevented.

Electron/Chromium contains native components, but this repository contains no C++ source authored for Veil. Keep Electron updated to receive upstream security fixes.

## Project structure

- `main.js` — desktop window, tabs, request filtering, permission defaults, and browser controls.
- `preload.js` — isolated bridge between the interface and Electron.
- `index.html` — browser toolbar, tab strip, and privacy settings.
- `.github/workflows/build.yml` — cross-platform packaging workflow.

## License

MIT
