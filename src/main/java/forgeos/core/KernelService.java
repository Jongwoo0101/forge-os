
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
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
 *
 * <h2>1.1.1 — 같은 질문을 같은 펄스에 여러 번 하지 않는다</h2>
 * <p>1.1.0 에서 메뉴바·앱 프로세스 표·활성 상태 보기는 <b>각자</b> {@code PS} 를
 * 불렀다. 셋이 같은 펄스 안에서 같은 질문을 세 번 한 셈이고, 커널은 그때마다
 * 프로세스 표 전체를 잠그고 DTO 목록을 새로 만들었다. 게다가 서로 <b>다른 순간</b>의
 * 답을 받으므로, 메뉴바의 "프로세스 5"와 표의 행 개수가 어긋나 보이는 일도 있었다.</p>
 *
 * <p>{@link #callCached}는 읽기 전용 시스템 콜의 결과를 <b>한 펄스 동안만</b> 붙들어
 * 둔다. 상태를 바꾸는 {@link #call}이 한 번이라도 지나가면 즉시 버린다. 비용이 줄고,
 * 덤으로 한 펄스 안의 모든 화면이 같은 순간을 보게 된다.</p>
 *
 * <h2>보이지 않는 창은 갱신하지 않는다</h2>
 * <p>{@link #onRefresh(Node, Runnable)}로 구독하면, 그 노드가 화면에 보이지 않는 동안
 * ({@code Dock}으로 최소화됐거나 아직 붙지 않았을 때) 갱신을 통째로 건너뛴다.
 * 최소화한 창이 계속 표를 다시 그리고 있을 이유가 없다.</p>
 */
public final class KernelService {

    /** UI 갱신 주기. 커널의 기본 타이머 tick(1초)과 맞춘다 — 더 빨라야 할 이유가 없다. */
    private static final Duration REFRESH_PERIOD = Duration.millis(1000);

    /** 로그 보관 상한. 무한히 쌓으면 몇 시간짜리 세션에서 힙을 먹는다. */
    private static final int LOG_CAPACITY = 800;

    private final EventLogger logger = new EventLogger();
    private final ObservableList<LogEntry> logs = FXCollections.observableArrayList();

    /**
     * 갱신 구독자.
     *
     * <p>콜백 안에서 구독을 해지하는 경우가 있어 1.1.0 은 펄스마다 {@code List.copyOf}
     * 로 복사본을 떴다. 읽기는 초당 한 번, 쓰기는 창을 여닫을 때뿐인 전형적인 COW
     * 대상이라 자료구조를 바꾸는 편이 맞다 — 펄스마다 생기던 쓰레기가 사라진다.</p>
     */
    private final List<Runnable> refreshListeners = new CopyOnWriteArrayList<>();

    /** 이번 펄스 동안 유효한 읽기 전용 시스템 콜의 답. {@link #callCached} 참고. */
    private final Map<String, SystemCallResult> pulseCache = new HashMap<>();

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
        // 커널에게 DEBUG 를 아예 만들지 말라고 일러 둔다. ForgeOS 에서 DEBUG 를
        // 보여 주는 화면은 하나도 없는데, 1.1.0 은 시스템 콜 하나마다 DEBUG 한 줄을
        // 만들어 문자열을 잇고 Platform.runLater 로 FX 스레드에 실어 보낸 뒤
        // 아무도 읽지 않는 리스트에 넣고 있었다. 초당 여남은 번씩.
        logger.setLevelEnabled(LogLevel.DEBUG, false);

        logger.addListener(entry -> {
            // 커널을 직접 만든 클라이언트가 DEBUG 를 켜 두었더라도 여기서 막는다.
            if (entry.getLevel() == LogLevel.DEBUG) {
                return;
            }
            Platform.runLater(() -> {
                if (logs.size() >= LOG_CAPACITY) {
                    logs.remove(0, logs.size() - LOG_CAPACITY + 1);
                }
                logs.add(entry);

                Consumer<LogEntry> sink = bootLogSink;
                if (sink != null) {
                    sink.accept(entry);
                }
            });
        });
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
        // 상태를 바꿀 수 있는 호출이 지나갔다. 이번 펄스에 받아 둔 답은 전부 낡았다.
        pulseCache.clear();
        return invoke(type, args);
    }

    /**
     * 읽기 전용 시스템 콜을 실행하되, 같은 펄스 안에서는 답을 재사용한다.
     *
     * <p>메뉴바·앱 프로세스 표·활성 상태 보기가 모두 {@code PS}를 필요로 한다.
     * 셋이 각자 부르면 커널은 한 펄스에 프로세스 표를 세 번 잠그고 DTO 목록을 세 번
     * 만든다. 답이 같을 것이 뻔한 질문이므로 한 번만 묻는다.</p>
     *
     * <p>캐시는 {@link #call}이 한 번이라도 지나가면 즉시 비워지고, 매 펄스 시작에도
     * 비워진다. 그래서 "낡은 값을 계속 보여 주는" 일은 생기지 않는다. 읽기 전용이
     * 아닌 종류를 넘기면 캐시하지 않고 그대로 실행한다 — 호출자가 실수해도 커널
     * 상태가 어긋나지는 않게.</p>
     *
     * @param type 시스템 콜 종류 (읽기 전용이어야 캐시된다)
     * @param args 인자
     * @return 커널의 응답
     */
    public SystemCallResult callCached(SystemCallType type, String... args) {
        if (!isCacheable(type, args)) {
            return call(type, args);
        }
        String key = cacheKey(type, args);
        SystemCallResult hit = pulseCache.get(key);
        if (hit != null) {
            return hit;
        }
        SystemCallResult result = invoke(type, args);
        pulseCache.put(key, result);
        return result;
    }

    private SystemCallResult invoke(SystemCallType type, String... args) {
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
     * 이 호출이 커널 상태를 바꾸지 않는가.
     *
     * <p>{@code SCHEDULER}·{@code SWAPINFO}는 인자가 없을 때만 조회다. 인자가 붙으면
     * 스케줄러를 갈아 끼우거나 교체 정책을 바꾸는 명령이 되므로 캐시 대상이 아니다.</p>
     */
    private static boolean isCacheable(SystemCallType type, String[] args) {
        return switch (type) {
            case PS, MEMINFO, UPTIME, FRAMETABLE, PAGETABLE, RES_INFO, DETECT -> true;
            case SCHEDULER, SWAPINFO -> args.length == 0;
            default -> false;
        };
    }

    private static String cacheKey(SystemCallType type, String[] args) {
        if (args.length == 0) {
            return type.name();
        }
        StringBuilder key = new StringBuilder(type.name());
        for (String arg : args) {
            key.append('\0').append(arg);
        }
        return key.toString();
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

    /**
     * 화면에 보일 때만 도는 주기적 갱신 구독을 건다.
     *
     * <p>Dock 으로 최소화한 창, 아직 장면에 붙지 않은 화면은 갱신할 이유가 없다.
     * 그런데도 1.1.0 은 최소화한 활성 상태 보기가 매초 표를 다시 채우고 게이지
     * 스프링을 돌렸다 — 사용자에게는 보이지 않는 일이 CPU 만 먹고 있었다.</p>
     *
     * @param scope    이 갱신이 그리는 화면 노드
     * @param listener 매 주기 FX 스레드에서 실행될 작업
     * @return 해지 핸들
     */
    public Subscription onRefresh(Node scope, Runnable listener) {
        Objects.requireNonNull(scope, "scope");
        return onRefresh(() -> {
            if (isShowing(scope)) {
                listener.run();
            }
        });
    }

    /**
     * 노드가 실제로 화면에 그려지고 있는가.
     *
     * <p>{@code Node.isVisible()}은 <b>자기 자신</b>의 플래그일 뿐이라 부모가 숨겨져
     * 있어도 참이다. 창을 최소화하면 숨겨지는 것은 앱 화면이 아니라 그것을 담은
     * 창이므로, 조상을 따라 올라가며 확인해야 한다.</p>
     */
    private static boolean isShowing(Node node) {
        if (node.getScene() == null) {
            return false;
        }
        for (Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private void firePulse() {
        // 펄스가 바뀌면 지난 펄스에 받아 둔 답은 전부 버린다.
        pulseCache.clear();
        // CopyOnWriteArrayList 라 순회 중 해지가 일어나도 안전하다 — 복사본을 뜨지 않는다.
        for (Runnable listener : refreshListeners) {
            listener.run();
        }
        pulseCache.clear();
    }

    /** 커널을 내리고 펄스를 멈춘다. 애플리케이션 종료 시 호출한다. */
    public void shutdown() {
        pulse.stop();
        pulseCache.clear();
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
