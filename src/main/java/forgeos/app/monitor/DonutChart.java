package forgeos.app.monitor;

import forgeos.ui.Motion;
import forgeos.ui.SpringValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * 비율 하나를 보여 주는 도넛 게이지.
 *
 * <h2>왜 막대가 아니라 도넛인가</h2>
 * <p>가운데가 비어 있으면 수치를 큼직하게 넣을 자리가 생긴다. "몇 퍼센트인가"와
 * "얼마나 찼는가"를 한 번에 읽게 하려는 것이고, 실제로 사람이 먼저 보는 것은
 * 숫자다. 고리는 그 숫자를 확인시켜 주는 보조다.</p>
 *
 * <h2>값이 튀지 않게</h2>
 * <p>1초마다 갱신되는 값을 그대로 대입하면 고리가 순간이동한다. 스프링을
 * 거치면 값 변화가 궤적으로 보여서, 수치가 오르는 중인지 내리는 중인지를
 * 숫자를 읽기 전에 알 수 있다.</p>
 */
final class DonutChart extends VBox {

    /** 고리의 바깥 지름(px). */
    private static final double SIZE = 112;

    /** 고리 두께(px). */
    private static final double THICKNESS = 11;

    private final Arc progress = new Arc();
    private final Label valueLabel = new Label("0%");
    private final Label captionLabel = new Label();
    private final Label detailLabel = new Label();

    private final SpringValue sweep = new SpringValue(value -> progress.setLength(value))
            .tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);

    DonutChart(String caption, String accentStyleClass) {
        getStyleClass().add("donut");
        setAlignment(Pos.CENTER);

        double radius = (SIZE - THICKNESS) / 2;

        Arc track = new Arc(SIZE / 2, SIZE / 2, radius, radius, 0, 360);
        track.setType(ArcType.OPEN);
        track.setFill(null);
        track.setStrokeWidth(THICKNESS);
        track.getStyleClass().add("donut-track");

        progress.setCenterX(SIZE / 2);
        progress.setCenterY(SIZE / 2);
        progress.setRadiusX(radius);
        progress.setRadiusY(radius);
        // 12시 방향에서 시작해 시계 방향으로 찬다. 시계와 같은 방향이라 설명이 필요 없다.
        progress.setStartAngle(90);
        progress.setLength(0);
        progress.setType(ArcType.OPEN);
        progress.setFill(null);
        progress.setStrokeWidth(THICKNESS);
        progress.setStrokeLineCap(StrokeLineCap.ROUND);
        progress.getStyleClass().addAll("donut-progress", accentStyleClass);

        Pane ring = new Pane(track, progress);
        ring.setPrefSize(SIZE, SIZE);
        ring.setMinSize(SIZE, SIZE);
        ring.setMaxSize(SIZE, SIZE);

        valueLabel.getStyleClass().add("donut-value");
        StackPane center = new StackPane(ring, valueLabel);

        captionLabel.setText(caption);
        captionLabel.getStyleClass().add("donut-caption");
        detailLabel.getStyleClass().add("donut-detail");

        getChildren().addAll(center, captionLabel, detailLabel);
        sweep.reset(0);
    }

    /**
     * 값을 갱신한다.
     *
     * @param ratio  0.0 ~ 1.0 비율
     * @param detail 고리 아래에 붙는 부연 (예: {@code "7 / 16 프레임"})
     */
    void setValue(double ratio, String detail) {
        double clamped = Math.max(0, Math.min(1, ratio));
        valueLabel.setText("%.0f%%".formatted(clamped * 100));
        detailLabel.setText(detail);
        // 음수 길이가 시계 방향이다.
        sweep.setTarget(-360 * clamped);
    }
}
