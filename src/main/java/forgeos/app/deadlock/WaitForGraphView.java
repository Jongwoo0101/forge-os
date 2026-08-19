package forgeos.app.deadlock;

import forgeframework.deadlock.ResourceRowDto;
import forgeframework.deadlock.WaitForEdgeDto;
import forgeos.ui.Motion;
import forgeos.ui.Styles;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wait-For 그래프 — "누가 누구를 기다리는가"를 그리는 시각화 맵.
 *
 * <h2>왜 이 그림이 필요한가</h2>
 * <p>{@code detect}가 돌려주는 것은 PID 목록과 간선 목록이다. 표로 보면
 * "P1이 P2를 기다리고, P2가 P3을 기다리고, P3이 P1을 기다린다"를 머릿속에서
 * 이어 붙여야 교착임을 알 수 있다. 원으로 그리면 <b>고리</b>가 눈에 그냥
 * 보인다. 교착 상태 설명에서 사이클 그림이 빠지지 않는 이유다.</p>
 *
 * <h2>배치</h2>
 * <p>프로세스를 원주 위에 균등 배치한다. 힘 기반(force-directed) 배치는 더
 * 예쁘지만 매 갱신마다 노드가 춤을 춰서, 1초마다 새로 그리는 화면에서는
 * 오히려 읽기 어렵다. 같은 프로세스가 항상 같은 자리에 있는 편이 낫다.</p>
 */
final class WaitForGraphView extends Pane {

    /** 프로세스 노드의 반지름(px). */
    private static final double NODE_RADIUS = 26;

    /** 원 배치의 가장자리 여백(px). */
    private static final double PADDING = 54;

    /** 간선의 휘어짐 정도. 0이면 직선 — 양방향 간선이 겹쳐 버린다. */
    private static final double CURVATURE = 0.18;

    private final Label emptyLabel = new Label("교착 상태가 없습니다.\n프로세스가 서로를 기다리기 시작하면 여기에 고리가 나타납니다.");

    private final List<Timeline> pulses = new ArrayList<>();

    private List<WaitForEdgeDto> edges = List.of();
    private List<ResourceRowDto> rows = List.of();
    private Set<Integer> deadlockedPids = Set.of();

    WaitForGraphView() {
        getStyleClass().add("wait-for-graph");
        emptyLabel.getStyleClass().add("graph-empty");

        // 노드를 절대 좌표로 놓는 Pane 은 자식 위치에서 기본·최소 크기를 역산한다.
        // 그대로 두면 한 번 커진 그래프가 창을 다시 못 줄이게 붙잡는다.
        // 이 화면은 주어진 자리에 맞춰 다시 그리면 되므로 크기를 요구하지 않는다.
        setMinSize(0, 0);
        setPrefSize(0, 0);

        widthProperty().addListener((obs, old, now) -> rebuild());
        heightProperty().addListener((obs, old, now) -> rebuild());
    }

    /**
     * 그래프 데이터를 갈아 끼운다.
     *
     * @param newRows           자원 표의 행 (프로세스 이름과 보유량의 출처)
     * @param newEdges          대기 간선
     * @param newDeadlockedPids 교착에 얽힌 PID
     */
    void update(List<ResourceRowDto> newRows, List<WaitForEdgeDto> newEdges, Set<Integer> newDeadlockedPids) {
        // 1초마다 같은 그림을 다시 그리면 교착 노드의 맥박 애니메이션이 매번
        // 처음으로 되돌아가 영원히 첫 프레임만 보인다. 값이 진짜 바뀐 때만 다시 그린다.
        if (newRows.equals(rows) && newEdges.equals(edges) && newDeadlockedPids.equals(deadlockedPids)) {
            return;
        }
        this.rows = List.copyOf(newRows);
        this.edges = List.copyOf(newEdges);
        this.deadlockedPids = Set.copyOf(newDeadlockedPids);
        rebuild();
    }

    private void rebuild() {
        // 이전 그림의 애니메이션은 노드가 사라져도 계속 돈다. 반드시 먼저 끊는다.
        pulses.forEach(Timeline::stop);
        pulses.clear();
        getChildren().clear();

        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (rows.isEmpty()) {
            showEmpty(width, height);
            return;
        }

        Map<Integer, Point2D> positions = layoutNodes(width, height);

        // 간선을 먼저 그려야 노드 아래로 깔린다.
        for (WaitForEdgeDto edge : edges) {
            Point2D from = positions.get(edge.waiterPid());
            Point2D to = positions.get(edge.holderPid());
            if (from == null || to == null || from.equals(to)) {
                continue;
            }
            drawEdge(from, to, edge);
        }

        for (ResourceRowDto row : rows) {
            Point2D position = positions.get(row.pid());
            if (position != null) {
                drawNode(row, position);
            }
        }

        if (edges.isEmpty()) {
            showEmpty(width, height);
        }
    }

    private void showEmpty(double width, double height) {
        emptyLabel.setWrapText(true);
        emptyLabel.setPrefWidth(Math.min(360, width - 40));
        getChildren().add(emptyLabel);
        emptyLabel.applyCss();
        emptyLabel.autosize();
        emptyLabel.setLayoutX((width - emptyLabel.getWidth()) / 2);
        emptyLabel.setLayoutY(height - emptyLabel.getHeight() - 16);
    }

    private Map<Integer, Point2D> layoutNodes(double width, double height) {
        Map<Integer, Point2D> positions = new LinkedHashMap<>();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.max(60, Math.min(width, height) / 2 - PADDING);

        int count = rows.size();
        if (count == 1) {
            positions.put(rows.get(0).pid(), new Point2D(centerX, centerY));
            return positions;
        }

        for (int i = 0; i < count; i++) {
            // 12시에서 시작해 시계 방향. PID 순서가 곧 배치 순서라 매번 같은 자리에 온다.
            double angle = -Math.PI / 2 + (2 * Math.PI * i) / count;
            positions.put(rows.get(i).pid(), new Point2D(
                    centerX + radius * Math.cos(angle),
                    centerY + radius * Math.sin(angle)));
        }
        return positions;
    }

    private void drawEdge(Point2D from, Point2D to, WaitForEdgeDto edge) {
        Point2D direction = to.subtract(from).normalize();
        Point2D start = from.add(direction.multiply(NODE_RADIUS));
        Point2D end = to.subtract(direction.multiply(NODE_RADIUS + 8));

        Point2D middle = start.midpoint(end);
        Point2D normal = new Point2D(-direction.getY(), direction.getX());
        Point2D control = middle.add(normal.multiply(start.distance(end) * CURVATURE));

        QuadCurve curve = new QuadCurve(
                start.getX(), start.getY(),
                control.getX(), control.getY(),
                end.getX(), end.getY());
        curve.setFill(null);
        curve.getStyleClass().add("graph-edge");

        getChildren().addAll(curve, arrowHead(control, end), edgeLabel(control, edge));
    }

    private Polygon arrowHead(Point2D control, Point2D tip) {
        Point2D direction = tip.subtract(control).normalize();
        Point2D normal = new Point2D(-direction.getY(), direction.getX());

        Point2D base = tip.subtract(direction.multiply(11));
        Point2D left = base.add(normal.multiply(5));
        Point2D right = base.subtract(normal.multiply(5));

        Polygon head = new Polygon(
                tip.getX(), tip.getY(),
                left.getX(), left.getY(),
                right.getX(), right.getY());
        head.getStyleClass().add("graph-arrow");
        return head;
    }

    private Label edgeLabel(Point2D control, WaitForEdgeDto edge) {
        Label label = new Label("%s 부족 %d".formatted(edge.resource(), edge.shortage()));
        label.getStyleClass().add("graph-edge-label");
        label.applyCss();
        label.autosize();
        label.setLayoutX(control.getX() - label.getWidth() / 2);
        label.setLayoutY(control.getY() - label.getHeight() / 2);
        return label;
    }

    private void drawNode(ResourceRowDto row, Point2D position) {
        boolean deadlocked = deadlockedPids.contains(row.pid());

        Circle halo = new Circle(NODE_RADIUS + 6);
        halo.getStyleClass().add("graph-node-halo");
        halo.setVisible(deadlocked);

        Circle disc = new Circle(NODE_RADIUS);
        disc.getStyleClass().add("graph-node-disc");

        Label caption = new Label("P" + row.pid() + "\n" + row.name());
        caption.getStyleClass().add("graph-node-label");
        caption.setWrapText(true);

        StackPane node = new StackPane(halo, disc, caption);
        node.getStyleClass().add("graph-node");
        node.pseudoClassStateChanged(Styles.DEADLOCKED, deadlocked);

        double size = (NODE_RADIUS + 8) * 2;
        node.setPrefSize(size, size);
        node.setLayoutX(position.getX() - size / 2);
        node.setLayoutY(position.getY() - size / 2);

        if (deadlocked) {
            pulse(halo);
        }
        getChildren().add(node);
    }

    /** 교착 노드의 후광을 천천히 숨 쉬게 한다. 정지 화면에서 "여기가 문제"를 가리키는 유일한 움직임이다. */
    private void pulse(Circle halo) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(halo.opacityProperty(), 0.15),
                        new KeyValue(halo.scaleXProperty(), 1),
                        new KeyValue(halo.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(1100),
                        new KeyValue(halo.opacityProperty(), 0.45, Motion.EASE_OUT),
                        new KeyValue(halo.scaleXProperty(), 1.14, Motion.EASE_OUT),
                        new KeyValue(halo.scaleYProperty(), 1.14, Motion.EASE_OUT)));
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        pulses.add(timeline);
    }

    /** 창이 닫힐 때 남아 있는 애니메이션을 정리한다. */
    void dispose() {
        pulses.forEach(Timeline::stop);
        pulses.clear();
    }
}
