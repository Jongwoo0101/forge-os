package forgeos.ui;

import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 라이트/다크 테마 전환기.
 *
 * <h2>왜 스타일시트를 통째로 갈아 끼우는가</h2>
 * <p>루트 노드에 {@code .light} 클래스를 붙여 토큰을 덮어쓰는 방법이 더 간단해
 * 보이지만, 그러면 <b>팝업이 따라오지 않는다</b>. 콘텍스트 메뉴·툴팁·콤보박스
 * 목록은 각자 자기 {@code Scene}을 갖고 그 루트에는 우리가 클래스를 붙일 수 없다.
 * 반면 팝업은 소유 Scene의 스타일시트를 물려받으므로, 토큰 정의 파일 자체를
 * 바꾸면 팝업까지 한 번에 따라온다.</p>
 *
 * <p>그래서 스타일시트를 <b>토큰</b>과 <b>구조</b>로 나눠 두었다.
 * {@code tokens-*.css}는 색만, {@code theme.css}/{@code apps.css}는 색을 직접
 * 쓰지 않고 토큰 이름만 참조한다.</p>
 *
 * <h2>전환은 부드럽게</h2>
 * <p>밝기가 한 프레임에 뒤집히면 눈이 아프다. 전환 순간에만 얇은 막을 덮었다
 * 걷어내서 급격한 명도 변화를 완충한다.</p>
 */
public final class ThemeManager {

    /** 테마 종류. */
    public enum Mode {
        /** 어두운 테마. 기본값이다 — 이 시뮬레이터는 밤에 오래 들여다보는 물건이다. */
        DARK("/forgeos/css/tokens-dark.css"),
        /** 밝은 테마. */
        LIGHT("/forgeos/css/tokens-light.css");

        private final String tokensPath;

        Mode(String tokensPath) {
            this.tokensPath = tokensPath;
        }
    }

    /** 토큰 뒤에 항상 따라붙는 구조 스타일시트. 순서가 곧 우선순위다. */
    private static final List<String> STRUCTURE = List.of(
            "/forgeos/css/theme.css",
            "/forgeos/css/apps.css");

    /** 전환 막이 덮이는 시간. */
    private static final Duration VEIL_IN = Duration.millis(110);

    /** 전환 막이 걷히는 시간. 걷힐 때가 더 느려야 "바뀌었구나"로 읽힌다. */
    private static final Duration VEIL_OUT = Duration.millis(280);

    private final Scene scene;
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.DARK);
    private final Region veil = new Region();

    private boolean switching;

    /**
     * 주어진 Scene에 테마를 적용한다. 생성 시점에 곧바로 다크 테마가 적용된다.
     *
     * @param scene 대상 Scene
     */
    public ThemeManager(Scene scene) {
        this.scene = scene;

        veil.getStyleClass().add("theme-veil");
        veil.setMouseTransparent(true);
        veil.setOpacity(0);
        veil.setVisible(false);

        applyStylesheets();
    }

    /**
     * 현재 테마.
     *
     * @return 테마 프로퍼티
     */
    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    /**
     * 현재 테마 값.
     *
     * @return 다크 또는 라이트
     */
    public Mode mode() {
        return mode.get();
    }

    /** 라이트 ↔ 다크를 뒤집는다. */
    public void toggle() {
        setMode(mode.get() == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    /**
     * 테마를 지정한다. 이미 같은 테마면 아무 일도 하지 않는다.
     *
     * @param target 적용할 테마
     */
    public void setMode(Mode target) {
        if (mode.get() == target || switching) {
            return;
        }
        switching = true;
        withVeil(() -> {
            mode.set(target);
            applyStylesheets();
        });
    }

    private void applyStylesheets() {
        List<String> sheets = new ArrayList<>();
        addIfPresent(sheets, mode.get().tokensPath);
        for (String path : STRUCTURE) {
            addIfPresent(sheets, path);
        }
        scene.getStylesheets().setAll(sheets);
    }

    private static void addIfPresent(List<String> sheets, String path) {
        URL url = ThemeManager.class.getResource(path);
        if (url != null) {
            sheets.add(url.toExternalForm());
        }
    }

    /**
     * 막을 덮고, 스타일을 갈아 끼우고, 막을 걷는다.
     *
     * <p>Scene 루트가 {@link Pane}이 아니면(테스트 등) 막 없이 바로 갈아 끼운다 —
     * 연출 때문에 기능이 막히면 안 된다.</p>
     */
    private void withVeil(Runnable swap) {
        if (!(scene.getRoot() instanceof Pane root)) {
            swap.run();
            switching = false;
            return;
        }

        if (!root.getChildren().contains(veil)) {
            root.getChildren().add(veil);
        }
        veil.toFront();
        veil.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(VEIL_IN, veil);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Motion.EASE_OUT);
        fadeIn.setOnFinished(e -> {
            swap.run();

            FadeTransition fadeOut = new FadeTransition(VEIL_OUT, veil);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setInterpolator(Motion.EASE_OUT);
            fadeOut.setOnFinished(done -> {
                veil.setVisible(false);
                switching = false;
            });
            fadeOut.play();
        });
        fadeIn.play();
    }
}
