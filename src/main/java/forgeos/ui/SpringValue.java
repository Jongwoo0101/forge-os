package forgeos.ui;

import javafx.animation.AnimationTimer;

import java.util.function.DoubleConsumer;

/**
 * 하나의 실수 값을 스프링 물리로 목표값까지 끌고 가는 애니메이터.
 *
 * <h2>왜 Transition 이 아니라 스프링인가</h2>
 * <p>JavaFX의 {@code TranslateTransition} 류는 "지속 시간"이 고정된 스크립트다.
 * 재생 도중 목표가 바뀌면 현재 속도를 버리고 새 애니메이션을 처음부터 시작하므로,
 * 사용자가 움직이는 창을 다시 잡아채는 순간 눈에 보이는 <i>턱</i>이 생긴다.
 * 스프링은 상태(위치·속도)를 계속 들고 있다가 목표만 갈아끼우면 되므로
 * 언제 방향이 바뀌어도 궤적이 이어진다. 이것이 인터페이스가 "살아 있다"고
 * 느껴지는 거의 유일한 이유다.</p>
 *
 * <h2>파라미터</h2>
 * <p>물리 삼종세트(질량·강성·감쇠) 대신 Apple이 쓰는 두 값을 쓴다.</p>
 * <ul>
 *   <li><b>response</b> — 목표에 도달하는 데 걸리는 체감 시간(초). 작을수록 날렵하다.
 *       지속 시간이 아니다 — 스프링에는 정해진 끝이 없다.</li>
 *   <li><b>damping</b> — 감쇠비. {@code 1.0}이면 튕김 없이 부드럽게 멎고,
 *       {@code 1.0} 미만이면 목표를 지나쳤다가 되돌아온다.</li>
 * </ul>
 * <p>기본값은 {@code damping = 1.0}이다. 튕김은 사용자의 제스처가 <b>운동량을
 * 실어 보냈을 때만</b> 어울린다. 그냥 나타난 메뉴가 출렁이면 싸구려로 보인다.</p>
 *
 * <h2>수치 안정성</h2>
 * <p>프레임 간격이 길어지면(창을 끌다 GC가 끼면 30ms도 나온다) 뻣뻣한 스프링은
 * 명시적 오일러 적분에서 발산한다. 그래서 프레임 시간을 잘게 쪼개
 * ({@value #SUB_STEP}초) 여러 번 적분한다.</p>
 */
public final class SpringValue extends AnimationTimer {

    /** 적분 서브스텝(초). 240Hz 상당 — 이보다 크면 뻣뻣한 스프링이 발산한다. */
    private static final double SUB_STEP = 1.0 / 240.0;

    /** 한 프레임에 소화할 최대 시간(초). 탭 전환 등으로 프레임이 크게 밀렸을 때의 폭주 방지. */
    private static final double MAX_FRAME = 0.1;

    /** 정지 판정 임계값(픽셀). 이보다 가까우면서 느리면 목표에 스냅하고 멈춘다. */
    private static final double SETTLE_DISTANCE = 0.25;

    /** 정지 판정 속도(픽셀/초). */
    private static final double SETTLE_VELOCITY = 0.25;

    private final DoubleConsumer sink;

    private double response = Motion.RESPONSE_STANDARD;
    private double damping = Motion.DAMPING_STANDARD;

    private double value;
    private double velocity;
    private double target;

    private long lastNanos;
    private boolean active;
    private Runnable onSettle;

    /**
     * 값이 바뀔 때마다 호출될 소비자를 받아 스프링을 만든다.
     *
     * @param sink 매 프레임 갱신된 값을 받아 실제 노드에 적용하는 함수
     */
    public SpringValue(DoubleConsumer sink) {
        this.sink = sink;
    }

    /**
     * 스프링 특성을 바꾼다.
     *
     * @param newResponse 반응 시간(초)
     * @param newDamping  감쇠비 (1.0 = 임계 감쇠)
     * @return 메서드 체이닝을 위한 자기 자신
     */
    public SpringValue tune(double newResponse, double newDamping) {
        this.response = Math.max(newResponse, 0.05);
        this.damping = Math.max(newDamping, 0.05);
        return this;
    }

    /**
     * 애니메이션 없이 현재 값을 즉시 설정한다. 속도도 함께 0으로 만든다.
     *
     * @param newValue 새 값
     */
    public void reset(double newValue) {
        stop();
        this.active = false;
        this.value = newValue;
        this.target = newValue;
        this.velocity = 0;
        sink.accept(newValue);
    }

    /**
     * 목표를 새로 지정한다. <b>진행 중이던 속도는 그대로 유지된다</b> —
     * 이것이 방향 전환에서 "벽에 부딪히는" 느낌을 없애는 핵심이다.
     *
     * @param newTarget 새 목표값
     */
    public void setTarget(double newTarget) {
        this.target = newTarget;
        if (isSettled()) {
            sink.accept(value);
            return;
        }
        if (!active) {
            active = true;
            lastNanos = 0;
            start();
        }
    }

    /**
     * 제스처가 놓여난 순간의 속도를 그대로 이어받는다.
     *
     * @param pixelsPerSecond 손가락(포인터)이 놓인 시점의 속도
     */
    public void handoffVelocity(double pixelsPerSecond) {
        this.velocity = pixelsPerSecond;
    }

    /**
     * 정지했을 때 한 번 실행할 작업을 건다.
     *
     * @param action 정지 콜백. {@code null}이면 해제
     */
    public void setOnSettle(Runnable action) {
        this.onSettle = action;
    }

    /**
     * 현재 값.
     *
     * @return 마지막으로 계산된 값
     */
    public double value() {
        return value;
    }

    @Override
    public void handle(long now) {
        if (lastNanos == 0) {
            lastNanos = now;
            return;
        }
        double frame = Math.min((now - lastNanos) / 1_000_000_000.0, MAX_FRAME);
        lastNanos = now;

        double omega = 2 * Math.PI / response;
        double remaining = frame;
        while (remaining > 0) {
            double step = Math.min(SUB_STEP, remaining);
            remaining -= step;

            double acceleration = -2 * damping * omega * velocity - omega * omega * (value - target);
            velocity += acceleration * step;
            value += velocity * step;
        }

        if (isSettled()) {
            value = target;
            velocity = 0;
            sink.accept(value);
            active = false;
            stop();
            if (onSettle != null) {
                Runnable action = onSettle;
                onSettle = null;
                action.run();
            }
            return;
        }
        sink.accept(value);
    }

    private boolean isSettled() {
        return Math.abs(value - target) < SETTLE_DISTANCE && Math.abs(velocity) < SETTLE_VELOCITY;
    }
}
