package forgeos.wm;

import forgeos.ui.Motion;
import forgeos.ui.SpringValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
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

    private double x;
    private double y;
    private double w;
    private double h;

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

        springX = new SpringValue(value -> {
            x = value;
            applyBounds();
        });
        springY = new SpringValue(value -> {
            y = value;
            applyBounds();
        });
        springW = new SpringValue(value -> {
            w = value;
            applyBounds();
        });
        springH = new SpringValue(value -> {
            h = value;
            applyBounds();
        });

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

    private void applyBounds() {
        setLayoutX(x);
        setLayoutY(y);
        setPrefSize(w, h);
        resize(w, h);
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

        titleBar.setOnMouseReleased(e -> settleAfterDrag());
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
