package forgeos.wm;

import forgeos.ui.Motion;
import forgeos.ui.SpringValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * ForgeOS의 앱 창 하나. JavaFX {@code Stage}가 아니라 데스크탑 위에 떠 있는
 * 순수 노드다 — 이것이 Custom MDI의 전부다.
 *
 * <h2>왜 Stage 가 아닌가</h2>
 * <p>{@code Stage}를 여러 개 띄우면 창은 공짜로 얻지만 OS의 창 관리 방식이
 * 그대로 노출된다. 우리가 만드는 것은 "운영체제 안의 운영체제"이므로 창은
 * 반드시 데스크탑 영역 안에 갇혀 있어야 하고, Dock으로 빨려 들어가야 하고,
 * 배경 유리 재질 위에 겹쳐 보여야 한다. 전부 노드일 때만 가능한 것들이다.</p>
 *
 * <h2>좌표계</h2>
 * <p>{@code managed = false}로 두고 위치·크기를 직접 관리한다. 부모
 * ({@link WindowManager})가 레이아웃에 관여하면 스프링이 쓴 값을 다음 펄스에
 * 되돌려 버린다.</p>
 *
 * <h2>1.1.1 — 창을 옮기는 것은 레이아웃이 아니다</h2>
 * <p>1.1.0 까지는 x·y·폭·높이 중 <b>무엇이 바뀌든</b> {@code setPrefSize} 와
 * {@code resize} 를 함께 불렀다. 그래서 창을 옆으로 1px 미는 동안에도 창 안의
 * 표·툴바·사이드바가 통째로 다시 배치됐다. 표가 든 창을 끌 때 눈에 띄게
 * 끊기던 원인이 이것이다.</p>
 *
 * <p>이제 위치와 크기는 갈라져 있다({@link #commitBounds()}). 위치만 바뀌면
 * {@code layoutX/Y} 만 건드리므로 자식은 자기가 움직였다는 사실조차 모른다.
 * 크기가 실제로 바뀐 프레임에만 레이아웃이 돈다.</p>
 *
 * <p>여기에 더해, <b>움직이는 동안에는 창을 비트맵으로 캐시한다</b>. 창에는
 * 반경 30px 짜리 그림자가 걸려 있어서, 캐시가 없으면 프레임마다 그 흐림을 다시
 * 계산한다. 이동은 캐시된 그림을 옮기는 것으로 충분하다.</p>
 */
public final class ForgeWindow extends VBox {

    /** 창의 최소 폭. 이보다 좁으면 툴바가 무너진다. */
    private static final double MIN_WIDTH = 420;

    /** 창의 최소 높이. */
    private static final double MIN_HEIGHT = 260;

    /** 가장자리에서 리사이즈로 인식하는 두께(px). */
    private static final double RESIZE_MARGIN = 6;

    /** 창을 화면 밖으로 끌고 나갈 때 최소한 남겨 두는 폭(px). 완전히 잃어버리면 되찾을 수 없다. */
    private static final double KEEP_VISIBLE = 120;

    /** 최소화됐을 때 Dock 으로 빨려 들어가는 최종 크기(px). */
    private static final double MINIMIZED_SIZE = 56;

    /** 창 상태. */
    public enum State {
        /** 일반. */
        NORMAL,
        /** 작업 영역 전체로 확대됨. */
        ZOOMED,
        /** Dock 으로 최소화됨. */
        MINIMIZED
    }

    private final WindowManager manager;
    private final String appId;
    private final HBox titleBar = new HBox();
    private final StackPane contentHolder = new StackPane();
    private final Label titleLabel = new Label();

    private final SpringValue springX;
    private final SpringValue springY;
    private final SpringValue springW;
    private final SpringValue springH;

    /**
     * 스프링 넷이 <b>공유하는</b> 프레임 후처리.
     *
     * <p>반드시 인스턴스 하나여야 한다. 스프링마다 {@code this::commitBounds} 를
     * 새로 만들어 넘기면 서로 다른 객체가 되어 프레임당 네 번 실행되고, 이 클래스가
     * 없애려던 문제가 그대로 돌아온다.</p>
     */
    private final Runnable boundsCommit = this::commitBounds;

    private double x;
    private double y;
    private double w;
    private double h;

    /** 마지막으로 실제 레이아웃에 반영한 크기. 이 값과 같으면 다시 배치하지 않는다. */
    private double appliedW = -1;
    private double appliedH = -1;


    private State state = State.NORMAL;
    private double[] restoreBounds;

    // ── 드래그 상태 ──
    private double grabOffsetX;
    private double grabOffsetY;
    private double dragVelocityX;
    private double dragVelocityY;
    private double lastDragX;
    private double lastDragY;
    private long lastDragNanos;

    // ── 리사이즈 상태 ──
    private boolean resizeNorth;
    private boolean resizeSouth;
    private boolean resizeEast;
    private boolean resizeWest;
    private boolean resizing;
    private double resizeAnchorX;
    private double resizeAnchorY;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;

    ForgeWindow(WindowManager manager, String appId, String title, Node content,
                double width, double height) {
        this.manager = manager;
        this.appId = appId;
        this.w = width;
        this.h = height;

        getStyleClass().add("forge-window");
        setManaged(false);

        // sink 는 값만 받아 적는다. 화면 반영은 프레임당 한 번, commitBounds 가 한다.
        springX = new SpringValue(value -> x = value).onFrame(boundsCommit);
        springY = new SpringValue(value -> y = value).onFrame(boundsCommit);
        springW = new SpringValue(value -> w = value).onFrame(boundsCommit);
        springH = new SpringValue(value -> h = value).onFrame(boundsCommit);

        buildTitleBar(title);

        contentHolder.getStyleClass().add("window-content");
        contentHolder.getChildren().add(content);
        VBox.setVgrow(contentHolder, Priority.ALWAYS);

        getChildren().addAll(titleBar, contentHolder);

        installFocusOnPress();
        installResizeHandlers();
    }

    private void buildTitleBar(String title) {
        titleLabel.setText(title);
        titleLabel.getStyleClass().add("window-title");

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        TrafficLights lights = new TrafficLights(
                () -> manager.close(this),
                () -> manager.minimize(this),
                this::toggleZoom);

        titleBar.getStyleClass().add("window-titlebar");
        titleBar.getChildren().addAll(lights, leftSpacer, titleLabel, rightSpacer);

        // 신호등 폭만큼 오른쪽에도 자리를 비워야 제목이 진짜 가운데에 온다.
        Region balance = new Region();
        balance.getStyleClass().add("titlebar-balance");
        titleBar.getChildren().add(balance);

        installDragHandlers();
    }

    // ────────────────────────────── 위치와 크기 ──────────────────────────────

    void placeAt(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        springX.reset(newX);
        springY.reset(newY);
        springW.reset(w);
        springH.reset(h);
        applyBounds();
    }

    /**
     * 위치와 크기를 화면에 반영한다.
     *
     * <p>크기는 <b>실제로 달라졌을 때만</b> 건드린다. {@code setPrefSize} 와
     * {@code resize} 는 창 안쪽 전체의 레이아웃을 다시 돌리게 만드는 호출이라,
     * 값이 그대로인데 부르면 매 프레임 헛일을 시키는 셈이 된다.</p>
     */
    private void applyBounds() {
        setLayoutX(x);
        setLayoutY(y);
        if (w != appliedW || h != appliedH) {
            appliedW = w;
            appliedH = h;
            setPrefSize(w, h);
            resize(w, h);
        }
    }

    /**
     * 스프링이 움직인 프레임의 후처리. 넷이 공유하므로 프레임당 정확히 한 번 돈다.
     *
     * <p>스프링이 전부 멎었으면 여기서 캐시도 걷는다 — 정지 콜백을 네 군데 걸면
     * 어느 것이 마지막인지 매번 따져야 하지만, "지금 움직이는 스프링이 있는가"는
     * 언제 물어도 답이 하나다.</p>
     */
    private void commitBounds() {
        applyBounds();
        if (!isSpringing()) {
            setMotionCache(false);
        }
    }

    private boolean isSpringing() {
        return springX.isActive() || springY.isActive()
                || springW.isActive() || springH.isActive();
    }

    /**
     * 움직이는 동안만 창을 비트맵으로 캐시한다.
     *
     * <p>창에는 큰 가우시안 그림자가 걸려 있다. 캐시가 없으면 창이 1px 움직일
     * 때마다 그 흐림을 다시 굽는다. 반대로 <b>크기가 바뀌는 동안에는 켜면 안 된다</b> —
     * 매 프레임 캐시가 무효화되어 굽는 비용만 한 번 더 드는 꼴이 되기 때문이다.</p>
     *
     * <p>"지금 켜져 있는가"를 따로 기억하지 않는다. {@link Motion} 의 여닫기 전환도
     * 같은 플래그를 건드리기 때문에, 기억해 둔 값과 실제 상태가 어긋날 수 있다.
     * 프로퍼티에 같은 값을 다시 넣는 것은 어차피 아무 일도 하지 않으므로,
     * 상태를 하나 더 두는 것보다 매번 그냥 쓰는 편이 안전하다.</p>
     */
    private void setMotionCache(boolean on) {
        setCacheHint(on ? CacheHint.SPEED : CacheHint.DEFAULT);
        setCache(on);
    }

    /**
     * 이 창이 차지하는 사각형.
     *
     * @return {@code [x, y, width, height]}
     */
    public double[] bounds() {
        return new double[]{x, y, w, h};
    }

    /**
     * 이 창을 띄운 앱의 식별자.
     *
     * @return 앱 ID
     */
    public String appId() {
        return appId;
    }

    /**
     * 제목 표시줄 텍스트.
     *
     * @return 제목
     */
    public String title() {
        return titleLabel.getText();
    }

    /**
     * 현재 창 상태.
     *
     * @return 상태
     */
    public State state() {
        return state;
    }

    // ────────────────────────────── 드래그 ──────────────────────────────

    private void installDragHandlers() {
        titleBar.setOnMousePressed(e -> {
            if (resizing) {
                return;
            }
            manager.focus(this);
            // 끄는 동안 그림자를 매 프레임 다시 굽지 않도록 창을 비트맵으로 굳힌다.
            setMotionCache(true);
            Point2D local = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            // 잡은 지점을 기억한다. 중심으로 스냅시키면 손에서 창이 튀는 느낌이 난다.
            grabOffsetX = local.getX() - x;
            grabOffsetY = local.getY() - y;
            lastDragX = local.getX();
            lastDragY = local.getY();
            lastDragNanos = System.nanoTime();
            dragVelocityX = 0;
            dragVelocityY = 0;
            e.consume();
        });

        titleBar.setOnMouseDragged(e -> {
            if (resizing) {
                return;
            }
            if (state == State.ZOOMED) {
                // 확대된 창을 끌면 원래 크기로 돌아오면서 손에 붙는다.
                unzoomForDrag(e);
                return;
            }
            Point2D local = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            trackVelocity(local);

            x = softClampX(local.getX() - grabOffsetX);
            y = softClampY(local.getY() - grabOffsetY);
            applyBounds();
            e.consume();
        });

        titleBar.setOnMouseReleased(e -> {
            settleAfterDrag();
            // 경계 밖이면 스프링이 돌아오는 중이므로 캐시를 유지한다.
            // 그 경우의 해제는 commitBounds 가 맡는다.
            if (!isSpringing()) {
                setMotionCache(false);
            }
        });
        titleBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleZoom();
            }
        });
    }

    private void trackVelocity(Point2D local) {
        long now = System.nanoTime();
        double dt = (now - lastDragNanos) / 1_000_000_000.0;
        if (dt > 0.001) {
            dragVelocityX = (local.getX() - lastDragX) / dt;
            dragVelocityY = (local.getY() - lastDragY) / dt;
            lastDragX = local.getX();
            lastDragY = local.getY();
            lastDragNanos = now;
        }
    }

    private void unzoomForDrag(MouseEvent e) {
        double[] restore = restoreBounds;
        state = State.NORMAL;
        restoreBounds = null;
        if (restore != null) {
            w = restore[2];
            h = restore[3];
        }
        Point2D local = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
        // 확대 상태에서 잡은 상대 위치를 축소된 창에서도 유지한다.
        grabOffsetX = Math.min(w - 40, w * 0.5);
        grabOffsetY = Math.max(0, Math.min(titleBar.getHeight(), 18));
        x = local.getX() - grabOffsetX;
        y = local.getY() - grabOffsetY;
        springW.reset(w);
        springH.reset(h);
        applyBounds();
    }

    private void settleAfterDrag() {
        Bounds area = manager.workArea();
        double minX = area.getMinX() - (w - KEEP_VISIBLE);
        double maxX = area.getMaxX() - KEEP_VISIBLE;
        double minY = area.getMinY();
        double maxY = area.getMaxY() - titleBar.getHeight();

        boolean outX = x < minX || x > maxX;
        boolean outY = y < minY || y > maxY;

        // 경계 안이면 아무것도 하지 않는다. 창은 던지는 물건이 아니므로 관성으로
        // 미끄러지면 안 된다. 경계를 벗어났을 때만 속도를 이어받아 되돌아온다.
        if (outX) {
            springX.reset(x);
            springX.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_PLAYFUL);
            springX.handoffVelocity(dragVelocityX);
            springX.setTarget(clamp(x, minX, maxX));
        }
        if (outY) {
            springY.reset(y);
            springY.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_PLAYFUL);
            springY.handoffVelocity(dragVelocityY);
            springY.setTarget(clamp(y, minY, maxY));
        }
    }

    private double softClampX(double raw) {
        Bounds area = manager.workArea();
        double minX = area.getMinX() - (w - KEEP_VISIBLE);
        double maxX = area.getMaxX() - KEEP_VISIBLE;
        if (raw < minX) {
            return minX - Motion.rubberband(minX - raw, area.getWidth());
        }
        if (raw > maxX) {
            return maxX + Motion.rubberband(raw - maxX, area.getWidth());
        }
        return raw;
    }

    private double softClampY(double raw) {
        Bounds area = manager.workArea();
        double minY = area.getMinY();
        double maxY = area.getMaxY() - titleBar.getHeight();
        if (raw < minY) {
            return minY - Motion.rubberband(minY - raw, area.getHeight());
        }
        if (raw > maxY) {
            return maxY + Motion.rubberband(raw - maxY, area.getHeight());
        }
        return raw;
    }

    // ────────────────────────────── 리사이즈 ──────────────────────────────

    private void installResizeHandlers() {
        addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (state == State.ZOOMED) {
                setCursor(Cursor.DEFAULT);
                return;
            }
            updateResizeZone(e.getX(), e.getY());
            setCursor(cursorForZone());
        });

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (state == State.ZOOMED) {
                return;
            }
            updateResizeZone(e.getX(), e.getY());
            if (!inResizeZone()) {
                return;
            }
            manager.focus(this);
            resizing = true;
            // 크기가 매 프레임 달라지므로 캐시는 굽는 비용만 늘린다.
            setMotionCache(false);
            Point2D local = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            resizeAnchorX = local.getX();
            resizeAnchorY = local.getY();
            resizeStartX = x;
            resizeStartY = y;
            resizeStartW = w;
            resizeStartH = h;
            e.consume();
        });

        addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!resizing) {
                return;
            }
            Point2D local = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            double dx = local.getX() - resizeAnchorX;
            double dy = local.getY() - resizeAnchorY;

            if (resizeEast) {
                w = Math.max(MIN_WIDTH, resizeStartW + dx);
            }
            if (resizeSouth) {
                h = Math.max(MIN_HEIGHT, resizeStartH + dy);
            }
            if (resizeWest) {
                double candidate = Math.max(MIN_WIDTH, resizeStartW - dx);
                x = resizeStartX + (resizeStartW - candidate);
                w = candidate;
            }
            if (resizeNorth) {
                double candidate = Math.max(MIN_HEIGHT, resizeStartH - dy);
                y = resizeStartY + (resizeStartH - candidate);
                h = candidate;
            }
            applyBounds();
            e.consume();
        });

        addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (resizing) {
                resizing = false;
                springX.reset(x);
                springY.reset(y);
                springW.reset(w);
                springH.reset(h);
            }
        });

        setOnMouseExited(e -> {
            if (!resizing) {
                setCursor(Cursor.DEFAULT);
            }
        });
    }

    private void updateResizeZone(double localX, double localY) {
        resizeWest = localX <= RESIZE_MARGIN;
        resizeEast = localX >= w - RESIZE_MARGIN;
        resizeNorth = localY <= RESIZE_MARGIN;
        resizeSouth = localY >= h - RESIZE_MARGIN;
    }

    private boolean inResizeZone() {
        return resizeNorth || resizeSouth || resizeEast || resizeWest;
    }

    private Cursor cursorForZone() {
        if (resizeNorth && resizeWest) {
            return Cursor.NW_RESIZE;
        }
        if (resizeNorth && resizeEast) {
            return Cursor.NE_RESIZE;
        }
        if (resizeSouth && resizeWest) {
            return Cursor.SW_RESIZE;
        }
        if (resizeSouth && resizeEast) {
            return Cursor.SE_RESIZE;
        }
        if (resizeNorth) {
            return Cursor.N_RESIZE;
        }
        if (resizeSouth) {
            return Cursor.S_RESIZE;
        }
        if (resizeWest) {
            return Cursor.W_RESIZE;
        }
        if (resizeEast) {
            return Cursor.E_RESIZE;
        }
        return Cursor.DEFAULT;
    }

    // ────────────────────────────── 상태 전환 ──────────────────────────────

    private void installFocusOnPress() {
        // 필터로 잡아야 내용물(버튼·표)이 이벤트를 먹어도 포커스는 먼저 옮겨진다.
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> manager.focus(this));
    }

    /** 전체화면 ↔ 원래 크기를 오간다. */
    public void toggleZoom() {
        if (state == State.ZOOMED) {
            double[] restore = restoreBounds;
            restoreBounds = null;
            state = State.NORMAL;
            if (restore != null) {
                springTo(restore[0], restore[1], restore[2], restore[3]);
            }
            return;
        }
        restoreBounds = new double[]{x, y, w, h};
        state = State.ZOOMED;
        Bounds area = manager.workArea();
        springTo(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
    }

    void minimizeTo(Point2D dockPoint, Runnable onDone) {
        if (state != State.ZOOMED) {
            restoreBounds = new double[]{x, y, w, h};
        }
        state = State.MINIMIZED;

        springTo(dockPoint.getX() - MINIMIZED_SIZE / 2,
                dockPoint.getY() - MINIMIZED_SIZE / 2,
                MINIMIZED_SIZE,
                MINIMIZED_SIZE);
        // 완료 통지는 페이드 한 곳에서만 한다. 스프링 정지에도 걸면 두 번 불린다.
        Motion.fadeOut(this, Duration.millis(260), onDone);
    }

    void restoreFromMinimized() {
        double[] restore = restoreBounds;
        restoreBounds = null;
        state = State.NORMAL;
        setVisible(true);
        Motion.fadeIn(this, Duration.millis(200), null);
        if (restore != null) {
            springTo(restore[0], restore[1], restore[2], restore[3]);
        }
    }

    private void springTo(double targetX, double targetY, double targetW, double targetH) {
        // 크기가 함께 변하는 전환(확대·최소화)은 어차피 매 프레임 레이아웃이 돈다.
        // 그런 프레임에서 비트맵까지 다시 구우면 느려지기만 한다.
        boolean sizeChanges = targetW != w || targetH != h;
        setMotionCache(!sizeChanges);

        springX.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);
        springY.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);
        springW.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);
        springH.tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);

        springX.setTarget(targetX);
        springY.setTarget(targetY);
        springW.setTarget(targetW);
        springH.setTarget(targetH);
    }

    /** 작업 영역이 바뀌었을 때(창 크기 변경) 확대된 창을 다시 맞춘다. */
    void onWorkAreaChanged() {
        if (state == State.ZOOMED) {
            Bounds area = manager.workArea();
            springTo(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
        }
    }

    private static double clamp(double value, double min, double max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
