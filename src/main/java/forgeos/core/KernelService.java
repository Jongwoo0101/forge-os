
package forgeos.core;

import forgeframework.api.ForgeConfig;
import forgeframework.api.ForgeFramework;
import forgeframework.kernel.Kernel;
import forgeframework.logger.EventLogger;
import forgeframework.logger.LogEntry;
import forgeframework.logger.LogLevel;
import forgeframework.syscall.SystemCallRequest;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 커널과 JavaFX UI 사이의 유일한 통로.
 *
 * <h2>스레드 경계</h2>
 * <p>이 클래스가 존재하는 첫 번째 이유는 스레드 경계를 한 곳에 가두기 위해서다.
 * 커널의 로그는 부팅 스레드·타이머 장치 스레드·프린터 스풀러 스레드 등
 * <b>여러 백그라운드 스레드</b>에서 올라온다. 그 값을 그대로 {@code ObservableList}에
 * 넣으면 JavaFX가 렌더 도중 리스트를 밟고 죽는다. 그래서 로그 유입 지점은
 * {@link #install()} 한 곳뿐이고, 거기서 전부 {@link Platform#runLater}로 넘긴다.</p>
 *
 * <p>반대로 시스템 콜({@link #call})은 FX 스레드에서 바로 호출한다. 커널의 시스템
 * 콜은 전부 인메모리 연산이라 프레임을 잡아먹지 않으며, 오히려 백그라운드로
 * 빼면 결과를 다시 FX로 넘기는 왕복이 생겨 UI가 한 박자 늦게 반응한다.
 * 유일한 예외가 부팅이다 — 부팅은 단계마다 의도적으로 잠들기 때문에
 * ({@link ForgeConfig#bootStageDelayMillis()}) 반드시 백그라운드로 돌린다.</p>
 *
 * <h2>새로고침 펄스</h2>
 * <p>앱마다 각자 {@code Timeline}을 돌리면 창을 네 개 열었을 때 초당 네 번의
 * 서로 어긋난 갱신이 일어난다. 갱신 주기는 여기 하나로 모으고
 * ({@link #onRefresh}) 앱은 구독만 한다.</p>
 */
public final class KernelService {

    /** UI 갱신 주기. 커널의 기본 타이머 tick(1초)과 맞춘다 — 더 빨라야 할 이유가 없다. */
    private static final Duration REFRESH_PERIOD = Duration.millis(1000);

    /** 로그 보관 상한. 무한히 쌓으면 몇 시간짜리 세션에서 힙을 먹는다. */
    private static final int LOG_CAPACITY = 800;

    private final EventLogger logger = new EventLogger();
    private final ObservableList<LogEntry> logs = FXCollections.observableArrayList();
    private final List<Runnable> refreshListeners = new ArrayList<>();
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);

    private final Timeline pulse = new Timeline(new KeyFrame(REFRESH_PERIOD, e -> firePulse()));

    private volatile Kernel kernel;
    private volatile Consumer<LogEntry> bootLogSink;

    /** 아직 부팅하지 않은 서비스를 만든다. */
    public KernelService() {
        pulse.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * 커널을 백그라운드 스레드에서 부팅한다. 즉시 반환한다.
     *
     * @param config    부팅에 사용할 설정
     * @param onLog     부팅 중 발생하는 커널 로그를 받을 콜백. <b>FX 스레드에서</b> 호출된다
     * @param onReady   부팅 완료 콜백. <b>FX 스레드에서</b> 호출된다
     * @param onFailure 부팅 실패 콜백. <b>FX 스레드에서</b> 호출된다
     */
    public void bootAsync(ForgeConfig config,
                          Consumer<LogEntry> onLog,
                          Runnable onReady,
                          Consumer<Throwable> onFailure) {
        Objects.requireNonNull(config, "config");
        this.bootLogSink = onLog;
        install();

        Thread bootThread = new Thread(() -> {
            try {
                Kernel booted = ForgeFramework.boot(config, logger);
                Platform.runLater(() -> {
                    this.kernel = booted;
                    running.set(true);
                    pulse.play();
                    onReady.run();
                });
            } catch (RuntimeException | Error e) {
                Platform.runLater(() -> onFailure.accept(e));
            }
        }, "forgeos-boot");

        // 데몬으로 두어야 부팅 도중 창을 닫아도 JVM이 남지 않는다.
        bootThread.setDaemon(true);
        bootThread.start();
    }

    /**
     * 커널 로그를 UI로 끌어오는 리스너를 건다.
     *
     * <p>여기가 백그라운드 → FX 스레드 경계의 유일한 지점이다.</p>
     */
    private void install() {
        logger.addListener(entry -> Platform.runLater(() -> {
            if (logs.size() >= LOG_CAPACITY) {
                logs.remove(0, logs.size() - LOG_CAPACITY + 1);
            }
            logs.add(entry);

            Consumer<LogEntry> sink = bootLogSink;
            if (sink != null) {
                sink.accept(entry);
            }
        }));
    }

    /** 부팅 화면이 끝난 뒤, 로그를 부팅 콘솔로 흘려보내던 통로를 끊는다. */
    public void detachBootLogSink() {
        this.bootLogSink = null;
    }

    /**
     * 커널에 직접 로그 한 줄을 남긴다. 부팅 시퀀스가 자기 진행 상황을 알릴 때 쓴다.
     *
     * @param level   로그 수준
     * @param message 메시지
     */
    public void log(LogLevel level, String message) {
        logger.log(level, message);
    }

    /**
     * 시스템 콜을 실행한다. <b>FX 스레드에서만</b> 호출해야 한다.
     *
     * @param type 시스템 콜 종류
     * @param args 인자
     * @return 커널의 응답. 커널이 아직 없거나 이미 내려갔으면 실패 결과
     */
    public SystemCallResult call(SystemCallType type, String... args) {
        Kernel current = kernel;
        if (current == null) {
            return SystemCallResult.failure("커널이 아직 부팅되지 않았습니다.");
        }
        SystemCallResult result = current.handleSystemCall(new SystemCallRequest(type, args));

        // shutdown 처럼 커널을 내리는 명령이 있으므로 매 호출 뒤 상태를 되비춘다.
        if (running.get() && !current.isRunning()) {
            running.set(false);
            pulse.stop();
        }
        return result;
    }

    /**
     * 커널 가동 여부를 다시 확인해 프로퍼티에 반영한다.
     *
     * <p>Terminal 앱은 명령어 계층({@code CommandRegistry})을 통해 커널을 직접
     * 호출하므로 {@link #call}을 거치지 않는다. 그 경로로 {@code shutdown}이
     * 실행되면 데스크탑은 커널이 내려간 사실을 모른다. 그래서 직접 호출하는
     * 쪽이 끝나고 이 메서드를 불러 준다.</p>
     */
    public void syncRunningState() {
        Kernel current = kernel;
        boolean alive = current != null && current.isRunning();
        if (running.get() != alive) {
            running.set(alive);
            if (!alive) {
                pulse.stop();
            }
        }
    }

    /**
     * 부팅이 끝난 커널을 반환한다.
     *
     * @return 커널. 부팅 전이면 {@code null}
     */
    public Kernel kernel() {
        return kernel;
    }

    /**
     * 커널 가동 여부.
     *
     * @return 부팅 완료 후 {@code shutdown} 전이면 참인 프로퍼티
     */
    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    /**
     * 커널이 남긴 로그 전체.
     *
     * @return FX 스레드에서만 읽어야 하는 관측 가능 리스트
     */
    public ObservableList<LogEntry> logs() {
        return logs;
    }

    /**
     * 주기적 갱신 구독을 건다.
     *
     * <p>구독 해지는 호출자가 신경 쓰지 않아도 되도록 {@link Subscription}으로
     * 돌려준다. 창이 닫힐 때 해지하지 않으면 죽은 UI를 계속 갱신하게 된다.</p>
     *
     * @param listener 매 주기 FX 스레드에서 실행될 작업
     * @return 해지 핸들
     */
    public Subscription onRefresh(Runnable listener) {
        refreshListeners.add(listener);
        return () -> refreshListeners.remove(listener);
    }

    private void firePulse() {
        // 콜백 안에서 구독을 해지하는 경우가 있으므로 복사본을 순회한다.
        for (Runnable listener : List.copyOf(refreshListeners)) {
            listener.run();
        }
    }

    /** 커널을 내리고 펄스를 멈춘다. 애플리케이션 종료 시 호출한다. */
    public void shutdown() {
        pulse.stop();
        Kernel current = kernel;
        if (current != null) {
            current.close();
        }
        running.set(false);
    }

    /** {@link KernelService#onRefresh} 구독 해지 핸들. */
    @FunctionalInterface
    public interface Subscription {
        /** 구독을 해지한다. 여러 번 호출해도 안전하다. */
        void cancel();
    }
}
