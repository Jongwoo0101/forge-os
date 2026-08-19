package forgeos;

import forgeos.app.AppCatalog;
import forgeos.boot.BootSequence;
import forgeos.core.KernelService;
import forgeos.desktop.DesktopPane;
import forgeos.ui.Motion;
import forgeos.ui.ThemeManager;
import forgeos.wm.WindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * ForgeOS 애플리케이션.
 *
 * <p>이 클래스가 하는 일은 세 가지뿐이다. 창을 하나 만들고, 부팅 시퀀스를
 * 돌리고, 부팅이 끝나면 데스크탑으로 갈아 끼운다. 나머지는 전부
 * {@link BootSequence}와 {@link DesktopPane}이 가져간다.</p>
 *
 * <h2>장면 전환</h2>
 * <p>{@code Scene}을 바꾸지 않고 같은 루트 안에서 레이어를 교체한다. Scene을
 * 갈아 끼우면 창이 한 프레임 깜빡이고 스타일시트가 다시 적용되면서 화면이
 * 튄다. 시네마틱 부팅의 마지막 순간에 그 깜빡임이 보이면 앞의 연출이 전부
 * 무의미해진다.</p>
 */
public final class ForgeOsApp extends Application {

    /** 창 초기 크기. 데스크탑 환경이므로 좁으면 창을 겹칠 수가 없다. */
    private static final double INITIAL_WIDTH = 1440;

    /** 창 초기 높이. */
    private static final double INITIAL_HEIGHT = 900;

    private final KernelService kernelService = new KernelService();
    private final StackPane root = new StackPane();

    private ThemeManager theme;

    /** JavaFX 런타임이 리플렉션으로 호출한다. */
    public ForgeOsApp() {
    }

    @Override
    public void start(Stage stage) {
        root.getStyleClass().add("forge-root");

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);

        // 스타일시트는 ThemeManager 가 소유한다. 토큰 파일을 갈아 끼우는 것이
        // 곧 테마 전환이므로, 목록을 두 곳에서 건드리면 반드시 어긋난다.
        theme = new ThemeManager(scene);

        BootSequence boot = new BootSequence(kernelService, () -> enterDesktop(scene));
        root.getChildren().add(boot);

        stage.setTitle("ForgeOS");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(e -> kernelService.shutdown());
        stage.show();

        boot.start();
    }

    private void enterDesktop(Scene scene) {
        DesktopPane desktop = new DesktopPane(
                kernelService, AppCatalog.defaults(), theme, this::quit);
        desktop.setOpacity(0);
        root.getChildren().add(desktop);

        Motion.fadeIn(desktop, Motion.FADE_SCENE, () -> {
            // 부팅 레이어는 완전히 덮인 다음에 치운다. 먼저 지우면 검은 화면이 한 번 스친다.
            root.getChildren().removeIf(node -> node instanceof BootSequence);
        });

        installShortcuts(scene, desktop.windowManager());
    }

    /**
     * 창 단축키. macOS의 Command, 그 외 플랫폼의 Ctrl에 모두 대응하도록
     * {@link KeyCombination#SHORTCUT_DOWN}을 쓴다.
     */
    private void installShortcuts(Scene scene, WindowManager windowManager) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
                windowManager::closeActive);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN),
                windowManager::minimizeActive);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN,
                        KeyCombination.SHIFT_DOWN),
                theme::toggle);
    }

    private void quit() {
        kernelService.shutdown();
        Platform.exit();
    }

    @Override
    public void stop() {
        kernelService.shutdown();
    }
}
