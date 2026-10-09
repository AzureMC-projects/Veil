package app.veil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.collections.ListChangeListener;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import netscape.javascript.JSObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class VeilBrowser extends Application {
    private static final String GREEN = "#a6f0bd";
    private static final String BG = "#0b100e";
    private static final String PANEL = "#111815";
    private static final String BORDER = "#29362e";
    private static final String TEXT = "#f0f5f1";
    private static final String MUTED = "#91a398";

    private final List<BrowserTab> tabs = new ArrayList<>();
    private final TabPane tabPane = new TabPane();
    private final TextField address = new TextField();
    private final Label status = new Label("Veil · Java desktop browser");
    private final Label security = new Label("◆ WebKit");
    private Button backButton, forwardButton, reloadButton;
    private Stage stage;
    private boolean syncingAddress;

    private static final class BrowserTab {
        final Tab tab;
        final WebView view;
        final WebEngine engine;
        BrowserTab(Tab tab, WebView view) { this.tab = tab; this.view = view; this.engine = view.getEngine(); }
    }

    @Override public void start(Stage stage) {
        this.stage = stage;
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookies);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(buildChrome());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabPane.setStyle("-fx-background-color:" + BG + ";-fx-tab-min-height:34px;-fx-tab-max-height:34px;");
        root.setCenter(tabPane);
        root.setBottom(buildStatus());
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab removed : change.getRemoved()) tabs.removeIf(t -> t.tab == removed);
                }
            }
            if (tabPane.getTabs().isEmpty() && stage.isShowing()) Platform.runLater(() -> newTab("https://duckduckgo.com/"));
            syncCurrent();
        });
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> syncCurrent());

        Scene scene = new Scene(root, 1380, 880);
        scene.setFill(Color.web(BG));
        scene.getStylesheets().add("data:text/css," + css().replace("#", "%23").replace(" ", "%20").replace("\n", ""));
        stage.setTitle("Veil Browser");
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
        newTab("https://duckduckgo.com/");
        scene.setOnKeyPressed(e -> {
            if ((e.isControlDown() || e.isMetaDown()) && e.getCode() == KeyCode.L) { address.requestFocus(); address.selectAll(); }
            if ((e.isControlDown() || e.isMetaDown()) && e.getCode() == KeyCode.T) newTab("https://duckduckgo.com/");
            if ((e.isControlDown() || e.isMetaDown()) && e.getCode() == KeyCode.W) closeCurrentTab();
            if ((e.isAltDown() && e.getCode() == KeyCode.LEFT)) goBack();
            if ((e.isAltDown() && e.getCode() == KeyCode.RIGHT)) goForward();
        });
    }

    private String css() {
        return ".root{-fx-font-family:'Inter','Segoe UI',sans-serif;-fx-base:#111815;-fx-background:#0b100e;}" +
        ".tab-pane .tab-header-area .tab-header-background{-fx-background-color:#0e1411;}" +
        ".tab-pane .tab{-fx-background-color:#151d18;-fx-background-radius:8 8 0 0;-fx-border-color:transparent;}" +
        ".tab-pane .tab:selected{-fx-background-color:#1b261f;-fx-border-color:#304136;-fx-border-radius:8 8 0 0;}" +
        ".tab .tab-label{-fx-text-fill:#e8f1ea;-fx-font-size:12px;}" +
        ".text-field{-fx-background-color:#19221c;-fx-text-fill:#edf4ef;-fx-prompt-text-fill:#8b9a90;-fx-background-radius:10;-fx-border-color:#2a392f;-fx-border-radius:10;-fx-padding:9 12;}" +
        ".button{-fx-background-color:transparent;-fx-text-fill:#bdc9c0;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 12;}" +
        ".button:hover{-fx-background-color:#26332a;-fx-text-fill:white;}" +
        ".label{-fx-text-fill:#e9f1eb;}" +
        ".progress-bar{-fx-accent:#a6f0bd;}" +
        ".scroll-bar:vertical,.scroll-bar:horizontal{-fx-background-color:#111815;}" +
        ".scroll-bar .thumb{-fx-background-color:#34463a;-fx-background-radius:8;}";
    }

    private VBox buildChrome() {
        Label logo = new Label("V");
        logo.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#102217;-fx-font-weight:bold;-fx-font-size:16px;-fx-background-radius:9;-fx-min-width:30px;-fx-min-height:30px;-fx-alignment:center;");
        Label word = new Label("veil");
        word.setFont(Font.font("System", FontWeight.BOLD, 17));
        HBox brand = new HBox(9, logo, word); brand.setAlignment(Pos.CENTER_LEFT);
        backButton = tool("‹", "Back"); backButton.setOnAction(e -> goBack());
        forwardButton = tool("›", "Forward"); forwardButton.setOnAction(e -> goForward());
        reloadButton = tool("↻", "Reload"); reloadButton.setOnAction(e -> { BrowserTab t = current(); if (t != null) t.engine.reload(); });
        address.setPromptText("Search privately or enter a web address");
        address.setOnAction(e -> navigate(address.getText()));
        HBox.setHgrow(address, Priority.ALWAYS);
        Button go = new Button("Go"); go.setStyle("-fx-background-color:#294c34;-fx-text-fill:#dff9e6;-fx-background-radius:7;");
        go.setOnAction(e -> navigate(address.getText()));
        security.setStyle("-fx-text-fill:" + GREEN + ";-fx-background-color:#16271b;-fx-border-color:#31533a;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 10;");
        Button home = tool("⌂", "Home"); home.setOnAction(e -> navigate("https://duckduckgo.com/"));
        Button newTab = tool("+", "New tab"); newTab.setOnAction(e -> newTab("https://duckduckgo.com/"));
        Button privacy = tool("☷", "Privacy and browser information"); privacy.setOnAction(e -> showPrivacy());
        HBox toolbar = new HBox(8, brand, backButton, forwardButton, reloadButton, address, go, security, home, newTab, privacy);
        toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.setPadding(new Insets(10, 14, 11, 14));
        HBox.setHgrow(address, Priority.ALWAYS);
        VBox chrome = new VBox(toolbar);
        chrome.setStyle("-fx-background-color:#0e1411;-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;");
        return chrome;
    }

    private HBox buildStatus() {
        Label dot = new Label("●"); dot.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:9px;");
        status.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label engine = new Label("Java · JavaFX WebView"); engine.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");
        HBox bar = new HBox(8, dot, status, spacer, engine); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(7, 14, 7, 14));
        bar.setStyle("-fx-background-color:#0e1411;-fx-border-color:" + BORDER + " transparent transparent transparent;");
        return bar;
    }

    private Button tool(String text, String tip) { Button b = new Button(text); b.setTooltip(new Tooltip(tip)); b.setMinWidth(34); return b; }

    private void newTab(String url) {
        WebView view = new WebView();
        view.setContextMenuEnabled(true);
        view.setZoom(1.0);
        WebEngine engine = view.getEngine();
        Tab tab = new Tab("New tab");
        tab.setClosable(true);
        BrowserTab bt = new BrowserTab(tab, view);
        tabs.add(bt);
        tab.setContent(view);
        tab.setOnClosed(e -> tabs.remove(bt));
        engine.titleProperty().addListener((obs, old, title) -> {
            String name = title == null || title.isBlank() ? "New tab" : title;
            tab.setText(name.length() > 26 ? name.substring(0, 23) + "…" : name);
            if (current() == bt) stage.setTitle(name + " — Veil");
        });
        engine.locationProperty().addListener((obs, old, location) -> {
            if (current() == bt && !syncingAddress) {
                syncingAddress = true; address.setText(location); syncingAddress = false;
                security.setText(location != null && location.startsWith("https:") ? "🔒 HTTPS" : "◇ WebKit");
            }
        });
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (current() == bt) {
                if (state == Worker.State.RUNNING) status.setText("Loading page…");
                else if (state == Worker.State.SUCCEEDED) status.setText("Page loaded · Check the site’s privacy policy");
                else if (state == Worker.State.FAILED) status.setText("Page failed to load · Check the address or connection");
                else if (state == Worker.State.CANCELLED) status.setText("Navigation stopped");
            }
        });
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        engine.load(safeUrl(url));
        syncCurrent();
    }

    private BrowserTab current() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null) return null;
        return tabs.stream().filter(t -> t.tab == selected).findFirst().orElse(null);
    }

    private void syncCurrent() {
        BrowserTab t = current();
        if (t == null) return;
        syncingAddress = true; address.setText(t.engine.getLocation() == null ? "" : t.engine.getLocation()); syncingAddress = false;
        backButton.setDisable(t.engine.getHistory().getCurrentIndex() <= 0); forwardButton.setDisable(t.engine.getHistory().getCurrentIndex() >= t.engine.getHistory().getEntries().size() - 1);
        stage.setTitle((t.engine.getTitle() == null || t.engine.getTitle().isBlank() ? "New tab" : t.engine.getTitle()) + " — Veil");
    }

    private String safeUrl(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return "https://duckduckgo.com/";
        if (value.matches("(?i)^https?://.*")) return value;
        if (value.matches("(?i)^(javascript|data|file|vbscript):.*")) return "https://duckduckgo.com/?q=" + enc(value);
        if (value.matches("(?i)^(localhost|127\\.0\\.0\\.1)(:\\d+)?([/].*)?$") ||
            value.matches("(?i)^([a-z0-9-]+\\.)+[a-z]{2,}(:\\d+)?([/].*)?$")) return "https://" + value;
        return "https://duckduckgo.com/?q=" + enc(value);
    }

    private String enc(String value) {
        try { return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception ignored) { return value; }
    }

    private void navigate(String input) {
        BrowserTab t = current();
        if (t == null) { newTab(safeUrl(input)); return; }
        String target = safeUrl(input);
        syncingAddress = true; address.setText(target); syncingAddress = false;
        t.engine.load(target);
        address.getParent().requestFocus();
    }

    private void goBack() { BrowserTab t = current(); if (t != null && t.engine.getHistory().getCurrentIndex() > 0) t.engine.getHistory().go(-1); syncCurrent(); }
    private void goForward() { BrowserTab t = current(); if (t != null && t.engine.getHistory().getCurrentIndex() < t.engine.getHistory().getEntries().size() - 1) t.engine.getHistory().go(1); syncCurrent(); }
    private void closeCurrentTab() { BrowserTab t = current(); if (t != null) tabPane.getTabs().remove(t.tab); }

    private void showPrivacy() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage); dialog.setTitle("About Veil privacy");
        ButtonType clear = new ButtonType("Clear cookies", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(clear, ButtonType.CLOSE);
        Label heading = new Label("Privacy, without pretending.");
        heading.setFont(Font.font("System", FontWeight.BOLD, 17));
        Label detail = new Label("Veil is a Java desktop browser using JavaFX WebView (WebKit). Search and navigation run in the desktop app. Site permissions are controlled by the underlying engine. This initial Java build does not yet include comprehensive network-level tracker blocking, fingerprinting protection, or a per-site permission manager.");
        detail.setWrapText(true); detail.setMaxWidth(430); detail.setStyle("-fx-text-fill:" + MUTED + ";");
        VBox content = new VBox(12, heading, detail); content.setPadding(new Insets(18)); content.setPrefWidth(470);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait().ifPresent(result -> {
            if (result == clear) {
                try {
                    CookieHandler handler = CookieHandler.getDefault();
                    if (handler instanceof CookieManager manager) manager.getCookieStore().removeAll();
                    status.setText("Cookie store cleared for Java's shared cookie handler");
                } catch (Exception ex) { status.setText("Could not clear cookies: " + ex.getMessage()); }
            }
        });
    }

    public static void main(String[] args) { launch(args); }
}
