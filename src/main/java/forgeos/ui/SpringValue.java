package forgeos.ui;

import javafx.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.List;
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
 * <h2>1.1.1 — 타이머 하나로 모은다</h2>
 * <p>1.1.0 까지 이 클래스는 {@link AnimationTimer}를 <b>상속</b>했다. 스프링 하나가
 * 곧 타이머 하나였다는 뜻이다. 창 하나에 스프링이 넷(x·y·폭·높이)이므로 창을
 * 여섯 개 열면 펄스마다 스물네 개의 타이머 콜백이 돌았고, 그중 넷은 <b>같은 창의
 * 같은 레이아웃을 네 번 다시 계산</b>했다.</p>
 *
 * <p>이제 스프링은 타이머가 아니다. 정적 {@link Driver} 하나가 활성 스프링 전부를
 * 한 프레임에 몰아서 적분하고, 그 뒤에 {@link #onFrame} 으로 등록된 후처리를
 * <b>중복 없이 한 번씩</b> 실행한다. 창 하나가 프레임당 레이아웃을 한 번만
 * 요청하게 만드는 것이 이 구조의 목적이다. 활성 스프링이 하나도 없으면 타이머
 * 자체가 멈추므로, 아무것도 움직이지 않을 때의 비용은 정확히 0이다.</p>
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
 * 오일러 적분에서 발산한다. 그래서 프레임 시간을 잘게 쪼개 여러 번 적분한다.
 * 다만 1.1.0 처럼 서브스텝을 <b>무조건 240Hz</b>로 잡으면, 뻣뻣하지 않은 스프링에서는
 * 필요 없는 반복을 매 프레임 도는 낭비가 된다. 안정성과 정확도를 정하는 것은 절대
 * 시간이 아니라 {@code omega * dt} 이므로, 이제 스프링의 고유 진동수에 맞춰 필요한
 * 만큼만 쪼갠다({@link #MAX_OMEGA_STEP}).</p>
 */
public final class SpringValue {

    /**
     * 서브스텝 하나의 절대 상한(초). 60Hz 상당.
     *
     * <p>느슨한 스프링이라도 한 걸음이 한 프레임보다 길어질 이유는 없다.
     * 실제 걸음 크기는 {@link #MAX_OMEGA_STEP} 이 정하고, 이 값은 그 위에 씌우는
     * 안전 뚜껑이다.</p>
     */
    private static final double MAX_SUB_STEP = 1.0 / 60.0;

    /**
     * 서브스텝 하나가 감당할 수 있는 {@code omega * dt} 상한.
     *
     * <p>적분의 안정성과 정확도는 절대 시간이 아니라 이 곱에 걸린다. 그래서
     * 240Hz 라는 <b>고정된 숫자</b>는 뻣뻣한 스프링에는 필요하고 느슨한 스프링에는
     * 낭비다. 이제 스프링의 고유 진동수에 맞춰 필요한 만큼만 쪼갠다.</p>
     *
     * <p>{@code 0.13} 에서 표준 스프링({@code response = 0.4})의 서브스텝은 약
     * 1/121초가 되어, 1.1.0 이 60fps 한 프레임에 네 번 돌리던 적분이 두 번으로 준다.
     * 이 크기에서 심플렉틱 오일러의 주기 오차는 걸음당 0.1% 미만이라 궤적은 눈으로
     * 구별되지 않는다. 값을 더 키우는 것은 권하지 않는다 — 안정 한계
     * ({@code omega * dt < 2}) 까지는 멀지만, 정확도는 이 곱의 제곱으로 나빠진다.</p>
     */
    private static final double MAX_OMEGA_STEP = 0.13;

    /** 한 프레임에 소화할 최대 시간(초). 탭 전환 등으로 프레임이 크게 밀렸을 때의 폭주 방지. */
    private static final double MAX_FRAME = 0.1;

    /** 정지 판정 임계값(픽셀). 이보다 가까우면서 느리면 목표에 스냅하고 멈춘다. */
    private static final double SETTLE_DISTANCE = 0.25;

    /** 정지 판정 속도(픽셀/초). */
    private static final double SETTLE_VELOCITY = 0.25;

    /** 활성 스프링 전부를 돌리는 단 하나의 타이머. */
    private static final Driver DRIVER = new Driver();

    private final DoubleConsumer sink;

    private double response = Motion.RESPONSE_STANDARD;
    private double damping = Motion.DAMPING_STANDARD;

    private double value;
    private double velocity;
    private double target;

    private boolean active;
    private Runnable onSettle;
    private Runnable onFrame;

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
     * 이 스프링이 움직인 프레임의 <b>끝</b>에 한 번 실행할 후처리를 건다.
     *
     * <p>같은 후처리를 여러 스프링에 걸어도 한 프레임에 한 번만 실행된다. 창처럼
     * 값 네 개가 하나의 레이아웃으로 수렴하는 대상에서, 값마다 레이아웃을 요청하는
     * 낭비를 없애기 위한 장치다.</p>
     *
     * @param action 프레임 후처리. {@code null}이면 해제
     * @return 메서드 체이닝을 위한 자기 자신
     */
    public SpringValue onFrame(Runnable action) {
        this.onFrame = action;
        return this;
    }

    /**
     * 애니메이션 없이 현재 값을 즉시 설정한다. 속도도 함께 0으로 만든다.
     *
     * @param newValue 새 값
     */
    public void reset(double newValue) {
        deactivate();
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
            value = newTarget;
            velocity = 0;
            sink.accept(value);
            deactivate();
            return;
        }
        if (!active) {
            active = true;
            DRIVER.add(this);
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

    /**
     * 지금 움직이는 중인지.
     *
     * @return 목표를 향해 적분 중이면 참
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 한 프레임을 적분한다. {@link Driver}만 호출한다.
     *
     * @param frame 프레임 시간(초)
     * @return 이번 프레임에 정지했으면 실행해야 할 정지 콜백, 없으면 {@code null}
     */
    private Runnable advance(double frame) {
        double omega = 2 * Math.PI / response;
        // 안정성과 정확도는 omega * dt 에 걸린다. 뻣뻣한 스프링일수록 잘게 쪼개고,
        // 느슨한 스프링은 큰 걸음으로 걷되 한 프레임을 넘지는 않는다.
        double subStep = Math.min(MAX_SUB_STEP, MAX_OMEGA_STEP / omega);
        double remaining = frame;

        while (remaining > 0) {
            double step = Math.min(subStep, remaining);
            remaining -= step;

            double acceleration = -2 * damping * omega * velocity - omega * omega * (value - target);
            velocity += acceleration * step;
            value += velocity * step;
        }

        if (isSettled()) {
            value = target;
            velocity = 0;
            sink.accept(value);
            deactivate();
            Runnable action = onSettle;
            onSettle = null;
            return action;
        }
        sink.accept(value);
        return null;
    }

    private void deactivate() {
        if (active) {
            active = false;
            DRIVER.remove(this);
        }
    }

    private boolean isSettled() {
        return Math.abs(value - target) < SETTLE_DISTANCE && Math.abs(velocity) < SETTLE_VELOCITY;
    }

    /**
     * 활성 스프링 전부를 한 프레임에 몰아 돌리는 단 하나의 타이머.
     *
     * <p>매 프레임 새 자료구조를 만들지 않는다. 애니메이션 코드가 프레임마다
     * 쓰레기를 만들면 그 쓰레기를 치우는 GC 가 다시 프레임을 밀어내기 때문이다 —
     * 부드러움을 목표로 하는 코드에서 할당은 그 자체로 버그에 가깝다.</p>
     */
    private static final class Driver extends AnimationTimer {

        private final List<SpringValue> springs = new ArrayList<>();
        private final List<Runnable> frameActions = new ArrayList<>(4);
        private final List<Runnable> settleActions = new ArrayList<>(4);

        private SpringValue[] buffer = new SpringValue[8];
        private boolean running;
        private long lastNanos;

        Driver() {
        }

        void add(SpringValue spring) {
            if (!springs.contains(spring)) {
                springs.add(spring);
            }
            if (!running) {
                running = true;
                lastNanos = 0;
                start();
            }
        }

        void remove(SpringValue spring) {
            springs.remove(spring);
        }

        @Override
        public void handle(long now) {
            if (lastNanos == 0) {
                lastNanos = now;
                return;
            }
            double frame = Math.min((now - lastNanos) / 1_000_000_000.0, MAX_FRAME);
            lastNanos = now;

            // 적분 도중 스프링이 스스로 목록에서 빠지므로 스냅샷을 뜬다.
            // 재사용 배열이라 프레임마다 할당이 생기지 않는다.
            int count = springs.size();
            if (buffer.length < count) {
                buffer = new SpringValue[Math.max(count, buffer.length * 2)];
            }
            for (int i = 0; i < count; i++) {
                buffer[i] = springs.get(i);
            }

            for (int i = 0; i < count; i++) {
                SpringValue spring = buffer[i];
                buffer[i] = null;
                if (!spring.active) {
                    continue;
                }
                Runnable settled = spring.advance(frame);
                if (settled != null) {
                    settleActions.add(settled);
                }
                Runnable action = spring.onFrame;
                // 같은 후처리를 여러 스프링이 공유한다 — 프레임당 한 번만 실행한다.
                if (action != null && !frameActions.contains(action)) {
                    frameActions.add(action);
                }
            }

            // 후처리가 먼저다. 정지 콜백은 "다 놓인 뒤"에 불려야 한다.
            runAndClear(frameActions);
            runAndClear(settleActions);

            if (springs.isEmpty()) {
                running = false;
                stop();
            }
        }

        private static void runAndClear(List<Runnable> actions) {
            for (int i = 0; i < actions.size(); i++) {
                actions.get(i).run();
            }
            actions.clear();
        }
    }
}
