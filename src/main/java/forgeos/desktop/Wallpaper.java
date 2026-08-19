package forgeos.desktop;

import forgeos.ui.ForgeMark;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * 바탕화면.
 *
 * <h2>구성</h2>
 * <p>아래에서 위로 그라디언트 바닥 → 중심 발광 → 동심원 → Forge 마크 → 워드마크다.
 * 전부 벡터라 어떤 해상도에서도 깨지지 않고, 색은 토큰을 따르므로 테마를 바꾸면
 * 배경화면도 같이 바뀐다.</p>
 *
 * <h2>레이아웃에 관여하지 않는다 (중요)</h2>
 * <p>{@link Circle}과 {@link ForgeMark}는 리사이즈 불가 노드라서, 그냥 두면 자기
 * 크기가 그대로 이 StackPane 의 <b>최소·기본 크기</b>가 된다. 반지름 500px 짜리 원
 * 하나만 있어도 바탕화면의 최소 높이가 1000px 이 되고, 그 최소값은 부모
 * ({@code DesktopPane} → Scene 루트)까지 그대로 전파된다. 그러면 창이 그보다
 * 작을 때 데스크탑이 창보다 커진 채 가운데 정렬되어 <b>메뉴바는 화면 위로,
 * Dock 은 화면 아래로 밀려 잘린다</b>. 실제로 그렇게 됐다.</p>
 *
 * <p>그래서 최소·기본 크기를 0 으로 못 박고 최대만 열어 둔다. 배경화면은 자리를
 * 요구하는 요소가 아니라 남는 자리를 채우는 요소다.</p>
 *
 * <h2>크기는 화면을 따라간다</h2>
 * <p>마크를 고정 픽셀로 두면 작은 창에서는 화면을 뒤덮고 큰 화면에서는 우표만 해진다.
 * 짧은 변에 비례해 배율을 정하되 위아래로 잘라, 어떤 창 크기에서도 비슷한 비중으로
 * 보이게 한다.</p>
 *
 * <h2>정중앙</h2>
 * <p>마크·동심원·워드마크는 모두 데스크탑의 정중앙에 놓인다. 한때 창이 화면
 * 가운데에 열린다는 이유로 왼쪽 위로 비켜 앉혔지만, 창이 하나도 없을 때 화면이
 * 눈에 띄게 기울어 보였다. 배경화면은 창을 피해 숨는 물건이 아니라 화면의 축을
 * 잡아 주는 물건이므로 중앙이 맞다 — 창에 가려지는 것은 배경화면의 정상적인 처지다.</p>
 */
final class Wallpaper extends StackPane {

    /** 마크의 기준 한 변(px). 실제 크기는 여기에 배율을 곱한 값이다. */
    private static final double MARK_SIZE = 320;

    /** 안쪽 동심원의 기준 반지름(px). */
    private static final double RING_INNER = 250;

    /** 바깥 동심원의 기준 반지름(px). */
    private static final double RING_OUTER = 375;

    /** 배율 1.0 이 되는 기준 화면 짧은 변(px). */
    private static final double REFERENCE = 900;

    /** 배율 하한 — 작은 창에서도 로고를 알아볼 수 있어야 한다. */
    private static final double MIN_SCALE = 0.55;

    /** 배율 상한 — 큰 화면에서 배경이 주인공이 되면 안 된다. */
    private static final double MAX_SCALE = 1.25;

    private final StackPane rings;
    private final VBox brand;
    private final Rectangle clip = new Rectangle();

    Wallpaper() {
        getStyleClass().add("wallpaper");

        Region glow = new Region();
        glow.getStyleClass().add("wallpaper-glow");

        rings = new StackPane(ring(RING_INNER), ring(RING_OUTER));
        rings.setMinSize(0, 0);
        rings.setPrefSize(0, 0);

        Label wordmark = new Label("G R A P H I C A L   O S");
        // JavaFX CSS 에는 letter-spacing 이 없다. 원본 SVG 의 넓은 자간을 흉내 내려면
        // 글자 사이에 공백을 직접 넣는 수밖에 없다.
        wordmark.getStyleClass().add("wallpaper-wordmark");

        brand = new VBox(new ForgeMark(MARK_SIZE, true), wordmark);
        brand.getStyleClass().add("wallpaper-brand");
        brand.setMinSize(0, 0);
        brand.setPrefSize(0, 0);

        getChildren().addAll(glow, rings, brand);

        // 이 세 줄이 메뉴바·Dock 이 잘리지 않게 하는 핵심이다. 위 주석 참고.
        setMinSize(0, 0);
        setPrefSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // 동심원은 화면 밖으로 흘러 나가도록 잡았으므로 잘라 준다.
        setClip(clip);

        // 바탕화면은 클릭을 받지 않는다. 빈 곳을 눌렀을 때 아무 일도 일어나지 않는 것이
        // 맞고, 무엇보다 마크가 마우스를 먹어 창 드래그를 방해하면 안 된다.
        setMouseTransparent(true);

        widthProperty().addListener((obs, old, now) -> rescaleBrand());
        heightProperty().addListener((obs, old, now) -> rescaleBrand());
    }

    private void rescaleBrand() {
        double width = getWidth();
        double height = getHeight();

        clip.setWidth(Math.max(0, width));
        clip.setHeight(Math.max(0, height));

        if (width <= 0 || height <= 0) {
            return;
        }

        double scale = clamp(Math.min(width, height) / REFERENCE, MIN_SCALE, MAX_SCALE);

        // 위치는 손대지 않는다. 둘 다 StackPane 의 기본 정렬(가운데)에 맡기고,
        // 배율만 화면 크기를 따라가게 한다. 스케일은 노드의 중심을 기준으로
        // 걸리므로 배율이 바뀌어도 중심은 그대로다.
        applyScale(rings, scale);
        applyScale(brand, scale);
    }

    private static void applyScale(Node node, double scale) {
        node.setScaleX(scale);
        node.setScaleY(scale);
    }

    private static Circle ring(double radius) {
        Circle circle = new Circle(radius);
        circle.getStyleClass().add("wallpaper-ring");
        circle.setFill(null);
        return circle;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
