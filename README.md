# Veil

**Privacy, made clear.**

Veil is a polished, responsive front-end concept for a privacy-first browser. It makes privacy easier to understand with a clear dashboard, plain-language tracker explanations, site permission controls, and separate personal and anonymous session modes.

## Preview

Open [index.html](./index.html) in any modern browser. No build step, package manager, framework, or C++ required.

## Features

- **Privacy overview** with a visual score and tracker/cookie/data-request summaries.
- **Readable tracker explanations** that explain what tracking can mean in everyday language.
- **Permission controls** for location, camera, and microphone in the interactive demo.
- **Personal and anonymous session selector** to preview separate browsing modes.
- **Responsive interface** designed for desktop and mobile screens.
- **Accessible interactions** with keyboard-friendly controls and status announcements.

## Run locally

1. Download or clone this repository.
2. Open `index.html` in your browser.

For a local static server, you can also run `python -m http.server 8000` from the project folder and visit `http://localhost:8000`.

## Important scope note

This repository currently contains a **front-end demonstration**, not a functioning browser engine or a real privacy protection service. The displayed scan numbers are illustrative; the demo does not inspect live network traffic, block real trackers, enforce operating-system permissions, or isolate browser storage between sessions. Those capabilities require a browser platform/runtime integration and security testing before being presented as real protections.

## Technology

- HTML
- CSS
- Vanilla JavaScript

No C++ and no external runtime dependencies.

## Product principle

Privacy should be understandable, visible, and under the user's control.
