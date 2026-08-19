package forgeos.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * on/off 토글 스위치.
 *
 * <p>JavaFX 기본 {@code CheckBox}로도 기능은 되지만, 은행원 알고리즘처럼
 * "켜는 순간 커널의 동작 방식이 바뀌는" 설정에는 체크박스가 어울리지 않는다.
 * 체크박스는 목록에서 항목을 고르는 도구고, 스위치는 상태를 바꾸는 도구다.
 * 손잡이가 실제로 이동하는 것을 보여 주면 "지금 켠 것"이 분명해진다.</p>
 */
public final class ToggleSwitch extends StackPane {

    /** 트랙 폭(px). */
    private static final double TRACK_WIDTH = 46;

    /** 트랙 높이(px). */
    private static final double TRACK_HEIGHT = 26;

    /** 손잡이 반지름(px). */
    private static final double THUMB_RADIUS = 10;

    /** 트랙 양 끝 여백(px). */
    private static final double INSET = 3;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Circle thumb = new Circle(THUMB_RADIUS);

    private final SpringValue slide = new SpringValue(thumb::setTranslateX)
            .tune(Motion.RESPONSE_SNAPPY, Motion.DAMPING_STANDARD);

    /** 꺼진 상태의 스위치를 만든다. */
    public ToggleSwitch() {
        getStyleClass().add("toggle-switch");

        Region track = new Region();
        track.getStyleClass().add("toggle-track");
        track.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        thumb.getStyleClass().add("toggle-thumb");

        getChildren().addAll(track, thumb);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        slide.reset(offPosition());
        setOnMouseClicked(e -> setSelected(!isSelected()));

        selected.addListener((obs, was, now) -> {
            pseudoClassStateChanged(Styles.ON, now);
            slide.setTarget(now ? onPosition() : offPosition());
        });
    }

    private static double offPosition() {
        return -(TRACK_WIDTH / 2 - THUMB_RADIUS - INSET);
    }

    private static double onPosition() {
        return TRACK_WIDTH / 2 - THUMB_RADIUS - INSET;
    }

    /**
     * 켜짐 여부 프로퍼티.
     *
     * @return 프로퍼티
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * 켜짐 여부.
     *
     * @return 켜져 있으면 참
     */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * 켜짐 여부를 설정한다. 애니메이션과 함께 반영된다.
     *
     * @param value 새 상태
     */
    public void setSelected(boolean value) {
        selected.set(value);
    }

    /**
     * 애니메이션 없이 상태를 맞춘다. 커널 상태를 화면에 되비출 때 쓴다 —
     * 사용자가 누르지도 않았는데 손잡이가 미끄러지면 누가 눌렀는지 헷갈린다.
     *
     * @param value 새 상태
     */
    public void syncSilently(boolean value) {
        if (selected.get() == value) {
            return;
        }
        selected.set(value);
        slide.reset(value ? onPosition() : offPosition());
    }
}
