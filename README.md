# Veil Browser

Veil is being built as a **standalone Java desktop application**. It is not a website pretending to be a browser; the browser interface runs in its own desktop window.

## Technology
- Java 21
- JavaFX WebView (WebKit browser engine)
- Maven

No C++ source code is written for Veil. JavaFX and its browser engine include native platform components as part of their runtime.

## Run Veil
Install **JDK 21** and **Maven 3.9+**, then run from the repository root:

```sh
mvn clean javafx:run
```

Veil opens a desktop window. Enter a web address such as `example.com`, or type a search such as `how does DNS work` and press Enter. Searches are sent to DuckDuckGo.

## Build
```sh
mvn clean package
```

A GitHub Actions workflow compiles the Java app on Windows, macOS, and Linux. It currently checks compilation; it does not yet publish a signed, self-contained installer. JavaFX WebView uses WebKit and may not support every feature used by modern websites.

## Current scope and privacy
The app includes tabs, address/search navigation, back/forward, reload, keyboard shortcuts, and a privacy-information dialog. This JavaFX version does **not** yet implement comprehensive network-level tracker blocking, anti-fingerprinting, or per-site permission management. Do not treat it as an anonymity tool.

## Source
- `src/main/java/app/veil/VeilBrowser.java` — desktop app, tabs, navigation and UI.
- `pom.xml` — Java and JavaFX dependencies.
- `.github/workflows/build-java.yml` — cross-platform compilation check.

## License
MIT
