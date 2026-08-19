package forgeos.wm;

import forgeos.ui.Glyphs;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 창 좌측 상단의 신호등 버튼 세 개.
 *
 * <p>macOS의 배치와 동작을 따르되 색은 Forge 팔레트를 쓴다. 닫기는 Forge Ember,
 * 최소화는 Molten Gold, 전체화면은 Electric Cyan이다.</p>
 *
 * <p>글리프(×, −, ⤢)는 <b>세 버튼 중 하나에라도 포인터가 올라갔을 때</b> 다 같이
 * 나타난다. 개별 호버로 하나씩 켜면 눈이 따라가느라 피곤하고, 무엇보다 실제
 * macOS가 그렇게 동작하지 않는다. 익숙한 것을 이유 없이 바꾸지 않는다.</p>
 *
 * <p>노출/은폐를 CSS의 {@code :hover}로 처리하지 않은 이유는 JavaFX CSS에
 * 전환(transition)이 없어서다. 그대로 두면 글리프가 툭 나타난다.</p>
 */
final class TrafficLights extends HBox {

    /** 버튼 반지름(px). CSS 로 빼지 않는 유일한 치수 — Circle 의 반지름은 CSS 속성이 아니다. */
    private static final double RADIUS = 6.5;

    /** 글리프 크기(px). */
    private static final double GLYPH_SIZE = 16;

    /** 글리프 노출 전환 시간 — 포인터를 스치기만 해도 반응해야 하므로 짧다. */
    private static final Duration REVEAL = Duration.millis(120);

    private final List<Node> glyphs = new ArrayList<>();

    TrafficLights(Runnable onClose, Runnable onMinimize, Runnable onZoom) {
        getStyleClass().add("traffic-lights");
        getChildren().addAll(
                button("light-close", Glyphs.LIGHT_CLOSE, onClose),
                button("light-minimize", Glyphs.LIGHT_MINIMIZE, onMinimize),
                button("light-zoom", Glyphs.LIGHT_ZOOM, onZoom));

        hoverProperty().addListener((obs, was, now) -> reveal(now));
    }

    private StackPane button(String styleClass, String glyphPath, Runnable action) {
        Circle disc = new Circle(RADIUS);
        disc.getStyleClass().addAll("light-disc", styleClass);

        Node glyph = Glyphs.stroked(glyphPath, GLYPH_SIZE, "light-glyph");
        glyph.setOpacity(0);
        glyph.setMouseTransparent(true);
        glyphs.add(glyph);

        StackPane pane = new StackPane(disc, glyph);
        pane.getStyleClass().add("light");
        // 클릭은 누를 때가 아니라 뗄 때 확정된다 — 잘못 눌렀으면 끌어서 벗어나 취소할 수 있다.
        pane.setOnMouseClicked(e -> action.run());
        return pane;
    }

    private void reveal(boolean visible) {
        for (Node glyph : glyphs) {
            FadeTransition fade = new FadeTransition(REVEAL, glyph);
            fade.setToValue(visible ? 1 : 0);
            fade.play();
        }
    }
}
