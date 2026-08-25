package forgeos.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * ForgeOS의 모든 아이콘. 전부 24×24 좌표계의 SVG 패스다.
 *
 * <p>비트맵을 쓰지 않는 이유는 두 가지다. Dock 확대는 아이콘을 2배 가까이
 * 키우는데 래스터 이미지는 그 순간 뭉개진다. 그리고 색을 CSS에서 바꿀 수 있어야
 * 활성/비활성, 정상/경고 상태를 자바 코드에서 칠하지 않아도 된다.</p>
 *
 * <p>선 두께·색은 CSS의 {@code .glyph} 클래스에서 정한다. 여기서는 모양만 정의한다.</p>
 */
public final class Glyphs {

    /** 아이콘이 그려진 기준 정사각형의 한 변. */
    private static final double VIEWBOX = 24;

    /** Terminal — 프롬프트 꺾쇠와 커서 밑줄. */
    public static final String TERMINAL = "M6 8.5L10 12L6 15.5 M13 16H18";

    /** Activity Monitor — 심전도 파형. */
    public static final String ACTIVITY = "M3 12H7L10 5L14 19L17 12H21";

    /** Finder — 서류철. */
    public static final String FINDER = "M3 8.5A2 2 0 0 1 5 6.5H9.2L11.2 9H19A2 2 0 0 1 21 11V17"
            + "A2 2 0 0 1 19 19H5A2 2 0 0 1 3 17Z";

    /** Deadlock Resolver — 서로를 기다리는 세 노드. */
    public static final String DEADLOCK = "M6.5 6.5m-2.6 0a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0"
            + " M17.5 6.5m-2.6 0a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0"
            + " M12 18m-2.6 0a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0"
            + " M9.1 6.5H14.9 M16.3 8.9L13.4 15.6 M10.6 15.6L7.7 8.9";

    /** Forge 마크 — 모루 위의 불꽃. */
    public static final String FORGE = "M12 3C12 3 8.5 6.6 8.5 10.2C8.5 12.2 10 13.6 12 13.6"
            + "C14 13.6 15.5 12.2 15.5 10.2C15.5 8.6 14.4 7.3 14.4 7.3"
            + "C14.4 8.7 13.6 9.6 12.8 9.6C12 9.6 11.6 8.9 11.6 8.1C11.6 6.4 12 3 12 3Z"
            + " M5 17H19 M7.5 17V20 M16.5 17V20";

    /** 폴더(파일 목록용). */
    public static final String FOLDER = "M3.5 8A1.6 1.6 0 0 1 5.1 6.4H9L10.8 8.6H18.9"
            + "A1.6 1.6 0 0 1 20.5 10.2V16.4A1.6 1.6 0 0 1 18.9 18H5.1A1.6 1.6 0 0 1 3.5 16.4Z";

    /** 파일(문서). */
    public static final String FILE = "M6.5 3.6H14L18 7.6V20.4H6.5Z M14 3.6V7.6H18";

    /** 오른쪽 꺾쇠 — 컬럼 뷰에서 "더 들어갈 수 있다"는 표시. */
    public static final String CHEVRON_RIGHT = "M10 8L14 12L10 16";

    /** 더하기. */
    public static final String PLUS = "M12 6V18 M6 12H18";

    /** 정지(프로세스 종료). */
    public static final String STOP = "M8 8H16V16H8Z";

    /** 새로고침. */
    public static final String REFRESH = "M20 12A8 8 0 1 1 17.6 6.4 M17.6 6.4V10.4H13.6";

    /** 해 — 라이트 테마. */
    public static final String SUN = "M12 5.4V3 M12 21v-2.4 M18.6 12H21 M3 12h2.4"
            + " M16.7 7.3L18.4 5.6 M5.6 18.4L7.3 16.7 M16.7 16.7L18.4 18.4 M5.6 5.6L7.3 7.3"
            + " M12 8.2a3.8 3.8 0 1 0 0 7.6a3.8 3.8 0 1 0 0-7.6";

    /** 달 — 다크 테마. */
    public static final String MOON = "M20.2 14.8A8.6 8.6 0 0 1 9.2 3.8A8.7 8.7 0 1 0 20.2 14.8Z";

    /** Notepad — 줄이 그어진 종이와 연필. */
    public static final String NOTEPAD = "M6.4 3.8H14L17.6 7.4V12"
            + " M6.4 3.8V20.2H12 M14 3.8V7.4H17.6"
            + " M9.2 9.6H13.6 M9.2 12.8H12.4"
            + " M20.4 13.6L21.8 15L16.2 20.6L13.8 21.2L14.4 18.8Z";

    /** Firefox — 자오선이 그려진 지구본. WebKit 이 그리는 웹이 여기서 열린다. */
    public static final String BROWSER = "M12 3.4a8.6 8.6 0 1 0 0 17.2a8.6 8.6 0 1 0 0-17.2"
            + " M3.4 12H20.6"
            + " M12 3.4c2.6 2.4 4 5.4 4 8.6s-1.4 6.2-4 8.6c-2.6-2.4-4-5.4-4-8.6s1.4-6.2 4-8.6";

    /** 저장 — 받침 위로 내려앉는 화살표. */
    public static final String SAVE = "M12 4.6V14.6 M8.4 11.2L12 14.8L15.6 11.2"
            + " M5.6 17V18.6A1.4 1.4 0 0 0 7 20H17A1.4 1.4 0 0 0 18.4 18.6V17";

    /** fork — 한 줄기에서 갈라져 나온 가지. */
    public static final String FORK = "M7.5 5.6m-2.2 0a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0-4.4 0"
            + " M16.5 18.4m-2.2 0a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0-4.4 0"
            + " M7.5 7.8V12.2A3 3 0 0 0 10.5 15.2H13.5A3 3 0 0 1 16.5 18.2";

    /** disk.img — 겹쳐 쌓인 원통. 영속화된 파일 시스템을 가리킨다. */
    public static final String DISK = "M12 4.4C15.9 4.4 19 5.5 19 6.9S15.9 9.4 12 9.4"
            + "S5 8.3 5 6.9S8.1 4.4 12 4.4Z"
            + " M5 6.9V17.1C5 18.5 8.1 19.6 12 19.6S19 18.5 19 17.1V6.9"
            + " M5 12C5 13.4 8.1 14.5 12 14.5S19 13.4 19 12";

    /** 뒤로. */
    public static final String ARROW_LEFT = "M14.6 6.4L9 12L14.6 17.6";

    /** 앞으로. */
    public static final String ARROW_RIGHT = "M9.4 6.4L15 12L9.4 17.6";

    /** 홈 — 브라우저 시작 페이지. */
    public static final String HOME = "M4.4 11.2L12 4.6L19.6 11.2"
            + " M6.6 9.6V19.4H17.4V9.6 M10.2 19.4V14.4H13.8V19.4";

    /** 닫기(탭·패널). 신호등의 그것과 모양은 같지만 쓰임이 달라 따로 둔다. */
    public static final String CLOSE = "M7.6 7.6L16.4 16.4 M16.4 7.6L7.6 16.4";

    /** 자물쇠 — 주소창의 https 표시. */
    public static final String LOCK = "M8.2 10.6V8.4A3.8 3.8 0 0 1 15.8 8.4V10.6"
            + " M6.8 10.6H17.2V18.8H6.8Z";

    /** 신호등 — 닫기. */
    public static final String LIGHT_CLOSE = "M8.6 8.6L15.4 15.4 M15.4 8.6L8.6 15.4";

    /** 신호등 — 최소화. */
    public static final String LIGHT_MINIMIZE = "M7.6 12H16.4";

    /** 신호등 — 전체화면. */
    public static final String LIGHT_ZOOM = "M8 14V16H10 M16 10V8H14";

    private Glyphs() {
    }

    /**
     * 선(stroke)으로 그리는 아이콘 노드를 만든다.
     *
     * @param path        24×24 좌표계의 SVG 패스
     * @param size        최종 한 변 길이(px)
     * @param styleClasses 적용할 CSS 클래스
     * @return 크기가 맞춰진 아이콘 노드
     */
    public static Node stroked(String path, double size, String... styleClasses) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(null);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.getStyleClass().add("glyph");
        svg.getStyleClass().addAll(styleClasses);
        return scaled(svg, size);
    }

    /**
     * 면(fill)으로 채우는 아이콘 노드를 만든다.
     *
     * @param path        24×24 좌표계의 SVG 패스
     * @param size        최종 한 변 길이(px)
     * @param styleClasses 적용할 CSS 클래스
     * @return 크기가 맞춰진 아이콘 노드
     */
    public static Node filled(String path, double size, String... styleClasses) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setStroke(null);
        svg.getStyleClass().add("glyph-filled");
        svg.getStyleClass().addAll(styleClasses);
        return scaled(svg, size);
    }

    private static Node scaled(SVGPath svg, double size) {
        double factor = size / VIEWBOX;
        svg.setScaleX(factor);
        svg.setScaleY(factor);

        // Group 으로 한 겹 감싸야 스케일이 레이아웃 크기에 반영된다.
        // SVGPath 에 직접 scale 을 주면 그림만 작아지고 자리는 24px 그대로 차지한다.
        return new Group(svg);
    }
}
