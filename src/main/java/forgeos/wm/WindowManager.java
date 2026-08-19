package forgeos.wm;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.core.KernelService;
import forgeos.ui.Motion;
import forgeos.ui.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 열려 있는 창 전부를 관리하는 레이어이자, ForgeOS의 "창 관리 시스템" 그 자체.
 *
 * <h2>앱 하나에 창 하나</h2>
 * <p>같은 앱을 Dock에서 다시 누르면 새 창이 아니라 기존 창이 앞으로 온다.
 * 커널 상태를 보여 주는 앱들이라 같은 화면을 두 개 띄울 이유가 없고, 그보다
 * 사용자가 "아까 그 창"을 찾는 비용이 훨씬 크다.</p>
 *
 * <h2>작업 영역</h2>
 * <p>{@link #workArea()}는 메뉴바와 Dock을 제외한 사각형이다. 창을 확대하거나
 * 드래그 경계를 계산할 때 전부 이 값을 기준으로 한다. 데스크탑이 값을 넣어
 * 주므로 창 관리자는 메뉴바·Dock의 존재를 알 필요가 없다.</p>
 */
public final class WindowManager extends Pane {

    /** 새 창을 이전 창에서 얼마나 어긋나게 놓을지(px). 완전히 겹치면 뒤 창이 사라진 줄 안다. */
    private static final double CASCADE_STEP = 30;

    /** 계단식 배치가 몇 번 반복되면 처음으로 돌아가는지. */
    private static final int CASCADE_WRAP = 6;

    /** 창이 열릴 때의 시작 스케일. 1에 가까울수록 "커지는" 게 아니라 "도착하는" 느낌이 난다. */
    private static final double OPEN_SCALE = 0.94;

    private final AppContext context;
    private final Map<String, ForgeWindow> openWindows = new LinkedHashMap<>();
    private final Map<ForgeWindow, Runnable> disposers = new HashMap<>();
    private final ObservableList<String> runningAppIds = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<ForgeWindow> activeWindow = new ReadOnlyObjectWrapper<>();
    private final ObjectProperty<Bounds> workArea =
            new SimpleObjectProperty<>(new BoundingBox(0, 0, 1280, 800));

    private Function<String, Point2D> dockAnchor = id -> new Point2D(getWidth() / 2, getHeight());
    private int cascadeIndex;

    /**
     * 창 관리자를 만든다.
     *
     * @param kernelService 앱들에게 전달할 커널 서비스
     */
    public WindowManager(KernelService kernelService) {
        this.context = new AppContext(kernelService, this);
        getStyleClass().add("window-layer");
        // 레이어 자체는 배경이 없다. 빈 곳을 클릭하면 아래(바탕화면)로 통과해야 한다.
        setPickOnBounds(false);

        workArea.addListener((obs, old, now) -> openWindows.values().forEach(ForgeWindow::onWorkAreaChanged));
    }

    /**
     * 앱을 연다. 이미 열려 있으면 앞으로 가져오고, 최소화돼 있으면 복원한다.
     *
     * @param app 실행할 앱
     */
    public void open(ForgeApp app) {
        ForgeWindow existing = openWindows.get(app.id());
        if (existing != null) {
            if (existing.state() == ForgeWindow.State.MINIMIZED) {
                existing.restoreFromMinimized();
            }
            focus(existing);
            return;
        }

        AppInstance instance = app.launch(context);
        ForgeWindow window = new ForgeWindow(
                this, app.id(), app.title(), instance.view(),
                app.preferredWidth(), app.preferredHeight());

        window.placeAt(nextX(app.preferredWidth()), nextY(app.preferredHeight()));

        openWindows.put(app.id(), window);
        disposers.put(window, instance.dispose());
        runningAppIds.add(app.id());
        getChildren().add(window);

        focus(window);
        Motion.materialize(window, OPEN_SCALE, Duration.millis(240));
    }

    /**
     * Dock 아이콘을 눌렀을 때의 동작 — 열려 있으면 토글, 아니면 실행.
     *
     * @param app 대상 앱
     */
    public void toggle(ForgeApp app) {
        ForgeWindow window = openWindows.get(app.id());
        if (window == null) {
            open(app);
            return;
        }
        if (window.state() == ForgeWindow.State.MINIMIZED) {
            window.restoreFromMinimized();
            focus(window);
            return;
        }
        if (activeWindow.get() == window) {
            minimize(window);
            return;
        }
        focus(window);
    }

    /**
     * 창을 최상단으로 올리고 활성 창으로 표시한다.
     *
     * @param window 대상 창
     */
    public void focus(ForgeWindow window) {
        if (activeWindow.get() == window && window.getParent() != null) {
            window.toFront();
            return;
        }
        ForgeWindow previous = activeWindow.get();
        if (previous != null) {
            previous.pseudoClassStateChanged(Styles.ACTIVE, false);
        }
        window.toFront();
        window.pseudoClassStateChanged(Styles.ACTIVE, true);
        activeWindow.set(window);
    }

    /**
     * 창을 닫고 앱의 정리 작업을 실행한다.
     *
     * @param window 대상 창
     */
    public void close(ForgeWindow window) {
        Motion.dematerialize(window, OPEN_SCALE, Duration.millis(180), () -> {
            getChildren().remove(window);
            openWindows.remove(window.appId());
            runningAppIds.remove(window.appId());

            Runnable dispose = disposers.remove(window);
            if (dispose != null) {
                dispose.run();
            }
            if (activeWindow.get() == window) {
                activeWindow.set(null);
                focusTopMost();
            }
        });
    }

    /**
     * 창을 Dock으로 최소화한다.
     *
     * @param window 대상 창
     */
    public void minimize(ForgeWindow window) {
        if (window.state() == ForgeWindow.State.MINIMIZED) {
            return;
        }
        Point2D anchor = dockAnchor.apply(window.appId());
        window.minimizeTo(anchor, () -> {
            if (activeWindow.get() == window) {
                activeWindow.set(null);
                focusTopMost();
            }
        });
    }

    private void focusTopMost() {
        for (int i = getChildren().size() - 1; i >= 0; i--) {
            if (getChildren().get(i) instanceof ForgeWindow candidate
                    && candidate.isVisible()
                    && candidate.state() != ForgeWindow.State.MINIMIZED) {
                focus(candidate);
                return;
            }
        }
    }

    private double nextX(double width) {
        Bounds area = workArea.get();
        double base = area.getMinX() + Math.max(24, (area.getWidth() - width) / 2 - 60);
        return base + (cascadeIndex % CASCADE_WRAP) * CASCADE_STEP;
    }

    private double nextY(double height) {
        Bounds area = workArea.get();
        double base = area.getMinY() + Math.max(16, (area.getHeight() - height) / 2 - 40);
        double result = base + (cascadeIndex % CASCADE_WRAP) * CASCADE_STEP;
        cascadeIndex++;
        return result;
    }

    /**
     * 메뉴바와 Dock을 제외한 창 배치 가능 영역.
     *
     * @return 작업 영역 사각형
     */
    public Bounds workArea() {
        return workArea.get();
    }

    /**
     * 작업 영역 프로퍼티. 데스크탑이 크기 변화에 맞춰 갱신한다.
     *
     * @return 작업 영역 프로퍼티
     */
    public ObjectProperty<Bounds> workAreaProperty() {
        return workArea;
    }

    /**
     * 현재 활성 창.
     *
     * @return 활성 창 프로퍼티 (없으면 {@code null})
     */
    public ReadOnlyObjectProperty<ForgeWindow> activeWindowProperty() {
        return activeWindow.getReadOnlyProperty();
    }

    /**
     * 실행 중인 앱 ID 목록. Dock의 실행 표시등이 이 목록을 본다.
     *
     * @return 관측 가능한 앱 ID 목록
     */
    public ObservableList<String> runningAppIds() {
        return runningAppIds;
    }

    /**
     * 최소화 애니메이션이 향할 지점을 알려 주는 함수를 등록한다.
     *
     * <p>Dock이 자기 아이콘의 화면 좌표를 알고 있으므로 Dock이 넣어 준다.
     * 창 관리자가 Dock의 내부 구조를 아는 것보다 이쪽이 결합이 얕다.</p>
     *
     * @param anchor 앱 ID → 창 레이어 좌표계의 목표점
     */
    public void setDockAnchor(Function<String, Point2D> anchor) {
        this.dockAnchor = anchor;
    }

    /**
     * 활성 창을 닫는다. 단축키(Cmd/Ctrl+W)에서 호출한다.
     */
    public void closeActive() {
        ForgeWindow window = activeWindow.get();
        if (window != null) {
            close(window);
        }
    }

    /**
     * 활성 창을 최소화한다. 단축키(Cmd/Ctrl+M)에서 호출한다.
     */
    public void minimizeActive() {
        ForgeWindow window = activeWindow.get();
        if (window != null) {
            minimize(window);
        }
    }
}
