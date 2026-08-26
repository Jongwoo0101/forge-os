package forgeos.boot;

import forgeframework.api.ForgeFramework;
import forgeos.ui.ForgeMark;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/**
 * 부팅 2단계 — 로고 스플래시.
 *
 * <h2>1.1.1 — MP4를 걷어내고 벡터로 다시 세웠다</h2>
 * <p>1.1.0의 2단계는 10초짜리 MP4였다. 영상은 세 가지 값을 치렀다. 리소스 2.4MB,
 * {@code javafx.media} 모듈과 그 네이티브 라이브러리, 그리고 <b>전환 순간에 디코더를
 * 세우느라 굳는 화면</b>. 그 대가로 얻은 것은 매번 똑같이 재생되는 10초였다.</p>
 *
 * <p>여기 있는 것은 전부 도형이다. {@link ForgeMark}는 배경화면이 쓰는 것과 <b>같은</b>
 * 벡터 로고이고, 고리는 {@link Arc} 넷이다. 그래서 어느 해상도에서도 깨지지 않고,
 * 준비 시간이 0이며, 길이를 상수 하나로 바꿀 수 있다.</p>
 *
 * <h2>무엇을 보여 주는가</h2>
 * <p>위에서 아래로 <b>후광 고리 → 로고 → 워드마크 → 스피너 → 상태 문구</b>다.
 * 후광은 데스크탑 배경화면의 동심원과 같은 물건이라, 부팅이 끝나고 배경이 드러날 때
 * "같은 세계"로 이어진다.</p>
 *
 * <p>스피너는 로고 아래에서 <b>시계 방향</b>으로 돈다. 고리 하나가 일정한 속도로만
 * 돌면 그것은 시계지 로딩이 아니므로, 호(弧)의 길이가 숨을 쉰다 — 짧아졌다 길어지며
 * 도는 것이 "무언가가 진행 중"으로 읽히는 형태다. 안쪽에 더 빠른 얇은 호를 하나 더
 * 겹쳐 깊이를 준다.</p>
 *
 * <h2>끝맺음</h2>
 * <p>마지막 0.5초에 호가 <b>완전한 원으로 닫히면서</b> 전체가 살짝 커진다. 로딩이
 * 흐지부지 사라지는 대신 "채워졌다"로 끝나야, 다음에 오는 데스크탑이 그 결과로 읽힌다.</p>
 */
final class BootSplash extends StackPane {

    // ── 시간 (초) ──────────────────────────────────────────────

    /** 스플래시 전체 길이. 부팅 연출에서 가장 비싼 자원은 사용자의 인내심이다. */
    private static final double TOTAL = 2.4;

    /** 등장 — 로고가 커지며 밝아지는 구간. */
    private static final double ENTER = 0.55;

    /** 끝맺음 — 호가 원으로 닫히는 구간. */
    private static final double EXIT = 0.5;

    /** 호의 길이가 한 번 숨쉬는 데 걸리는 시간. */
    private static final double BREATH = 1.5;

    // ── 회전 속도 (도/초, 양수가 시계 방향) ────────────────────

    private static final double SPIN_SWEEP = 210;
    private static final double SPIN_INNER = 335;
    private static final double SPIN_HALO_OUTER = 11;
    private static final double SPIN_HALO_INNER = 17;

    // ── 치수 (px) ──────────────────────────────────────────────

    private static final double MARK_SIZE = 132;
    private static final double SPINNER_BOX = 84;
    private static final double SPINNER_RADIUS = 34;
    private static final double INNER_RADIUS = 23;
    private static final double HALO_OUTER = 268;
    private static final double HALO_INNER = 188;

    /** 호가 가장 짧아졌을 때의 각도. */
    private static final double SWEEP_MIN = 34;

    /** 호가 가장 길어졌을 때의 각도. */
    private static final double SWEEP_MAX = 292;

    /**
     * 상태 문구와 그것이 나타나는 시각(초).
     *
     * <p>커널은 1단계에서 이미 다 떴다. 그러니 여기서 "커널 초기화 중"이라고 쓰면
     * 거짓말이다. 이 구간에 실제로 일어나는 일 — 데스크탑을 세우는 일 — 만 적는다.</p>
     */
    private static final String[] STATUS_TEXT = {
        "커널 준비 완료",
        "그래픽 계층 초기화",
        "데스크탑 구성 중",
    };

    private static final double[] STATUS_AT = {0.30, 1.00, 1.60};

    // ── 노드 ───────────────────────────────────────────────────

    private final VBox column = new VBox();
    private final Group halo = new Group();
    private final Circle haloOuter = new Circle(HALO_OUTER);
    private final Circle haloInner = new Circle(HALO_INNER);

    private final Arc sweep = new Arc();
    private final Arc inner = new Arc();
    private final Circle head = new Circle(3.2);

    private final Label wordmark = new Label("F O R G E O S");
    private final Label caption = new Label();
    private final Label status = new Label(STATUS_TEXT[0]);

    // ── 상태 ───────────────────────────────────────────────────

    private double sweepAngle = 90;
    private double innerAngle = -30;
    private double haloOuterAngle;
    private double haloInnerAngle;
    private int statusIndex;

    private long startNanos;
    private long lastNanos;
    private boolean finished;
    private Runnable onFinished;

    private final AnimationTimer clock = new AnimationTimer() {
        @Override
        public void handle(long now) {
            tick(now);
        }
    };

    BootSplash() {
        getStyleClass().add("boot-splash");
        setMinSize(0, 0);

        buildHalo();
        buildColumn();

        getChildren().addAll(halo, column);

        // 스플래시는 클릭을 직접 받지 않는다. 건너뛰기는 BootSequence 가 화면 전체에서 받는다.
        setMouseTransparent(true);
    }

    /**
     * 후광 고리.
     *
     * <p>점선 원을 <b>노드째 회전</b>시킨다. 원 자체는 회전 대칭이라 돌려도 아무 일이
     * 일어나지 않지만, 점선이 있으면 점들이 궤도를 따라 흐른다. 고리를 여러 조각으로
     * 나눠 그리는 것보다 훨씬 싸고, 훨씬 가늘게 보인다.</p>
     *
     * <p><b>레이아웃에 참여시키면 안 된다.</b> 반지름 268px 짜리 원은 그대로 두면
     * 자기 크기가 이 화면의 최소 높이가 되어, 작은 창에서 스플래시가 창보다 커진다
     * ({@code Wallpaper} 가 같은 이유로 같은 처리를 한다).</p>
     */
    private void buildHalo() {
        haloOuter.getStyleClass().add("boot-halo-outer");
        haloOuter.setFill(null);
        haloInner.getStyleClass().add("boot-halo-inner");
        haloInner.setFill(null);

        halo.getChildren().addAll(haloOuter, haloInner);
        halo.setMouseTransparent(true);
    }

    private void buildColumn() {
        caption.setText("ForgeFramework v" + ForgeFramework.version() + " kernel");

        wordmark.getStyleClass().add("boot-splash-wordmark");
        caption.getStyleClass().add("boot-splash-caption");
        status.getStyleClass().add("boot-splash-status");

        column.getStyleClass().add("boot-splash-column");
        column.setAlignment(Pos.CENTER);
        column.setMinSize(0, 0);
        column.getChildren().addAll(
                buildMark(),
                wordmark,
                caption,
                buildSpinner(),
                status);

        // 등장 애니메이션의 시작 상태. 첫 프레임 전에 미리 앉혀 둬야 깜빡이지 않는다.
        column.setOpacity(0);
        column.setScaleX(0.9);
        column.setScaleY(0.9);
    }

    /**
     * 로고를 <b>고정 크기 상자</b>에 담아 돌려준다.
     *
     * <p>{@link ForgeMark}는 {@code Group}이고, 그 안에는 가우시안 흐림과 발광
     * 그림자가 들어 있다. 효과는 노드의 경계를 <b>바깥으로 부풀리므로</b>, 그대로
     * {@code VBox}에 넣으면 로고가 차지하는 세로 높이가 132px 이 아니라 "132px 과
     * 발광이 번진 만큼"이 된다. 그러면 코드에 적힌 숫자와 화면의 간격이 어긋나고,
     * 발광의 세기를 조절할 때마다 워드마크가 따라 움직인다.</p>
     *
     * <p>상자에 크기를 못 박아 두면 레이아웃은 132px 만 본다. JavaFX 는 clip 을
     * 지정하지 않는 한 자식이 부모 경계를 넘어 그려지므로, 발광은 그대로 번진다.</p>
     */
    private StackPane buildMark() {
        StackPane box = new StackPane(new ForgeMark(MARK_SIZE, true));
        box.setMinSize(MARK_SIZE, MARK_SIZE);
        box.setPrefSize(MARK_SIZE, MARK_SIZE);
        box.setMaxSize(MARK_SIZE, MARK_SIZE);
        return box;
    }

    /**
     * 로고 아래의 스피너.
     *
     * <p>JavaFX {@link Arc}의 각도는 수학 관습을 따른다 — 0도가 3시 방향이고 양수가
     * <b>반</b>시계 방향이다. 그래서 시계 방향으로 그리려면 길이가 음수여야 하고,
     * 시계 방향으로 돌리려면 시작 각도가 <b>줄어들어야</b> 한다. 여기서 부호를 한 번
     * 틀리면 고리가 반대로 돈다.</p>
     */
    private Pane buildSpinner() {
        double center = SPINNER_BOX / 2;

        Circle track = new Circle(center, center, SPINNER_RADIUS);
        track.getStyleClass().add("boot-spinner-track");
        track.setFill(null);

        configureArc(sweep, center, SPINNER_RADIUS, "boot-spinner-sweep");
        configureArc(inner, center, INNER_RADIUS, "boot-spinner-inner");
        inner.setLength(-66);

        head.getStyleClass().add("boot-spinner-head");

        Pane pane = new Pane(track, inner, sweep, head);
        pane.setMinSize(SPINNER_BOX, SPINNER_BOX);
        pane.setPrefSize(SPINNER_BOX, SPINNER_BOX);
        pane.setMaxSize(SPINNER_BOX, SPINNER_BOX);
        return pane;
    }

    private static void configureArc(Arc arc, double center, double radius, String styleClass) {
        arc.setCenterX(center);
        arc.setCenterY(center);
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().add(styleClass);
    }

    /**
     * 스플래시를 재생한다.
     *
     * @param completion 끝났을 때 실행할 작업 (FX 스레드, 정확히 한 번)
     */
    void play(Runnable completion) {
        this.onFinished = completion;
        this.startNanos = 0;
        this.lastNanos = 0;
        setVisible(true);
        clock.start();
    }

    /** 남은 연출을 건너뛴다. 완료 콜백은 그대로 한 번 실행된다. */
    void skip() {
        finish();
    }

    /** 타이머를 멈추고 자원을 정리한다. 창 종료에서 호출한다. */
    void dispose() {
        clock.stop();
        onFinished = null;
        finished = true;
    }

    // ────────────────────────────── 프레임 ──────────────────────────────

    private void tick(long now) {
        if (startNanos == 0) {
            startNanos = now;
            lastNanos = now;
            return;
        }
        double elapsed = (now - startNanos) / 1_000_000_000.0;
        // 프레임이 크게 밀려도(첫 레이아웃·GC) 고리가 한 바퀴 튀지 않도록 걸음을 자른다.
        double frame = Math.min((now - lastNanos) / 1_000_000_000.0, 0.05);
        lastNanos = now;

        advanceRotation(frame);
        applyEntrance(elapsed);
        applySweep(elapsed);
        applyStatus(elapsed);

        if (elapsed >= TOTAL) {
            finish();
        }
    }

    /** 각도는 시간이 아니라 <b>지난 프레임 시간</b>으로 민다. 프레임이 밀려도 속도가 같다. */
    private void advanceRotation(double frame) {
        // Arc 는 반시계가 양수이므로 시계 방향은 빼기.
        sweepAngle = wrap(sweepAngle - SPIN_SWEEP * frame);
        innerAngle = wrap(innerAngle - SPIN_INNER * frame);
        // Node.setRotate 는 화면 좌표계라 시계 방향이 양수다. 같은 방향인데 부호가 반대다.
        haloOuterAngle = wrap(haloOuterAngle + SPIN_HALO_OUTER * frame);
        haloInnerAngle = wrap(haloInnerAngle + SPIN_HALO_INNER * frame);

        inner.setStartAngle(innerAngle);
        haloOuter.setRotate(haloOuterAngle);
        haloInner.setRotate(haloInnerAngle);
    }

    private void applyEntrance(double elapsed) {
        double enter = smoothstep(clamp01(elapsed / ENTER));
        double exit = smoothstep(exitProgress(elapsed));

        column.setOpacity(enter);
        // 끝맺음에서 살짝 커진다 — "닫혔다"가 아니라 "채워졌다"로 읽히게.
        double scale = 0.9 + 0.1 * enter + 0.05 * exit;
        column.setScaleX(scale);
        column.setScaleY(scale);

        halo.setOpacity(enter);
        halo.setScaleX(0.94 + 0.06 * enter);
        halo.setScaleY(0.94 + 0.06 * enter);
    }

    private void applySweep(double elapsed) {
        double breath = 0.5 - 0.5 * Math.cos(2 * Math.PI * elapsed / BREATH);
        double length = SWEEP_MIN + (SWEEP_MAX - SWEEP_MIN) * breath;

        // 마지막 구간에서는 숨쉬기를 접고 완전한 원으로 닫는다.
        double exit = smoothstep(exitProgress(elapsed));
        length = length + (360 - length) * exit;

        sweep.setStartAngle(sweepAngle);
        sweep.setLength(-length);

        // 불티는 호의 머리에 붙어 다닌다. 머리가 어디인지 눈으로 알려 주는 유일한 표시다.
        double headAngle = Math.toRadians(sweepAngle - length);
        double center = SPINNER_BOX / 2;
        head.setCenterX(center + SPINNER_RADIUS * Math.cos(headAngle));
        head.setCenterY(center - SPINNER_RADIUS * Math.sin(headAngle));
        // 원이 닫히면 머리와 꼬리가 만나므로 불티는 조용히 사라진다.
        head.setOpacity(1 - exit);
        // 안쪽 호도 함께 물러난다. 끝맺음은 <b>고리 하나</b>가 닫히는 그림이어야
        // 깨끗하다 — 완성된 원 안에서 다른 호가 계속 돌면 아직 진행 중으로 읽힌다.
        inner.setOpacity(1 - exit);
    }

    private void applyStatus(double elapsed) {
        while (statusIndex + 1 < STATUS_TEXT.length && elapsed >= STATUS_AT[statusIndex + 1]) {
            statusIndex++;
            status.setText(STATUS_TEXT[statusIndex]);
        }
    }

    private static double exitProgress(double elapsed) {
        return clamp01((elapsed - (TOTAL - EXIT)) / EXIT);
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        clock.stop();
        Runnable action = onFinished;
        onFinished = null;
        if (action != null) {
            action.run();
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private static double wrap(double degrees) {
        double value = degrees % 360;
        return value < 0 ? value + 360 : value;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** 0에서 1로 부드럽게 — 양 끝의 기울기가 0이라 시작과 끝에 턱이 없다. */
    private static double smoothstep(double value) {
        return value * value * (3 - 2 * value);
    }
}
