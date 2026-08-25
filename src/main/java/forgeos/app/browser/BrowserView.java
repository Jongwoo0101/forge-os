package forgeos.app.browser;

import forgeos.ui.Glyphs;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 브라우저 화면 — 탭 · 주소창 · 진행 표시.
 *
 * <h2>도구 모음은 항상 "선택된 탭"을 비춘다</h2>
 * <p>탭마다 주소창을 두면 화면이 두 줄로 두꺼워지고, 탭 하나에 하나씩
 * 프로퍼티를 바인딩하면 탭을 옮길 때마다 바인딩을 끊고 다시 걸어야 한다.
 * 대신 각 탭이 자기 상태가 바뀔 때마다 {@link #syncToolbar()}를 부르고,
 * 도구 모음은 <b>지금 선택된 탭만</b> 읽는다. 바인딩이 없으니 끊을 것도 없다.</p>
 *
 * <h2>주소창은 주소도 검색도 받는다</h2>
 * <p>{@code://}가 있으면 그대로, 점이 박힌 한 덩어리면 {@code https://}를 붙이고,
 * 나머지는 검색어로 본다. 사람이 주소창에 무엇을 칠지 고민하지 않게 하려는 것이
 * 현대 브라우저가 주소창과 검색창을 합친 이유다.</p>
 */
final class BrowserView extends BorderPane {

    /** {@code scheme://} 로 시작하는가. */
    private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    /** 공백 없이 점이 박힌 한 덩어리 — 도메인으로 본다. */
    private static final Pattern HOSTLIKE = Pattern.compile("^[^\\s/:]+\\.[^\\s/:]{2,}(:\\d+)?(/.*)?$");

    /** 검색 엔진. 질의를 뒤에 붙이면 된다. */
    private static final String SEARCH = "https://duckduckgo.com/?q=";

    private final TabPane tabs = new TabPane();
    private final TextField address = new TextField();
    private final ProgressBar progress = new ProgressBar();

    private final Button backButton =
            new Button(null, Glyphs.stroked(Glyphs.ARROW_LEFT, 15, "button-glyph"));
    private final Button forwardButton =
            new Button(null, Glyphs.stroked(Glyphs.ARROW_RIGHT, 15, "button-glyph"));
    private final Button reloadButton =
            new Button(null, Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));
    private final Node lockGlyph = Glyphs.stroked(Glyphs.LOCK, 13, "browser-lock");

    BrowserView() {
        getStyleClass().add("browser");

        tabs.getStyleClass().add("browser-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> syncToolbar());

        // 마지막 탭까지 닫히면 빈 창이 남는다. 브라우저가 아니라 고장으로 보인다.
        tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
            if (tabs.getTabs().isEmpty()) {
                openTab(null);
            }
        });

        setTop(buildChrome());
        setCenter(tabs);

        openTab(null);
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private VBox buildChrome() {
        backButton.getStyleClass().add("browser-nav");
        backButton.setOnAction(e -> go(-1));

        forwardButton.getStyleClass().add("browser-nav");
        forwardButton.setOnAction(e -> go(1));

        reloadButton.getStyleClass().add("browser-nav");
        reloadButton.setOnAction(e -> reloadOrStop());

        Button home = new Button(null, Glyphs.stroked(Glyphs.HOME, 15, "button-glyph"));
        home.getStyleClass().add("browser-nav");
        home.setOnAction(e -> {
            BrowserTab tab = current();
            if (tab != null) {
                tab.loadStartPage();
            }
        });

        address.getStyleClass().addAll("field", "browser-address");
        address.setPromptText("검색하거나 주소를 입력하세요");
        HBox.setHgrow(address, Priority.ALWAYS);
        address.setOnAction(e -> navigate(address.getText()));

        HBox addressBox = new HBox(lockGlyph, address);
        addressBox.getStyleClass().add("browser-address-box");
        addressBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addressBox, Priority.ALWAYS);

        Button newTab = new Button(null, Glyphs.stroked(Glyphs.PLUS, 15, "button-glyph"));
        newTab.getStyleClass().add("browser-nav");
        newTab.setOnAction(e -> openTab(null));

        HBox bar = new HBox(backButton, forwardButton, reloadButton, home, addressBox, newTab);
        bar.getStyleClass().addAll("toolbar", "browser-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        progress.getStyleClass().add("browser-progress");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);

        return new VBox(bar, progress);
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private BrowserTab openTab(String url) {
        BrowserTab tab = new BrowserTab();
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
        if (url == null) {
            tab.loadStartPage();
        } else {
            tab.engine.load(url);
        }
        return tab;
    }

    private void navigate(String input) {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        String typed = input == null ? "" : input.trim();
        if (typed.isEmpty()) {
            return;
        }
        if (StartPage.ADDRESS.equals(typed)) {
            tab.loadStartPage();
            return;
        }
        tab.engine.load(resolve(typed));
    }

    /**
     * 주소창에 친 것을 실제 URL 로 바꾼다.
     *
     * @param typed 사용자가 친 문자열 (공백 제거된 상태)
     * @return 이동할 URL
     */
    private static String resolve(String typed) {
        if (SCHEME.matcher(typed).matches()) {
            return typed;
        }
        if (typed.startsWith("localhost") || HOSTLIKE.matcher(typed).matches()) {
            return "https://" + typed;
        }
        return SEARCH + URLEncoder.encode(typed, StandardCharsets.UTF_8);
    }

    private void go(int offset) {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        WebHistory history = tab.engine.getHistory();
        int target = history.getCurrentIndex() + offset;
        if (target >= 0 && target < history.getEntries().size()) {
            history.go(offset);
        }
    }

    private void reloadOrStop() {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        if (tab.engine.getLoadWorker().getState() == Worker.State.RUNNING) {
            tab.engine.getLoadWorker().cancel();
        } else {
            tab.engine.reload();
        }
    }

    private BrowserTab current() {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        return (selected instanceof BrowserTab tab) ? tab : null;
    }

    /** 선택된 탭의 상태를 도구 모음에 반영한다. 탭이 스스로 자기 변화를 알려 올 때 호출된다. */
    private void syncToolbar() {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        String location = tab.displayAddress();

        // 사용자가 주소창에 무언가 치고 있는 중이면 밑에서 글자를 바꿔치기하지 않는다.
        if (!address.isFocused()) {
            address.setText(location);
        }
        boolean secure = location.startsWith("https://") || location.startsWith(StartPage.ADDRESS);
        lockGlyph.setVisible(secure);
        lockGlyph.setManaged(secure);

        WebHistory history = tab.engine.getHistory();
        backButton.setDisable(history.getCurrentIndex() <= 0);
        forwardButton.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1);

        boolean loading = tab.engine.getLoadWorker().getState() == Worker.State.RUNNING;
        progress.setVisible(loading);
        progress.setManaged(loading);
        progress.setProgress(tab.engine.getLoadWorker().getProgress());
    }

    void dispose() {
        // WebView 는 창이 사라지면 함께 GC 대상이 된다. 다만 로딩 중인 요청은
        // 명시적으로 끊어 준다 — 닫은 페이지가 네트워크를 계속 쓰고 있을 이유가 없다.
        for (Tab tab : tabs.getTabs()) {
            if (tab instanceof BrowserTab browserTab) {
                browserTab.engine.getLoadWorker().cancel();
                browserTab.engine.load(null);
            }
        }
    }

    // ────────────────────────────── 탭 ──────────────────────────────

    /**
     * 탭 하나 — {@link WebView} 와 그 엔진.
     *
     * <p>{@code final} 로 두는 것이 중요하다. 생성자가 자기 자신의 메서드를
     * 부르는데({@code setContent} 등) 상속 가능한 클래스에서 그러면
     * {@code -Xlint:this-escape} 가 경고를 낸다. 이 프로젝트의 기준선은 경고 0건이다.</p>
     */
    private final class BrowserTab extends Tab {

        private final WebView web = new WebView();
        private final WebEngine engine = web.getEngine();

        BrowserTab() {
            getStyleClass().add("browser-page");
            setContent(web);
            setText("새 탭");

            engine.titleProperty().addListener((obs, old, title) -> setText(tabLabel(title)));
            engine.locationProperty().addListener((obs, old, now) -> {
                setText(tabLabel(engine.getTitle()));
                notifyChanged();
            });
            engine.getLoadWorker().stateProperty().addListener((obs, old, now) -> notifyChanged());
            engine.getLoadWorker().progressProperty().addListener((obs, old, now) -> notifyChanged());
            engine.getHistory().getEntries().addListener(
                    (ListChangeListener<WebHistory.Entry>) change -> notifyChanged());

            // target="_blank" 나 window.open 은 새 창이 아니라 새 탭으로 받는다.
            // 데스크탑 안에서 창을 또 띄우면 우리 창 관리자가 모르는 창이 생긴다.
            engine.setCreatePopupHandler(features -> openTab(null).engine);
        }

        private void loadStartPage() {
            engine.loadContent(StartPage.html());
            setText("새 탭");
            notifyChanged();
        }

        /** 주소창에 보여 줄 문자열. 내장 시작 페이지는 실제 URL 이 없다. */
        private String displayAddress() {
            String location = engine.getLocation();
            return (location == null || location.isBlank() || "about:blank".equals(location))
                    ? StartPage.ADDRESS
                    : location;
        }

        private String tabLabel(String title) {
            if (title != null && !title.isBlank()) {
                return title.length() > 24 ? title.substring(0, 23) + "…" : title;
            }
            String location = displayAddress();
            return StartPage.ADDRESS.equals(location) ? "새 탭" : location;
        }

        private void notifyChanged() {
            if (tabs.getSelectionModel().getSelectedItem() == this) {
                syncToolbar();
            }
        }
    }
}
