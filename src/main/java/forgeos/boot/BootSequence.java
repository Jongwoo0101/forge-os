package forgeos.boot;

import forgeframework.api.ForgeConfig;
import forgeframework.api.ForgeFramework;
import forgeframework.logger.LogEntry;
import forgeframework.logger.LogLevel;
import forgeos.core.KernelService;
import forgeos.ui.Motion;
import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 시네마틱 부팅 시퀀스 — 터미널 로그 → MP4 애니메이션 → 데스크탑.
 *
 * <h2>타이밍을 무엇에 맞추는가</h2>
 * <p>세 단계를 고정된 초로 이어 붙이면 반드시 어긋난다. 커널 부팅은 기계마다
 * 걸리는 시간이 다르고, 타이핑은 로그 길이에 따라 달라지기 때문이다.
 * 그래서 이 클래스는 <b>시간이 아니라 사건</b>으로 넘어간다.</p>
 * <ol>
 *   <li>1 → 2: 커널 부팅 완료 <b>그리고</b> 타이핑 큐 소진. 둘 다 만족해야 넘어간다.</li>
 *   <li>2 → 3: {@code MediaPlayer}의 재생 종료 이벤트(또는 실패).</li>
 * </ol>
 * <p>단 하나 시간으로 강제하는 것이 {@link #MIN_CONSOLE_MILLIS}다. 빠른 기계에서
 * 부팅 로그가 0.3초 만에 끝나면 화면이 깜빡인 것처럼 보인다. 최소한 이만큼은
 * 머문다.</p>
 *
 * <h2>건너뛰기</h2>
 * <p>ESC 또는 클릭으로 언제든 데스크탑으로 넘어갈 수 있다. 개발 중에 부팅
 * 영상을 200번 보는 것은 누구에게도 도움이 되지 않는다. 다만 커널 부팅 자체는
 * 건너뛸 수 없으므로, 부팅이 끝나기 전에 건너뛰면 완료를 기다렸다가 전환한다.</p>
 */
public final class BootSequence extends StackPane {

    /** 부팅 애니메이션 리소스 경로. */
    private static final String VIDEO_RESOURCE = "/assets/forgeOS-Booting-Animation2.mp4";

    /** 부팅 콘솔이 화면에 머무는 최소 시간(ms). */
    private static final long MIN_CONSOLE_MILLIS = 1200;

    /**
     * 부팅 단계 사이의 지연(ms).
     *
     * <p>커널 기본값(150ms)보다 길게 잡았다. 커널 입장에서는 의미 없는 대기지만
     * 부팅 화면 입장에서는 이것이 곧 리듬이다 — 로그가 한 줄씩 "찍히는" 것처럼
     * 보이려면 줄과 줄 사이에 사람이 인지할 만한 간격이 있어야 한다.</p>
     */
    private static final long BOOT_STAGE_DELAY_MILLIS = 260;

    private final KernelService kernelService;
    private final Runnable onDesktopReady;

    private final BootConsole console = new BootConsole();
    private final BootVideo video = new BootVideo();

    private long consoleShownAt;
    private boolean kernelReady;
    private boolean consoleDrained;
    private boolean advanced;
    private boolean finished;
    private boolean skipRequested;

    /**
     * 부팅 시퀀스를 만든다. 생성만으로는 아무 일도 일어나지 않는다 —
     * {@link #start()}를 호출해야 커널이 뜬다.
     *
     * @param kernelService  부팅시킬 커널 서비스
     * @param onDesktopReady 3단계 진입 콜백 (FX 스레드)
     */
    public BootSequence(KernelService kernelService, Runnable onDesktopReady) {
        this.kernelService = kernelService;
        this.onDesktopReady = onDesktopReady;

        getStyleClass().add("boot-root");
        video.setVisible(false);
        video.setOpacity(0);
        getChildren().addAll(video, console);

        setOnMouseClicked(e -> skip());
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE) {
                skip();
            }
        });
    }

    /** 1단계를 시작하고, 백그라운드에서 커널을 부팅한다. */
    public void start() {
        consoleShownAt = System.currentTimeMillis();
        printBanner();

        ForgeConfig config = ForgeConfig.defaults()
                .withBootStageDelayMillis(BOOT_STAGE_DELAY_MILLIS);

        kernelService.bootAsync(
                config,
                this::onKernelLog,
                this::onKernelReady,
                this::onKernelFailure);

        requestFocus();
    }

    private void printBanner() {
        console.println("=================================================");
        console.println(" " + ForgeFramework.name() + " v" + ForgeFramework.version());
        console.println(" Operating System Kernel Architecture Engine");
        console.println("=================================================");
    }

    /** 커널이 남기는 로그를 그대로 화면에 흘린다. 부팅 화면은 연출이 아니라 실제 로그다. */
    private void onKernelLog(LogEntry entry) {
        if (entry.getLevel() == LogLevel.DEBUG) {
            return;
        }
        console.println(entry.toFormattedString());
    }

    private void onKernelReady() {
        kernelReady = true;
        kernelService.log(LogLevel.INFO, "ForgeOS 데스크탑 환경 준비 완료");

        long elapsed = System.currentTimeMillis() - consoleShownAt;
        long remaining = Math.max(0, MIN_CONSOLE_MILLIS - elapsed);

        // 최소 체류 시간을 채운 뒤에야 "큐가 비면 넘어간다"를 예약한다.
        if (remaining == 0) {
            armConsoleDrain();
        } else {
            PauseTransition wait = new PauseTransition(Duration.millis(remaining));
            wait.setOnFinished(e -> armConsoleDrain());
            wait.play();
        }
    }

    private void armConsoleDrain() {
        if (skipRequested) {
            console.flush();
        }
        console.whenDrained(() -> {
            consoleDrained = true;
            advanceToVideo();
        });
    }

    private void onKernelFailure(Throwable error) {
        console.println("[FATAL] 커널 부팅에 실패했습니다: " + error.getMessage());
        console.println("[FATAL] ForgeOS를 시작할 수 없습니다.");
    }

    private void advanceToVideo() {
        if (advanced || !kernelReady || !consoleDrained) {
            return;
        }
        advanced = true;
        kernelService.detachBootLogSink();

        if (skipRequested) {
            finish();
            return;
        }

        Motion.fadeOut(console, Motion.FADE_SCENE, () -> {
            video.setOpacity(0);
            video.play(VIDEO_RESOURCE, this::finish);
            Motion.fadeIn(video, Motion.FADE, null);
        });
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        video.dispose();
        onDesktopReady.run();
    }

    /**
     * 남은 연출을 건너뛴다. 커널 부팅이 아직이면 완료 직후 곧바로 데스크탑으로 간다.
     */
    public void skip() {
        if (skipRequested) {
            return;
        }
        skipRequested = true;

        if (!kernelReady) {
            // 커널 부팅은 건너뛸 수 없다. onKernelReady 에서 skipRequested 를 보고 바로 넘어간다.
            return;
        }
        if (advanced) {
            video.dispose();
            finish();
            return;
        }
        console.flush();
    }
}
