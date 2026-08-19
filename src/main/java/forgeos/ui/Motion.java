package forgeos.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * ForgeOS 전역 모션 상수와 자주 쓰는 전환 헬퍼.
 *
 * <p>여기 있는 숫자는 취향이 아니라 규칙이다. 창이 열릴 때와 Dock 아이콘이
 * 커질 때가 서로 다른 속도로 움직이면 화면 전체가 제각각으로 보인다.
 * 새 애니메이션을 추가할 때는 새 숫자를 만들지 말고 아래 상수 중 하나를 고른다.</p>
 */
public final class Motion {

    /** 표준 반응 시간(초). 창 이동·확대 등 대부분의 UI 전환. */
    public static final double RESPONSE_STANDARD = 0.4;

    /** 빠른 반응 시간(초). 포인터를 직접 따라가는 요소(Dock 확대 등). */
    public static final double RESPONSE_SNAPPY = 0.28;

    /** 임계 감쇠 — 튕김 없음. 기본값. */
    public static final double DAMPING_STANDARD = 1.0;

    /** 약한 저감쇠 — 살짝 튕김. 제스처가 운동량을 실어 보냈을 때만 쓴다. */
    public static final double DAMPING_PLAYFUL = 0.78;

    /** 페이드 표준 시간. */
    public static final Duration FADE = Duration.millis(320);

    /** 부팅 단계 전환 페이드 — 장면이 통째로 바뀌므로 조금 더 길게. */
    public static final Duration FADE_SCENE = Duration.millis(620);

    /**
     * 감속 기반 easing.
     *
     * <p>제스처를 따라가지 않는(=중간에 잡아챌 수 없는) 전환에만 쓴다.
     * 붙잡을 수 있는 요소라면 {@link SpringValue}를 써야 한다.</p>
     */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1, 0.3, 1);

    /** 되돌아가는 전환용 — {@link #EASE_OUT}의 제어점을 뒤집은 곡선. */
    public static final Interpolator EASE_IN = Interpolator.SPLINE(0.7, 0, 0.84, 0);

    private Motion() {
    }

    /**
     * 노드를 서서히 나타나게 한다.
     *
     * @param node     대상
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void fadeIn(Node node, Duration duration, Runnable onFinish) {
        node.setVisible(true);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        if (onFinish != null) {
            fade.setOnFinished(e -> onFinish.run());
        }
        fade.play();
    }

    /**
     * 노드를 서서히 사라지게 한다. 끝나면 {@code visible=false}로 만들어
     * 마우스 이벤트가 투명한 잔상에 먹히지 않도록 한다.
     *
     * @param node     대상
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void fadeOut(Node node, Duration duration, Runnable onFinish) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);
        fade.setInterpolator(EASE_IN);
        fade.setOnFinished(e -> {
            node.setVisible(false);
            if (onFinish != null) {
                onFinish.run();
            }
        });
        fade.play();
    }

    /**
     * 유리 재질이 "도착"하는 느낌으로 나타나게 한다.
     *
     * <p>불투명도만 올리면 그림이 배경에 스며 나오는 것처럼 보인다. 실제 재질은
     * 크기와 함께 잡히므로 스케일을 같이 움직여야 물성이 생긴다.</p>
     *
     * @param node     대상
     * @param fromScale 시작 스케일 (1보다 작게)
     * @param duration 지속 시간
     */
    public static void materialize(Node node, double fromScale, Duration duration) {
        node.setOpacity(0);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);
        node.setVisible(true);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.scaleXProperty(), fromScale),
                        new KeyValue(node.scaleYProperty(), fromScale)),
                new KeyFrame(duration,
                        new KeyValue(node.opacityProperty(), 1, EASE_OUT),
                        new KeyValue(node.scaleXProperty(), 1, EASE_OUT),
                        new KeyValue(node.scaleYProperty(), 1, EASE_OUT)));
        timeline.play();
    }

    /**
     * 사라질 때는 나타날 때의 경로를 거꾸로 되짚는다.
     *
     * <p>들어온 길과 나가는 길이 다르면 공간 감각이 끊긴다.</p>
     *
     * @param node     대상
     * @param toScale  끝 스케일
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void dematerialize(Node node, double toScale, Duration duration, Runnable onFinish) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), node.getOpacity()),
                        new KeyValue(node.scaleXProperty(), node.getScaleX()),
                        new KeyValue(node.scaleYProperty(), node.getScaleY())),
                new KeyFrame(duration,
                        new KeyValue(node.opacityProperty(), 0, EASE_IN),
                        new KeyValue(node.scaleXProperty(), toScale, EASE_IN),
                        new KeyValue(node.scaleYProperty(), toScale, EASE_IN)));
        timeline.setOnFinished(e -> {
            node.setVisible(false);
            if (onFinish != null) {
                onFinish.run();
            }
        });
        timeline.play();
    }

    /**
     * 경계를 넘어선 만큼을 점점 덜 따라가게 만드는 고무줄 저항.
     *
     * <p>딱 멈추면 "굳었다"로 읽히고, 저항이 이어지면 "여기까지"로 읽힌다.</p>
     *
     * @param overshoot 경계를 넘어선 거리
     * @param dimension 기준 치수(대개 컨테이너의 폭이나 높이)
     * @return 실제로 이동시킬 거리
     */
    public static double rubberband(double overshoot, double dimension) {
        double constant = 0.55;
        return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
    }
}
