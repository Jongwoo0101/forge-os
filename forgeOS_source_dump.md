# ForgeOS Source Dump

총 소스 파일 수 : **39개**

- 모듈 : `forgeOS` (os)
- 포함 확장자 : `.java`, `.css`

---

## Files

- `src/main/java/forgeos/ForgeOsApp.java`
- `src/main/java/forgeos/Launcher.java`
- `src/main/java/forgeos/app/AppCatalog.java`
- `src/main/java/forgeos/app/AppContext.java`
- `src/main/java/forgeos/app/AppInstance.java`
- `src/main/java/forgeos/app/ForgeApp.java`
- `src/main/java/forgeos/app/deadlock/DeadlockResolverApp.java`
- `src/main/java/forgeos/app/deadlock/DeadlockResolverView.java`
- `src/main/java/forgeos/app/deadlock/WaitForGraphView.java`
- `src/main/java/forgeos/app/finder/FinderApp.java`
- `src/main/java/forgeos/app/finder/FinderView.java`
- `src/main/java/forgeos/app/monitor/ActivityMonitorApp.java`
- `src/main/java/forgeos/app/monitor/ActivityMonitorView.java`
- `src/main/java/forgeos/app/monitor/DonutChart.java`
- `src/main/java/forgeos/app/terminal/TerminalApp.java`
- `src/main/java/forgeos/app/terminal/TerminalView.java`
- `src/main/java/forgeos/boot/BootConsole.java`
- `src/main/java/forgeos/boot/BootSequence.java`
- `src/main/java/forgeos/boot/BootVideo.java`
- `src/main/java/forgeos/core/KernelService.java`
- `src/main/java/forgeos/desktop/DesktopPane.java`
- `src/main/java/forgeos/desktop/DockView.java`
- `src/main/java/forgeos/desktop/MenuBarView.java`
- `src/main/java/forgeos/desktop/Wallpaper.java`
- `src/main/java/forgeos/ui/ForgeMark.java`
- `src/main/java/forgeos/ui/Glyphs.java`
- `src/main/java/forgeos/ui/Motion.java`
- `src/main/java/forgeos/ui/SpringValue.java`
- `src/main/java/forgeos/ui/Styles.java`
- `src/main/java/forgeos/ui/ThemeManager.java`
- `src/main/java/forgeos/ui/ToggleSwitch.java`
- `src/main/java/forgeos/wm/ForgeWindow.java`
- `src/main/java/forgeos/wm/TrafficLights.java`
- `src/main/java/forgeos/wm/WindowManager.java`
- `src/main/java/module-info.java`
- `src/main/resources/forgeos/css/apps.css`
- `src/main/resources/forgeos/css/theme.css`
- `src/main/resources/forgeos/css/tokens-dark.css`
- `src/main/resources/forgeos/css/tokens-light.css`

---

# 1. ForgeOsApp.java

**Path**
`src/main/java/forgeos/ForgeOsApp.java`

```java
package forgeos;

import forgeos.app.AppCatalog;
import forgeos.boot.BootSequence;
import forgeos.core.KernelService;
import forgeos.desktop.DesktopPane;
import forgeos.ui.Motion;
import forgeos.ui.ThemeManager;
import forgeos.wm.WindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * ForgeOS 애플리케이션.
 *
 * <p>이 클래스가 하는 일은 세 가지뿐이다. 창을 하나 만들고, 부팅 시퀀스를
 * 돌리고, 부팅이 끝나면 데스크탑으로 갈아 끼운다. 나머지는 전부
 * {@link BootSequence}와 {@link DesktopPane}이 가져간다.</p>
 *
 * <h2>장면 전환</h2>
 * <p>{@code Scene}을 바꾸지 않고 같은 루트 안에서 레이어를 교체한다. Scene을
 * 갈아 끼우면 창이 한 프레임 깜빡이고 스타일시트가 다시 적용되면서 화면이
 * 튄다. 시네마틱 부팅의 마지막 순간에 그 깜빡임이 보이면 앞의 연출이 전부
 * 무의미해진다.</p>
 */
public final class ForgeOsApp extends Application {

    /** 창 초기 크기. 데스크탑 환경이므로 좁으면 창을 겹칠 수가 없다. */
    private static final double INITIAL_WIDTH = 1440;

    /** 창 초기 높이. */
    private static final double INITIAL_HEIGHT = 900;

    private final KernelService kernelService = new KernelService();
    private final StackPane root = new StackPane();

    private ThemeManager theme;

    /** JavaFX 런타임이 리플렉션으로 호출한다. */
    public ForgeOsApp() {
    }

    @Override
    public void start(Stage stage) {
        root.getStyleClass().add("forge-root");

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);

        // 스타일시트는 ThemeManager 가 소유한다. 토큰 파일을 갈아 끼우는 것이
        // 곧 테마 전환이므로, 목록을 두 곳에서 건드리면 반드시 어긋난다.
        theme = new ThemeManager(scene);

        BootSequence boot = new BootSequence(kernelService, () -> enterDesktop(scene));
        root.getChildren().add(boot);

        stage.setTitle("ForgeOS");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(e -> kernelService.shutdown());
        stage.show();

        boot.start();
    }

    private void enterDesktop(Scene scene) {
        DesktopPane desktop = new DesktopPane(
                kernelService, AppCatalog.defaults(), theme, this::quit);
        desktop.setOpacity(0);
        root.getChildren().add(desktop);

        Motion.fadeIn(desktop, Motion.FADE_SCENE, () -> {
            // 부팅 레이어는 완전히 덮인 다음에 치운다. 먼저 지우면 검은 화면이 한 번 스친다.
            root.getChildren().removeIf(node -> node instanceof BootSequence);
        });

        installShortcuts(scene, desktop.windowManager());
    }

    /**
     * 창 단축키. macOS의 Command, 그 외 플랫폼의 Ctrl에 모두 대응하도록
     * {@link KeyCombination#SHORTCUT_DOWN}을 쓴다.
     */
    private void installShortcuts(Scene scene, WindowManager windowManager) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
                windowManager::closeActive);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN),
                windowManager::minimizeActive);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN,
                        KeyCombination.SHIFT_DOWN),
                theme::toggle);
    }

    private void quit() {
        kernelService.shutdown();
        Platform.exit();
    }

    @Override
    public void stop() {
        kernelService.shutdown();
    }
}
```

---

# 2. Launcher.java

**Path**
`src/main/java/forgeos/Launcher.java`

```java
package forgeos;

import javafx.application.Application;

/**
 * ForgeOS 진입점.
 *
 * <p>{@link ForgeOsApp}을 직접 {@code main} 클래스로 지정하지 않는 이유가 있다.
 * JVM은 메인 클래스가 {@code Application}의 서브클래스이면 JavaFX 런타임을 먼저
 * 확인하는데, 모듈 경로에 JavaFX가 없으면 "JavaFX runtime components are missing"
 * 한 줄만 남기고 죽는다. 한 겹 감싸 두면 그 검사를 우회해서 클래스패스 실행도
 * 가능해지고, 실패하더라도 실제 스택 트레이스를 볼 수 있다.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * ForgeOS를 기동한다.
     *
     * @param args JavaFX에 그대로 전달되는 실행 인자
     */
    public static void main(String[] args) {
        Application.launch(ForgeOsApp.class, args);
    }
}
```

---

# 3. AppCatalog.java

**Path**
`src/main/java/forgeos/app/AppCatalog.java`

```java
package forgeos.app;

import forgeos.app.deadlock.DeadlockResolverApp;
import forgeos.app.finder.FinderApp;
import forgeos.app.monitor.ActivityMonitorApp;
import forgeos.app.terminal.TerminalApp;

import java.util.List;

/**
 * ForgeOS에 내장된 앱 목록.
 *
 * <p>목록의 <b>순서가 곧 Dock의 순서</b>다. 터미널이 맨 앞인 이유는 이 시뮬레이터에서
 * 모든 것이 결국 명령어로 되기 때문이고, 교착 상태 관리자가 맨 뒤인 이유는 앞의
 * 셋으로 상황을 만든 다음에야 쓸 일이 생기기 때문이다. Dock은 사용 빈도 순이
 * 아니라 <b>작업 순서</b>대로 놓여 있을 때 길잡이가 된다.</p>
 */
public final class AppCatalog {

    private AppCatalog() {
    }

    /**
     * 기본 앱 목록.
     *
     * @return 순서가 보장된 불변 목록
     */
    public static List<ForgeApp> defaults() {
        return List.of(
                new TerminalApp(),
                new ActivityMonitorApp(),
                new FinderApp(),
                new DeadlockResolverApp());
    }
}
```

---

# 4. AppContext.java

**Path**
`src/main/java/forgeos/app/AppContext.java`

```java
package forgeos.app;

import forgeos.core.KernelService;
import forgeos.wm.WindowManager;

/**
 * 앱이 바깥 세계에 닿는 유일한 통로.
 *
 * <p>앱이 커널이나 창 관리자를 전역 정적 변수로 집어 오면 테스트도 못 하고,
 * 어느 앱이 무엇을 건드리는지도 알 수 없게 된다. 필요한 것만 생성자로 준다.</p>
 *
 * @param kernel  커널 서비스 (시스템 콜, 로그, 주기 갱신)
 * @param windows 창 관리자 (다른 앱 열기, 작업 영역 조회)
 */
public record AppContext(KernelService kernel, WindowManager windows) {
}
```

---

# 5. AppInstance.java

**Path**
`src/main/java/forgeos/app/AppInstance.java`

```java
package forgeos.app;

import javafx.scene.Node;

/**
 * 실행된 앱 하나 — 화면과 뒷정리 한 쌍.
 *
 * <p>{@code dispose}를 옵션으로 두지 않은 이유가 있다. 앱은 거의 예외 없이
 * {@code KernelService.onRefresh}를 구독하는데, 창을 닫을 때 해지하지 않으면
 * 이미 화면에서 사라진 표를 계속 갱신하게 된다. 창을 스무 번 여닫으면 초당
 * 스무 번의 유령 갱신이 남는다. 반환 타입에 강제로 넣어 두면 잊을 수가 없다.</p>
 *
 * @param view    창 안에 들어갈 화면
 * @param dispose 창이 닫힐 때 실행할 정리 작업 (할 일이 없으면 빈 람다)
 */
public record AppInstance(Node view, Runnable dispose) {
}
```

---

# 6. ForgeApp.java

**Path**
`src/main/java/forgeos/app/ForgeApp.java`

```java
package forgeos.app;

/**
 * ForgeOS 데스크탑에서 실행할 수 있는 앱.
 *
 * <p>앱은 창을 만들지 않는다. 내용물만 만들고, 창·신호등·드래그·최소화는
 * {@code WindowManager}가 전부 가져간다. 앱 하나를 추가할 때 창 관리 코드를
 * 다시 쓰지 않아도 되게 하려는 경계다.</p>
 */
public interface ForgeApp {

    /**
     * 앱 식별자. Dock과 창 관리자가 "같은 앱"을 판단하는 기준이다.
     *
     * @return 소문자 식별자
     */
    String id();

    /**
     * 창 제목 표시줄과 Dock 툴팁에 쓰이는 이름.
     *
     * @return 사람이 읽는 이름
     */
    String title();

    /**
     * Dock 아이콘으로 쓸 SVG 패스 (24×24 좌표계).
     *
     * @return {@code forgeos.ui.Glyphs}의 상수 중 하나
     */
    String iconPath();

    /**
     * 창을 처음 열 때의 폭.
     *
     * @return 픽셀
     */
    double preferredWidth();

    /**
     * 창을 처음 열 때의 높이.
     *
     * @return 픽셀
     */
    double preferredHeight();

    /**
     * 앱 화면을 만든다. 창이 열릴 때마다 한 번 호출된다.
     *
     * @param context 커널 접근과 창 관리자
     * @return 화면과 정리 작업
     */
    AppInstance launch(AppContext context);
}
```

---

# 7. DeadlockResolverApp.java

**Path**
`src/main/java/forgeos/app/deadlock/DeadlockResolverApp.java`

```java
package forgeos.app.deadlock;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * 교착 상태 관리자 — 은행원 알고리즘 설정과 Wait-For 그래프.
 *
 * <p>이 앱은 커널의 Phase 6 기능(자원 할당, 회피, 탐지, 복구)을 눈으로 보게
 * 만드는 것이 목적이다. 터미널에서 {@code res_req}를 치는 것과 기능은 같지만,
 * 회피(banker)를 껐다 켰을 때 무엇이 달라지는지는 그림으로 봐야 안다.</p>
 */
public final class DeadlockResolverApp implements ForgeApp {

    @Override
    public String id() {
        return "deadlock-resolver";
    }

    @Override
    public String title() {
        return "교착 상태 관리자";
    }

    @Override
    public String iconPath() {
        return Glyphs.DEADLOCK;
    }

    @Override
    public double preferredWidth() {
        return 1000;
    }

    @Override
    public double preferredHeight() {
        return 600;
    }

    @Override
    public AppInstance launch(AppContext context) {
        DeadlockResolverView view = new DeadlockResolverView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 8. DeadlockResolverView.java

**Path**
`src/main/java/forgeos/app/deadlock/DeadlockResolverView.java`

```java
package forgeos.app.deadlock;

import forgeframework.deadlock.BankerConfigDto;
import forgeframework.deadlock.DeadlockDetectDto;
import forgeframework.deadlock.DeadlockRecoverDto;
import forgeframework.deadlock.ResourceRowDto;
import forgeframework.deadlock.ResourceSnapshotDto;
import forgeframework.deadlock.VictimPolicy;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Styles;
import forgeos.ui.ToggleSwitch;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 교착 상태 관리자 화면.
 *
 * <h2>화면 구성의 근거</h2>
 * <p>왼쪽은 <b>숫자</b>(Allocation / Max / Need), 오른쪽은 <b>관계</b>(누가 누구를
 * 기다리는가)다. 은행원 알고리즘은 행렬을 보는 알고리즘이고 교착 탐지는 그래프를
 * 보는 알고리즘이라, 둘을 나란히 두면 같은 상황을 두 방식으로 동시에 볼 수 있다.</p>
 *
 * <h2>토글은 커널의 값을 되비춘다</h2>
 * <p>스위치를 UI 상태로 들고 있으면 터미널에서 {@code banker off}를 쳤을 때
 * 화면과 커널이 어긋난다. 스위치는 명령을 보내기만 하고, 표시는 항상 커널이
 * 돌려준 {@link BankerConfigDto}를 따른다.</p>
 */
final class DeadlockResolverView extends BorderPane {

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ToggleSwitch bankerSwitch = new ToggleSwitch();
    private final ComboBox<VictimPolicy> policyBox = new ComboBox<>();
    private final Label availableLabel = new Label();
    private final Label statusLabel = new Label("대기 중");

    private final ObservableList<ResourceRowDto> rows = FXCollections.observableArrayList();
    private final TableView<ResourceRowDto> table = new TableView<>(rows);
    private final WaitForGraphView graph = new WaitForGraphView();

    private final TextField pidField = new TextField();
    private final TextField r1Field = new TextField();
    private final TextField r2Field = new TextField();
    private final TextField r3Field = new TextField();

    /** 스위치·콤보를 코드로 되돌릴 때 리스너가 다시 커널을 부르지 않도록 막는 빗장. */
    private boolean syncingSwitch;
    private boolean syncingPolicy;

    DeadlockResolverView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("deadlock-resolver");

        buildTable();

        setTop(buildToolbar());
        setLeft(buildLeftPanel());
        setCenter(graph);
        setBottom(buildRequestBar());

        this.subscription = kernelService.onRefresh(this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private HBox buildToolbar() {
        Label bankerCaption = new Label("은행원 알고리즘 (회피)");
        bankerCaption.getStyleClass().add("toolbar-caption");

        bankerSwitch.selectedProperty().addListener((obs, was, now) -> {
            if (syncingSwitch) {
                return;
            }
            SystemCallResult result = kernelService.call(
                    SystemCallType.BANKER, now ? "on" : "off");
            report(result, now ? "회피를 켰습니다." : "회피를 껐습니다.");
            refresh();
        });

        policyBox.getItems().setAll(VictimPolicy.values());
        policyBox.setConverter(new PolicyConverter());
        policyBox.getStyleClass().add("policy-box");
        policyBox.setOnAction(e -> {
            VictimPolicy policy = policyBox.getValue();
            if (policy == null || syncingPolicy) {
                return;
            }
            SystemCallResult result = kernelService.call(
                    SystemCallType.BANKER, "policy", policy.name().toLowerCase());
            report(result, "희생자 선정 정책을 바꿨습니다.");
        });

        Button detect = new Button("탐지");
        detect.getStyleClass().add("toolbar-button");
        detect.setOnAction(e -> runDetect());

        Button recover = new Button("복구");
        recover.getStyleClass().add("toolbar-button");
        recover.pseudoClassStateChanged(Styles.DANGER, true);
        recover.setOnAction(e -> runRecover());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().add("toolbar-status");

        HBox bar = new HBox(bankerSwitch, bankerCaption,
                new Label("희생자"), policyBox, detect, recover, spacer, statusLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTable() {
        TableColumn<ResourceRowDto, String> process = new TableColumn<>("프로세스");
        process.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                "P%d %s".formatted(cell.getValue().pid(), cell.getValue().name())));
        process.setPrefWidth(120);

        table.getColumns().add(process);
        table.getColumns().add(vectorColumn("Allocation", ResourceRowDto::allocation));
        table.getColumns().add(vectorColumn("Max", ResourceRowDto::max));
        table.getColumns().add(vectorColumn("Need", ResourceRowDto::need));

        TableColumn<ResourceRowDto, String> pending = new TableColumn<>("대기 요청");
        pending.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().blocked() ? format(cell.getValue().pendingRequest()) : "—"));
        pending.setPrefWidth(96);

        table.getColumns().add(pending);
        table.getStyleClass().add("resource-table");
        table.setPlaceholder(new Label("등록된 프로세스가 없습니다.\n터미널에서 exec 로 프로세스를 만들어 보세요."));
    }

    private TableColumn<ResourceRowDto, String> vectorColumn(
            String title, java.util.function.Function<ResourceRowDto, List<Integer>> extractor) {
        TableColumn<ResourceRowDto, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(format(extractor.apply(cell.getValue()))));
        column.getStyleClass().add("column-numeric");
        column.setPrefWidth(92);
        return column;
    }

    private VBox buildLeftPanel() {
        availableLabel.getStyleClass().add("available-chip");
        availableLabel.setWrapText(true);

        VBox panel = new VBox(availableLabel, table);
        panel.getStyleClass().add("deadlock-left");
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    private HBox buildRequestBar() {
        pidField.setPromptText("PID");
        pidField.getStyleClass().addAll("field", "field-narrow");
        r1Field.setPromptText("R1");
        r1Field.getStyleClass().addAll("field", "field-narrow");
        r2Field.setPromptText("R2");
        r2Field.getStyleClass().addAll("field", "field-narrow");
        r3Field.setPromptText("R3");
        r3Field.getStyleClass().addAll("field", "field-narrow");

        Button max = new Button("최대 선언");
        max.getStyleClass().add("toolbar-button");
        max.setOnAction(e -> sendVector(SystemCallType.RES_MAX, "최대 요구량을 선언했습니다."));

        Button request = new Button("요청");
        request.getStyleClass().add("toolbar-button");
        request.setOnAction(e -> sendVector(SystemCallType.RES_REQ, "자원을 요청했습니다."));

        Button release = new Button("반납");
        release.getStyleClass().add("toolbar-button");
        release.setOnAction(e -> sendVector(SystemCallType.RES_FREE, "자원을 반납했습니다."));

        Label hint = new Label("은행원 알고리즘은 Max 선언이 있어야 의미가 있습니다. 최대 선언 → 요청 순서로 시도해 보세요.");
        hint.getStyleClass().add("request-hint");

        HBox bar = new HBox(new Label("PID"), pidField,
                new Label("벡터"), r1Field, r2Field, r3Field,
                max, request, release, hint);
        bar.getStyleClass().addAll("toolbar", "request-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void sendVector(SystemCallType type, String successMessage) {
        String pid = pidField.getText().trim();
        if (pid.isEmpty()) {
            statusLabel.setText("PID를 입력하세요.");
            return;
        }
        List<String> args = new ArrayList<>();
        args.add(pid);
        args.add(valueOrZero(r1Field));
        args.add(valueOrZero(r2Field));
        args.add(valueOrZero(r3Field));

        SystemCallResult result = kernelService.call(type, args.toArray(new String[0]));
        report(result, successMessage);
        refresh();
    }

    private void runDetect() {
        SystemCallResult result = kernelService.call(SystemCallType.DETECT);
        if (!result.isSuccess()) {
            report(result, "");
            return;
        }
        DeadlockDetectDto dto = result.dataAs(DeadlockDetectDto.class);
        statusLabel.setText(dto.hasDeadlock()
                ? "교착 발견 — 얽힌 프로세스 " + dto.deadlockedPids()
                : "교착 없음 (검사한 프로세스 %d개)".formatted(dto.inspected()));
        applyDetection(dto);
    }

    private void runRecover() {
        SystemCallResult result = kernelService.call(SystemCallType.RECOVER);
        if (!result.isSuccess()) {
            report(result, "");
            return;
        }
        DeadlockRecoverDto dto = result.dataAs(DeadlockRecoverDto.class);
        if (dto.victims().isEmpty()) {
            statusLabel.setText("복구할 교착이 없습니다.");
        } else {
            statusLabel.setText("희생 %s · 회수 %s · 깨어남 %s".formatted(
                    dto.victims(), dto.reclaimed(), dto.wokenPids()));
        }
        refresh();
    }

    private void refresh() {
        SystemCallResult info = kernelService.call(SystemCallType.RES_INFO);
        if (!info.isSuccess()) {
            return;
        }
        ResourceSnapshotDto snapshot = info.dataAs(ResourceSnapshotDto.class);
        rows.setAll(snapshot.rows());

        availableLabel.setText("가용 %s / 전체 %s  ·  자원 %s".formatted(
                format(snapshot.available()),
                format(snapshot.total()),
                String.join(" ", snapshot.labels())));

        syncingSwitch = true;
        bankerSwitch.syncSilently(snapshot.bankerEnabled());
        syncingSwitch = false;

        if (policyBox.getValue() != snapshot.victimPolicy()) {
            syncingPolicy = true;
            policyBox.setValue(snapshot.victimPolicy());
            syncingPolicy = false;
        }

        SystemCallResult detect = kernelService.call(SystemCallType.DETECT);
        if (detect.isSuccess()) {
            applyDetection(detect.dataAs(DeadlockDetectDto.class));
        }
    }

    private void applyDetection(DeadlockDetectDto dto) {
        Set<Integer> deadlocked = new HashSet<>(dto.deadlockedPids());
        graph.update(rows, dto.waitForEdges(), deadlocked);
    }

    private void report(SystemCallResult result, String successMessage) {
        statusLabel.setText(result.isSuccess()
                ? (result.getMessage() == null || result.getMessage().isBlank()
                        ? successMessage : result.getMessage())
                : result.getMessage());
    }

    private static String valueOrZero(TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? "0" : text;
    }

    private static String format(List<Integer> vector) {
        return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(" ")) + "]";
    }

    void dispose() {
        subscription.cancel();
        graph.dispose();
    }

    /** 정책 enum 을 사람이 읽는 말로 바꾼다. {@code LOWEST_ALLOCATION} 은 설명이 아니다. */
    private static final class PolicyConverter extends StringConverter<VictimPolicy> {
        @Override
        public String toString(VictimPolicy policy) {
            if (policy == null) {
                return "";
            }
            return switch (policy) {
                case LOWEST_ALLOCATION -> "적게 가진 쪽";
                case HIGHEST_ALLOCATION -> "많이 가진 쪽";
                case YOUNGEST_FIRST -> "가장 나중에 만들어진 쪽";
            };
        }

        @Override
        public VictimPolicy fromString(String text) {
            return VictimPolicy.parse(text);
        }
    }
}
```

---

# 9. WaitForGraphView.java

**Path**
`src/main/java/forgeos/app/deadlock/WaitForGraphView.java`

```java
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
```

---

# 10. FinderApp.java

**Path**
`src/main/java/forgeos/app/finder/FinderApp.java`

```java
package forgeos.app.finder;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * Finder — 커널 파일 시스템을 macOS 다단 컬럼 뷰로 탐색하는 앱.
 *
 * <p>트리 뷰 대신 컬럼 뷰를 고른 이유는 이 파일 시스템이 <b>inode 기반</b>이기
 * 때문이다. 컬럼 뷰는 "지금 어느 디렉터리 안에 있는가"와 "그 디렉터리에 무엇이
 * 함께 있는가"를 동시에 보여 준다. 깊이 들어갈수록 경로가 가로로 쌓여서
 * 되짚어 나오기도 쉽다.</p>
 */
public final class FinderApp implements ForgeApp {

    @Override
    public String id() {
        return "finder";
    }

    @Override
    public String title() {
        return "Finder";
    }

    @Override
    public String iconPath() {
        return Glyphs.FINDER;
    }

    @Override
    public double preferredWidth() {
        return 900;
    }

    @Override
    public double preferredHeight() {
        return 520;
    }

    @Override
    public AppInstance launch(AppContext context) {
        FinderView view = new FinderView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 11. FinderView.java

**Path**
`src/main/java/forgeos/app/finder/FinderView.java`

```java
package forgeos.app.finder;

import forgeframework.filesystem.DirectoryEntryDto;
import forgeframework.filesystem.FileContentDto;
import forgeframework.filesystem.FileListDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Motion;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finder 화면 — 다단 컬럼 뷰.
 *
 * <h2>컬럼은 곧 경로다</h2>
 * <p>{@link #columnPaths}의 <i>i</i>번째 원소가 <i>i</i>번째 컬럼이 보여 주는
 * 디렉터리의 절대경로다. 디렉터리를 고르면 그 뒤 컬럼을 전부 잘라내고 새
 * 컬럼을 붙인다. 화면 상태와 경로 상태를 따로 관리하면 반드시 어긋나므로
 * 하나만 두고 화면은 거기서 파생시킨다.</p>
 */
final class FinderView extends BorderPane {

    /** 컬럼 하나의 폭(px). */
    private static final double COLUMN_WIDTH = 232;

    /** 루트 경로. */
    private static final String ROOT = "/";

    private final KernelService kernelService;

    private final HBox columns = new HBox();
    private final ScrollPane columnScroller = new ScrollPane(columns);
    private final List<String> columnPaths = new ArrayList<>();
    private final Label pathLabel = new Label(ROOT);
    private final VBox preview = new VBox();

    FinderView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("finder");

        columns.getStyleClass().add("finder-columns");
        columnScroller.getStyleClass().add("finder-scroll");
        columnScroller.setFitToHeight(true);
        columnScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        preview.getStyleClass().add("finder-preview");
        preview.setVisible(false);
        preview.setManaged(false);

        setTop(buildToolbar());
        setCenter(columnScroller);
        setRight(preview);

        pushColumn(ROOT);
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private HBox buildToolbar() {
        pathLabel.getStyleClass().add("finder-path");

        Button newFolder = new Button("새 폴더", Glyphs.stroked(Glyphs.FOLDER, 14, "button-glyph"));
        newFolder.getStyleClass().add("toolbar-button");
        newFolder.setOnAction(e -> create(SystemCallType.MKDIR, "새 폴더", "폴더 이름"));

        Button newFile = new Button("새 파일", Glyphs.stroked(Glyphs.FILE, 14, "button-glyph"));
        newFile.getStyleClass().add("toolbar-button");
        newFile.setOnAction(e -> create(SystemCallType.TOUCH, "새 파일", "파일 이름"));

        Button refresh = new Button("새로고침", Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));
        refresh.getStyleClass().add("toolbar-button");
        refresh.setOnAction(e -> reloadLastColumn());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(pathLabel, spacer, newFolder, newFile, refresh);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 컬럼 관리 ──────────────────────────────

    private void pushColumn(String path) {
        FileListDto listing = list(path);
        if (listing == null) {
            return;
        }

        ListView<DirectoryEntryDto> column = new ListView<>();
        column.getStyleClass().add("finder-column");
        column.setPrefWidth(COLUMN_WIDTH);
        column.setMinWidth(COLUMN_WIDTH);
        column.setCellFactory(view -> new EntryCell());
        column.getItems().setAll(listing.entries());
        column.setPlaceholder(new Label("비어 있음"));

        int index = columnPaths.size();
        column.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                onEntrySelected(index, path, entry);
            }
        });

        columnPaths.add(path);
        columns.getChildren().add(column);
        pathLabel.setText(path);

        Motion.fadeIn(column, Duration.millis(160), null);
        // 새 컬럼이 화면 밖에 생기면 사용자가 스크롤을 찾아야 한다. 따라가 준다.
        columnScroller.setHvalue(1);
    }

    private void onEntrySelected(int columnIndex, String parentPath, DirectoryEntryDto entry) {
        truncateColumnsAfter(columnIndex);

        String childPath = join(parentPath, entry.name());
        if ("DIRECTORY".equals(entry.type())) {
            hidePreview();
            pushColumn(childPath);
            return;
        }
        pathLabel.setText(childPath);
        showPreview(parentPath, entry);
    }

    private void truncateColumnsAfter(int columnIndex) {
        while (columnPaths.size() > columnIndex + 1) {
            columnPaths.remove(columnPaths.size() - 1);
            columns.getChildren().remove(columns.getChildren().size() - 1);
        }
    }

    private void reloadLastColumn() {
        if (columnPaths.isEmpty()) {
            return;
        }
        String path = columnPaths.get(columnPaths.size() - 1);
        truncateColumnsAfter(columnPaths.size() - 2);
        pushColumn(path);
    }

    // ────────────────────────────── 미리보기 ──────────────────────────────

    private void showPreview(String parentPath, DirectoryEntryDto entry) {
        preview.getChildren().clear();

        Node icon = Glyphs.stroked(Glyphs.FILE, 52, "preview-icon");
        Label name = new Label(entry.name());
        name.getStyleClass().add("preview-name");
        Label meta = new Label("%s · %d바이트".formatted(entry.type(), entry.size()));
        meta.getStyleClass().add("preview-meta");

        Label body = new Label(readContent(parentPath, entry.name()));
        body.getStyleClass().add("preview-body");
        body.setWrapText(true);

        preview.getChildren().addAll(icon, name, meta, body);
        preview.setVisible(true);
        preview.setManaged(true);
        Motion.fadeIn(preview, Duration.millis(180), null);
    }

    private void hidePreview() {
        preview.setVisible(false);
        preview.setManaged(false);
    }

    private String readContent(String parentPath, String name) {
        SystemCallResult result = kernelService.call(SystemCallType.CAT, parentPath, name);
        if (!result.isSuccess()) {
            return result.getMessage();
        }
        FileContentDto dto = result.dataAs(FileContentDto.class);
        return dto.content().isEmpty() ? "(빈 파일)" : dto.content();
    }

    // ────────────────────────────── 생성 ──────────────────────────────

    private void create(SystemCallType type, String title, String prompt) {
        String parent = columnPaths.isEmpty() ? ROOT : columnPaths.get(columnPaths.size() - 1);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(parent + " 안에 만들 이름");
        dialog.setContentText(prompt);

        Optional<String> name = dialog.showAndWait();
        if (name.isEmpty() || name.get().isBlank()) {
            return;
        }
        SystemCallResult result = kernelService.call(type, parent, name.get().trim());
        if (result.isSuccess()) {
            reloadLastColumn();
        } else {
            pathLabel.setText(result.getMessage());
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private FileListDto list(String path) {
        SystemCallResult result = kernelService.call(SystemCallType.LS, path, ".");
        if (!result.isSuccess()) {
            pathLabel.setText(result.getMessage());
            return null;
        }
        return result.dataAs(FileListDto.class);
    }

    private static String join(String parent, String name) {
        return ROOT.equals(parent) ? ROOT + name : parent + "/" + name;
    }

    void dispose() {
        // Finder 는 주기 갱신을 구독하지 않는다. 파일 시스템은 사용자가 바꿀 때만 바뀐다.
    }

    /** 아이콘 + 이름 + (디렉터리면) 꺾쇠로 구성된 항목 셀. */
    private static final class EntryCell extends ListCell<DirectoryEntryDto> {
        @Override
        protected void updateItem(DirectoryEntryDto entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean directory = "DIRECTORY".equals(entry.type());

            Label name = new Label(entry.name());
            name.getStyleClass().add("entry-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(
                    Glyphs.stroked(directory ? Glyphs.FOLDER : Glyphs.FILE, 16, "entry-icon"),
                    name,
                    spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("entry-row");

            if (directory) {
                row.getChildren().add(Glyphs.stroked(Glyphs.CHEVRON_RIGHT, 14, "entry-chevron"));
            } else {
                Label size = new Label(entry.size() + "B");
                size.getStyleClass().add("entry-size");
                row.getChildren().add(size);
            }

            setGraphic(row);
            setText(null);
        }
    }
}
```

---

# 12. ActivityMonitorApp.java

**Path**
`src/main/java/forgeos/app/monitor/ActivityMonitorApp.java`

```java
package forgeos.app.monitor;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * 활성 상태 보기 — 프로세스 표와 메모리 게이지.
 *
 * <p>커널이 돌려주는 Record DTO({@code ProcessDto}, {@code MemorySnapshot})를
 * 문자열로 만들지 않고 그대로 {@code TableView}와 게이지에 넣는다. CLI가 텍스트로
 * 포맷하던 것을 GUI가 다시 파싱하는 순간, 포맷이 바뀔 때마다 화면이 깨진다.</p>
 */
public final class ActivityMonitorApp implements ForgeApp {

    @Override
    public String id() {
        return "activity-monitor";
    }

    @Override
    public String title() {
        return "활성 상태 보기";
    }

    @Override
    public String iconPath() {
        return Glyphs.ACTIVITY;
    }

    @Override
    public double preferredWidth() {
        return 920;
    }

    @Override
    public double preferredHeight() {
        return 540;
    }

    @Override
    public AppInstance launch(AppContext context) {
        ActivityMonitorView view = new ActivityMonitorView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 13. ActivityMonitorView.java

**Path**
`src/main/java/forgeos/app/monitor/ActivityMonitorView.java`

```java
package forgeos.app.monitor;

import forgeframework.memory.HeapSnapshot;
import forgeframework.memory.MemorySnapshot;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.process.SchedulerDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Styles;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 활성 상태 보기 화면.
 *
 * <h2>선택을 잃지 않기</h2>
 * <p>1초마다 표를 통째로 갈아 끼우면 사용자가 고른 행이 매초 풀린다. 프로세스를
 * 고르고 종료 버튼으로 손을 옮기는 사이에 선택이 사라지는 표는 쓸 수가 없다.
 * 그래서 갱신 전에 PID를 기억했다가 갱신 후 같은 PID를 다시 고른다.</p>
 */
final class ActivityMonitorView extends BorderPane {

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ObservableList<ProcessDto> processes = FXCollections.observableArrayList();
    private final TableView<ProcessDto> table = new TableView<>(processes);

    private final DonutChart frameChart = new DonutChart("물리 프레임", "accent-ember");
    private final DonutChart heapChart = new DonutChart("힙 사용량", "accent-gold");
    private final DonutChart tlbChart = new DonutChart("TLB 적중률", "accent-cyan");

    private final Label schedulerLabel = new Label("스케줄러 —");
    private final Label tlbDetailLabel = new Label();
    private final TextField processNameField = new TextField();
    private final TextField burstTimeField = new TextField();

    ActivityMonitorView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("activity-monitor");

        buildTable();

        BorderPane left = new BorderPane();
        left.getStyleClass().add("monitor-left");
        left.setTop(buildToolbar());
        left.setCenter(table);

        setCenter(left);
        setRight(buildSidebar());

        this.subscription = kernelService.onRefresh(this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private void buildTable() {
        TableColumn<ProcessDto, Integer> pid = new TableColumn<>("PID");
        pid.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().pid()));
        pid.getStyleClass().add("column-numeric");
        pid.setPrefWidth(64);

        TableColumn<ProcessDto, String> name = new TableColumn<>("이름");
        name.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().name()));
        name.setPrefWidth(160);

        TableColumn<ProcessDto, ProcessState> state = new TableColumn<>("상태");
        state.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().state()));
        state.setCellFactory(column -> new StateCell());
        state.setPrefWidth(110);

        TableColumn<ProcessDto, String> cpu = new TableColumn<>("CPU 사용");
        cpu.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().cpuTimeUsed() + " / " + cell.getValue().burstTime()));
        cpu.getStyleClass().add("column-numeric");
        cpu.setPrefWidth(110);

        TableColumn<ProcessDto, String> progress = new TableColumn<>("진행");
        progress.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(formatProgress(cell.getValue())));
        progress.setPrefWidth(130);

        table.getColumns().add(pid);
        table.getColumns().add(name);
        table.getColumns().add(state);
        table.getColumns().add(cpu);
        table.getColumns().add(progress);

        table.getStyleClass().add("process-table");
        table.setPlaceholder(new Label("실행 중인 프로세스가 없습니다.\n위에서 새 프로세스를 만들어 보세요."));
        // 리사이즈 정책은 기본값(UNCONSTRAINED)을 그대로 둔다. CONSTRAINED_RESIZE_POLICY 는
        // JavaFX 20에서 deprecated 되었고, 이 프로젝트는 -Werror 기준선을 지킨다.
        // 대신 마지막 컬럼이 남는 폭을 먹도록 넉넉한 pref 를 준다.
    }

    private HBox buildToolbar() {
        processNameField.setPromptText("프로세스 이름");
        processNameField.getStyleClass().add("field");
        burstTimeField.setPromptText("버스트");
        burstTimeField.getStyleClass().addAll("field", "field-narrow");

        Button exec = new Button("실행", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        exec.getStyleClass().add("toolbar-button");
        exec.setOnAction(e -> execProcess());

        Button kill = new Button("강제 종료", Glyphs.stroked(Glyphs.STOP, 14, "button-glyph"));
        kill.getStyleClass().add("toolbar-button");
        kill.pseudoClassStateChanged(Styles.DANGER, true);
        kill.setOnAction(e -> killSelected());
        kill.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        schedulerLabel.getStyleClass().add("toolbar-status");

        HBox bar = new HBox(processNameField, burstTimeField, exec, kill, spacer, schedulerLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildSidebar() {
        tlbDetailLabel.getStyleClass().add("sidebar-note");

        VBox sidebar = new VBox(frameChart, heapChart, tlbChart, tlbDetailLabel);
        sidebar.getStyleClass().add("monitor-sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);
        return sidebar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void execProcess() {
        String name = processNameField.getText().isBlank()
                ? "proc" + (processes.size() + 1)
                : processNameField.getText().trim();

        SystemCallResult result = burstTimeField.getText().isBlank()
                ? kernelService.call(SystemCallType.EXEC, name)
                : kernelService.call(SystemCallType.EXEC, name, burstTimeField.getText().trim());

        if (result.isSuccess()) {
            processNameField.clear();
            burstTimeField.clear();
        }
        refresh();
    }

    private void killSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        kernelService.call(SystemCallType.KILL, String.valueOf(selected.pid()));
        refresh();
    }

    private void refresh() {
        refreshProcesses();
        refreshMemory();
        refreshScheduler();
    }

    private void refreshProcesses() {
        SystemCallResult result = kernelService.call(SystemCallType.PS);
        if (!result.isSuccess()) {
            return;
        }
        List<ProcessDto> snapshot = result.dataAsList(ProcessDto.class);

        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        Integer selectedPid = selected == null ? null : selected.pid();

        processes.setAll(snapshot);

        if (selectedPid != null) {
            for (ProcessDto process : processes) {
                if (process.pid() == selectedPid) {
                    table.getSelectionModel().select(process);
                    break;
                }
            }
        }
    }

    private void refreshMemory() {
        SystemCallResult result = kernelService.call(SystemCallType.MEMINFO);
        if (!result.isSuccess()) {
            return;
        }
        MemorySnapshot snapshot = result.dataAs(MemorySnapshot.class);

        int totalFrames = snapshot.totalFrames();
        double frameRatio = totalFrames == 0 ? 0 : (double) snapshot.usedFrames() / totalFrames;
        frameChart.setValue(frameRatio,
                "%d / %d 프레임 · %dKB".formatted(
                        snapshot.usedFrames(), totalFrames, snapshot.frameSize()));

        long heapCapacity = 0;
        long heapUsed = 0;
        for (HeapSnapshot heap : snapshot.heapByPid().values()) {
            heapCapacity += heap.capacity();
            heapUsed += heap.used();
        }
        double heapRatio = heapCapacity == 0 ? 0 : (double) heapUsed / heapCapacity;
        heapChart.setValue(heapRatio,
                heapCapacity == 0
                        ? "할당된 힙 없음"
                        : "%d / %d · 프로세스 %d개".formatted(
                                heapUsed, heapCapacity, snapshot.heapByPid().size()));

        tlbChart.setValue(snapshot.tlbHitRatio(),
                "적중 %d · 실패 %d".formatted(snapshot.tlbHits(), snapshot.tlbMisses()));

        tlbDetailLabel.setText("TLB는 주소 변환 캐시입니다. 같은 페이지를 반복 접근할수록\n"
                + "적중률이 오릅니다 — 터미널에서 translate 를 여러 번 실행해 보세요.");
    }

    private void refreshScheduler() {
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER);
        if (!result.isSuccess()) {
            return;
        }
        SchedulerDto dto = result.dataAs(SchedulerDto.class);
        schedulerLabel.setText(dto.preemptive()
                ? "스케줄러 %s · 퀀텀 %d".formatted(dto.name(), dto.timeQuantum())
                : "스케줄러 %s · 비선점".formatted(dto.name()));
    }

    private static String formatProgress(ProcessDto process) {
        if (process.burstTime() <= 0) {
            return "—";
        }
        double ratio = Math.min(1, (double) process.cpuTimeUsed() / process.burstTime());
        int filled = (int) Math.round(ratio * 10);
        return "▰".repeat(filled) + "▱".repeat(10 - filled);
    }

    void dispose() {
        subscription.cancel();
    }

    /** 상태를 색 배지로 보여 주는 셀. 색은 CSS의 {@code .state-*} 클래스가 정한다. */
    private static final class StateCell extends TableCell<ProcessDto, ProcessState> {
        @Override
        protected void updateItem(ProcessState state, boolean empty) {
            super.updateItem(state, empty);
            if (empty || state == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(state.name());
            badge.getStyleClass().addAll("state-badge", "state-" + state.name().toLowerCase());
            setGraphic(badge);
            setText(null);
        }
    }
}
```

---

# 14. DonutChart.java

**Path**
`src/main/java/forgeos/app/monitor/DonutChart.java`

```java
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
```

---

# 15. TerminalApp.java

**Path**
`src/main/java/forgeos/app/terminal/TerminalApp.java`

```java
package forgeos.app.terminal;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * 터미널 — ForgeCLI의 명령어를 GUI 창 안에서 그대로 쓰는 앱.
 *
 * <p>명령어 해석은 한 줄도 새로 쓰지 않았다. {@code forgecli}의
 * {@code StandardCommands.createRegistry(...)}가 만들어 주는 레지스트리를
 * 그대로 쓴다. 그래서 CLI에 명령이 하나 추가되면 이 앱에도 자동으로 생긴다.</p>
 */
public final class TerminalApp implements ForgeApp {

    @Override
    public String id() {
        return "terminal";
    }

    @Override
    public String title() {
        return "터미널";
    }

    @Override
    public String iconPath() {
        return Glyphs.TERMINAL;
    }

    @Override
    public double preferredWidth() {
        return 780;
    }

    @Override
    public double preferredHeight() {
        return 480;
    }

    @Override
    public AppInstance launch(AppContext context) {
        TerminalView view = new TerminalView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 16. TerminalView.java

**Path**
`src/main/java/forgeos/app/terminal/TerminalView.java`

```java
package forgeos.app.terminal;

import forgeframework.cli.command.Command;
import forgeframework.cli.command.CommandRegistry;
import forgeframework.cli.command.StandardCommands;
import forgeframework.cli.shell.ShellContext;
import forgeframework.cli.shell.ShellPrompt;
import forgeframework.kernel.Kernel;
import forgeframework.syscall.SystemCallResult;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * 터미널 화면.
 *
 * <h2>출력 한도</h2>
 * <p>{@code tree}나 {@code help}를 반복 호출하면 {@link TextFlow}의 자식 노드가
 * 수만 개로 불어나고, 그때부터 스크롤이 눈에 띄게 버벅인다. TextFlow는 자식
 * 개수에 선형으로 느려지므로 상한을 두고 오래된 줄부터 버린다.</p>
 *
 * <h2>왜 TextArea 가 아닌가</h2>
 * <p>실패한 명령을 붉게, 프롬프트를 청록으로 칠하려면 조각마다 다른 스타일이
 * 필요하다. {@code TextArea}는 통짜 텍스트라 그게 안 된다.</p>
 */
final class TerminalView extends BorderPane {

    /** 화면에 유지하는 최대 텍스트 조각 수. */
    private static final int MAX_SEGMENTS = 4000;

    /** 되짚을 수 있는 명령 기록 수. */
    private static final int MAX_HISTORY = 200;

    private final KernelService kernelService;
    private final CommandRegistry registry;
    private final ShellContext shellContext = new ShellContext();
    private final ShellPrompt shellPrompt = new ShellPrompt(shellContext);

    private final TextFlow output = new TextFlow();
    private final ScrollPane scroller = new ScrollPane(output);
    private final TextField input = new TextField();
    private final Label promptLabel = new Label();

    private final List<String> history = new ArrayList<>();
    private int historyCursor;

    TerminalView(AppContext context) {
        this.kernelService = context.kernel();
        this.registry = StandardCommands.createRegistry(shellContext);

        getStyleClass().add("terminal");

        output.getStyleClass().add("terminal-output");
        scroller.getStyleClass().add("terminal-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        promptLabel.getStyleClass().add("terminal-prompt");
        input.getStyleClass().add("terminal-input");
        HBox.setHgrow(input, Priority.ALWAYS);

        HBox inputRow = new HBox(promptLabel, input);
        inputRow.getStyleClass().add("terminal-input-row");

        setCenter(scroller);
        setBottom(inputRow);

        input.setOnAction(e -> submit());
        input.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        // 창이 열리면 곧바로 입력을 받을 수 있어야 한다. 한 번 더 클릭하게 만들지 않는다.
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                Platform.runLater(input::requestFocus);
            }
        });

        printWelcome();
        refreshPrompt();
    }

    private void printWelcome() {
        append("ForgeFramework Shell에 오신 것을 환영합니다. 'help'를 입력해보세요.\n", "terminal-notice");
        append("↑/↓ 로 이전 명령, Tab 으로 명령어 자동 완성, clear 로 화면 정리.\n\n", "terminal-dim");
    }

    private void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> {
                recallHistory(-1);
                event.consume();
            }
            case DOWN -> {
                recallHistory(1);
                event.consume();
            }
            case TAB -> {
                complete();
                event.consume();
            }
            case L -> {
                if (event.isShortcutDown()) {
                    clear();
                    event.consume();
                }
            }
            default -> {
                // 나머지는 TextField 기본 동작.
            }
        }
    }

    private void submit() {
        String line = input.getText().trim();
        input.clear();
        if (line.isEmpty()) {
            return;
        }

        remember(line);
        append(shellPrompt.render(), "terminal-prompt-echo");
        append(line + "\n", "terminal-echo");

        if ("clear".equals(line)) {
            clear();
            return;
        }

        Kernel kernel = kernelService.kernel();
        if (kernel == null) {
            append("커널이 아직 준비되지 않았습니다.\n", "terminal-error");
            return;
        }

        SystemCallResult result = registry.dispatch(kernel, line);
        String message = result.getMessage();
        if (message != null && !message.isBlank()) {
            append(message + "\n", result.isSuccess() ? "terminal-result" : "terminal-error");
        }

        // shutdown 은 이 경로로도 실행될 수 있다. 데스크탑이 그 사실을 알아야 한다.
        kernelService.syncRunningState();
        refreshPrompt();
        scrollToBottom();
    }

    private void remember(String line) {
        if (history.isEmpty() || !history.get(history.size() - 1).equals(line)) {
            history.add(line);
            if (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
        historyCursor = history.size();
    }

    private void recallHistory(int direction) {
        if (history.isEmpty()) {
            return;
        }
        int next = historyCursor + direction;
        if (next < 0) {
            next = 0;
        }
        if (next >= history.size()) {
            historyCursor = history.size();
            input.clear();
            return;
        }
        historyCursor = next;
        input.setText(history.get(next));
        input.positionCaret(input.getText().length());
    }

    private void complete() {
        String typed = input.getText();
        if (typed.isBlank() || typed.contains(" ")) {
            return;
        }
        List<String> matches = registry.getAll().stream()
                .map(Command::name)
                .filter(name -> name.startsWith(typed))
                .toList();

        if (matches.size() == 1) {
            input.setText(matches.get(0) + " ");
            input.positionCaret(input.getText().length());
            return;
        }
        if (matches.size() > 1) {
            append(String.join("  ", matches) + "\n", "terminal-dim");
            scrollToBottom();
        }
    }

    private void clear() {
        output.getChildren().clear();
    }

    private void refreshPrompt() {
        promptLabel.setText(shellPrompt.render());
    }

    private void append(String text, String styleClass) {
        Text segment = new Text(text);
        segment.getStyleClass().add(styleClass);
        output.getChildren().add(segment);

        if (output.getChildren().size() > MAX_SEGMENTS) {
            output.getChildren().remove(0, output.getChildren().size() - MAX_SEGMENTS);
        }
    }

    private void scrollToBottom() {
        // 레이아웃이 끝난 뒤라야 vvalue 가 새 높이를 기준으로 계산된다.
        Platform.runLater(() -> scroller.setVvalue(1));
    }

    void dispose() {
        // 터미널은 주기 갱신을 구독하지 않는다. 정리할 것이 없다는 사실을
        // 명시적으로 남겨 둔다 — 나중에 구독을 추가하면 여기에 해지를 넣어야 한다.
    }
}
```

---

# 17. BootConsole.java

**Path**
`src/main/java/forgeos/boot/BootConsole.java`

```java
package forgeos.boot;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 부팅 1단계 — 커널 로그가 한 글자씩 찍히는 터미널 화면.
 *
 * <h2>왜 큐인가</h2>
 * <p>줄이 들어오는 속도(커널 부팅 단계마다 한 줄)와 찍히는 속도(글자당 몇 ms)는
 * 서로 무관하다. 로그가 몰아서 들어오면 화면이 순간이동하고, 반대로 타이핑이
 * 빠르면 다음 줄을 기다리며 멈춘다. 그래서 유입과 출력을 큐로 분리하고,
 * 출력은 항상 일정한 리듬으로만 흐르게 한다. 부팅이 끝나도 큐가 빌 때까지는
 * 다음 단계로 넘어가지 않는다({@link #whenDrained}).</p>
 */
final class BootConsole extends StackPane {

    /** 글자 하나가 찍히는 간격(초). 너무 빠르면 로그가 아니라 깜빡임으로 보인다. */
    private static final double CHAR_INTERVAL = 0.0065;

    /** 줄이 끝난 뒤의 숨. 사람이 한 줄을 읽었다고 느끼는 최소 간격이다. */
    private static final double LINE_PAUSE = 0.055;

    private final VBox lines = new VBox();
    private final ScrollPane scroller = new ScrollPane();
    private final Deque<String> pending = new ArrayDeque<>();
    private final Label cursor = new Label("█");

    private Label currentLine;
    private String currentText = "";
    private int typedChars;
    private double accumulator;
    private long lastNanos;
    private boolean typing;
    private boolean drainRequested;
    private Runnable onDrained;

    private final AnimationTimer typewriter = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastNanos == 0) {
                lastNanos = now;
                return;
            }
            accumulator += Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
            lastNanos = now;
            pump();
        }
    };

    BootConsole() {
        getStyleClass().add("boot-console");

        lines.getStyleClass().add("boot-console-lines");

        HBox cursorRow = new HBox(cursor);
        cursorRow.getStyleClass().add("boot-console-cursor-row");

        VBox column = new VBox(lines, cursorRow);
        column.getStyleClass().add("boot-console-column");
        VBox.setVgrow(lines, Priority.NEVER);

        scroller.setContent(column);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("boot-console-scroll");
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        cursor.getStyleClass().add("boot-console-cursor");
        blink();

        setAlignment(Pos.TOP_LEFT);
        getChildren().add(scroller);
    }

    /** 커서가 살아 있다는 신호. 정확히 이것 하나 때문에 화면이 정지 화면으로 안 보인다. */
    private void blink() {
        FadeTransition fade = new FadeTransition(Duration.millis(560), cursor);
        fade.setFromValue(1);
        fade.setToValue(0.05);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    /**
     * 찍을 줄을 큐에 넣는다. 이미 타이핑 중이면 뒤에 붙는다.
     *
     * @param line 출력할 한 줄
     */
    void println(String line) {
        pending.add(line);
        startTyping();
    }

    private void startTyping() {
        if (typing) {
            return;
        }
        typing = true;
        lastNanos = 0;
        typewriter.start();
    }

    private void stopTyping() {
        typing = false;
        typewriter.stop();
    }

    /**
     * 큐가 모두 소진된 시점에 한 번 실행할 작업을 건다.
     *
     * @param action 소진 콜백
     */
    void whenDrained(Runnable action) {
        this.onDrained = action;
        this.drainRequested = true;
        if (isDrained()) {
            fireDrained();
        }
    }

    /** 남은 줄을 전부 즉시 출력하고 타이핑을 끝낸다. (건너뛰기) */
    void flush() {
        commitCurrentLine();
        while (!pending.isEmpty()) {
            appendLine(pending.poll());
        }
        stopTyping();
        scroller.setVvalue(1);
        if (drainRequested) {
            fireDrained();
        }
    }

    private void pump() {
        while (accumulator >= CHAR_INTERVAL) {
            if (currentLine == null) {
                if (pending.isEmpty()) {
                    accumulator = 0;
                    if (drainRequested) {
                        fireDrained();
                    }
                    return;
                }
                currentText = pending.poll();
                typedChars = 0;
                currentLine = newLine("");
            }

            if (typedChars >= currentText.length()) {
                accumulator -= LINE_PAUSE;
                commitCurrentLine();
                continue;
            }

            typedChars++;
            currentLine.setText(currentText.substring(0, typedChars));
            accumulator -= CHAR_INTERVAL;
        }
        scroller.setVvalue(1);
    }

    private void commitCurrentLine() {
        if (currentLine != null) {
            currentLine.setText(currentText);
            currentLine = null;
            currentText = "";
            typedChars = 0;
        }
    }

    private void appendLine(String text) {
        newLine(text);
    }

    private Label newLine(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("boot-console-line");
        lines.getChildren().add(label);
        return label;
    }

    private boolean isDrained() {
        return pending.isEmpty() && currentLine == null;
    }

    private void fireDrained() {
        if (onDrained == null) {
            return;
        }
        Runnable action = onDrained;
        onDrained = null;
        drainRequested = false;
        stopTyping();
        action.run();
    }
}
```

---

# 18. BootSequence.java

**Path**
`src/main/java/forgeos/boot/BootSequence.java`

```java
package forgeos.boot;

import forgeframework.api.ForgeConfig;
import forgeframework.api.ForgeFramework;
import forgeframework.logger.LogEntry;
import forgeframework.logger.LogLevel;
import forgeos.core.KernelService;
import forgeos.ui.Motion;
import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 시네마틱 부팅 시퀀스 — 터미널 로그 → MP4 애니메이션 → 데스크탑.
 *
 * <h2>타이밍을 무엇에 맞추는가</h2>
 * <p>세 단계를 고정된 초로 이어 붙이면 반드시 어긋난다. 커널 부팅은 기계마다
 * 걸리는 시간이 다르고, 타이핑은 로그 길이에 따라 달라지기 때문이다.
 * 그래서 이 클래스는 <b>시간이 아니라 사건</b>으로 넘어간다.</p>
 * <ol>
 *   <li>1 → 2: 커널 부팅 완료 <b>그리고</b> 타이핑 큐 소진. 둘 다 만족해야 넘어간다.</li>
 *   <li>2 → 3: {@code MediaPlayer}의 재생 종료 이벤트(또는 실패).</li>
 * </ol>
 * <p>단 하나 시간으로 강제하는 것이 {@link #MIN_CONSOLE_MILLIS}다. 빠른 기계에서
 * 부팅 로그가 0.3초 만에 끝나면 화면이 깜빡인 것처럼 보인다. 최소한 이만큼은
 * 머문다.</p>
 *
 * <h2>건너뛰기</h2>
 * <p>ESC 또는 클릭으로 언제든 데스크탑으로 넘어갈 수 있다. 개발 중에 부팅
 * 영상을 200번 보는 것은 누구에게도 도움이 되지 않는다. 다만 커널 부팅 자체는
 * 건너뛸 수 없으므로, 부팅이 끝나기 전에 건너뛰면 완료를 기다렸다가 전환한다.</p>
 */
public final class BootSequence extends StackPane {

    /** 부팅 애니메이션 리소스 경로. */
    private static final String VIDEO_RESOURCE = "/assets/forgeOS-Booting-Animation2.mp4";

    /** 부팅 콘솔이 화면에 머무는 최소 시간(ms). */
    private static final long MIN_CONSOLE_MILLIS = 1200;

    /**
     * 부팅 단계 사이의 지연(ms).
     *
     * <p>커널 기본값(150ms)보다 길게 잡았다. 커널 입장에서는 의미 없는 대기지만
     * 부팅 화면 입장에서는 이것이 곧 리듬이다 — 로그가 한 줄씩 "찍히는" 것처럼
     * 보이려면 줄과 줄 사이에 사람이 인지할 만한 간격이 있어야 한다.</p>
     */
    private static final long BOOT_STAGE_DELAY_MILLIS = 260;

    private final KernelService kernelService;
    private final Runnable onDesktopReady;

    private final BootConsole console = new BootConsole();
    private final BootVideo video = new BootVideo();

    private long consoleShownAt;
    private boolean kernelReady;
    private boolean consoleDrained;
    private boolean advanced;
    private boolean finished;
    private boolean skipRequested;

    /**
     * 부팅 시퀀스를 만든다. 생성만으로는 아무 일도 일어나지 않는다 —
     * {@link #start()}를 호출해야 커널이 뜬다.
     *
     * @param kernelService  부팅시킬 커널 서비스
     * @param onDesktopReady 3단계 진입 콜백 (FX 스레드)
     */
    public BootSequence(KernelService kernelService, Runnable onDesktopReady) {
        this.kernelService = kernelService;
        this.onDesktopReady = onDesktopReady;

        getStyleClass().add("boot-root");
        video.setVisible(false);
        video.setOpacity(0);
        getChildren().addAll(video, console);

        setOnMouseClicked(e -> skip());
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE) {
                skip();
            }
        });
    }

    /** 1단계를 시작하고, 백그라운드에서 커널을 부팅한다. */
    public void start() {
        consoleShownAt = System.currentTimeMillis();
        printBanner();

        ForgeConfig config = ForgeConfig.defaults()
                .withBootStageDelayMillis(BOOT_STAGE_DELAY_MILLIS);

        kernelService.bootAsync(
                config,
                this::onKernelLog,
                this::onKernelReady,
                this::onKernelFailure);

        requestFocus();
    }

    private void printBanner() {
        console.println("=================================================");
        console.println(" " + ForgeFramework.name() + " v" + ForgeFramework.version());
        console.println(" Operating System Kernel Architecture Engine");
        console.println("=================================================");
    }

    /** 커널이 남기는 로그를 그대로 화면에 흘린다. 부팅 화면은 연출이 아니라 실제 로그다. */
    private void onKernelLog(LogEntry entry) {
        if (entry.getLevel() == LogLevel.DEBUG) {
            return;
        }
        console.println(entry.toFormattedString());
    }

    private void onKernelReady() {
        kernelReady = true;
        kernelService.log(LogLevel.INFO, "ForgeOS 데스크탑 환경 준비 완료");

        long elapsed = System.currentTimeMillis() - consoleShownAt;
        long remaining = Math.max(0, MIN_CONSOLE_MILLIS - elapsed);

        // 최소 체류 시간을 채운 뒤에야 "큐가 비면 넘어간다"를 예약한다.
        if (remaining == 0) {
            armConsoleDrain();
        } else {
            PauseTransition wait = new PauseTransition(Duration.millis(remaining));
            wait.setOnFinished(e -> armConsoleDrain());
            wait.play();
        }
    }

    private void armConsoleDrain() {
        if (skipRequested) {
            console.flush();
        }
        console.whenDrained(() -> {
            consoleDrained = true;
            advanceToVideo();
        });
    }

    private void onKernelFailure(Throwable error) {
        console.println("[FATAL] 커널 부팅에 실패했습니다: " + error.getMessage());
        console.println("[FATAL] ForgeOS를 시작할 수 없습니다.");
    }

    private void advanceToVideo() {
        if (advanced || !kernelReady || !consoleDrained) {
            return;
        }
        advanced = true;
        kernelService.detachBootLogSink();

        if (skipRequested) {
            finish();
            return;
        }

        Motion.fadeOut(console, Motion.FADE_SCENE, () -> {
            video.setOpacity(0);
            video.play(VIDEO_RESOURCE, this::finish);
            Motion.fadeIn(video, Motion.FADE, null);
        });
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        video.dispose();
        onDesktopReady.run();
    }

    /**
     * 남은 연출을 건너뛴다. 커널 부팅이 아직이면 완료 직후 곧바로 데스크탑으로 간다.
     */
    public void skip() {
        if (skipRequested) {
            return;
        }
        skipRequested = true;

        if (!kernelReady) {
            // 커널 부팅은 건너뛸 수 없다. onKernelReady 에서 skipRequested 를 보고 바로 넘어간다.
            return;
        }
        if (advanced) {
            video.dispose();
            finish();
            return;
        }
        console.flush();
    }
}
```

---

# 19. BootVideo.java

**Path**
`src/main/java/forgeos/boot/BootVideo.java`

```java
package forgeos.boot;

import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 부팅 2단계 — MP4 부팅 애니메이션.
 *
 * <h2>jar 안의 미디어 문제</h2>
 * <p>JavaFX {@link Media}는 {@code jar:} URL을 재생하지 못한다. IDE에서 돌릴 때는
 * 리소스가 디렉터리(build/resources/main)에 풀려 있어 잘 되다가, 배포용 jar로
 * 묶는 순간 조용히 실패한다. 그래서 리소스 URL의 프로토콜이 {@code file}이
 * 아니면 임시 파일로 한 번 복사해서 재생한다. 배포 시점에 발견하는 것보다
 * 지금 다섯 줄 더 쓰는 편이 싸다.</p>
 *
 * <h2>실패해도 부팅은 계속된다</h2>
 * <p>코덱이 없거나 파일이 빠져 있으면 그냥 다음 단계로 넘어간다. 부팅
 * 애니메이션 때문에 운영체제가 못 뜨는 것은 말이 안 된다.</p>
 */
final class BootVideo extends StackPane {

    private final MediaView view = new MediaView();
    private MediaPlayer player;

    BootVideo() {
        getStyleClass().add("boot-video");
        view.setPreserveRatio(false);
        view.setSmooth(true);
        getChildren().add(view);

        // 영상은 화면을 꽉 채운다. MediaView 는 부모 크기를 따라가지 않으므로 직접 묶는다.
        widthProperty().addListener((ObservableValue<? extends Number> obs, Number old, Number now) ->
                view.setFitWidth(now.doubleValue()));
        heightProperty().addListener((ObservableValue<? extends Number> obs, Number old, Number now) ->
                view.setFitHeight(now.doubleValue()));
    }

    /**
     * 애니메이션을 재생한다.
     *
     * @param resourcePath 클래스패스 리소스 경로
     * @param onFinished   재생이 끝났거나 재생할 수 없을 때 호출될 콜백 (항상 정확히 한 번)
     */
    void play(String resourcePath, Runnable onFinished) {
        String source = resolveSource(resourcePath);
        if (source == null) {
            onFinished.run();
            return;
        }

        try {
            player = new MediaPlayer(new Media(source));
        } catch (RuntimeException e) {
            // 코덱 미지원 등. 부팅을 막을 이유가 없다.
            onFinished.run();
            return;
        }

        OneShot guard = new OneShot(onFinished);
        player.setOnEndOfMedia(guard::fire);
        player.setOnError(guard::fire);
        player.setOnHalted(guard::fire);
        player.setOnStopped(guard::fire);

        view.setMediaPlayer(player);
        player.play();
    }

    /** 재생을 중단하고 자원을 정리한다. 건너뛰기와 창 종료에서 호출한다. */
    void dispose() {
        if (player != null) {
            player.setOnEndOfMedia(null);
            player.setOnError(null);
            player.setOnHalted(null);
            player.setOnStopped(null);
            player.stop();
            player.dispose();
            player = null;
        }
    }

    private String resolveSource(String resourcePath) {
        URL url = BootVideo.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        if ("file".equals(url.getProtocol())) {
            return url.toExternalForm();
        }
        return extractToTempFile(resourcePath);
    }

    private String extractToTempFile(String resourcePath) {
        try (InputStream in = BootVideo.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
            Path temp = Files.createTempFile("forgeos-boot", suffix);
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toUri().toString();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** 콜백이 두 번 불리지 않도록 막는 래퍼. onError 와 onHalted 가 겹쳐 오는 경우가 있다. */
    private static final class OneShot {
        private final Runnable action;
        private boolean fired;

        OneShot(Runnable action) {
            this.action = action;
        }

        void fire() {
            if (fired) {
                return;
            }
            fired = true;
            action.run();
        }
    }
}
```

---

# 20. KernelService.java

**Path**
`src/main/java/forgeos/core/KernelService.java`

```java
package forgeos.core;

import forgeframework.api.ForgeConfig;
import forgeframework.api.ForgeFramework;
import forgeframework.kernel.Kernel;
import forgeframework.logger.EventLogger;
import forgeframework.logger.LogEntry;
import forgeframework.logger.LogLevel;
import forgeframework.syscall.SystemCallRequest;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 커널과 JavaFX UI 사이의 유일한 통로.
 *
 * <h2>스레드 경계</h2>
 * <p>이 클래스가 존재하는 첫 번째 이유는 스레드 경계를 한 곳에 가두기 위해서다.
 * 커널의 로그는 부팅 스레드·타이머 장치 스레드·프린터 스풀러 스레드 등
 * <b>여러 백그라운드 스레드</b>에서 올라온다. 그 값을 그대로 {@code ObservableList}에
 * 넣으면 JavaFX가 렌더 도중 리스트를 밟고 죽는다. 그래서 로그 유입 지점은
 * {@link #install()} 한 곳뿐이고, 거기서 전부 {@link Platform#runLater}로 넘긴다.</p>
 *
 * <p>반대로 시스템 콜({@link #call})은 FX 스레드에서 바로 호출한다. 커널의 시스템
 * 콜은 전부 인메모리 연산이라 프레임을 잡아먹지 않으며, 오히려 백그라운드로
 * 빼면 결과를 다시 FX로 넘기는 왕복이 생겨 UI가 한 박자 늦게 반응한다.
 * 유일한 예외가 부팅이다 — 부팅은 단계마다 의도적으로 잠들기 때문에
 * ({@link ForgeConfig#bootStageDelayMillis()}) 반드시 백그라운드로 돌린다.</p>
 *
 * <h2>새로고침 펄스</h2>
 * <p>앱마다 각자 {@code Timeline}을 돌리면 창을 네 개 열었을 때 초당 네 번의
 * 서로 어긋난 갱신이 일어난다. 갱신 주기는 여기 하나로 모으고
 * ({@link #onRefresh}) 앱은 구독만 한다.</p>
 */
public final class KernelService {

    /** UI 갱신 주기. 커널의 기본 타이머 tick(1초)과 맞춘다 — 더 빨라야 할 이유가 없다. */
    private static final Duration REFRESH_PERIOD = Duration.millis(1000);

    /** 로그 보관 상한. 무한히 쌓으면 몇 시간짜리 세션에서 힙을 먹는다. */
    private static final int LOG_CAPACITY = 800;

    private final EventLogger logger = new EventLogger();
    private final ObservableList<LogEntry> logs = FXCollections.observableArrayList();
    private final List<Runnable> refreshListeners = new ArrayList<>();
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);

    private final Timeline pulse = new Timeline(new KeyFrame(REFRESH_PERIOD, e -> firePulse()));

    private volatile Kernel kernel;
    private volatile Consumer<LogEntry> bootLogSink;

    /** 아직 부팅하지 않은 서비스를 만든다. */
    public KernelService() {
        pulse.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * 커널을 백그라운드 스레드에서 부팅한다. 즉시 반환한다.
     *
     * @param config    부팅에 사용할 설정
     * @param onLog     부팅 중 발생하는 커널 로그를 받을 콜백. <b>FX 스레드에서</b> 호출된다
     * @param onReady   부팅 완료 콜백. <b>FX 스레드에서</b> 호출된다
     * @param onFailure 부팅 실패 콜백. <b>FX 스레드에서</b> 호출된다
     */
    public void bootAsync(ForgeConfig config,
                          Consumer<LogEntry> onLog,
                          Runnable onReady,
                          Consumer<Throwable> onFailure) {
        Objects.requireNonNull(config, "config");
        this.bootLogSink = onLog;
        install();

        Thread bootThread = new Thread(() -> {
            try {
                Kernel booted = ForgeFramework.boot(config, logger);
                Platform.runLater(() -> {
                    this.kernel = booted;
                    running.set(true);
                    pulse.play();
                    onReady.run();
                });
            } catch (RuntimeException | Error e) {
                Platform.runLater(() -> onFailure.accept(e));
            }
        }, "forgeos-boot");

        // 데몬으로 두어야 부팅 도중 창을 닫아도 JVM이 남지 않는다.
        bootThread.setDaemon(true);
        bootThread.start();
    }

    /**
     * 커널 로그를 UI로 끌어오는 리스너를 건다.
     *
     * <p>여기가 백그라운드 → FX 스레드 경계의 유일한 지점이다.</p>
     */
    private void install() {
        logger.addListener(entry -> Platform.runLater(() -> {
            if (logs.size() >= LOG_CAPACITY) {
                logs.remove(0, logs.size() - LOG_CAPACITY + 1);
            }
            logs.add(entry);

            Consumer<LogEntry> sink = bootLogSink;
            if (sink != null) {
                sink.accept(entry);
            }
        }));
    }

    /** 부팅 화면이 끝난 뒤, 로그를 부팅 콘솔로 흘려보내던 통로를 끊는다. */
    public void detachBootLogSink() {
        this.bootLogSink = null;
    }

    /**
     * 커널에 직접 로그 한 줄을 남긴다. 부팅 시퀀스가 자기 진행 상황을 알릴 때 쓴다.
     *
     * @param level   로그 수준
     * @param message 메시지
     */
    public void log(LogLevel level, String message) {
        logger.log(level, message);
    }

    /**
     * 시스템 콜을 실행한다. <b>FX 스레드에서만</b> 호출해야 한다.
     *
     * @param type 시스템 콜 종류
     * @param args 인자
     * @return 커널의 응답. 커널이 아직 없거나 이미 내려갔으면 실패 결과
     */
    public SystemCallResult call(SystemCallType type, String... args) {
        Kernel current = kernel;
        if (current == null) {
            return SystemCallResult.failure("커널이 아직 부팅되지 않았습니다.");
        }
        SystemCallResult result = current.handleSystemCall(new SystemCallRequest(type, args));

        // shutdown 처럼 커널을 내리는 명령이 있으므로 매 호출 뒤 상태를 되비춘다.
        if (running.get() && !current.isRunning()) {
            running.set(false);
            pulse.stop();
        }
        return result;
    }

    /**
     * 커널 가동 여부를 다시 확인해 프로퍼티에 반영한다.
     *
     * <p>Terminal 앱은 명령어 계층({@code CommandRegistry})을 통해 커널을 직접
     * 호출하므로 {@link #call}을 거치지 않는다. 그 경로로 {@code shutdown}이
     * 실행되면 데스크탑은 커널이 내려간 사실을 모른다. 그래서 직접 호출하는
     * 쪽이 끝나고 이 메서드를 불러 준다.</p>
     */
    public void syncRunningState() {
        Kernel current = kernel;
        boolean alive = current != null && current.isRunning();
        if (running.get() != alive) {
            running.set(alive);
            if (!alive) {
                pulse.stop();
            }
        }
    }

    /**
     * 부팅이 끝난 커널을 반환한다.
     *
     * @return 커널. 부팅 전이면 {@code null}
     */
    public Kernel kernel() {
        return kernel;
    }

    /**
     * 커널 가동 여부.
     *
     * @return 부팅 완료 후 {@code shutdown} 전이면 참인 프로퍼티
     */
    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    /**
     * 커널이 남긴 로그 전체.
     *
     * @return FX 스레드에서만 읽어야 하는 관측 가능 리스트
     */
    public ObservableList<LogEntry> logs() {
        return logs;
    }

    /**
     * 주기적 갱신 구독을 건다.
     *
     * <p>구독 해지는 호출자가 신경 쓰지 않아도 되도록 {@link Subscription}으로
     * 돌려준다. 창이 닫힐 때 해지하지 않으면 죽은 UI를 계속 갱신하게 된다.</p>
     *
     * @param listener 매 주기 FX 스레드에서 실행될 작업
     * @return 해지 핸들
     */
    public Subscription onRefresh(Runnable listener) {
        refreshListeners.add(listener);
        return () -> refreshListeners.remove(listener);
    }

    private void firePulse() {
        // 콜백 안에서 구독을 해지하는 경우가 있으므로 복사본을 순회한다.
        for (Runnable listener : List.copyOf(refreshListeners)) {
            listener.run();
        }
    }

    /** 커널을 내리고 펄스를 멈춘다. 애플리케이션 종료 시 호출한다. */
    public void shutdown() {
        pulse.stop();
        Kernel current = kernel;
        if (current != null) {
            current.close();
        }
        running.set(false);
    }

    /** {@link KernelService#onRefresh} 구독 해지 핸들. */
    @FunctionalInterface
    public interface Subscription {
        /** 구독을 해지한다. 여러 번 호출해도 안전하다. */
        void cancel();
    }
}
```

---

# 21. DesktopPane.java

**Path**
`src/main/java/forgeos/desktop/DesktopPane.java`

```java
package forgeos.desktop;

import forgeos.app.ForgeApp;
import forgeos.core.KernelService;
import forgeos.ui.Motion;
import forgeos.ui.ThemeManager;
import forgeos.wm.WindowManager;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 부팅이 끝난 뒤의 화면 전체 — 바탕화면, 창 레이어, 상단 메뉴바, 하단 Dock.
 *
 * <h2>레이어 순서</h2>
 * <p>아래에서 위로 바탕화면 → 창 → 크롬(메뉴바·Dock) → 종료 오버레이다.
 * 크롬이 창보다 위에 있어야 창을 화면 끝까지 끌어도 메뉴바 아래로 지나가는
 * 것처럼 보인다. 반투명한 크롬 아래로 내용이 흘러가는 것은 가려서 숨기는 게
 * 아니라 <b>깊이</b>를 만드는 장치다.</p>
 */
public final class DesktopPane extends StackPane {

    /** 메뉴바·Dock 과 창 사이에 남기는 여백(px). */
    private static final double CHROME_GAP = 8;

    /** Dock 과 화면 아래 모서리 사이의 여백(px). 0 이면 Dock 이 바닥에 붙어 답답하다. */
    private static final double DOCK_MARGIN = 14;

    private final WindowManager windowManager;
    private final MenuBarView menuBar;
    private final DockView dock;
    private final StackPane shutdownOverlay;

    /**
     * 데스크탑을 만든다.
     *
     * @param kernelService 커널 서비스
     * @param apps          Dock 에 올릴 앱 목록 (순서가 곧 Dock 순서)
     * @param theme         라이트/다크 전환기 (메뉴바의 토글이 사용한다)
     * @param onQuit        ForgeOS 자체를 종료할 때 실행할 작업
     */
    public DesktopPane(KernelService kernelService, List<ForgeApp> apps,
                       ThemeManager theme, Runnable onQuit) {
        getStyleClass().add("desktop");

        Wallpaper wallpaper = new Wallpaper();

        windowManager = new WindowManager(kernelService);
        menuBar = new MenuBarView(kernelService, windowManager, theme, onQuit);
        dock = new DockView(windowManager, apps);
        shutdownOverlay = buildShutdownOverlay(onQuit);

        getChildren().addAll(wallpaper, windowManager, menuBar, dock, shutdownOverlay);

        // StackPane 안의 Region 은 기본적으로 영역을 꽉 채운다. 바탕화면·창 레이어·
        // 오버레이는 그래서 손댈 것이 없지만, 크롬 두 개는 반대로 <b>막아 줘야</b> 한다.
        // 그냥 두면 배경이 있는 메뉴바가 화면 전체를 덮어 아래를 다 가린다.
        // (색·여백이 아니라 레이아웃 제약이므로 CSS 가 아니라 여기 있는 것이 맞다.)
        menuBar.setMaxWidth(Double.MAX_VALUE);
        menuBar.setMaxHeight(Region.USE_PREF_SIZE);
        dock.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // 데스크탑은 창보다 커질 수 없다. 자식 중 하나라도 큰 최소 크기를 요구하면
        // StackPane 은 그 값을 받아들여 자기 최소 크기로 삼고, 그러면 창보다 큰
        // 데스크탑이 가운데 정렬되면서 메뉴바가 위로, Dock 이 아래로 밀려 잘린다.
        // (실제로 배경화면의 동심원 때문에 그렇게 됐다 — Wallpaper 주석 참고.)
        setMinSize(0, 0);

        StackPane.setAlignment(menuBar, Pos.TOP_CENTER);
        StackPane.setAlignment(dock, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dock, new Insets(0, 0, DOCK_MARGIN, 0));

        bindWorkArea();

        kernelService.runningProperty().addListener((obs, was, running) -> {
            if (was && !running) {
                showShutdownOverlay();
            }
        });
    }

    /**
     * 작업 영역은 "창이 놓일 수 있는 곳"이다. 메뉴바와 Dock 이 차지한 만큼을 뺀다.
     *
     * <p>높이가 아직 0인 첫 레이아웃에서도 계산이 돌아가야 하므로 리스너를 붙여
     * 크기가 정해질 때마다 다시 계산한다.</p>
     */
    private void bindWorkArea() {
        Runnable recompute = () -> {
            double top = menuBar.getHeight() + CHROME_GAP;
            double bottom = dock.getHeight() + DOCK_MARGIN + CHROME_GAP;
            double width = Math.max(0, getWidth());
            double height = Math.max(0, getHeight() - top - bottom);
            windowManager.workAreaProperty().set(new BoundingBox(0, top, width, height));
        };

        widthProperty().addListener((obs, old, now) -> recompute.run());
        heightProperty().addListener((obs, old, now) -> recompute.run());
        menuBar.heightProperty().addListener((obs, old, now) -> recompute.run());
        dock.heightProperty().addListener((obs, old, now) -> recompute.run());
    }

    private StackPane buildShutdownOverlay(Runnable onQuit) {
        Label title = new Label("커널이 종료되었습니다");
        title.getStyleClass().add("shutdown-title");

        Label detail = new Label("ForgeFramework 커널이 정상적으로 내려갔습니다.\n"
                + "다시 사용하려면 ForgeOS를 재시작하세요.");
        detail.getStyleClass().add("shutdown-detail");

        Button quit = new Button("ForgeOS 종료");
        quit.getStyleClass().add("shutdown-button");
        quit.setOnAction(e -> onQuit.run());

        VBox column = new VBox(title, detail, quit);
        column.getStyleClass().add("shutdown-column");
        column.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(column);
        overlay.getStyleClass().add("shutdown-overlay");
        overlay.setVisible(false);
        overlay.setOpacity(0);
        return overlay;
    }

    private void showShutdownOverlay() {
        shutdownOverlay.toFront();
        Motion.fadeIn(shutdownOverlay, Duration.millis(420), null);
    }

    /**
     * 창 관리자. 단축키 설치를 위해 애플리케이션이 가져간다.
     *
     * @return 창 관리자
     */
    public WindowManager windowManager() {
        return windowManager;
    }
}
```

---

# 22. DockView.java

**Path**
`src/main/java/forgeos/desktop/DockView.java`

```java
package forgeos.desktop;

import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;
import forgeos.ui.Motion;
import forgeos.ui.SpringValue;
import forgeos.ui.Styles;
import forgeos.wm.WindowManager;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 화면 하단의 Dock.
 *
 * <h2>확대(magnification)</h2>
 * <p>포인터에 가까운 아이콘일수록 커진다. 단순한 장식처럼 보이지만 실제로는
 * 작은 아이콘을 정확히 겨냥하는 비용을 낮춰 준다 — 목표가 커지면 맞히기 쉽다.</p>
 *
 * <p>확대 배율은 CSS로 뺄 수 없어(스케일은 레이아웃이 아니라 변환이다) 여기
 * 상수로 둔다. 대신 값 자체는 포인터를 <b>따라가는</b> 스프링으로 적용한다.
 * 목표 배율을 즉시 대입하면 포인터를 빠르게 움직일 때 아이콘이 계단처럼
 * 튀는데, 스프링을 거치면 손의 궤적을 따라 흐른다.</p>
 */
final class DockView extends HBox {

    /** 포인터 바로 아래 아이콘의 최대 배율. */
    private static final double MAX_SCALE = 1.38;

    /**
     * 확대가 퍼지는 거리(px). <b>아이콘 사각형의 가장자리로부터</b> 잰다.
     *
     * <p>중심으로부터 재면 두 가지가 동시에 잘못된다. 포인터가 아이콘 안에서
     * 가장자리 쪽으로 가기만 해도 그 아이콘이 줄어들고, 반대로 이웃 아이콘은
     * 중심 간 거리(아이콘 폭 + 간격)만큼 떨어져 있는데도 크게 반응한다.
     * 가장자리 기준으로 바꾸면 아이콘 안에서는 거리가 0이라 항상 최대 배율이고,
     * 이웃은 순수하게 <b>사이 간격</b>만큼만 떨어진 것으로 계산된다.</p>
     */
    private static final double SPREAD = 26;

    /** 아이콘 한 변(px). */
    private static final double ICON_SIZE = 30;

    private final WindowManager windowManager;
    private final Map<String, DockItem> items = new LinkedHashMap<>();
    private final List<DockItem> ordered = new ArrayList<>();

    DockView(WindowManager windowManager, List<ForgeApp> apps) {
        this.windowManager = windowManager;
        getStyleClass().add("dock");
        setPickOnBounds(false);

        for (ForgeApp app : apps) {
            DockItem item = new DockItem(app);
            items.put(app.id(), item);
            ordered.add(item);
            getChildren().add(item);
        }

        addEventFilter(MouseEvent.MOUSE_MOVED, e -> magnify(e.getX()));
        setOnMouseExited(e -> magnify(Double.NaN));

        windowManager.runningAppIds().addListener(
                (ListChangeListener<String>) change -> refreshRunningState());

        // 최소화 애니메이션이 어디로 날아가야 하는지는 Dock 만 안다.
        windowManager.setDockAnchor(this::anchorFor);
    }

    private void magnify(double pointerX) {
        for (DockItem item : ordered) {
            item.scaleTo(Double.isNaN(pointerX) ? 1 : scaleFor(item, pointerX));
        }
    }

    /**
     * 포인터 위치에 대한 아이콘 하나의 목표 배율.
     *
     * <p>아이콘 사각형 <b>안</b>이면 거리가 0이라 최대 배율이 그대로 나온다.
     * 밖이면 사각형에서 벗어난 만큼만 거리로 세고, 그 거리에 가우시안 감쇠를
     * 적용한다. 이웃 아이콘은 아이콘 사이 간격만큼 떨어져 있으므로 살짝만
     * 들리고, 그 너머는 사실상 움직이지 않는다.</p>
     */
    private static double scaleFor(DockItem item, double pointerX) {
        Bounds bounds = item.getBoundsInParent();
        double distance = Math.max(0,
                Math.max(bounds.getMinX() - pointerX, pointerX - bounds.getMaxX()));
        double falloff = Math.exp(-(distance * distance) / (SPREAD * SPREAD));
        return 1 + (MAX_SCALE - 1) * falloff;
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
        private final SpringValue scale;

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

            Tooltip tooltip = new Tooltip(app.title());
            tooltip.setShowDelay(Duration.millis(350));
            Tooltip.install(this, tooltip);

            scale = new SpringValue(value -> {
                tile.setScaleX(value);
                tile.setScaleY(value);
                // 커질 때 아래쪽이 Dock 바닥을 뚫지 않도록 위로 밀어 올린다.
                tile.setTranslateY(-(value - 1) * ICON_SIZE * 0.55);
            }).tune(Motion.RESPONSE_SNAPPY, Motion.DAMPING_STANDARD);
            scale.reset(1);

            // 눌리는 순간 반응한다. 떼는 순간까지 기다리면 죽은 버튼처럼 느껴진다.
            setOnMousePressed(e -> tile.setOpacity(0.65));
            setOnMouseReleased(e -> tile.setOpacity(1));
            setOnMouseClicked(e -> windowManager.toggle(app));
        }

        void scaleTo(double target) {
            scale.setTarget(target);
        }

        void setRunning(boolean running) {
            runningDot.setVisible(running);
            pseudoClassStateChanged(Styles.RUNNING, running);
        }
    }
}
```

---

# 23. MenuBarView.java

**Path**
`src/main/java/forgeos/desktop/MenuBarView.java`

```java
package forgeos.desktop;

import forgeframework.kernel.UptimeDto;
import forgeframework.memory.MemorySnapshot;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.ThemeManager;
import forgeos.wm.ForgeWindow;
import forgeos.wm.WindowManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 화면 상단의 메뉴바.
 *
 * <p>왼쪽은 "지금 무엇을 쓰고 있는가"(Forge 마크 + 활성 창 이름), 오른쪽은
 * "지금 커널이 어떤 상태인가"(프로세스 수, 메모리, 가동 시간, 시계)다.
 * 어느 앱을 쓰고 있든 커널 상태가 항상 한 줄로 보이는 것이 이 시뮬레이터의
 * 핵심 가치라고 보고 자리를 내줬다.</p>
 */
final class MenuBarView extends HBox {

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E) a h:mm");

    private final KernelService kernelService;

    private final Label activeAppLabel = new Label("ForgeOS");
    private final Label processChip = chip("프로세스 —");
    private final Label memoryChip = chip("메모리 —");
    private final Label uptimeChip = chip("가동 —");
    private final Label clockLabel = new Label();

    private final Button themeButton = new Button();

    private final Timeline clock = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> tickClock()));

    MenuBarView(KernelService kernelService, WindowManager windowManager,
                ThemeManager theme, Runnable onQuit) {
        this.kernelService = kernelService;
        getStyleClass().add("menu-bar");

        activeAppLabel.getStyleClass().add("menu-active-app");
        clockLabel.getStyleClass().add("menu-clock");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                Glyphs.stroked(Glyphs.FORGE, 15, "menu-logo"),
                activeAppLabel,
                forgeMenu(windowManager, onQuit),
                spacer,
                processChip, memoryChip, uptimeChip, themeToggle(theme), clockLabel);

        windowManager.activeWindowProperty().addListener((obs, old, now) ->
                activeAppLabel.setText(now == null ? "ForgeOS" : now.title()));

        tickClock();
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        kernelService.onRefresh(this::refreshStats);
    }

    /**
     * 테마 토글.
     *
     * <p>아이콘은 <b>지금 상태</b>가 아니라 <b>누르면 갈 곳</b>을 보여 준다.
     * 다크 테마에서 해를 띄워 두면 "누르면 밝아지겠구나"로 읽힌다. 반대로 하면
     * 매번 한 번 더 생각하게 된다.</p>
     */
    private Button themeToggle(ThemeManager theme) {
        themeButton.getStyleClass().add("menu-icon-button");
        themeButton.setOnAction(e -> theme.toggle());

        Tooltip tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(400));
        Tooltip.install(themeButton, tooltip);

        Runnable sync = () -> {
            boolean dark = theme.mode() == ThemeManager.Mode.DARK;
            themeButton.setGraphic(Glyphs.stroked(
                    dark ? Glyphs.SUN : Glyphs.MOON, 15, "menu-icon-glyph"));
            tooltip.setText(dark ? "라이트 테마로 전환" : "다크 테마로 전환");
        };
        sync.run();
        theme.modeProperty().addListener((obs, old, now) -> sync.run());

        return themeButton;
    }

    private MenuButton forgeMenu(WindowManager windowManager, Runnable onQuit) {
        MenuItem closeWindow = new MenuItem("창 닫기");
        closeWindow.setOnAction(e -> windowManager.closeActive());

        MenuItem minimizeWindow = new MenuItem("창 최소화");
        minimizeWindow.setOnAction(e -> windowManager.minimizeActive());

        MenuItem zoomWindow = new MenuItem("전체 화면 전환");
        zoomWindow.setOnAction(e -> {
            ForgeWindow active = windowManager.activeWindowProperty().get();
            if (active != null) {
                active.toggleZoom();
            }
        });

        MenuItem shutdown = new MenuItem("커널 종료");
        shutdown.setOnAction(e -> kernelService.call(SystemCallType.SHUTDOWN));

        MenuItem quit = new MenuItem("ForgeOS 종료");
        quit.setOnAction(e -> onQuit.run());

        MenuButton menu = new MenuButton("창");
        menu.getStyleClass().add("menu-button");
        menu.getItems().addAll(
                closeWindow, minimizeWindow, zoomWindow,
                new SeparatorMenuItem(), shutdown, quit);
        return menu;
    }

    private void tickClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    private void refreshStats() {
        SystemCallResult ps = kernelService.call(SystemCallType.PS);
        if (ps.isSuccess()) {
            List<ProcessDto> processes = ps.dataAsList(ProcessDto.class);
            long alive = processes.stream()
                    .filter(p -> p.state() != ProcessState.TERMINATED)
                    .count();
            processChip.setText("프로세스 " + alive);
        }

        SystemCallResult mem = kernelService.call(SystemCallType.MEMINFO);
        if (mem.isSuccess()) {
            MemorySnapshot snapshot = mem.dataAs(MemorySnapshot.class);
            int total = snapshot.totalFrames();
            double ratio = total == 0 ? 0 : (double) snapshot.usedFrames() / total;
            memoryChip.setText("메모리 %.0f%%".formatted(ratio * 100));
        }

        SystemCallResult uptime = kernelService.call(SystemCallType.UPTIME);
        if (uptime.isSuccess()) {
            UptimeDto dto = uptime.dataAs(UptimeDto.class);
            uptimeChip.setText("가동 " + formatUptime(dto.uptimeSeconds()));
        }
    }

    private static String formatUptime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return "%d시간 %d분".formatted(hours, minutes % 60);
        }
        if (minutes > 0) {
            return "%d분 %d초".formatted(minutes, seconds % 60);
        }
        return seconds + "초";
    }

    private static Label chip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("menu-chip");
        return label;
    }
}
```

---

# 24. Wallpaper.java

**Path**
`src/main/java/forgeos/desktop/Wallpaper.java`

```java
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
 * <h2>왜 가운데가 아닌가</h2>
 * <p>창은 화면 한가운데에 열린다. 로고를 정중앙에 두면 창을 열 때마다 로고
 * 한가운데가 가려져 매번 반쪽만 보인다. 살짝 왼쪽 위로 비켜 앉힌다.</p>
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

    /** 중앙에서 왼쪽으로 비켜 앉는 정도(화면 폭 대비). */
    private static final double OFFSET_X_RATIO = 0.07;

    /** 중앙에서 위로 올라가는 정도(화면 높이 대비). */
    private static final double OFFSET_Y_RATIO = 0.05;

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

        widthProperty().addListener((obs, old, now) -> relayoutBrand());
        heightProperty().addListener((obs, old, now) -> relayoutBrand());
    }

    private void relayoutBrand() {
        double width = getWidth();
        double height = getHeight();

        clip.setWidth(Math.max(0, width));
        clip.setHeight(Math.max(0, height));

        if (width <= 0 || height <= 0) {
            return;
        }

        double scale = clamp(Math.min(width, height) / REFERENCE, MIN_SCALE, MAX_SCALE);
        double offsetX = -width * OFFSET_X_RATIO;
        double offsetY = -height * OFFSET_Y_RATIO;

        // 마크와 동심원은 같은 중심을 공유해야 하므로 같은 값으로 함께 움직인다.
        apply(rings, scale, offsetX, offsetY);
        apply(brand, scale, offsetX, offsetY);
    }

    private static void apply(Node node, double scale, double offsetX, double offsetY) {
        node.setScaleX(scale);
        node.setScaleY(scale);
        node.setTranslateX(offsetX);
        node.setTranslateY(offsetY);
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
```

---

# 25. ForgeMark.java

**Path**
`src/main/java/forgeos/ui/ForgeMark.java`

```java
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
                    glow(256, 245, 130, "mark-glow-ember", 54),
                    glow(256, 245, 70, "mark-glow-violet", 42));
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
     */
    private static Circle glow(double x, double y, double radius, String styleClass, double blur) {
        Circle circle = dot(x, y, radius, styleClass);
        circle.setEffect(new GaussianBlur(blur));
        return circle;
    }
}
```

---

# 26. Glyphs.java

**Path**
`src/main/java/forgeos/ui/Glyphs.java`

```java
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
```

---

# 27. Motion.java

**Path**
`src/main/java/forgeos/ui/Motion.java`

```java
package forgeos.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * ForgeOS 전역 모션 상수와 자주 쓰는 전환 헬퍼.
 *
 * <p>여기 있는 숫자는 취향이 아니라 규칙이다. 창이 열릴 때와 Dock 아이콘이
 * 커질 때가 서로 다른 속도로 움직이면 화면 전체가 제각각으로 보인다.
 * 새 애니메이션을 추가할 때는 새 숫자를 만들지 말고 아래 상수 중 하나를 고른다.</p>
 */
public final class Motion {

    /** 표준 반응 시간(초). 창 이동·확대 등 대부분의 UI 전환. */
    public static final double RESPONSE_STANDARD = 0.4;

    /** 빠른 반응 시간(초). 포인터를 직접 따라가는 요소(Dock 확대 등). */
    public static final double RESPONSE_SNAPPY = 0.28;

    /** 임계 감쇠 — 튕김 없음. 기본값. */
    public static final double DAMPING_STANDARD = 1.0;

    /** 약한 저감쇠 — 살짝 튕김. 제스처가 운동량을 실어 보냈을 때만 쓴다. */
    public static final double DAMPING_PLAYFUL = 0.78;

    /** 페이드 표준 시간. */
    public static final Duration FADE = Duration.millis(320);

    /** 부팅 단계 전환 페이드 — 장면이 통째로 바뀌므로 조금 더 길게. */
    public static final Duration FADE_SCENE = Duration.millis(620);

    /**
     * 감속 기반 easing.
     *
     * <p>제스처를 따라가지 않는(=중간에 잡아챌 수 없는) 전환에만 쓴다.
     * 붙잡을 수 있는 요소라면 {@link SpringValue}를 써야 한다.</p>
     */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.16, 1, 0.3, 1);

    /** 되돌아가는 전환용 — {@link #EASE_OUT}의 제어점을 뒤집은 곡선. */
    public static final Interpolator EASE_IN = Interpolator.SPLINE(0.7, 0, 0.84, 0);

    private Motion() {
    }

    /**
     * 노드를 서서히 나타나게 한다.
     *
     * @param node     대상
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void fadeIn(Node node, Duration duration, Runnable onFinish) {
        node.setVisible(true);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        if (onFinish != null) {
            fade.setOnFinished(e -> onFinish.run());
        }
        fade.play();
    }

    /**
     * 노드를 서서히 사라지게 한다. 끝나면 {@code visible=false}로 만들어
     * 마우스 이벤트가 투명한 잔상에 먹히지 않도록 한다.
     *
     * @param node     대상
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void fadeOut(Node node, Duration duration, Runnable onFinish) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);
        fade.setInterpolator(EASE_IN);
        fade.setOnFinished(e -> {
            node.setVisible(false);
            if (onFinish != null) {
                onFinish.run();
            }
        });
        fade.play();
    }

    /**
     * 유리 재질이 "도착"하는 느낌으로 나타나게 한다.
     *
     * <p>불투명도만 올리면 그림이 배경에 스며 나오는 것처럼 보인다. 실제 재질은
     * 크기와 함께 잡히므로 스케일을 같이 움직여야 물성이 생긴다.</p>
     *
     * @param node     대상
     * @param fromScale 시작 스케일 (1보다 작게)
     * @param duration 지속 시간
     */
    public static void materialize(Node node, double fromScale, Duration duration) {
        node.setOpacity(0);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);
        node.setVisible(true);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.scaleXProperty(), fromScale),
                        new KeyValue(node.scaleYProperty(), fromScale)),
                new KeyFrame(duration,
                        new KeyValue(node.opacityProperty(), 1, EASE_OUT),
                        new KeyValue(node.scaleXProperty(), 1, EASE_OUT),
                        new KeyValue(node.scaleYProperty(), 1, EASE_OUT)));
        timeline.play();
    }

    /**
     * 사라질 때는 나타날 때의 경로를 거꾸로 되짚는다.
     *
     * <p>들어온 길과 나가는 길이 다르면 공간 감각이 끊긴다.</p>
     *
     * @param node     대상
     * @param toScale  끝 스케일
     * @param duration 지속 시간
     * @param onFinish 완료 후 작업 ({@code null} 허용)
     */
    public static void dematerialize(Node node, double toScale, Duration duration, Runnable onFinish) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.opacityProperty(), node.getOpacity()),
                        new KeyValue(node.scaleXProperty(), node.getScaleX()),
                        new KeyValue(node.scaleYProperty(), node.getScaleY())),
                new KeyFrame(duration,
                        new KeyValue(node.opacityProperty(), 0, EASE_IN),
                        new KeyValue(node.scaleXProperty(), toScale, EASE_IN),
                        new KeyValue(node.scaleYProperty(), toScale, EASE_IN)));
        timeline.setOnFinished(e -> {
            node.setVisible(false);
            if (onFinish != null) {
                onFinish.run();
            }
        });
        timeline.play();
    }

    /**
     * 경계를 넘어선 만큼을 점점 덜 따라가게 만드는 고무줄 저항.
     *
     * <p>딱 멈추면 "굳었다"로 읽히고, 저항이 이어지면 "여기까지"로 읽힌다.</p>
     *
     * @param overshoot 경계를 넘어선 거리
     * @param dimension 기준 치수(대개 컨테이너의 폭이나 높이)
     * @return 실제로 이동시킬 거리
     */
    public static double rubberband(double overshoot, double dimension) {
        double constant = 0.55;
        return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
    }
}
```

---

# 28. SpringValue.java

**Path**
`src/main/java/forgeos/ui/SpringValue.java`

```java
package forgeos.ui;

import javafx.animation.AnimationTimer;

import java.util.function.DoubleConsumer;

/**
 * 하나의 실수 값을 스프링 물리로 목표값까지 끌고 가는 애니메이터.
 *
 * <h2>왜 Transition 이 아니라 스프링인가</h2>
 * <p>JavaFX의 {@code TranslateTransition} 류는 "지속 시간"이 고정된 스크립트다.
 * 재생 도중 목표가 바뀌면 현재 속도를 버리고 새 애니메이션을 처음부터 시작하므로,
 * 사용자가 움직이는 창을 다시 잡아채는 순간 눈에 보이는 <i>턱</i>이 생긴다.
 * 스프링은 상태(위치·속도)를 계속 들고 있다가 목표만 갈아끼우면 되므로
 * 언제 방향이 바뀌어도 궤적이 이어진다. 이것이 인터페이스가 "살아 있다"고
 * 느껴지는 거의 유일한 이유다.</p>
 *
 * <h2>파라미터</h2>
 * <p>물리 삼종세트(질량·강성·감쇠) 대신 Apple이 쓰는 두 값을 쓴다.</p>
 * <ul>
 *   <li><b>response</b> — 목표에 도달하는 데 걸리는 체감 시간(초). 작을수록 날렵하다.
 *       지속 시간이 아니다 — 스프링에는 정해진 끝이 없다.</li>
 *   <li><b>damping</b> — 감쇠비. {@code 1.0}이면 튕김 없이 부드럽게 멎고,
 *       {@code 1.0} 미만이면 목표를 지나쳤다가 되돌아온다.</li>
 * </ul>
 * <p>기본값은 {@code damping = 1.0}이다. 튕김은 사용자의 제스처가 <b>운동량을
 * 실어 보냈을 때만</b> 어울린다. 그냥 나타난 메뉴가 출렁이면 싸구려로 보인다.</p>
 *
 * <h2>수치 안정성</h2>
 * <p>프레임 간격이 길어지면(창을 끌다 GC가 끼면 30ms도 나온다) 뻣뻣한 스프링은
 * 명시적 오일러 적분에서 발산한다. 그래서 프레임 시간을 잘게 쪼개
 * ({@value #SUB_STEP}초) 여러 번 적분한다.</p>
 */
public final class SpringValue extends AnimationTimer {

    /** 적분 서브스텝(초). 240Hz 상당 — 이보다 크면 뻣뻣한 스프링이 발산한다. */
    private static final double SUB_STEP = 1.0 / 240.0;

    /** 한 프레임에 소화할 최대 시간(초). 탭 전환 등으로 프레임이 크게 밀렸을 때의 폭주 방지. */
    private static final double MAX_FRAME = 0.1;

    /** 정지 판정 임계값(픽셀). 이보다 가까우면서 느리면 목표에 스냅하고 멈춘다. */
    private static final double SETTLE_DISTANCE = 0.25;

    /** 정지 판정 속도(픽셀/초). */
    private static final double SETTLE_VELOCITY = 0.25;

    private final DoubleConsumer sink;

    private double response = Motion.RESPONSE_STANDARD;
    private double damping = Motion.DAMPING_STANDARD;

    private double value;
    private double velocity;
    private double target;

    private long lastNanos;
    private boolean active;
    private Runnable onSettle;

    /**
     * 값이 바뀔 때마다 호출될 소비자를 받아 스프링을 만든다.
     *
     * @param sink 매 프레임 갱신된 값을 받아 실제 노드에 적용하는 함수
     */
    public SpringValue(DoubleConsumer sink) {
        this.sink = sink;
    }

    /**
     * 스프링 특성을 바꾼다.
     *
     * @param newResponse 반응 시간(초)
     * @param newDamping  감쇠비 (1.0 = 임계 감쇠)
     * @return 메서드 체이닝을 위한 자기 자신
     */
    public SpringValue tune(double newResponse, double newDamping) {
        this.response = Math.max(newResponse, 0.05);
        this.damping = Math.max(newDamping, 0.05);
        return this;
    }

    /**
     * 애니메이션 없이 현재 값을 즉시 설정한다. 속도도 함께 0으로 만든다.
     *
     * @param newValue 새 값
     */
    public void reset(double newValue) {
        stop();
        this.active = false;
        this.value = newValue;
        this.target = newValue;
        this.velocity = 0;
        sink.accept(newValue);
    }

    /**
     * 목표를 새로 지정한다. <b>진행 중이던 속도는 그대로 유지된다</b> —
     * 이것이 방향 전환에서 "벽에 부딪히는" 느낌을 없애는 핵심이다.
     *
     * @param newTarget 새 목표값
     */
    public void setTarget(double newTarget) {
        this.target = newTarget;
        if (isSettled()) {
            sink.accept(value);
            return;
        }
        if (!active) {
            active = true;
            lastNanos = 0;
            start();
        }
    }

    /**
     * 제스처가 놓여난 순간의 속도를 그대로 이어받는다.
     *
     * @param pixelsPerSecond 손가락(포인터)이 놓인 시점의 속도
     */
    public void handoffVelocity(double pixelsPerSecond) {
        this.velocity = pixelsPerSecond;
    }

    /**
     * 정지했을 때 한 번 실행할 작업을 건다.
     *
     * @param action 정지 콜백. {@code null}이면 해제
     */
    public void setOnSettle(Runnable action) {
        this.onSettle = action;
    }

    /**
     * 현재 값.
     *
     * @return 마지막으로 계산된 값
     */
    public double value() {
        return value;
    }

    @Override
    public void handle(long now) {
        if (lastNanos == 0) {
            lastNanos = now;
            return;
        }
        double frame = Math.min((now - lastNanos) / 1_000_000_000.0, MAX_FRAME);
        lastNanos = now;

        double omega = 2 * Math.PI / response;
        double remaining = frame;
        while (remaining > 0) {
            double step = Math.min(SUB_STEP, remaining);
            remaining -= step;

            double acceleration = -2 * damping * omega * velocity - omega * omega * (value - target);
            velocity += acceleration * step;
            value += velocity * step;
        }

        if (isSettled()) {
            value = target;
            velocity = 0;
            sink.accept(value);
            active = false;
            stop();
            if (onSettle != null) {
                Runnable action = onSettle;
                onSettle = null;
                action.run();
            }
            return;
        }
        sink.accept(value);
    }

    private boolean isSettled() {
        return Math.abs(value - target) < SETTLE_DISTANCE && Math.abs(velocity) < SETTLE_VELOCITY;
    }
}
```

---

# 29. Styles.java

**Path**
`src/main/java/forgeos/ui/Styles.java`

```java
package forgeos.ui;

import javafx.css.PseudoClass;

/**
 * ForgeOS가 쓰는 CSS 의사 클래스 모음.
 *
 * <p>상태를 자바에서 {@code setStyle}로 칠하지 않고 의사 클래스만 토글한다.
 * 그래야 "활성 창은 어떻게 보이는가", "실행 중인 Dock 아이콘은 어떻게
 * 보이는가"를 CSS 파일 한 줄로 바꿀 수 있다. 색과 치수가 자바 코드로 새어
 * 들어가는 순간 디자인 변경은 컴파일이 필요한 일이 된다.</p>
 */
public final class Styles {

    /** 활성(최상단) 창. 비활성 창은 그림자와 채도를 낮춰 뒤로 물러나게 한다. */
    public static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    /** Dock 아이콘이 가리키는 앱이 실행 중일 때. */
    public static final PseudoClass RUNNING = PseudoClass.getPseudoClass("running");

    /** 위험한 동작(프로세스 강제 종료, 교착 복구)을 하는 버튼. */
    public static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");

    /** 교착 상태에 얽힌 프로세스 노드. */
    public static final PseudoClass DEADLOCKED = PseudoClass.getPseudoClass("deadlocked");

    /** 켜짐 상태의 토글 스위치. */
    public static final PseudoClass ON = PseudoClass.getPseudoClass("on");

    private Styles() {
    }
}
```

---

# 30. ThemeManager.java

**Path**
`src/main/java/forgeos/ui/ThemeManager.java`

```java
package forgeos.ui;

import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 라이트/다크 테마 전환기.
 *
 * <h2>왜 스타일시트를 통째로 갈아 끼우는가</h2>
 * <p>루트 노드에 {@code .light} 클래스를 붙여 토큰을 덮어쓰는 방법이 더 간단해
 * 보이지만, 그러면 <b>팝업이 따라오지 않는다</b>. 콘텍스트 메뉴·툴팁·콤보박스
 * 목록은 각자 자기 {@code Scene}을 갖고 그 루트에는 우리가 클래스를 붙일 수 없다.
 * 반면 팝업은 소유 Scene의 스타일시트를 물려받으므로, 토큰 정의 파일 자체를
 * 바꾸면 팝업까지 한 번에 따라온다.</p>
 *
 * <p>그래서 스타일시트를 <b>토큰</b>과 <b>구조</b>로 나눠 두었다.
 * {@code tokens-*.css}는 색만, {@code theme.css}/{@code apps.css}는 색을 직접
 * 쓰지 않고 토큰 이름만 참조한다.</p>
 *
 * <h2>전환은 부드럽게</h2>
 * <p>밝기가 한 프레임에 뒤집히면 눈이 아프다. 전환 순간에만 얇은 막을 덮었다
 * 걷어내서 급격한 명도 변화를 완충한다.</p>
 */
public final class ThemeManager {

    /** 테마 종류. */
    public enum Mode {
        /** 어두운 테마. 기본값이다 — 이 시뮬레이터는 밤에 오래 들여다보는 물건이다. */
        DARK("/forgeos/css/tokens-dark.css"),
        /** 밝은 테마. */
        LIGHT("/forgeos/css/tokens-light.css");

        private final String tokensPath;

        Mode(String tokensPath) {
            this.tokensPath = tokensPath;
        }
    }

    /** 토큰 뒤에 항상 따라붙는 구조 스타일시트. 순서가 곧 우선순위다. */
    private static final List<String> STRUCTURE = List.of(
            "/forgeos/css/theme.css",
            "/forgeos/css/apps.css");

    /** 전환 막이 덮이는 시간. */
    private static final Duration VEIL_IN = Duration.millis(110);

    /** 전환 막이 걷히는 시간. 걷힐 때가 더 느려야 "바뀌었구나"로 읽힌다. */
    private static final Duration VEIL_OUT = Duration.millis(280);

    private final Scene scene;
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.DARK);
    private final Region veil = new Region();

    private boolean switching;

    /**
     * 주어진 Scene에 테마를 적용한다. 생성 시점에 곧바로 다크 테마가 적용된다.
     *
     * @param scene 대상 Scene
     */
    public ThemeManager(Scene scene) {
        this.scene = scene;

        veil.getStyleClass().add("theme-veil");
        veil.setMouseTransparent(true);
        veil.setOpacity(0);
        veil.setVisible(false);

        applyStylesheets();
    }

    /**
     * 현재 테마.
     *
     * @return 테마 프로퍼티
     */
    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    /**
     * 현재 테마 값.
     *
     * @return 다크 또는 라이트
     */
    public Mode mode() {
        return mode.get();
    }

    /** 라이트 ↔ 다크를 뒤집는다. */
    public void toggle() {
        setMode(mode.get() == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    /**
     * 테마를 지정한다. 이미 같은 테마면 아무 일도 하지 않는다.
     *
     * @param target 적용할 테마
     */
    public void setMode(Mode target) {
        if (mode.get() == target || switching) {
            return;
        }
        switching = true;
        withVeil(() -> {
            mode.set(target);
            applyStylesheets();
        });
    }

    private void applyStylesheets() {
        List<String> sheets = new ArrayList<>();
        addIfPresent(sheets, mode.get().tokensPath);
        for (String path : STRUCTURE) {
            addIfPresent(sheets, path);
        }
        scene.getStylesheets().setAll(sheets);
    }

    private static void addIfPresent(List<String> sheets, String path) {
        URL url = ThemeManager.class.getResource(path);
        if (url != null) {
            sheets.add(url.toExternalForm());
        }
    }

    /**
     * 막을 덮고, 스타일을 갈아 끼우고, 막을 걷는다.
     *
     * <p>Scene 루트가 {@link Pane}이 아니면(테스트 등) 막 없이 바로 갈아 끼운다 —
     * 연출 때문에 기능이 막히면 안 된다.</p>
     */
    private void withVeil(Runnable swap) {
        if (!(scene.getRoot() instanceof Pane root)) {
            swap.run();
            switching = false;
            return;
        }

        if (!root.getChildren().contains(veil)) {
            root.getChildren().add(veil);
        }
        veil.toFront();
        veil.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(VEIL_IN, veil);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Motion.EASE_OUT);
        fadeIn.setOnFinished(e -> {
            swap.run();

            FadeTransition fadeOut = new FadeTransition(VEIL_OUT, veil);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setInterpolator(Motion.EASE_OUT);
            fadeOut.setOnFinished(done -> {
                veil.setVisible(false);
                switching = false;
            });
            fadeOut.play();
        });
        fadeIn.play();
    }
}
```

---

# 31. ToggleSwitch.java

**Path**
`src/main/java/forgeos/ui/ToggleSwitch.java`

```java
package forgeos.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * on/off 토글 스위치.
 *
 * <p>JavaFX 기본 {@code CheckBox}로도 기능은 되지만, 은행원 알고리즘처럼
 * "켜는 순간 커널의 동작 방식이 바뀌는" 설정에는 체크박스가 어울리지 않는다.
 * 체크박스는 목록에서 항목을 고르는 도구고, 스위치는 상태를 바꾸는 도구다.
 * 손잡이가 실제로 이동하는 것을 보여 주면 "지금 켠 것"이 분명해진다.</p>
 */
public final class ToggleSwitch extends StackPane {

    /** 트랙 폭(px). */
    private static final double TRACK_WIDTH = 46;

    /** 트랙 높이(px). */
    private static final double TRACK_HEIGHT = 26;

    /** 손잡이 반지름(px). */
    private static final double THUMB_RADIUS = 10;

    /** 트랙 양 끝 여백(px). */
    private static final double INSET = 3;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Circle thumb = new Circle(THUMB_RADIUS);

    private final SpringValue slide = new SpringValue(thumb::setTranslateX)
            .tune(Motion.RESPONSE_SNAPPY, Motion.DAMPING_STANDARD);

    /** 꺼진 상태의 스위치를 만든다. */
    public ToggleSwitch() {
        getStyleClass().add("toggle-switch");

        Region track = new Region();
        track.getStyleClass().add("toggle-track");
        track.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        track.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        thumb.getStyleClass().add("toggle-thumb");

        getChildren().addAll(track, thumb);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        slide.reset(offPosition());
        setOnMouseClicked(e -> setSelected(!isSelected()));

        selected.addListener((obs, was, now) -> {
            pseudoClassStateChanged(Styles.ON, now);
            slide.setTarget(now ? onPosition() : offPosition());
        });
    }

    private static double offPosition() {
        return -(TRACK_WIDTH / 2 - THUMB_RADIUS - INSET);
    }

    private static double onPosition() {
        return TRACK_WIDTH / 2 - THUMB_RADIUS - INSET;
    }

    /**
     * 켜짐 여부 프로퍼티.
     *
     * @return 프로퍼티
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * 켜짐 여부.
     *
     * @return 켜져 있으면 참
     */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * 켜짐 여부를 설정한다. 애니메이션과 함께 반영된다.
     *
     * @param value 새 상태
     */
    public void setSelected(boolean value) {
        selected.set(value);
    }

    /**
     * 애니메이션 없이 상태를 맞춘다. 커널 상태를 화면에 되비출 때 쓴다 —
     * 사용자가 누르지도 않았는데 손잡이가 미끄러지면 누가 눌렀는지 헷갈린다.
     *
     * @param value 새 상태
     */
    public void syncSilently(boolean value) {
        if (selected.get() == value) {
            return;
        }
        selected.set(value);
        slide.reset(value ? onPosition() : offPosition());
    }
}
```

---

# 32. ForgeWindow.java

**Path**
`src/main/java/forgeos/wm/ForgeWindow.java`

```java
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
```

---

# 33. TrafficLights.java

**Path**
`src/main/java/forgeos/wm/TrafficLights.java`

```java
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
```

---

# 34. WindowManager.java

**Path**
`src/main/java/forgeos/wm/WindowManager.java`

```java
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
```

---

# 35. module-info.java

**Path**
`src/main/java/module-info.java`

```java
/**
 * ForgeOS — ForgeFramework 커널 위에 올라가는 JavaFX 데스크탑 환경.
 *
 * <p>커널({@code forgeframework})과 명령어 계층({@code forgeframework.cli})에
 * <b>단방향으로만</b> 의존한다. 커널은 ForgeOS의 존재를 모르며, 알 필요도 없다.</p>
 *
 * <h2>내보내지 않는 이유</h2>
 * <p>ForgeOS는 라이브러리가 아니라 최종 애플리케이션이므로 어떤 패키지도
 * 공개 API로 약속하지 않는다. 다만 JavaFX 런타임이 {@code Application}
 * 서브클래스를 리플렉션으로 생성해야 하므로 {@code forgeos} 패키지만
 * {@code javafx.graphics}에 한정 공개한다.</p>
 */
module forgeos {

    requires javafx.controls;
    requires javafx.graphics;

    /** 부팅 애니메이션(MP4) 재생용. 이 모듈이 빠지면 시네마틱 부팅 2단계가 통째로 사라진다. */
    requires javafx.media;

    /** 커널. ForgeOS의 모든 상태는 여기서 나온다. */
    requires forgeframework;

    /** 명령어 계층 — Terminal 앱이 CLI와 같은 명령어 세트를 그대로 쓴다. */
    requires forgeframework.cli;

    /** JavaFX 런타임이 {@code ForgeOsApp}을 리플렉션으로 생성할 수 있도록 한정 공개. */
    exports forgeos to javafx.graphics;
}
```

---

# 36. apps.css

**Path**
`src/main/resources/forgeos/css/apps.css`

```css
/*
 * ForgeOS 내장 앱 스타일 — 터미널 · 활성 상태 보기 · Finder · 교착 상태 관리자.
 *
 * theme.css 와 같은 규칙: 색은 tokens-*.css 가 정한 이름만 참조한다.
 * 여기서 새 색을 만들면 앱마다 조금씩 다른 회색이 생기고, 그 순간 화면 전체가
 * "같은 시스템"으로 보이지 않게 된다. 라이트 테마에서는 아예 안 보이기도 한다.
 */

/* ─────────────────────────── 터미널 ─────────────────────────── */
/*
 * 터미널만은 두 테마에서 모두 어둡다. 라이트 테마를 켰다고 셸이 하얘지면
 * ps · meminfo 의 고정폭 출력이 문서처럼 보이고, 무엇보다 부팅 콘솔과
 * 같은 물건이라는 감각이 끊긴다. 그래서 이 구역만 고정색을 쓴다.
 */

.terminal {
    -fx-background-color: #06090d;
    -fx-background-radius: 0 0 12px 12px;
}

.terminal-scroll,
.terminal-scroll > .viewport {
    -fx-background-color: transparent;
}

.terminal-output {
    -fx-padding: 14px 16px;
    -fx-line-spacing: 1.5px;
}

/*
 * 터미널 안의 모든 글자는 고정폭이다. ps · meminfo 같은 명령이 공백으로 열을
 * 맞춰 출력하므로, 비례폭 글꼴을 쓰면 표가 통째로 무너진다.
 */
.terminal-output .text,
.terminal-input,
.terminal-prompt {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 12.5px;
}

.terminal-result {
    -fx-fill: #d5dbe4;
}

.terminal-error {
    -fx-fill: #ff9c85;
}

.terminal-echo {
    -fx-fill: #eceff4;
}

.terminal-prompt-echo {
    -fx-fill: #22d3ee;
}

.terminal-notice {
    -fx-fill: #ffd189;
}

.terminal-dim {
    -fx-fill: #6a7383;
}

.terminal-input-row {
    -fx-fill-height: false;
    -fx-background-color: rgba(255, 255, 255, 0.04);
    -fx-border-color: rgba(255, 255, 255, 0.08) transparent transparent transparent;
    -fx-border-width: 1px 0 0 0;
    -fx-padding: 8px 16px;
    -fx-spacing: 6px;
    -fx-alignment: center-left;
}

.terminal-prompt {
    -fx-text-fill: #22d3ee;
}

/* 입력줄은 상자가 아니라 출력의 연장선처럼 보여야 한다 — 테두리도 배경도 없다. */
.terminal-input {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-text-fill: #eceff4;
    -fx-highlight-fill: rgba(34, 211, 238, 0.30);
    -fx-padding: 0;
}

.terminal-input:focused {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
}

/* ─────────────────────────── 활성 상태 보기 ─────────────────────────── */

.activity-monitor {
    -fx-background-color: transparent;
}

.monitor-left {
    -fx-background-color: transparent;
}

.monitor-sidebar {
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent transparent transparent -edge-line;
    -fx-border-width: 0 0 0 1px;
    -fx-padding: 18px 20px;
    -fx-spacing: 20px;
    -fx-pref-width: 236px;
}

.sidebar-note {
    -fx-font-size: 10.5px;
    -fx-text-fill: -text-dim;
    -fx-line-spacing: 2px;
    -fx-text-alignment: center;
    -fx-wrap-text: true;
}

.donut {
    -fx-spacing: 6px;
}

.donut-track {
    -fx-stroke: -fill-medium;
}

.donut-progress {
    -fx-stroke: -electric-cyan;
}

/* 도넛의 강조색은 무엇을 재는지에 따라 다르다.
   물리 프레임은 소진되면 곤란하므로 Ember, 힙은 주의 신호라 Gold,
   TLB 적중률은 높을수록 좋은 값이라 Cyan 이다. */
.accent-ember {
    -fx-stroke: -forge-ember;
}

.accent-gold {
    -fx-stroke: -molten-gold;
}

.accent-cyan {
    -fx-stroke: -electric-cyan;
}

.donut-value {
    -fx-font-size: 21px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-primary;
}

.donut-caption {
    -fx-font-size: 12px;
    -fx-text-fill: -text-secondary;
}

.donut-detail {
    -fx-font-size: 10.5px;
    -fx-text-fill: -text-dim;
}

/* 상태 배지 — 색이 곧 의미다. 텍스트를 읽기 전에 상태가 보여야 한다. */
.state-badge {
    -fx-font-size: 10.5px;
    -fx-font-weight: bold;
    -fx-padding: 2px 8px;
    -fx-background-radius: 5px;
}

.state-new {
    -fx-background-color: -fill-medium;
    -fx-text-fill: -text-secondary;
}

.state-ready {
    -fx-background-color: -accent-tint;
    -fx-text-fill: -accent-text;
}

.state-running {
    -fx-background-color: -accent-tint-strong;
    -fx-text-fill: -accent-text;
}

.state-waiting {
    -fx-background-color: -warn-tint;
    -fx-text-fill: -warn-text;
}

.state-terminated {
    -fx-background-color: -danger-tint;
    -fx-text-fill: -danger-text;
}

/* ─────────────────────────── Finder ─────────────────────────── */

.finder {
    -fx-background-color: transparent;
}

.finder-path {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 12px;
    -fx-text-fill: -text-secondary;
}

.finder-scroll,
.finder-scroll > .viewport {
    -fx-background-color: transparent;
}

.finder-columns {
    -fx-background-color: transparent;
}

/* 컬럼 사이의 세로선이 macOS Finder 의 그 선이다. 컬럼이 여러 개라는 사실을
   말해 주는 유일한 장치이므로 지우면 안 된다. */
.finder-column {
    -fx-background-color: transparent;
    -fx-border-color: transparent -edge-line transparent transparent;
    -fx-border-width: 0 1px 0 0;
}

.entry-row {
    -fx-fill-height: false;
    -fx-padding: 6px 10px;
    -fx-spacing: 8px;
}

.entry-name {
    -fx-font-size: 12.5px;
    -fx-text-fill: -text-primary;
}

.entry-size {
    -fx-font-size: 10.5px;
    -fx-text-fill: -text-dim;
}

.entry-icon {
    -fx-stroke: -electric-cyan;
    -fx-stroke-width: 1.6;
}

.list-cell:filled .entry-icon {
    -fx-stroke: -molten-gold;
}

.entry-chevron {
    -fx-stroke: -text-dim;
    -fx-stroke-width: 1.8;
}

.finder-preview {
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent transparent transparent -edge-line;
    -fx-border-width: 0 0 0 1px;
    -fx-padding: 24px 20px;
    -fx-spacing: 10px;
    -fx-alignment: top-center;
    -fx-pref-width: 250px;
}

.preview-icon {
    -fx-stroke: -text-secondary;
    -fx-stroke-width: 1.2;
}

.preview-name {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-primary;
}

.preview-meta {
    -fx-font-size: 11px;
    -fx-text-fill: -text-dim;
}

.preview-body {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 11.5px;
    -fx-text-fill: -text-secondary;
    -fx-background-color: -fill-sunken;
    -fx-background-radius: 8px;
    -fx-padding: 10px 12px;
    -fx-min-width: 210px;
    -fx-line-spacing: 2px;
}

/* ─────────────────────────── 교착 상태 관리자 ─────────────────────────── */

.deadlock-resolver {
    -fx-background-color: transparent;
}

.deadlock-left {
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent -edge-line transparent transparent;
    -fx-border-width: 0 1px 0 0;
    -fx-padding: 12px;
    -fx-spacing: 10px;
    -fx-pref-width: 470px;
}

.available-chip {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 11.5px;
    -fx-text-fill: -accent-text;
    -fx-background-color: -accent-tint;
    -fx-background-radius: 8px;
    -fx-border-color: -accent-border;
    -fx-border-radius: 8px;
    -fx-padding: 8px 12px;
}

.resource-table .table-cell {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 11.5px;
}

.request-bar {
    -fx-border-color: -edge-line transparent transparent transparent;
    -fx-border-width: 1px 0 0 0;
    -fx-spacing: 6px;
}

.request-hint {
    -fx-font-size: 10.5px;
    -fx-text-fill: -text-dim;
    -fx-wrap-text: true;
    -fx-max-width: 320px;
}

.policy-box {
    -fx-pref-width: 168px;
}

/* ── Wait-For 그래프 ── */

.graph-empty {
    -fx-font-size: 11.5px;
    -fx-text-fill: -text-dim;
    -fx-text-alignment: center;
}

.graph-edge {
    -fx-stroke: -warn-border;
    -fx-stroke-width: 1.6;
}

.graph-arrow {
    -fx-fill: -molten-gold;
}

.graph-edge-label {
    -fx-font-size: 10px;
    -fx-text-fill: -warn-text;
    -fx-background-color: -warn-tint;
    -fx-background-radius: 5px;
    -fx-padding: 1px 5px;
}

.graph-node-disc {
    -fx-stroke-width: 1.2;
}

.graph-node-label {
    -fx-font-size: 10.5px;
    -fx-text-fill: -text-secondary;
    -fx-text-alignment: center;
}

.graph-node-halo {
    -fx-fill: -forge-ember;
}

/* 교착에 얽힌 노드만 Ember 로 물든다. 다른 노드가 전부 무채색이라 눈이
   곧바로 여기로 간다 — 사이클을 찾아 헤맬 필요가 없다. */
.graph-node:deadlocked .graph-node-disc {
    -fx-stroke: -danger-border;
    -fx-stroke-width: 1.8;
}

.graph-node:deadlocked .graph-node-label {
    -fx-text-fill: -danger-text;
    -fx-font-weight: bold;
}
```

---

# 37. theme.css

**Path**
`src/main/resources/forgeos/css/theme.css`

```css
/*
 * ForgeOS 구조 스타일 — 부팅 화면 · 바탕화면 · 메뉴바 · Dock · 창 · 공통 컨트롤.
 *
 * 규칙 둘.
 *
 * 1. 색과 치수는 자바 코드에 두지 않는다. setStyle() 이 등장하는 순간 디자인
 *    변경은 컴파일이 필요한 일이 되고, 두 곳에 흩어진 값은 반드시 어긋난다.
 *    자바가 하는 일은 styleClass 와 의사 클래스를 붙였다 떼는 것뿐이다.
 *
 * 2. 이 파일에는 리터럴 색을 쓰지 않는다. 전부 tokens-dark.css / tokens-light.css
 *    가 정의한 이름만 참조한다. 여기에 #1a1a1a 하나를 적는 순간 그 요소만
 *    라이트 테마에서 검게 남는다. (예외는 부팅 화면과 브랜드 마크 — 아래 주석 참고.)
 */

/* ─────────────────────────── 기본 ─────────────────────────── */

.root {
    -fx-font-family: "SF Pro Text", "Apple SD Gothic Neo", "Pretendard",
                     "Noto Sans KR", "Malgun Gothic", sans-serif;
    -fx-font-size: 13px;
    -fx-text-fill: -text-primary;
    -fx-background-color: -forge-bg;
    -fx-focus-color: -electric-cyan;
    -fx-faint-focus-color: -accent-tint;
}

.forge-root {
    -fx-background-color: -forge-bg;
}

/* 아이콘 기본값. 굵기를 한 곳에서 정해야 아이콘들이 같은 붓으로 그린 것처럼 보인다. */
.glyph {
    -fx-stroke: -text-secondary;
    -fx-stroke-width: 1.7;
    -fx-fill: null;
}

.glyph-filled {
    -fx-fill: -text-secondary;
    -fx-stroke: null;
}

/* 테마 전환 순간에만 덮이는 막. 명도가 한 프레임에 뒤집히는 것을 완충한다. */
.theme-veil {
    -fx-background-color: -veil;
}

/* ─────────────────────────── 부팅 ─────────────────────────── */
/*
 * 부팅 화면은 테마를 따르지 않는 유일한 화면이다. 전원을 넣은 기계의 콘솔은
 * 라이트 모드라는 것이 없다 — 어느 테마를 쓰든 검은 화면에서 시작해야
 * "지금 커널이 올라오는 중"으로 읽힌다. 그래서 여기만 리터럴 색을 쓴다.
 */

.boot-root {
    -fx-background-color: #000000;
}

.boot-console {
    -fx-background-color: #000000;
    -fx-padding: 44px 56px;
}

.boot-console-scroll,
.boot-console-scroll > .viewport {
    -fx-background-color: transparent;
}

.boot-console-column {
    -fx-spacing: 0;
}

.boot-console-line,
.boot-console-cursor {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 13.5px;
    /* 형광 초록 대신 살짝 청록으로 기울인 회백색 — 터미널 흉내가 아니라 실제 콘솔의 색이다. */
    -fx-text-fill: #c8d3d8;
}

.boot-console-cursor {
    -fx-text-fill: #22d3ee;
}

.boot-console-cursor-row {
    -fx-padding: 0;
}

.boot-video {
    -fx-background-color: #000000;
}

/* ─────────────────────────── 바탕화면 ─────────────────────────── */
/*
 * 배경화면은 이미지 파일이 아니라 벡터다. assets/forgeOS-logo.svg 의 패스를
 * ForgeMark 로 다시 세워 그리므로 어떤 해상도에서도 깨지지 않고, 테마에 따라
 * 밝기가 따라온다. 그라디언트 자체는 테마마다 다르므로 tokens-*.css 에 있다.
 */

.desktop {
    -fx-background-color: -forge-bg;
}

.wallpaper-brand {
    -fx-alignment: center;
    -fx-spacing: 24px;
}

.wallpaper-wordmark {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", "Consolas", monospace;
    -fx-font-size: 15px;
    -fx-font-weight: bold;
}

.wallpaper-ring {
    -fx-stroke-width: 1;
    -fx-fill: null;
}

/* ── 로고 마크 ──
   원본 SVG 의 색을 그대로 옮겼다. 브랜드 마크는 테마가 바뀌어도 같은 물건이므로
   토큰이 아니라 고정색이며, 밝기 조절은 .wallpaper-brand 의 불투명도가 맡는다. */

.mark-base {
    -fx-fill: #0f172a;
    -fx-stroke: #334155;
    -fx-stroke-width: 3;
}

.mark-body {
    -fx-fill: #1e293b;
    -fx-stroke: #334155;
    -fx-stroke-width: 3;
}

.mark-steel-left {
    -fx-fill: linear-gradient(to bottom right, #334155, #1e293b);
    -fx-stroke: #475569;
    -fx-stroke-width: 3;
}

.mark-steel-right {
    -fx-fill: linear-gradient(to bottom right, #475569, #334155);
    -fx-stroke: #64748b;
    -fx-stroke-width: 3;
}

.mark-steel-top {
    -fx-fill: linear-gradient(to bottom right, #64748b, #475569);
    -fx-stroke: #94a3b8;
    -fx-stroke-width: 3;
}

.mark-circuit {
    -fx-stroke: #ff6b00;
    -fx-stroke-width: 3.5;
}

.mark-circuit-node {
    -fx-fill: #ffc400;
}

.mark-window-back {
    -fx-stroke: #8b5cf6;
    -fx-stroke-width: 3;
    -fx-opacity: 0.6;
}

.mark-window-front {
    -fx-stroke: #d946ef;
    -fx-stroke-width: 4;
    -fx-opacity: 0.9;
}

/* 로고 안의 작은 신호등. ForgeOS 의 창 제어 버튼과 같은 순서·같은 뜻이다. */
.mark-dot-close {
    -fx-fill: #ff5f56;
}

.mark-dot-minimize {
    -fx-fill: #ffbd2e;
}

.mark-dot-zoom {
    -fx-fill: #27c93f;
}

/* 불꽃의 발광. SVG 의 feGaussianBlur 를 dropshadow 로 대신한다 —
   JavaFX CSS 의 효과 함수는 그림자 계열뿐이지만, 색을 밝게 주면 발광이 된다. */
.mark-flame {
    -fx-fill: linear-gradient(from 0% 100% to 100% 0%,
        #ff2a00 0%, #ff7700 50%, #ffc400 90%, #fff5d6 100%);
    -fx-stroke: null;
    -fx-effect: dropshadow(gaussian, rgba(255, 106, 0, 0.85), 64, 0.35, 0, 0);
}

.mark-flame-core {
    -fx-fill: rgba(255, 255, 255, 0.95);
    -fx-stroke: null;
}

.mark-spark-gold {
    -fx-fill: #ffc400;
}

.mark-spark-violet {
    -fx-fill: #d946ef;
}

.mark-spark-white {
    -fx-fill: #ffffff;
}

.mark-glow-ember {
    -fx-fill: rgba(255, 85, 0, 0.34);
}

.mark-glow-violet {
    -fx-fill: rgba(217, 70, 239, 0.26);
}

/* ─────────────────────────── 메뉴바 ─────────────────────────── */

.menu-bar {
    /* HBox 는 기본적으로 자식을 세로로 꽉 늘린다. 그대로 두면 배경이 있는 칩과
       메뉴 버튼이 바 높이 전체를 차지해 답답해진다. 자연스러운 높이로 두고 가운데 정렬. */
    -fx-fill-height: false;
    -fx-background-color: -glass-thin;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
    -fx-padding: 0 14px;
    -fx-spacing: 12px;
    -fx-alignment: center-left;
    -fx-min-height: 30px;
    -fx-pref-height: 30px;
    -fx-effect: dropshadow(gaussian, -shadow-menu, 18, 0, 0, 4);
}

.menu-logo {
    -fx-stroke: -forge-ember;
    -fx-stroke-width: 1.6;
}

.menu-active-app {
    -fx-font-size: 12.5px;
    -fx-font-weight: bold;
}

.menu-chip {
    -fx-font-size: 11.5px;
    -fx-text-fill: -text-secondary;
    -fx-background-color: -fill-soft;
    -fx-background-radius: 6px;
    -fx-padding: 2px 8px;
}

.menu-clock {
    -fx-font-size: 12px;
    -fx-text-fill: -text-primary;
    -fx-padding: 0 2px;
}

.menu-button {
    -fx-background-color: transparent;
    -fx-text-fill: -text-secondary;
    -fx-font-size: 12.5px;
    -fx-padding: 2px 6px;
}

.menu-button:hover {
    -fx-background-color: -fill-medium;
    -fx-background-radius: 5px;
}

.menu-button > .arrow-button {
    -fx-padding: 0;
}

.menu-button > .arrow-button > .arrow {
    -fx-background-color: -text-dim;
    -fx-padding: 2px 3px;
}

/* 테마 토글처럼 아이콘 하나만 있는 버튼. */
.menu-icon-button {
    -fx-background-color: transparent;
    -fx-background-radius: 6px;
    -fx-padding: 3px 6px;
    -fx-cursor: hand;
}

.menu-icon-button:hover {
    -fx-background-color: -fill-medium;
}

.menu-icon-glyph {
    -fx-stroke: -text-secondary;
    -fx-stroke-width: 1.7;
}

.menu-icon-button:hover .menu-icon-glyph {
    -fx-stroke: -molten-gold;
}

.context-menu {
    -fx-background-color: -glass-thick;
    -fx-background-radius: 10px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 10px;
    -fx-padding: 5px;
    -fx-effect: dropshadow(gaussian, -shadow-popup, 26, 0, 0, 10);
}

.menu-item {
    -fx-background-radius: 6px;
    -fx-padding: 5px 12px;
}

.menu-item .label {
    -fx-text-fill: -text-primary;
    -fx-font-size: 12.5px;
}

.menu-item:focused {
    -fx-background-color: -accent-tint-strong;
}

/* ─────────────────────────── Dock ─────────────────────────── */

.dock {
    -fx-fill-height: false;
    -fx-background-color: -glass-regular;
    -fx-background-radius: 22px;
    -fx-border-color: -edge-light;
    -fx-border-radius: 22px;
    -fx-border-width: 1px;
    /*
     * 아이콘 사이 간격. 좁으면 하나를 겨냥하다 옆을 누르게 되고, 확대 효과도
     * 이웃과 겹쳐 뭉개진다. 간격을 바꾸면 DockView.SPREAD 도 같이 키워야 한다.
     */
    -fx-padding: 10px 18px;
    -fx-spacing: 20px;
    -fx-alignment: bottom-center;
    /* 큰 표면일수록 그림자가 깊어야 유리처럼 떠 보인다. */
    -fx-effect: dropshadow(gaussian, -shadow-dock, 42, 0, 0, 14);
}

.dock-item {
    -fx-cursor: hand;
}

.dock-item-column {
    -fx-alignment: bottom-center;
    -fx-spacing: 4px;
}

.dock-tile {
    -fx-min-width: 46px;
    -fx-min-height: 46px;
    -fx-pref-width: 46px;
    -fx-pref-height: 46px;
    -fx-background-radius: 13px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 13px;
}

.dock-item:hover .dock-glyph {
    -fx-stroke: -text-primary;
}

.dock-item:running .dock-tile {
    -fx-border-color: -accent-border;
}

.dock-item:running .dock-glyph {
    -fx-stroke: -electric-cyan;
}

.dock-glyph {
    -fx-stroke: -text-secondary;
    -fx-stroke-width: 1.8;
}

.dock-running-dot {
    -fx-fill: -electric-cyan;
}

/* ─────────────────────────── 창 ─────────────────────────── */

.window-layer {
    -fx-background-color: transparent;
}

.forge-window {
    -fx-background-color: -glass-thick;
    -fx-background-radius: 12px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 12px;
    -fx-border-width: 1px;
    /* 비활성 창의 그림자는 얕다. 깊이가 곧 "지금 이것을 보고 있다"는 신호다. */
    -fx-effect: dropshadow(gaussian, -shadow-window, 26, 0, 0, 10);
}

.forge-window:active {
    -fx-border-color: -edge-strong;
    -fx-effect: dropshadow(gaussian, -shadow-window-active, 54, 0, 0, 22);
}

.window-titlebar {
    -fx-fill-height: false;
    -fx-background-radius: 12px 12px 0 0;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
    -fx-padding: 0 12px;
    -fx-spacing: 8px;
    -fx-alignment: center-left;
    -fx-min-height: 38px;
    -fx-pref-height: 38px;
    -fx-cursor: default;
}

.window-title {
    -fx-font-size: 12.5px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-secondary;
}

.forge-window:active .window-title {
    -fx-text-fill: -text-primary;
}

/* 신호등 폭(3 × 13 + 2 × 8 여백)만큼 오른쪽을 비워 제목을 진짜 가운데로 보낸다. */
.titlebar-balance {
    -fx-min-width: 55px;
    -fx-pref-width: 55px;
}

.window-content {
    -fx-background-color: -window-body;
    -fx-background-radius: 0 0 12px 12px;
}

/* ─────────────────────────── 신호등 ─────────────────────────── */

.traffic-lights {
    -fx-fill-height: false;
    -fx-spacing: 8px;
    -fx-alignment: center-left;
    -fx-padding: 0 4px 0 0;
}

.light {
    -fx-cursor: hand;
}

.light-disc {
    -fx-stroke: rgba(0, 0, 0, 0.22);
    -fx-stroke-width: 0.5;
}

.light-close {
    -fx-fill: -forge-ember;
}

.light-minimize {
    -fx-fill: -molten-gold;
}

.light-zoom {
    -fx-fill: -electric-cyan;
}

.light-glyph {
    -fx-stroke: rgba(0, 0, 0, 0.62);
    -fx-stroke-width: 1.6;
}

/* 비활성 창의 신호등은 회색이다 — 어느 창이 살아 있는지 색만으로 알 수 있다. */
.forge-window .light-disc {
    -fx-fill: -fill-strong;
}

.forge-window:active .light-close {
    -fx-fill: -forge-ember;
}

.forge-window:active .light-minimize {
    -fx-fill: -molten-gold;
}

.forge-window:active .light-zoom {
    -fx-fill: -electric-cyan;
}

/* ─────────────────────────── 공통 컨트롤 ─────────────────────────── */

.toolbar {
    -fx-fill-height: false;
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
    -fx-padding: 8px 12px;
    -fx-spacing: 8px;
}

.toolbar-caption,
.toolbar .label {
    -fx-text-fill: -text-secondary;
    -fx-font-size: 12px;
}

.toolbar-status {
    -fx-text-fill: -text-dim;
    -fx-font-size: 11.5px;
}

.toolbar-button {
    -fx-background-color: -fill-medium;
    -fx-background-radius: 7px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 7px;
    -fx-text-fill: -text-primary;
    -fx-font-size: 12px;
    -fx-padding: 5px 11px;
    -fx-cursor: hand;
    -fx-graphic-text-gap: 6px;
}

.toolbar-button:hover {
    -fx-background-color: -fill-strong;
}

.toolbar-button:pressed {
    -fx-background-color: -fill-soft;
}

.toolbar-button:disabled {
    -fx-opacity: 0.38;
}

/* 되돌릴 수 없는 동작. 색으로 미리 경고하되, 확인 대화상자까지 띄우지는 않는다 —
   시뮬레이터에서 프로세스를 죽이는 것은 되돌릴 수 없지만 치명적이지도 않다. */
.toolbar-button:danger {
    -fx-background-color: -danger-tint;
    -fx-border-color: -danger-border;
    -fx-text-fill: -danger-text;
}

.toolbar-button:danger:hover {
    -fx-background-color: -danger-tint-strong;
}

.button-glyph {
    -fx-stroke: -text-secondary;
    -fx-stroke-width: 1.8;
}

.field {
    -fx-background-color: -fill-sunken;
    -fx-background-radius: 7px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 7px;
    -fx-text-fill: -text-primary;
    -fx-prompt-text-fill: -text-dim;
    -fx-font-size: 12px;
    -fx-padding: 5px 9px;
    -fx-pref-width: 130px;
}

.field:focused {
    -fx-border-color: -accent-border;
}

.field-narrow {
    -fx-pref-width: 62px;
}

/* ── 표 ── */

.table-view {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-table-cell-border-color: transparent;
    -fx-padding: 0;
    -fx-fixed-cell-size: 34px;
}

.table-view > .virtual-flow > .clipped-container > .sheet > .table-row-cell {
    -fx-background-color: transparent;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
}

.table-view > .virtual-flow > .clipped-container > .sheet > .table-row-cell:odd {
    -fx-background-color: -fill-subtle;
}

.table-view > .virtual-flow > .clipped-container > .sheet > .table-row-cell:selected {
    -fx-background-color: -accent-tint;
}

.table-view .table-cell {
    -fx-text-fill: -text-primary;
    -fx-font-size: 12px;
    -fx-padding: 0 10px;
    -fx-alignment: center-left;
}

.table-view .column-numeric {
    -fx-alignment: center-right;
}

.table-view .column-header,
.table-view .column-header-background,
.table-view .column-header-background .filler {
    -fx-background-color: transparent;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
    -fx-size: 30px;
}

.table-view .column-header .label {
    -fx-text-fill: -text-dim;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-alignment: center-left;
    -fx-padding: 0 10px;
}

.table-view .placeholder .label {
    -fx-text-fill: -text-dim;
    -fx-font-size: 12px;
    -fx-text-alignment: center;
}

/* ── 목록 ── */

.list-view {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-padding: 4px;
}

.list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: -text-primary;
    -fx-padding: 0;
    -fx-background-radius: 7px;
}

.list-cell:filled:hover {
    -fx-background-color: -fill-medium;
}

.list-cell:filled:selected {
    -fx-background-color: -accent-tint;
}

/* ── 스크롤 ── */

.scroll-pane,
.scroll-pane > .viewport {
    -fx-background-color: transparent;
}

.scroll-bar {
    -fx-background-color: transparent;
    -fx-padding: 2px;
}

.scroll-bar .track {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
}

.scroll-bar .thumb {
    -fx-background-color: -thumb-fill;
    -fx-background-radius: 6px;
}

.scroll-bar .thumb:hover {
    -fx-background-color: -thumb-fill-hover;
}

.scroll-bar > .increment-button,
.scroll-bar > .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
    -fx-pref-width: 0;
    -fx-pref-height: 0;
}

.scroll-bar > .increment-button > .increment-arrow,
.scroll-bar > .decrement-button > .decrement-arrow {
    -fx-background-color: transparent;
    -fx-padding: 0;
}

/* ── 콤보 박스 ── */

.combo-box {
    -fx-background-color: -fill-medium;
    -fx-background-radius: 7px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 7px;
    -fx-padding: 1px 2px;
}

.combo-box .list-cell {
    -fx-text-fill: -text-primary;
    -fx-font-size: 12px;
    -fx-padding: 4px 8px;
}

.combo-box > .arrow-button > .arrow {
    -fx-background-color: -text-dim;
}

.combo-box-popup .list-view {
    -fx-background-color: -glass-thick;
    -fx-background-radius: 9px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 9px;
    -fx-effect: dropshadow(gaussian, -shadow-popup, 24, 0, 0, 8);
}

/* ── 툴팁 ── */

.tooltip {
    -fx-background-color: -glass-thick;
    -fx-background-radius: 7px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 7px;
    -fx-text-fill: -text-primary;
    -fx-font-size: 11.5px;
    -fx-padding: 5px 9px;
    -fx-effect: dropshadow(gaussian, -shadow-popup, 16, 0, 0, 5);
}

/* ── 토글 스위치 ── */

.toggle-switch {
    -fx-cursor: hand;
}

.toggle-track {
    -fx-background-color: -fill-strong;
    -fx-background-radius: 13px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 13px;
}

.toggle-switch:on .toggle-track {
    -fx-background-color: -accent-tint-strong;
    -fx-border-color: -accent-border;
}

.toggle-thumb {
    -fx-fill: #f7f9fc;
    -fx-stroke: rgba(0, 0, 0, 0.12);
    -fx-stroke-width: 0.5;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 6, 0, 0, 1);
}

/* ─────────────────────────── 종료 오버레이 ─────────────────────────── */

.shutdown-overlay {
    -fx-background-color: -scrim;
}

.shutdown-column {
    -fx-spacing: 14px;
    -fx-max-width: 420px;
    -fx-max-height: 260px;
    -fx-background-color: -glass-thick;
    -fx-background-radius: 16px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 16px;
    -fx-padding: 32px 36px;
    -fx-effect: dropshadow(gaussian, -shadow-window-active, 40, 0, 0, 16);
}

.shutdown-title {
    -fx-font-size: 19px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-primary;
}

.shutdown-detail {
    -fx-font-size: 12.5px;
    -fx-text-fill: -text-secondary;
    -fx-text-alignment: center;
    -fx-line-spacing: 3px;
}

.shutdown-button {
    -fx-background-color: -danger-tint;
    -fx-background-radius: 8px;
    -fx-border-color: -danger-border;
    -fx-border-radius: 8px;
    -fx-text-fill: -danger-text;
    -fx-font-size: 12.5px;
    -fx-padding: 7px 18px;
    -fx-cursor: hand;
}

.shutdown-button:hover {
    -fx-background-color: -danger-tint-strong;
}
```

---

# 38. tokens-dark.css

**Path**
`src/main/resources/forgeos/css/tokens-dark.css`

```css
/*
 * ForgeOS 다크 테마 토큰.
 *
 * 이 파일에는 <b>색만</b> 있다. 구조와 치수는 theme.css / apps.css 가 갖고,
 * 그쪽은 리터럴 색을 쓰지 않고 여기 이름만 참조한다. 테마를 바꾼다는 것은
 * 이 파일을 tokens-light.css 로 통째로 교체한다는 뜻이다(ThemeManager 참고).
 *
 * 팝업(콘텍스트 메뉴·툴팁·콤보 목록)은 자기 Scene 을 갖지만 소유 Scene 의
 * 스타일시트를 물려받는다. 루트에 클래스를 붙이는 방식 대신 파일을 갈아 끼우는
 * 이유가 이것이다 — 이렇게 해야 팝업까지 같이 따라온다.
 *
 * 브랜드 3색(Ember / Gold / Cyan)은 두 테마에서 값이 같다. 신호등과 로고는
 * 테마와 무관하게 같은 물건이어야 하기 때문이다. 대비 조정이 필요한 곳은
 * -*-text / -*-tint 계열 토큰이 따로 맡는다.
 */

.root {
    /* ── 브랜드 ── */
    -forge-ember:        #ff6b4a;
    -molten-gold:        #ffb627;
    -electric-cyan:      #22d3ee;

    /* ── 바탕 ── */
    -forge-bg:           #06080b;

    /* 유리 재질. JavaFX 에는 backdrop-filter 가 없으므로 반투명 + 밝은 윗선 +
       그림자로 두께를 만든다. 큰 표면일수록 더 불투명하고 그림자가 깊다. */
    -glass-thin:         rgba(24, 28, 35, 0.62);
    -glass-regular:      rgba(20, 24, 30, 0.80);
    -glass-thick:        rgba(16, 19, 25, 0.90);
    -window-body:        rgba(9, 11, 15, 0.82);

    /* ── 경계 ── */
    -edge-light:         rgba(255, 255, 255, 0.14);
    -edge-line:          rgba(255, 255, 255, 0.07);
    -edge-strong:        rgba(255, 255, 255, 0.20);

    /* ── 글자 ── */
    -text-primary:       #eceff4;
    -text-secondary:     #9aa5b4;
    -text-dim:           #6a7383;

    /* ── 면 채움. 다크에서는 흰색을 얹고, 라이트에서는 검정을 얹는다. ── */
    -fill-subtle:        rgba(255, 255, 255, 0.025);
    -fill-soft:          rgba(255, 255, 255, 0.05);
    -fill-medium:        rgba(255, 255, 255, 0.08);
    -fill-strong:        rgba(255, 255, 255, 0.13);
    -fill-sunken:        rgba(0, 0, 0, 0.32);
    -thumb-fill:         rgba(255, 255, 255, 0.16);
    -thumb-fill-hover:   rgba(255, 255, 255, 0.28);

    /* ── 의미색 ── */
    -accent-tint:        rgba(34, 211, 238, 0.16);
    -accent-tint-strong: rgba(34, 211, 238, 0.34);
    -accent-border:      rgba(34, 211, 238, 0.45);
    -accent-text:        #8be9f5;

    -danger-tint:        rgba(255, 107, 74, 0.16);
    -danger-tint-strong: rgba(255, 107, 74, 0.28);
    -danger-border:      rgba(255, 107, 74, 0.36);
    -danger-text:        #ffb3a0;

    -warn-tint:          rgba(255, 182, 39, 0.18);
    -warn-border:        rgba(255, 182, 39, 0.55);
    -warn-text:          #ffd58a;

    /* ── 그림자. 라이트에서도 그림자는 검정이지만 훨씬 옅다. ── */
    -shadow-menu:        rgba(0, 0, 0, 0.35);
    -shadow-popup:       rgba(0, 0, 0, 0.55);
    -shadow-dock:        rgba(0, 0, 0, 0.62);
    -shadow-window:      rgba(0, 0, 0, 0.45);
    -shadow-window-active: rgba(0, 0, 0, 0.70);

    /* ── 덮개 ── */
    -scrim:              rgba(4, 6, 9, 0.88);
    -veil:               #05070a;
}

/* ─────────────────────────── 테마별 그라디언트 ─────────────────────────── */
/*
 * 그라디언트는 looked-up color 로 뺄 수 없다(토큰은 단색만 담는다).
 * 그래서 배경화면처럼 테마마다 그림이 달라지는 규칙은 토큰 파일이 직접 갖는다.
 */

.wallpaper {
    -fx-background-color:
        radial-gradient(center 26% 34%, radius 58%, rgba(255, 107, 74, 0.14), transparent),
        radial-gradient(center 86% 84%, radius 62%, rgba(34, 211, 238, 0.12), transparent),
        radial-gradient(center 62% 12%, radius 44%, rgba(217, 70, 239, 0.10), transparent),
        linear-gradient(to bottom right, #0d121b 0%, #080b12 52%, #04060a 100%);
}

/* 마크 뒤의 불빛. 로고의 불꽃이 실제로 주변을 밝히는 것처럼 보이게 한다. */
.wallpaper-glow {
    -fx-background-color:
        radial-gradient(center 39% 44%, radius 34%, rgba(255, 106, 0, 0.20), transparent),
        radial-gradient(center 39% 44%, radius 20%, rgba(217, 70, 239, 0.12), transparent);
}

.wallpaper-ring {
    -fx-stroke: rgba(255, 255, 255, 0.045);
}

/* 배경화면이 창을 방해하면 안 된다. 또렷하게 그리되 전체를 눌러 둔다. */
.wallpaper-brand {
    -fx-opacity: 0.5;
}

.wallpaper-wordmark {
    -fx-text-fill: rgba(217, 70, 239, 0.75);
}

.dock-tile {
    -fx-background-color: linear-gradient(to bottom right,
        rgba(255, 255, 255, 0.10), rgba(255, 255, 255, 0.03));
}

.window-titlebar {
    -fx-background-color: linear-gradient(to bottom,
        rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
}

.graph-node-disc {
    -fx-fill: linear-gradient(to bottom right, #1b212b, #10141b);
    -fx-stroke: rgba(255, 255, 255, 0.16);
}

.graph-node:deadlocked .graph-node-disc {
    -fx-fill: linear-gradient(to bottom right, #3a1a14, #24100c);
}

.wait-for-graph {
    -fx-background-color: radial-gradient(center 50% 42%, radius 62%,
        rgba(255, 255, 255, 0.035), transparent);
}
```

---

# 39. tokens-light.css

**Path**
`src/main/resources/forgeos/css/tokens-light.css`

```css
/*
 * ForgeOS 라이트 테마 토큰.
 *
 * tokens-dark.css 와 같은 이름을 같은 개수만큼 정의한다. 하나라도 빠지면 그
 * 토큰만 해석되지 않아 검은 사각형이 남으므로, 값을 바꿀 때는 반드시 두 파일을
 * 같이 연다.
 *
 * 브랜드 3색은 다크와 값이 같다. 신호등과 로고는 테마가 바뀌어도 같은 물건이다.
 * 대신 대비가 필요한 자리(-*-text)는 밝은 배경에서 읽히도록 훨씬 어둡게 잡았다.
 * 밝은 배경 위의 옅은 산호색 글씨는 그냥 안 보인다.
 */

.root {
    /* ── 브랜드 (다크와 동일) ── */
    -forge-ember:        #ff6b4a;
    -molten-gold:        #ffb627;
    -electric-cyan:      #22d3ee;

    /* ── 바탕 ── */
    -forge-bg:           #eef1f6;

    /* 라이트에서도 유리는 유리다. 다만 바탕이 밝으므로 흰색을 얹어 띄운다. */
    -glass-thin:         rgba(252, 253, 255, 0.72);
    -glass-regular:      rgba(250, 251, 254, 0.84);
    -glass-thick:        rgba(253, 254, 255, 0.93);
    -window-body:        rgba(255, 255, 255, 0.88);

    /* ── 경계 ──
       윗선은 여전히 흰색이다(빛을 받는 모서리). 구분선만 어두워진다. */
    -edge-light:         rgba(255, 255, 255, 0.95);
    -edge-line:          rgba(15, 23, 42, 0.10);
    -edge-strong:        rgba(15, 23, 42, 0.20);

    /* ── 글자 ── */
    -text-primary:       #111826;
    -text-secondary:     #4b5567;
    -text-dim:           #7b8697;

    /* ── 면 채움 ── */
    -fill-subtle:        rgba(15, 23, 42, 0.025);
    -fill-soft:          rgba(15, 23, 42, 0.05);
    -fill-medium:        rgba(15, 23, 42, 0.075);
    -fill-strong:        rgba(15, 23, 42, 0.12);
    -fill-sunken:        rgba(15, 23, 42, 0.05);
    -thumb-fill:         rgba(15, 23, 42, 0.20);
    -thumb-fill-hover:   rgba(15, 23, 42, 0.32);

    /* ── 의미색 ── */
    -accent-tint:        rgba(8, 145, 165, 0.14);
    -accent-tint-strong: rgba(8, 145, 165, 0.28);
    -accent-border:      rgba(8, 145, 165, 0.45);
    -accent-text:        #0a6675;

    -danger-tint:        rgba(214, 62, 32, 0.12);
    -danger-tint-strong: rgba(214, 62, 32, 0.22);
    -danger-border:      rgba(214, 62, 32, 0.34);
    -danger-text:        #99301a;

    -warn-tint:          rgba(202, 130, 0, 0.16);
    -warn-border:        rgba(202, 130, 0, 0.50);
    -warn-text:          #7d5300;

    /* ── 그림자. 라이트에서는 아주 옅어야 한다. 다크 그림자를 그대로 쓰면
           화면이 지저분해지고 카드가 떠 있는 게 아니라 때가 낀 것처럼 보인다. ── */
    -shadow-menu:        rgba(15, 23, 42, 0.08);
    -shadow-popup:       rgba(15, 23, 42, 0.16);
    -shadow-dock:        rgba(15, 23, 42, 0.18);
    -shadow-window:      rgba(15, 23, 42, 0.10);
    -shadow-window-active: rgba(15, 23, 42, 0.22);

    /* ── 덮개 ── */
    -scrim:              rgba(238, 241, 246, 0.90);
    -veil:               #f4f6fa;
}

/* ─────────────────────────── 테마별 그라디언트 ─────────────────────────── */

.wallpaper {
    -fx-background-color:
        radial-gradient(center 26% 34%, radius 58%, rgba(255, 137, 74, 0.16), transparent),
        radial-gradient(center 86% 84%, radius 62%, rgba(34, 190, 220, 0.14), transparent),
        radial-gradient(center 62% 12%, radius 44%, rgba(217, 70, 239, 0.09), transparent),
        linear-gradient(to bottom right, #f7f9fc 0%, #eaeef5 55%, #dfe5ee 100%);
}

.wallpaper-glow {
    -fx-background-color:
        radial-gradient(center 39% 44%, radius 34%, rgba(255, 138, 40, 0.22), transparent),
        radial-gradient(center 39% 44%, radius 20%, rgba(217, 70, 239, 0.10), transparent);
}

.wallpaper-ring {
    -fx-stroke: rgba(15, 23, 42, 0.055);
}

/* 밝은 바탕에서는 같은 불투명도가 더 세게 느껴진다. 더 눌러 둔다. */
.wallpaper-brand {
    -fx-opacity: 0.34;
}

.wallpaper-wordmark {
    -fx-text-fill: rgba(150, 40, 170, 0.85);
}

.dock-tile {
    -fx-background-color: linear-gradient(to bottom right,
        rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.55));
}

.window-titlebar {
    -fx-background-color: linear-gradient(to bottom,
        rgba(255, 255, 255, 0.85), rgba(255, 255, 255, 0.35));
}

.graph-node-disc {
    -fx-fill: linear-gradient(to bottom right, #ffffff, #e8edf5);
    -fx-stroke: rgba(15, 23, 42, 0.18);
}

.graph-node:deadlocked .graph-node-disc {
    -fx-fill: linear-gradient(to bottom right, #ffe7e0, #ffd2c6);
}

.wait-for-graph {
    -fx-background-color: radial-gradient(center 50% 42%, radius 62%,
        rgba(15, 23, 42, 0.030), transparent);
}
```

---

