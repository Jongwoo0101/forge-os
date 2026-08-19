package forgeos.desktop;

import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;
import forgeos.ui.Styles;
import forgeos.wm.WindowManager;
import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 화면 하단의 Dock.
 *
 * <h2>확대 대신 이름표</h2>
 * <p>처음에는 macOS 처럼 포인터에 가까운 아이콘을 키웠지만, 어떤 감쇠 곡선을 써도
 * <b>겨냥한 것 말고 옆 아이콘까지 함께 들리는</b> 느낌을 지울 수 없었다. 아이콘이
 * 네 개뿐이고 간격이 넓은 Dock 에서는 확대가 겨냥을 돕기는커녕 "무엇을 가리키고
 * 있는가"를 오히려 흐린다.</p>
 *
 * <p>그래서 크기는 건드리지 않고, 가리킨 아이콘 <b>바로 위 가운데</b>에 앱 이름만
 * 띄운다. 답해야 할 질문("이게 무슨 앱이지?")에 정확히 답하면서 화면은 가만히 있는다.</p>
 *
 * <h2>왜 Tooltip 이 아닌가</h2>
 * <p>JavaFX {@code Tooltip}은 포인터를 따라 <b>아래·오른쪽</b>에 뜬다. Dock 은 화면
 * 맨 아래에 있으므로 이름표가 아이콘을 가리거나 화면 밖으로 밀린다. 게다가 Tooltip
 * 은 별도의 네이티브 팝업 창이라, 데스크탑 안에서 모든 것이 노드인 이 프로젝트의
 * 구성과도 어긋난다. 그냥 Dock 안의 라벨 하나로 만든다.</p>
 *
 * <p>이름표는 {@code managed = false} 다. 레이아웃에 참여하면 Dock 높이가 이름표
 * 몫만큼 커져서 작업 영역이 줄고, 이름표가 없을 때도 그 공백이 남는다.</p>
 */
final class DockView extends StackPane {

    /** 아이콘 한 변(px). */
    private static final double ICON_SIZE = 30;

    /** 이름표와 Dock 윗면 사이 간격(px). */
    private static final double LABEL_GAP = 10;

    /** 이름표가 나타나고 사라지는 시간. 스치듯 지나가도 따라올 만큼 짧아야 한다. */
    private static final Duration LABEL_FADE = Duration.millis(110);

    private final WindowManager windowManager;
    private final HBox bar = new HBox();
    private final Label nameLabel = new Label();
    private final Map<String, DockItem> items = new LinkedHashMap<>();

    private FadeTransition labelFade;

    DockView(WindowManager windowManager, List<ForgeApp> apps) {
        this.windowManager = windowManager;
        getStyleClass().add("dock-host");
        setPickOnBounds(false);

        bar.getStyleClass().add("dock");
        for (ForgeApp app : apps) {
            DockItem item = new DockItem(app);
            items.put(app.id(), item);
            bar.getChildren().add(item);
        }

        nameLabel.getStyleClass().add("dock-name");
        nameLabel.setManaged(false);
        nameLabel.setMouseTransparent(true);
        nameLabel.setVisible(false);
        nameLabel.setOpacity(0);

        getChildren().addAll(bar, nameLabel);

        // 이름표는 Dock 을 완전히 벗어날 때만 사라진다. 아이콘 사이를 지나가는 동안
        // 깜빡이면 눈이 피로해진다 — 인접한 아이콘으로 옮겨갈 때는 자리만 옮긴다.
        bar.hoverProperty().addListener((obs, was, now) -> {
            if (!now) {
                hideName();
            }
        });

        windowManager.runningAppIds().addListener(
                (ListChangeListener<String>) change -> refreshRunningState());

        // 최소화 애니메이션이 어디로 날아가야 하는지는 Dock 만 안다.
        windowManager.setDockAnchor(this::anchorFor);
    }

    /**
     * 가리킨 아이콘 바로 위 가운데에 이름표를 놓는다.
     *
     * <p>세로 위치는 아이콘이 아니라 <b>Dock 전체의 윗면</b>을 기준으로 잡는다.
     * 아이콘 위쪽 여백을 기준으로 하면 이름표가 유리 바 안쪽에 걸쳐 앉아서,
     * Dock 위에 떠 있는 것이 아니라 Dock 에 박힌 것처럼 보인다.</p>
     */
    private void showName(DockItem item, String title) {
        nameLabel.setText(title);
        nameLabel.applyCss();
        nameLabel.autosize();

        Point2D iconCenter = item.localToScene(item.getWidth() / 2, 0);
        double centerX = sceneToLocal(iconCenter).getX();

        nameLabel.setLayoutX(centerX - nameLabel.getWidth() / 2);
        nameLabel.setLayoutY(-nameLabel.getHeight() - LABEL_GAP);

        fadeLabelTo(1);
    }

    private void hideName() {
        fadeLabelTo(0);
    }

    private void fadeLabelTo(double target) {
        if (labelFade != null) {
            labelFade.stop();
        }
        if (target > 0) {
            nameLabel.setVisible(true);
        }
        labelFade = new FadeTransition(LABEL_FADE, nameLabel);
        labelFade.setToValue(target);
        labelFade.setOnFinished(e -> nameLabel.setVisible(nameLabel.getOpacity() > 0));
        labelFade.play();
    }

    private void refreshRunningState() {
        for (Map.Entry<String, DockItem> entry : items.entrySet()) {
            boolean running = windowManager.runningAppIds().contains(entry.getKey());
            entry.getValue().setRunning(running);
        }
    }

    /** 앱 아이콘의 중심을 창 레이어 좌표계로 변환해 돌려준다. */
    private Point2D anchorFor(String appId) {
        DockItem item = items.get(appId);
        if (item == null) {
            return new Point2D(getWidth() / 2, getHeight());
        }
        Point2D sceneCenter = item.localToScene(
                item.getWidth() / 2, item.getHeight() / 2);
        return windowManager.sceneToLocal(sceneCenter);
    }

    /** Dock 아이콘 하나. */
    private final class DockItem extends StackPane {

        private final Circle runningDot = new Circle();

        DockItem(ForgeApp app) {
            getStyleClass().add("dock-item");

            Node icon = Glyphs.stroked(app.iconPath(), ICON_SIZE, "dock-glyph");
            StackPane tile = new StackPane(icon);
            tile.getStyleClass().add("dock-tile");

            runningDot.getStyleClass().add("dock-running-dot");
            runningDot.setRadius(2.2);
            runningDot.setVisible(false);

            VBox column = new VBox(tile, runningDot);
            column.getStyleClass().add("dock-item-column");
            getChildren().add(column);

            hoverProperty().addListener((obs, was, now) -> {
                if (now) {
                    showName(this, app.title());
                }
            });

            // 눌리는 순간 반응한다. 떼는 순간까지 기다리면 죽은 버튼처럼 느껴진다.
            setOnMousePressed(e -> tile.setOpacity(0.6));
            setOnMouseReleased(e -> tile.setOpacity(1));
            setOnMouseClicked(e -> windowManager.toggle(app));
        }

        void setRunning(boolean running) {
            runningDot.setVisible(running);
            pseudoClassStateChanged(Styles.RUNNING, running);
        }
    }
}
