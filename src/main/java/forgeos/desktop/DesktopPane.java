package forgeos.desktop;

import forgeos.app.ForgeApp;
import forgeos.core.KernelService;
import forgeos.ui.Motion;
import forgeos.ui.ThemeManager;
import forgeos.wm.WindowManager;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 부팅이 끝난 뒤의 화면 전체 — 바탕화면, 창 레이어, 상단 메뉴바, 하단 Dock.
 *
 * <h2>레이어 순서</h2>
 * <p>아래에서 위로 바탕화면 → 창 → 크롬(메뉴바·Dock) → 종료 오버레이다.
 * 크롬이 창보다 위에 있어야 창을 화면 끝까지 끌어도 메뉴바 아래로 지나가는
 * 것처럼 보인다. 반투명한 크롬 아래로 내용이 흘러가는 것은 가려서 숨기는 게
 * 아니라 <b>깊이</b>를 만드는 장치다.</p>
 */
public final class DesktopPane extends StackPane {

    /** 메뉴바·Dock 과 창 사이에 남기는 여백(px). */
    private static final double CHROME_GAP = 8;

    /** Dock 과 화면 아래 모서리 사이의 여백(px). 0 이면 Dock 이 바닥에 붙어 답답하다. */
    private static final double DOCK_MARGIN = 14;

    private final WindowManager windowManager;
    private final MenuBarView menuBar;
    private final DockView dock;
    private final StackPane shutdownOverlay;

    /**
     * 데스크탑을 만든다.
     *
     * @param kernelService 커널 서비스
     * @param apps          Dock 에 올릴 앱 목록 (순서가 곧 Dock 순서)
     * @param theme         라이트/다크 전환기 (메뉴바의 토글이 사용한다)
     * @param onQuit        ForgeOS 자체를 종료할 때 실행할 작업
     */
    public DesktopPane(KernelService kernelService, List<ForgeApp> apps,
                       ThemeManager theme, Runnable onQuit) {
        getStyleClass().add("desktop");

        Wallpaper wallpaper = new Wallpaper();

        windowManager = new WindowManager(kernelService);
        menuBar = new MenuBarView(kernelService, windowManager, theme, onQuit);
        dock = new DockView(windowManager, apps);
        shutdownOverlay = buildShutdownOverlay(onQuit);

        getChildren().addAll(wallpaper, windowManager, menuBar, dock, shutdownOverlay);

        // StackPane 안의 Region 은 기본적으로 영역을 꽉 채운다. 바탕화면·창 레이어·
        // 오버레이는 그래서 손댈 것이 없지만, 크롬 두 개는 반대로 <b>막아 줘야</b> 한다.
        // 그냥 두면 배경이 있는 메뉴바가 화면 전체를 덮어 아래를 다 가린다.
        // (색·여백이 아니라 레이아웃 제약이므로 CSS 가 아니라 여기 있는 것이 맞다.)
        menuBar.setMaxWidth(Double.MAX_VALUE);
        menuBar.setMaxHeight(Region.USE_PREF_SIZE);
        dock.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // 데스크탑은 창보다 커질 수 없다. 자식 중 하나라도 큰 최소 크기를 요구하면
        // StackPane 은 그 값을 받아들여 자기 최소 크기로 삼고, 그러면 창보다 큰
        // 데스크탑이 가운데 정렬되면서 메뉴바가 위로, Dock 이 아래로 밀려 잘린다.
        // (실제로 배경화면의 동심원 때문에 그렇게 됐다 — Wallpaper 주석 참고.)
        setMinSize(0, 0);

        StackPane.setAlignment(menuBar, Pos.TOP_CENTER);
        StackPane.setAlignment(dock, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dock, new Insets(0, 0, DOCK_MARGIN, 0));

        bindWorkArea();

        kernelService.runningProperty().addListener((obs, was, running) -> {
            if (was && !running) {
                showShutdownOverlay();
            }
        });
    }

    /**
     * 작업 영역은 "창이 놓일 수 있는 곳"이다. 메뉴바와 Dock 이 차지한 만큼을 뺀다.
     *
     * <p>높이가 아직 0인 첫 레이아웃에서도 계산이 돌아가야 하므로 리스너를 붙여
     * 크기가 정해질 때마다 다시 계산한다.</p>
     */
    private void bindWorkArea() {
        Runnable recompute = () -> {
            double top = menuBar.getHeight() + CHROME_GAP;
            double bottom = dock.getHeight() + DOCK_MARGIN + CHROME_GAP;
            double width = Math.max(0, getWidth());
            double height = Math.max(0, getHeight() - top - bottom);
            windowManager.workAreaProperty().set(new BoundingBox(0, top, width, height));
        };

        widthProperty().addListener((obs, old, now) -> recompute.run());
        heightProperty().addListener((obs, old, now) -> recompute.run());
        menuBar.heightProperty().addListener((obs, old, now) -> recompute.run());
        dock.heightProperty().addListener((obs, old, now) -> recompute.run());
    }

    private StackPane buildShutdownOverlay(Runnable onQuit) {
        Label title = new Label("커널이 종료되었습니다");
        title.getStyleClass().add("shutdown-title");

        Label detail = new Label("ForgeFramework 커널이 정상적으로 내려갔습니다.\n"
                + "다시 사용하려면 ForgeOS를 재시작하세요.");
        detail.getStyleClass().add("shutdown-detail");

        Button quit = new Button("ForgeOS 종료");
        quit.getStyleClass().add("shutdown-button");
        quit.setOnAction(e -> onQuit.run());

        VBox column = new VBox(title, detail, quit);
        column.getStyleClass().add("shutdown-column");
        column.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(column);
        overlay.getStyleClass().add("shutdown-overlay");
        overlay.setVisible(false);
        overlay.setOpacity(0);
        return overlay;
    }

    private void showShutdownOverlay() {
        shutdownOverlay.toFront();
        Motion.fadeIn(shutdownOverlay, Duration.millis(420), null);
    }

    /**
     * 창 관리자. 단축키 설치를 위해 애플리케이션이 가져간다.
     *
     * @return 창 관리자
     */
    public WindowManager windowManager() {
        return windowManager;
    }
}
