package forgeos.desktop;

import forgeframework.kernel.UptimeDto;
import forgeframework.memory.MemorySnapshot;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.ThemeManager;
import forgeos.wm.ForgeWindow;
import forgeos.wm.WindowManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 화면 상단의 메뉴바.
 *
 * <p>왼쪽은 "지금 무엇을 쓰고 있는가"(Forge 마크 + 활성 창 이름), 오른쪽은
 * "지금 커널이 어떤 상태인가"(프로세스 수, 메모리, 스왑, 가동 시간, 시계)다.
 * 어느 앱을 쓰고 있든 커널 상태가 항상 한 줄로 보이는 것이 이 시뮬레이터의
 * 핵심 가치라고 보고 자리를 내줬다.</p>
 */
final class MenuBarView extends HBox {

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E) a h:mm");

    private final KernelService kernelService;

    private final Label activeAppLabel = new Label("ForgeOS");
    private final Label processChip = chip("프로세스 —");
    private final Label memoryChip = chip("메모리 —");
    private final Label swapChip = chip("스왑 —");
    private final Label uptimeChip = chip("가동 —");
    private final Label clockLabel = new Label();

    private final Button themeButton = new Button();

    private final Timeline clock = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> tickClock()));

    MenuBarView(KernelService kernelService, WindowManager windowManager,
                ThemeManager theme, Runnable onQuit) {
        this.kernelService = kernelService;
        getStyleClass().add("menu-bar");

        activeAppLabel.getStyleClass().add("menu-active-app");
        clockLabel.getStyleClass().add("menu-clock");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                Glyphs.stroked(Glyphs.FORGE, 15, "menu-logo"),
                activeAppLabel,
                forgeMenu(windowManager, onQuit),
                spacer,
                processChip, memoryChip, swapChip, uptimeChip, themeToggle(theme), clockLabel);

        windowManager.activeWindowProperty().addListener((obs, old, now) ->
                activeAppLabel.setText(now == null ? "ForgeOS" : now.title()));

        tickClock();
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        kernelService.onRefresh(this, this::refreshStats);
    }

    /**
     * 테마 토글.
     *
     * <p>아이콘은 <b>지금 상태</b>가 아니라 <b>누르면 갈 곳</b>을 보여 준다.
     * 다크 테마에서 해를 띄워 두면 "누르면 밝아지겠구나"로 읽힌다. 반대로 하면
     * 매번 한 번 더 생각하게 된다.</p>
     */
    private Button themeToggle(ThemeManager theme) {
        themeButton.getStyleClass().add("menu-icon-button");
        themeButton.setOnAction(e -> theme.toggle());

        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(400));
        Tooltip.install(themeButton, tooltip);

        Runnable sync = () -> {
            boolean dark = theme.mode() == ThemeManager.Mode.DARK;
            themeButton.setGraphic(Glyphs.stroked(
                    dark ? Glyphs.SUN : Glyphs.MOON, 15, "menu-icon-glyph"));
            tooltip.setText(dark ? "라이트 테마로 전환" : "다크 테마로 전환");
        };
        sync.run();
        theme.modeProperty().addListener((obs, old, now) -> sync.run());

        return themeButton;
    }

    private MenuButton forgeMenu(WindowManager windowManager, Runnable onQuit) {
        MenuItem closeWindow = new MenuItem("창 닫기");
        closeWindow.setOnAction(e -> windowManager.closeActive());

        MenuItem minimizeWindow = new MenuItem("창 최소화");
        minimizeWindow.setOnAction(e -> windowManager.minimizeActive());

        MenuItem zoomWindow = new MenuItem("전체 화면 전환");
        zoomWindow.setOnAction(e -> {
            ForgeWindow active = windowManager.activeWindowProperty().get();
            if (active != null) {
                active.toggleZoom();
            }
        });

        MenuItem shutdown = new MenuItem("커널 종료");
        shutdown.setOnAction(e -> kernelService.call(SystemCallType.SHUTDOWN));

        MenuItem quit = new MenuItem("ForgeOS 종료");
        quit.setOnAction(e -> onQuit.run());

        MenuButton menu = new MenuButton("창");
        menu.getStyleClass().add("menu-button");
        menu.getItems().addAll(
                closeWindow, minimizeWindow, zoomWindow,
                new SeparatorMenuItem(), shutdown, quit);
        return menu;
    }

    private void tickClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    /**
     * 메뉴바의 커널 상태 칩을 갱신한다.
     *
     * <p>1.1.1 부터 {@code callCached} 로 묻는다. 같은 펄스에 앱 프로세스 표와 활성
     * 상태 보기도 {@code PS}·{@code MEMINFO} 를 필요로 하는데, 셋이 각자 부르면
     * 커널이 같은 일을 세 번 한다. 게다가 서로 다른 순간의 답을 받으므로 메뉴바가
     * "프로세스 5"라고 하는데 표에는 여섯 줄이 있는 어긋남도 생겼다.</p>
     */
    private void refreshStats() {
        SystemCallResult ps = kernelService.callCached(SystemCallType.PS);
        if (ps.isSuccess()) {
            List<ProcessDto> processes = ps.dataAsList(ProcessDto.class);
            long alive = processes.stream()
                    .filter(p -> p.state() != ProcessState.TERMINATED)
                    .count();
            processChip.setText("프로세스 " + alive);
        }

        SystemCallResult mem = kernelService.callCached(SystemCallType.MEMINFO);
        if (mem.isSuccess()) {
            MemorySnapshot snapshot = mem.dataAs(MemorySnapshot.class);
            int total = snapshot.totalFrames();
            double ratio = total == 0 ? 0 : (double) snapshot.usedFrames() / total;
            memoryChip.setText("메모리 %.0f%%".formatted(ratio * 100));

            // 스왑이 꺼진 커널(swapSlots=0)에서는 칩 자체를 감춘다. 항상 "0"인 숫자는
            // 자리만 먹고 아무것도 알려 주지 않는다.
            boolean swapOn = snapshot.swapTotalSlots() > 0;
            swapChip.setVisible(swapOn);
            swapChip.setManaged(swapOn);
            if (swapOn) {
                swapChip.setText("스왑 %d/%d · 폴트 %d".formatted(
                        snapshot.swapUsedSlots(), snapshot.swapTotalSlots(), snapshot.pageFaults()));
            }
        }

        SystemCallResult uptime = kernelService.callCached(SystemCallType.UPTIME);
        if (uptime.isSuccess()) {
            UptimeDto dto = uptime.dataAs(UptimeDto.class);
            uptimeChip.setText("가동 " + formatUptime(dto.uptimeSeconds()));
        }
    }

    private static String formatUptime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return "%d시간 %d분".formatted(hours, minutes % 60);
        }
        if (minutes > 0) {
            return "%d분 %d초".formatted(minutes, seconds % 60);
        }
        return seconds + "초";
    }

    private static Label chip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("menu-chip");
        return label;
    }
}
