package forgeos.ui;

import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * ForgeOS 로고 마크 — 모루 위에서 타오르는 불꽃.
 *
 * <h2>왜 SVG 파일을 그대로 쓰지 않는가</h2>
 * <p>{@code assets/forgeOS-logo.svg}가 원본이지만 JavaFX는 SVG 파일을 읽지 못한다.
 * PNG로 굽는 방법도 있으나 배경화면 크기(수백 px)에서 확대하면 가장자리가 뭉개지고,
 * 무엇보다 <b>테마에 따라 색을 바꿀 수 없다</b>. 그래서 원본의 패스 데이터만
 * 가져와 {@link SVGPath}로 다시 세웠다. 색·그라디언트·발광은 전부 CSS의
 * {@code .mark-*} 클래스가 정하므로 라이트/다크 전환에 그대로 따라온다.</p>
 *
 * <p>좌표계는 원본 SVG와 같은 512×512다. 원본을 고칠 일이 생기면 패스 문자열만
 * 그대로 옮겨 오면 된다.</p>
 */
public final class ForgeMark extends Group {

    /** 원본 SVG의 viewBox 한 변. */
    private static final double VIEWBOX = 512;

    // ── 원본 forgeOS-logo.svg 에서 가져온 패스 데이터 ──

    private static final String BASE = "M 120 370 L 392 370 L 420 415 L 92 415 Z";
    private static final String BODY = "M 190 280 L 322 280 L 340 370 L 172 370 Z";
    private static final String STEEL_LEFT = "M 80 180 L 256 180 L 256 280 L 150 280 L 60 205 Z";
    private static final String STEEL_RIGHT = "M 256 180 L 432 180 L 452 225 L 372 280 L 256 280 Z";
    private static final String STEEL_TOP = "M 80 180 L 150 135 L 452 135 L 432 180 Z";
    private static final String CIRCUIT = "M 105 210 L 150 240 L 205 240";
    private static final String WINDOW_BACK = "M 330 200 L 380 200 L 360 225 L 310 225 Z";
    private static final String WINDOW_FRONT = "M 290 225 L 340 225 L 320 250 L 270 250 Z";
    private static final String FLAME =
            "M 256 60 C 295 110, 340 150, 315 220 C 290 190, 275 205, 256 225 "
            + "C 237 205, 222 190, 197 220 C 172 150, 217 110, 256 60 Z";
    private static final String FLAME_CORE =
            "M 256 115 C 275 150, 295 170, 282 205 C 268 192, 256 200, 256 210 "
            + "C 256 200, 244 192, 230 205 C 217 170, 237 150, 256 115 Z";

    /**
     * 지정한 크기의 마크를 만든다.
     *
     * @param size          마크의 목표 한 변(px)
     * @param withAtmosphere 뒤쪽 발광 원반을 함께 그릴지. 배경화면처럼 넓은 자리에서는
     *                       켜고, 작은 아이콘 자리에서는 꺼야 뭉개지지 않는다
     */
    public ForgeMark(double size, boolean withAtmosphere) {
        Group content = new Group();

        if (withAtmosphere) {
            content.getChildren().addAll(
                    glow(256, 245, 130, "mark-glow-ember", 34),
                    glow(256, 245, 70, "mark-glow-violet", 26));
        }

        content.getChildren().addAll(
                filled(BASE, "mark-base"),
                filled(BODY, "mark-body"),
                filled(STEEL_LEFT, "mark-steel-left"),
                filled(STEEL_RIGHT, "mark-steel-right"),
                filled(STEEL_TOP, "mark-steel-top"),
                stroked(CIRCUIT, "mark-circuit"),
                dot(205, 240, 4.5, "mark-circuit-node"),
                stroked(WINDOW_BACK, "mark-window-back"),
                stroked(WINDOW_FRONT, "mark-window-front"),
                dot(282, 235, 3, "mark-dot-close"),
                dot(292, 235, 3, "mark-dot-minimize"),
                dot(302, 235, 3, "mark-dot-zoom"),
                filled(FLAME, "mark-flame"),
                filled(FLAME_CORE, "mark-flame-core"),
                dot(215, 70, 4, "mark-spark-gold"),
                dot(295, 50, 4.5, "mark-spark-violet"),
                dot(260, 25, 3.5, "mark-spark-white"));

        double factor = size / VIEWBOX;
        content.setScaleX(factor);
        content.setScaleY(factor);
        getChildren().add(content);
        setMouseTransparent(true);
    }

    private static SVGPath filled(String path, String styleClass) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.getStyleClass().add(styleClass);
        return svg;
    }

    private static SVGPath stroked(String path, String styleClass) {
        SVGPath svg = filled(path, styleClass);
        svg.setFill(null);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        return svg;
    }

    private static Circle dot(double x, double y, double radius, String styleClass) {
        Circle circle = new Circle(x, y, radius);
        circle.getStyleClass().add(styleClass);
        return circle;
    }

    /**
     * 발광 원반. 흐림 반경은 CSS로 표현할 수 없어서
     * ({@code -fx-effect}는 그림자 계열만 지원한다) 여기서 직접 건다.
     *
     * <p><b>1.1.1</b> — 반경을 34/26으로 낮췄다. 가우시안 흐림의 비용은 반경에
     * 비례해서 커지는데, 원반이 이미 반투명한 색이라 반경을 더 키워도 눈에 보이는
     * 것은 거의 달라지지 않았다. 배경화면은 비트맵으로 캐시되지만, 창 크기를 바꿀
     * 때마다 다시 굽는 값이므로 싼 편이 낫다.</p>
     */
    private static Circle glow(double x, double y, double radius, String styleClass, double blur) {
        Circle circle = dot(x, y, radius, styleClass);
        circle.setEffect(new GaussianBlur(blur));
        return circle;
    }
}
