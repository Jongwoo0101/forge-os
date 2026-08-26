# ForgeOS Source Dump

총 소스 파일 수 : **45개**

- 모듈 : `forgeOS` (os)
- 포함 확장자 : `.java`, `.css`

---

## Files

- `src/main/java/forgeos/ForgeOsApp.java`
- `src/main/java/forgeos/Launcher.java`
- `src/main/java/forgeos/app/AppCatalog.java`
- `src/main/java/forgeos/app/AppContext.java`
- `src/main/java/forgeos/app/AppInstance.java`
- `src/main/java/forgeos/app/AppProcessTable.java`
- `src/main/java/forgeos/app/ForgeApp.java`
- `src/main/java/forgeos/app/browser/BrowserView.java`
- `src/main/java/forgeos/app/browser/FirefoxApp.java`
- `src/main/java/forgeos/app/browser/StartPage.java`
- `src/main/java/forgeos/app/deadlock/DeadlockResolverApp.java`
- `src/main/java/forgeos/app/deadlock/DeadlockResolverView.java`
- `src/main/java/forgeos/app/deadlock/WaitForGraphView.java`
- `src/main/java/forgeos/app/finder/FinderApp.java`
- `src/main/java/forgeos/app/finder/FinderView.java`
- `src/main/java/forgeos/app/monitor/ActivityMonitorApp.java`
- `src/main/java/forgeos/app/monitor/ActivityMonitorView.java`
- `src/main/java/forgeos/app/monitor/DonutChart.java`
- `src/main/java/forgeos/app/notepad/NotepadApp.java`
- `src/main/java/forgeos/app/notepad/NotepadView.java`
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

import forgeos.app.browser.FirefoxApp;
import forgeos.app.deadlock.DeadlockResolverApp;
import forgeos.app.finder.FinderApp;
import forgeos.app.monitor.ActivityMonitorApp;
import forgeos.app.notepad.NotepadApp;
import forgeos.app.terminal.TerminalApp;

import java.util.List;

/**
 * ForgeOS에 내장된 앱 목록.
 *
 * <p>목록의 <b>순서가 곧 Dock의 순서</b>다. 터미널이 맨 앞인 이유는 이 시뮬레이터에서
 * 모든 것이 결국 명령어로 되기 때문이고, 교착 상태 관리자가 맨 뒤인 이유는 앞의
 * 앱들로 상황을 만든 다음에야 쓸 일이 생기기 때문이다. Dock은 사용 빈도 순이
 * 아니라 <b>작업 순서</b>대로 놓여 있을 때 길잡이가 된다.</p>
 *
 * <p>1.1.0 에서 둘이 늘었다. 메모장은 Finder 바로 뒤다 — 파일을 찾는 일과 파일을
 * 쓰는 일은 이어진 하나의 동작이기 때문이다. Firefox 는 커널과 무관한 유일한 앱이라
 * 커널 계열 앱들과 교착 상태 관리자 사이에 선을 긋듯 놓았다.</p>
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
                new NotepadApp(),
                new FirefoxApp(),
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

# 6. AppProcessTable.java

**Path**
`src/main/java/forgeos/app/AppProcessTable.java`

```java
package forgeos.app;

import forgeframework.process.ExecResultDto;
import forgeframework.process.ProcessControlBlock;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.core.KernelService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 열려 있는 앱 창과 커널 프로세스를 1:1로 묶어 두는 표.
 *
 * <h2>왜 필요했나</h2>
 * <p>{@code 1.1.0}까지 ForgeOS의 앱 창은 순수 JavaFX 노드였다. Firefox를 여섯 개
 * 열어도 커널은 그 사실을 몰랐고, 활성 상태 보기의 프로세스 표는 비어 있었으며
 * 메모리 게이지도 0이었다. <b>운영체제 시뮬레이터의 데스크탑에서 앱을 실행했는데
 * 그 운영체제가 모른다</b>는 것은 설명하기 어려운 상태다. 창을 열면 프로세스가
 * 생기고 메모리를 먹는다 — 여기서부터가 데스크탑이다.</p>
 *
 * <h2>앱 프로세스는 CPU를 두고 다투지 않는다</h2>
 * <p>커널의 프로세스는 전부 "버스트 시간만큼 CPU를 쓰고 끝나는 배치 작업"이다.
 * GUI 앱은 그런 물건이 아니다 — 끝나지 않고, 대부분의 시간을 <b>입력을 기다리며</b>
 * 보낸다. 그래서 앱 프로세스는 만들자마자 {@code io_req … keyboard}로 키보드
 * 대기열에 넣어 {@code WAITING}으로 재운다.</p>
 *
 * <p>이것은 편법이 아니라 실제 모델과 같다. 그리고 재우지 않으면 FCFS에서
 * <b>첫 번째로 열린 앱이 CPU를 영원히 붙들고</b> 사용자가 만든 프로세스가 하나도
 * 진행되지 않는다. 비선점 스케줄러에 끝나지 않는 프로세스를 올리면 그렇게 된다.</p>
 *
 * <p>터미널에서 {@code type} 을 치면 키보드 인터럽트가 대기열의 맨 앞을 깨우므로
 * 앱 프로세스 하나가 READY로 돌아올 수 있다. 그래서 매 갱신 펄스마다
 * {@link #sweep()}이 <b>다시 재운다</b> — 입력을 처리하고 다시 기다림으로
 * 돌아가는 GUI 앱의 실제 동작과 같다.</p>
 *
 * <h2>프로세스를 죽이면 창이 닫힌다</h2>
 * <p>활성 상태 보기에서 {@code firefox} 프로세스를 강제 종료하면 Firefox 창이
 * 닫힌다. 표와 화면이 같은 사실을 가리키게 하려면 방향이 양쪽으로 다 통해야 한다.
 * 한쪽으로만 통하면 표는 장식이 된다.</p>
 */
public final class AppProcessTable {

    /**
     * 앱 프로세스의 버스트 시간.
     *
     * <p>GUI 앱은 끝나지 않으므로 실제로는 아무 값이나 상관없다. 그럼에도 큰 값을
     * 두는 이유는, 표의 진행 막대가 거의 비어 있는 채로 남아 "이건 끝나려고 도는
     * 일이 아니다"를 보여 주기 때문이다.</p>
     */
    private static final long APP_BURST_TIME = 999;

    /** 앱 프로세스의 우선순위. 가장 낮게 둬서 어쩌다 깨어나도 사용자 프로세스를 밀지 않는다. */
    private static final int APP_PRIORITY = ProcessControlBlock.MAX_PRIORITY_VALUE;

    /** 앱 프로세스를 재워 둘 장치. 키보드를 고른 이유는 GUI 앱이 실제로 기다리는 것이 입력이기 때문이다. */
    private static final String PARK_DEVICE = "keyboard";

    private final KernelService kernelService;
    private final Consumer<String> onProcessLost;

    /** 앱 id → 커널 PID. 창이 열려 있는 동안만 항목이 존재한다. */
    private final Map<String, Integer> pidByAppId = new HashMap<>();

    /**
     * 표를 만들고 갱신 펄스에 붙는다.
     *
     * @param kernelService 시스템 콜 통로
     * @param onProcessLost 창은 열려 있는데 프로세스가 사라졌을 때 호출된다 (창을 닫으라는 뜻)
     */
    public AppProcessTable(KernelService kernelService, Consumer<String> onProcessLost) {
        this.kernelService = kernelService;
        this.onProcessLost = onProcessLost;
        kernelService.onRefresh(this::sweep);
    }

    /**
     * 앱 창이 열렸다. 커널에 프로세스를 만들고 메모리를 할당한 뒤 재운다.
     *
     * <p>커널이 아직 없거나 이미 내려갔으면 조용히 아무것도 하지 않는다. 데스크탑은
     * 커널 없이도 떠 있어야 한다 — {@code shutdown} 뒤에 창을 여는 것이 예외로
     * 터지면 종료 화면에서 앱이 죽는다.</p>
     *
     * @param app 열린 앱
     */
    public void launch(ForgeApp app) {
        if (pidByAppId.containsKey(app.id())) {
            return;
        }
        SystemCallResult exec = kernelService.call(SystemCallType.EXEC,
                app.id(), String.valueOf(APP_BURST_TIME), String.valueOf(APP_PRIORITY));
        if (!exec.isSuccess()) {
            return;
        }
        int pid = exec.dataAs(ExecResultDto.class).pid();
        pidByAppId.put(app.id(), pid);

        if (app.memoryFootprint() > 0) {
            // 실패해도(프레임이 꽉 찼어도) 프로세스는 그대로 둔다. 메모리가 모자라
            // 앱이 안 열리는 것보다, 열리되 힙이 비어 있는 편이 낫다.
            kernelService.call(SystemCallType.MALLOC,
                    String.valueOf(pid), String.valueOf(app.memoryFootprint()));
        }
        park(pid);
    }

    /**
     * 앱 창이 닫혔다. 프로세스를 종료한다.
     *
     * <p>메모리는 따로 반납하지 않는다 — 커널이 프로세스 종료 리스너에서
     * {@code releaseProcess}로 주소 공간을 통째로 회수한다.</p>
     *
     * @param appId 닫힌 앱의 id
     */
    public void terminate(String appId) {
        Integer pid = pidByAppId.remove(appId);
        if (pid != null) {
            kernelService.call(SystemCallType.KILL, String.valueOf(pid));
        }
    }

    /**
     * 이 앱이 지금 쓰고 있는 PID.
     *
     * @param appId 앱 id
     * @return PID. 열려 있지 않으면 {@code -1}
     */
    public int pidOf(String appId) {
        return pidByAppId.getOrDefault(appId, -1);
    }

    /**
     * 매 갱신 펄스마다 앱 프로세스의 상태를 살핀다.
     *
     * <ul>
     *   <li>깨어나 있으면 다시 재운다 (입력을 처리하고 대기로 돌아가는 것과 같다)</li>
     *   <li>사라졌으면 창을 닫으라고 알린다 (사용자가 표에서 강제 종료한 경우)</li>
     * </ul>
     */
    private void sweep() {
        if (pidByAppId.isEmpty()) {
            return;
        }
        SystemCallResult ps = kernelService.call(SystemCallType.PS);
        if (!ps.isSuccess()) {
            return;
        }
        Map<Integer, ProcessState> stateByPid = new HashMap<>();
        for (ProcessDto process : ps.dataAsList(ProcessDto.class)) {
            stateByPid.put(process.pid(), process.state());
        }

        List<String> lost = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : Map.copyOf(pidByAppId).entrySet()) {
            ProcessState state = stateByPid.get(entry.getValue());
            if (state == null || state == ProcessState.TERMINATED) {
                lost.add(entry.getKey());
            } else if (state != ProcessState.WAITING) {
                park(entry.getValue());
            }
        }

        for (String appId : lost) {
            pidByAppId.remove(appId);
            onProcessLost.accept(appId);
        }
    }

    private void park(int pid) {
        kernelService.call(SystemCallType.IO_REQ, String.valueOf(pid), PARK_DEVICE);
    }
}
```

---

# 7. ForgeApp.java

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
     * 이 앱이 커널에서 차지할 힙 크기(바이트).
     *
     * <p>창이 열리면 {@code AppProcessTable}이 이만큼 {@code malloc}한다.
     * 기본 프레임 크기가 4바이트짜리 16장뿐인 커널이므로 값은 아주 작아야 한다 —
     * 여기서 넉넉하게 잡으면 앱 몇 개를 여는 것만으로 물리 메모리가 차서
     * 사용자가 만드는 프로세스가 곧바로 스왑으로 밀린다.</p>
     *
     * <p>기본값 4바이트는 프레임 딱 한 장이다. 더 무거운 앱만 재정의한다.</p>
     *
     * @return 할당할 바이트 수. 0이면 할당하지 않는다
     */
    default int memoryFootprint() {
        return 4;
    }

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

# 8. BrowserView.java

**Path**
`src/main/java/forgeos/app/browser/BrowserView.java`

```java
package forgeos.app.browser;

import forgeos.ui.Glyphs;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 브라우저 화면 — 탭 · 주소창 · 진행 표시.
 *
 * <h2>도구 모음은 항상 "선택된 탭"을 비춘다</h2>
 * <p>탭마다 주소창을 두면 화면이 두 줄로 두꺼워지고, 탭 하나에 하나씩
 * 프로퍼티를 바인딩하면 탭을 옮길 때마다 바인딩을 끊고 다시 걸어야 한다.
 * 대신 각 탭이 자기 상태가 바뀔 때마다 {@link #syncToolbar()}를 부르고,
 * 도구 모음은 <b>지금 선택된 탭만</b> 읽는다. 바인딩이 없으니 끊을 것도 없다.</p>
 *
 * <h2>주소창은 주소도 검색도 받는다</h2>
 * <p>{@code://}가 있으면 그대로, 점이 박힌 한 덩어리면 {@code https://}를 붙이고,
 * 나머지는 검색어로 본다. 사람이 주소창에 무엇을 칠지 고민하지 않게 하려는 것이
 * 현대 브라우저가 주소창과 검색창을 합친 이유다.</p>
 */
final class BrowserView extends BorderPane {

    /** {@code scheme://} 로 시작하는가. */
    private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    /** 공백 없이 점이 박힌 한 덩어리 — 도메인으로 본다. */
    private static final Pattern HOSTLIKE = Pattern.compile("^[^\\s/:]+\\.[^\\s/:]{2,}(:\\d+)?(/.*)?$");

    /** 검색 엔진. 질의를 뒤에 붙이면 된다. */
    private static final String SEARCH = "https://duckduckgo.com/?q=";

    private final TabPane tabs = new TabPane();
    private final TextField address = new TextField();
    private final ProgressBar progress = new ProgressBar();

    private final Button backButton =
            new Button(null, Glyphs.stroked(Glyphs.ARROW_LEFT, 15, "button-glyph"));
    private final Button forwardButton =
            new Button(null, Glyphs.stroked(Glyphs.ARROW_RIGHT, 15, "button-glyph"));
    private final Button reloadButton =
            new Button(null, Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));
    private final Node lockGlyph = Glyphs.stroked(Glyphs.LOCK, 13, "browser-lock");

    BrowserView() {
        getStyleClass().add("browser");

        tabs.getStyleClass().add("browser-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> syncToolbar());

        // 마지막 탭까지 닫히면 빈 창이 남는다. 브라우저가 아니라 고장으로 보인다.
        tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
            if (tabs.getTabs().isEmpty()) {
                openTab(null);
            }
        });

        setTop(buildChrome());
        setCenter(tabs);

        openTab(null);
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private VBox buildChrome() {
        backButton.getStyleClass().add("browser-nav");
        backButton.setOnAction(e -> go(-1));

        forwardButton.getStyleClass().add("browser-nav");
        forwardButton.setOnAction(e -> go(1));

        reloadButton.getStyleClass().add("browser-nav");
        reloadButton.setOnAction(e -> reloadOrStop());

        Button home = new Button(null, Glyphs.stroked(Glyphs.HOME, 15, "button-glyph"));
        home.getStyleClass().add("browser-nav");
        home.setOnAction(e -> {
            BrowserTab tab = current();
            if (tab != null) {
                tab.loadStartPage();
            }
        });

        address.getStyleClass().addAll("field", "browser-address");
        address.setPromptText("검색하거나 주소를 입력하세요");
        HBox.setHgrow(address, Priority.ALWAYS);
        address.setOnAction(e -> navigate(address.getText()));

        HBox addressBox = new HBox(lockGlyph, address);
        addressBox.getStyleClass().add("browser-address-box");
        addressBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addressBox, Priority.ALWAYS);

        Button newTab = new Button(null, Glyphs.stroked(Glyphs.PLUS, 15, "button-glyph"));
        newTab.getStyleClass().add("browser-nav");
        newTab.setOnAction(e -> openTab(null));

        HBox bar = new HBox(backButton, forwardButton, reloadButton, home, addressBox, newTab);
        bar.getStyleClass().addAll("toolbar", "browser-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        progress.getStyleClass().add("browser-progress");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);

        return new VBox(bar, progress);
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private BrowserTab openTab(String url) {
        BrowserTab tab = new BrowserTab();
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
        if (url == null) {
            tab.loadStartPage();
        } else {
            tab.engine.load(url);
        }
        return tab;
    }

    private void navigate(String input) {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        String typed = input == null ? "" : input.trim();
        if (typed.isEmpty()) {
            return;
        }
        if (StartPage.ADDRESS.equals(typed)) {
            tab.loadStartPage();
            return;
        }
        tab.engine.load(resolve(typed));
    }

    /**
     * 주소창에 친 것을 실제 URL 로 바꾼다.
     *
     * @param typed 사용자가 친 문자열 (공백 제거된 상태)
     * @return 이동할 URL
     */
    private static String resolve(String typed) {
        if (SCHEME.matcher(typed).matches()) {
            return typed;
        }
        if (typed.startsWith("localhost") || HOSTLIKE.matcher(typed).matches()) {
            return "https://" + typed;
        }
        return SEARCH + URLEncoder.encode(typed, StandardCharsets.UTF_8);
    }

    private void go(int offset) {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        WebHistory history = tab.engine.getHistory();
        int target = history.getCurrentIndex() + offset;
        if (target >= 0 && target < history.getEntries().size()) {
            history.go(offset);
        }
    }

    private void reloadOrStop() {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        if (tab.engine.getLoadWorker().getState() == Worker.State.RUNNING) {
            tab.engine.getLoadWorker().cancel();
        } else {
            tab.engine.reload();
        }
    }

    private BrowserTab current() {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        return (selected instanceof BrowserTab tab) ? tab : null;
    }

    /** 선택된 탭의 상태를 도구 모음에 반영한다. 탭이 스스로 자기 변화를 알려 올 때 호출된다. */
    private void syncToolbar() {
        BrowserTab tab = current();
        if (tab == null) {
            return;
        }
        String location = tab.displayAddress();

        // 사용자가 주소창에 무언가 치고 있는 중이면 밑에서 글자를 바꿔치기하지 않는다.
        if (!address.isFocused()) {
            address.setText(location);
        }
        boolean secure = location.startsWith("https://") || location.startsWith(StartPage.ADDRESS);
        lockGlyph.setVisible(secure);
        lockGlyph.setManaged(secure);

        WebHistory history = tab.engine.getHistory();
        backButton.setDisable(history.getCurrentIndex() <= 0);
        forwardButton.setDisable(history.getCurrentIndex() >= history.getEntries().size() - 1);

        boolean loading = tab.engine.getLoadWorker().getState() == Worker.State.RUNNING;
        progress.setVisible(loading);
        progress.setManaged(loading);
        progress.setProgress(tab.engine.getLoadWorker().getProgress());
    }

    void dispose() {
        // WebView 는 창이 사라지면 함께 GC 대상이 된다. 다만 로딩 중인 요청은
        // 명시적으로 끊어 준다 — 닫은 페이지가 네트워크를 계속 쓰고 있을 이유가 없다.
        for (Tab tab : tabs.getTabs()) {
            if (tab instanceof BrowserTab browserTab) {
                browserTab.engine.getLoadWorker().cancel();
                browserTab.engine.load(null);
            }
        }
    }

    // ────────────────────────────── 탭 ──────────────────────────────

    /**
     * 탭 하나 — {@link WebView} 와 그 엔진.
     *
     * <p>{@code final} 로 두는 것이 중요하다. 생성자가 자기 자신의 메서드를
     * 부르는데({@code setContent} 등) 상속 가능한 클래스에서 그러면
     * {@code -Xlint:this-escape} 가 경고를 낸다. 이 프로젝트의 기준선은 경고 0건이다.</p>
     */
    private final class BrowserTab extends Tab {

        private final WebView web = new WebView();
        private final WebEngine engine = web.getEngine();

        BrowserTab() {
            getStyleClass().add("browser-page");
            setContent(web);
            setText("새 탭");

            engine.titleProperty().addListener((obs, old, title) -> setText(tabLabel(title)));
            engine.locationProperty().addListener((obs, old, now) -> {
                setText(tabLabel(engine.getTitle()));
                notifyChanged();
            });
            engine.getLoadWorker().stateProperty().addListener((obs, old, now) -> notifyChanged());
            engine.getLoadWorker().progressProperty().addListener((obs, old, now) -> notifyChanged());
            engine.getHistory().getEntries().addListener(
                    (ListChangeListener<WebHistory.Entry>) change -> notifyChanged());

            // target="_blank" 나 window.open 은 새 창이 아니라 새 탭으로 받는다.
            // 데스크탑 안에서 창을 또 띄우면 우리 창 관리자가 모르는 창이 생긴다.
            engine.setCreatePopupHandler(features -> openTab(null).engine);
        }

        private void loadStartPage() {
            engine.loadContent(StartPage.html());
            setText("새 탭");
            notifyChanged();
        }

        /** 주소창에 보여 줄 문자열. 내장 시작 페이지는 실제 URL 이 없다. */
        private String displayAddress() {
            String location = engine.getLocation();
            return (location == null || location.isBlank() || "about:blank".equals(location))
                    ? StartPage.ADDRESS
                    : location;
        }

        private String tabLabel(String title) {
            if (title != null && !title.isBlank()) {
                return title.length() > 24 ? title.substring(0, 23) + "…" : title;
            }
            String location = displayAddress();
            return StartPage.ADDRESS.equals(location) ? "새 탭" : location;
        }

        private void notifyChanged() {
            if (tabs.getSelectionModel().getSelectedItem() == this) {
                syncToolbar();
            }
        }
    }
}
```

---

# 9. FirefoxApp.java

**Path**
`src/main/java/forgeos/app/browser/FirefoxApp.java`

```java
package forgeos.app.browser;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * Firefox — ForgeOS의 기본 웹 브라우저.
 *
 * <h2>엔진에 관한 정직한 설명</h2>
 * <p>이 앱은 Mozilla 의 Gecko 를 품고 있지 않다. JavaFX 가 들고 있는 렌더링
 * 엔진은 {@code javafx.web} 의 <b>WebKit</b> 하나뿐이고, 자바 프로세스 안에서
 * Gecko 를 띄울 방법은 없다. 그래서 이 앱은 "ForgeOS 창 안에서 도는 브라우저"이며,
 * 이름과 자리(기본 브라우저)를 Firefox 에게 준 것이다. 호스트에 설치된 진짜
 * Firefox 를 실행하는 길도 있었지만, 그러면 창이 ForgeOS 바깥으로 튀어나가
 * 가상 데스크탑이라는 전제가 깨진다.</p>
 */
public final class FirefoxApp implements ForgeApp {

    @Override
    public String id() {
        return "firefox";
    }

    @Override
    public String title() {
        return "Firefox";
    }

    @Override
    public String iconPath() {
        return Glyphs.BROWSER;
    }

    @Override
    public double preferredWidth() {
        return 1040;
    }

    @Override
    public double preferredHeight() {
        return 660;
    }

    /**
     * 브라우저는 이 데스크탑에서 가장 무거운 앱이다.
     *
     * <p>농담이 아니라 사실이다 — 다른 다섯 앱은 커널의 표를 그려 주기만 하지만
     * 이 앱만 WebKit 엔진을 통째로 들고 있다. 활성 상태 보기에서 힙 게이지가
     * Firefox 하나에 눈에 띄게 움직이는 것은 그래서 옳은 그림이다.</p>
     *
     * @return 8바이트 (프레임 두 장)
     */
    @Override
    public int memoryFootprint() {
        return 8;
    }

    @Override
    public AppInstance launch(AppContext context) {
        BrowserView view = new BrowserView();
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 10. StartPage.java

**Path**
`src/main/java/forgeos/app/browser/StartPage.java`

```java
package forgeos.app.browser;

/**
 * 브라우저의 시작 페이지 HTML.
 *
 * <h2>왜 원격 주소가 아니라 내장 문서인가</h2>
 * <p>홈을 실제 사이트로 두면 네트워크가 없는 자리(발표장·기내·사내망)에서 앱을
 * 열자마자 오류 화면이 뜬다. 브라우저를 처음 켠 사람이 가장 먼저 보는 화면이
 * 오류인 것은 앱이 고장 난 것과 구별되지 않는다. 내장 문서는 오프라인에서도
 * 항상 뜨고, 링크를 누르는 순간에야 네트워크가 필요해진다.</p>
 *
 * <h2>이 문서만은 테마를 따르지 않는다</h2>
 * <p>WebView 안쪽은 ForgeOS 의 스타일시트가 닿지 않는 별개의 문서 세계다.
 * 토큰을 넘겨 두 벌을 만들 수도 있지만, 터미널·부팅 화면과 같은 이유로
 * 여기는 고정 다크로 둔다 — 웹 페이지가 어떤 색이든 브라우저의 시작 화면은
 * 자기 색을 갖는 편이 "지금 보는 것이 웹이 아니라 브라우저"임을 알려 준다.</p>
 */
final class StartPage {

    /** 주소창에 표시할 가짜 주소. 내장 문서라 실제 URL 이 없다. */
    static final String ADDRESS = "forge://start";

    private StartPage() {
    }

    /**
     * 시작 페이지 문서를 만든다.
     *
     * @return 완결된 HTML 문서
     */
    static String html() {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                <meta charset="utf-8">
                <title>Firefox — ForgeOS</title>
                <style>
                  * { box-sizing: border-box; }
                  body {
                    margin: 0; min-height: 100vh;
                    display: flex; flex-direction: column;
                    align-items: center; justify-content: center;
                    background: radial-gradient(1200px 600px at 50% -10%, #1b2230 0%, #0a0d12 60%);
                    color: #eceff4;
                    font-family: -apple-system, "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
                  }
                  .mark { font-size: 44px; letter-spacing: -1px; font-weight: 700; }
                  .mark span { color: #ff6b4a; }
                  .sub { margin-top: 8px; color: #6a7383; font-size: 13px; }
                  form { margin: 30px 0 34px; width: min(560px, 82vw); }
                  input {
                    width: 100%; padding: 13px 18px; border-radius: 11px;
                    border: 1px solid rgba(255,255,255,0.10);
                    background: rgba(0,0,0,0.35); color: #eceff4; font-size: 15px;
                    outline: none;
                  }
                  input:focus { border-color: rgba(34,211,238,0.45); }
                  .tiles {
                    display: grid; grid-template-columns: repeat(3, 152px);
                    gap: 12px;
                  }
                  a.tile {
                    display: block; padding: 16px 14px; border-radius: 12px;
                    background: rgba(255,255,255,0.04);
                    border: 1px solid rgba(255,255,255,0.07);
                    color: #eceff4; text-decoration: none;
                  }
                  a.tile:hover { background: rgba(255,255,255,0.09); }
                  a.tile b { display: block; font-size: 13.5px; }
                  a.tile em { display: block; margin-top: 3px; font-style: normal;
                              font-size: 11px; color: #6a7383; }
                  footer { margin-top: 34px; color: #4d5563; font-size: 11px; }
                </style>
                </head>
                <body>
                  <div class="mark">Fire<span>fox</span></div>
                  <div class="sub">ForgeOS 기본 브라우저 · WebKit 렌더링</div>
                  <form action="https://duckduckgo.com/" method="get">
                    <input name="q" autofocus placeholder="검색하거나 주소를 입력하세요">
                  </form>
                  <div class="tiles">
                    <a class="tile" href="https://www.mozilla.org/ko/firefox/">
                      <b>Mozilla</b><em>mozilla.org</em></a>
                    <a class="tile" href="https://developer.mozilla.org/ko/">
                      <b>MDN Web Docs</b><em>developer.mozilla.org</em></a>
                    <a class="tile" href="https://openjfx.io/">
                      <b>OpenJFX</b><em>openjfx.io</em></a>
                    <a class="tile" href="https://github.com/jongwoo0101">
                      <b>ForgeFramework</b><em>github.com</em></a>
                    <a class="tile" href="https://docs.oracle.com/en/java/javase/21/">
                      <b>Java 21 Docs</b><em>docs.oracle.com</em></a>
                    <a class="tile" href="https://ko.wikipedia.org/wiki/운영_체제">
                      <b>운영 체제</b><em>ko.wikipedia.org</em></a>
                  </div>
                  <footer>이 페이지는 ForgeOS 안에 들어 있습니다. 네트워크 없이도 열립니다.</footer>
                </body>
                </html>
                """;
    }
}
```

---

# 11. DeadlockResolverApp.java

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

# 12. DeadlockResolverView.java

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

# 13. WaitForGraphView.java

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

# 14. FinderApp.java

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

# 15. FinderView.java

**Path**
`src/main/java/forgeos/app/finder/FinderView.java`

```java
package forgeos.app.finder;

import forgeframework.filesystem.DirectoryEntryDto;
import forgeframework.filesystem.FileContentDto;
import forgeframework.filesystem.FileListDto;
import forgeframework.filesystem.SyncResultDto;
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

        // 1.1.0 의 disk.img. 여기까지 눌러야 파일이 재부팅을 견딘다는 사실을
        // 명령어 없이 알 수 있는 자리다.
        Button sync = new Button("디스크에 기록", Glyphs.stroked(Glyphs.DISK, 14, "button-glyph"));
        sync.getStyleClass().add("toolbar-button");
        sync.setOnAction(e -> syncDisk());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(pathLabel, spacer, newFolder, newFile, refresh, sync);
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

    // ────────────────────────────── 영속화 ──────────────────────────────

    /**
     * 파일 시스템을 {@code disk.img} 에 내려쓴다.
     *
     * <p>커널을 {@code --disk} 없이 띄웠으면 이미지가 없다. 그 경우를 실패로
     * 보고하지 않는 이유는, 이미지가 없는 것이 오류가 아니라 기본 설정이기
     * 때문이다({@code diskImagePath} 기본값 null). 대신 무엇을 해야 영속되는지를 말해 준다.</p>
     */
    private void syncDisk() {
        SystemCallResult result = kernelService.call(SystemCallType.SYNC);
        if (!result.isSuccess()) {
            pathLabel.setText(result.getMessage());
            return;
        }
        SyncResultDto sync = result.dataAs(SyncResultDto.class);
        pathLabel.setText(sync.persisted()
                ? "%s · %d바이트 · 블록 %d/%d · inode %d/%d".formatted(
                        sync.imagePath(), sync.bytesWritten(),
                        sync.usedBlocks(), sync.totalBlocks(),
                        sync.usedInodes(), sync.totalInodes())
                : "디스크 이미지가 없어 메모리에만 남습니다 (커널을 --disk 로 띄우면 영속됩니다)");
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

# 16. ActivityMonitorApp.java

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
 * <p>1.1.0 부터 탭이 둘이다 — 프로세스(스케줄러·우선순위·큐 등급)와
 * 메모리(프레임 테이블·페이지 테이블·스왑). 게이지는 두 탭에 공통이라 사이드바에 있다.</p>
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
        return 1020;
    }

    @Override
    public double preferredHeight() {
        return 660;
    }

    @Override
    public AppInstance launch(AppContext context) {
        ActivityMonitorView view = new ActivityMonitorView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 17. ActivityMonitorView.java

**Path**
`src/main/java/forgeos/app/monitor/ActivityMonitorView.java`

```java
package forgeos.app.monitor;

import forgeframework.memory.FrameInfo;
import forgeframework.memory.HeapSnapshot;
import forgeframework.memory.MemorySnapshot;
import forgeframework.memory.PageInfo;
import forgeframework.memory.PageReplacementPolicy;
import forgeframework.process.ForkResultDto;
import forgeframework.process.PriorityResultDto;
import forgeframework.process.ProcessControlBlock;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.process.SchedulerDto;
import forgeframework.process.SchedulerQueueDto;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
 *
 * <h2>1.1.0 — 커널이 넓어진 만큼 화면도 넓어졌다</h2>
 * <p>커널이 스케줄러 6종·스왑·fork(COW)를 갖게 되면서, 이 앱이 답해야 할 질문이
 * 늘었다. "누가 CPU를 기다리는가"만이 아니라 <b>"왜 저 프로세스가 먼저 뽑혔는가"</b>
 * (스케줄러·우선순위·큐 등급), <b>"메모리가 어디로 갔는가"</b>(프레임·스왑·COW 공유)
 * 까지다. 그래서 표는 탭 둘로 나뉜다 — 프로세스 탭이 앞의 질문을, 메모리 탭이
 * 뒤의 질문을 맡는다. 게이지는 두 탭에 공통이라 사이드바에 남는다.</p>
 *
 * <h2>주기 갱신과 사용자 조작이 부딪히지 않게</h2>
 * <p>스케줄러·교체 정책 콤보 상자는 커널 값을 되비추면서 동시에 사용자 입력을
 * 받는다. 되비추는 순간에도 {@code setValue}는 변경 이벤트를 쏘므로, 그대로 두면
 * 1초마다 커널에 "스케줄러를 지금 값으로 바꿔라"는 시스템 콜이 날아간다.
 * {@link #syncing} 이 그 순환을 끊는다.</p>
 */
final class ActivityMonitorView extends BorderPane {

    /** 게이지 지름. 사이드바에 넷이 세로로 들어가야 해서 기본값보다 작다. */
    private static final double GAUGE_SIZE = 96;

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ObservableList<ProcessDto> processes = FXCollections.observableArrayList();
    private final TableView<ProcessDto> table = new TableView<>(processes);

    private final ObservableList<FrameInfo> frames = FXCollections.observableArrayList();
    private final TableView<FrameInfo> frameTable = new TableView<>(frames);

    private final ObservableList<PageInfo> pages = FXCollections.observableArrayList();
    private final TableView<PageInfo> pageTable = new TableView<>(pages);

    private final DonutChart frameChart = new DonutChart("물리 프레임", "accent-ember", GAUGE_SIZE);
    private final DonutChart heapChart = new DonutChart("힙 사용량", "accent-gold", GAUGE_SIZE);
    private final DonutChart tlbChart = new DonutChart("TLB 적중률", "accent-cyan", GAUGE_SIZE);
    private final DonutChart swapChart = new DonutChart("스왑 슬롯", "accent-violet", GAUGE_SIZE);

    private final ComboBox<Algorithm> schedulerBox = new ComboBox<>();
    private final ComboBox<PageReplacementPolicy> policyBox = new ComboBox<>();

    private final TabPane tabs = new TabPane();
    private final FlowPane queueStrip = new FlowPane();
    private final Label pageTableTitle = new Label("페이지 테이블");
    private final Label faultLabel = new Label();
    private final Label statusLabel = new Label();

    private final TextField processNameField = new TextField();
    private final TextField burstTimeField = new TextField();
    private final TextField priorityField = new TextField();

    /** 커널 값을 콤보에 되비추는 중. 그동안 변경 이벤트는 무시한다. */
    private boolean syncing;

    ActivityMonitorView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("activity-monitor");

        buildProcessTable();
        buildFrameTable();
        buildPageTable();

        BorderPane left = new BorderPane();
        left.getStyleClass().add("monitor-left");
        left.setTop(buildToolbar());
        left.setCenter(buildTabs());
        left.setBottom(buildStatusBar());

        setCenter(left);
        setRight(buildSidebar());

        this.subscription = kernelService.onRefresh(this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private void buildProcessTable() {
        table.getColumns().add(column("PID", 56, true, p -> String.valueOf(p.pid())));
        table.getColumns().add(column("이름", 128, false, ProcessDto::name));
        // 부모 PID 는 fork 가 생기면서 의미를 얻은 열이다. 부모가 없으면 -1 이 온다.
        table.getColumns().add(column("PPID", 58, true,
                p -> p.parentPid() < 0 ? "—" : String.valueOf(p.parentPid())));
        table.getColumns().add(column("우선", 54, true, p -> String.valueOf(p.priority())));
        // 큐 등급은 MLFQ 에서만 뜻이 있다. 다른 스케줄러에서는 전부 같은 값이라 가로줄만 보여 준다.
        table.getColumns().add(column("큐", 48, true,
                p -> p.queueLevel() < 0 ? "—" : "Q" + p.queueLevel()));

        TableColumn<ProcessDto, ProcessState> state = new TableColumn<>("상태");
        state.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().state()));
        state.setCellFactory(col -> new StateCell());
        state.setPrefWidth(104);
        table.getColumns().add(state);

        table.getColumns().add(column("CPU 사용", 96, true,
                p -> p.cpuTimeUsed() + " / " + p.burstTime()));
        table.getColumns().add(column("진행", 124, false, ActivityMonitorView::formatProgress));

        table.getStyleClass().add("process-table");
        table.setPlaceholder(new Label("실행 중인 프로세스가 없습니다.\n위에서 새 프로세스를 만들어 보세요."));
        table.setContextMenu(buildRowMenu());
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> refreshPageTable());
        // 리사이즈 정책은 기본값(UNCONSTRAINED)을 그대로 둔다. CONSTRAINED_RESIZE_POLICY 는
        // JavaFX 20에서 deprecated 되었고, 이 프로젝트는 -Werror 기준선을 지킨다.
    }

    private void buildFrameTable() {
        frameTable.getColumns().add(column("프레임", 66, true, f -> "#" + f.frameNumber()));
        frameTable.getColumns().add(column("상태", 68, false, f -> f.allocated() ? "USED" : "FREE"));
        frameTable.getColumns().add(column("PID", 56, true,
                f -> f.allocated() ? String.valueOf(f.ownerPid()) : "—"));
        frameTable.getColumns().add(column("페이지", 62, true,
                f -> f.allocated() ? "#" + f.pageNumber() : "—"));
        // 참조 수가 2 이상이면 fork 로 공유 중인 프레임이다. 그 프레임은 스왑 대상에서도 빠진다.
        frameTable.getColumns().add(column("REF", 52, true,
                f -> f.allocated() ? String.valueOf(f.refCount()) : "—"));
        frameTable.getColumns().add(column("플래그", 120, false, ActivityMonitorView::frameFlags));

        frameTable.getStyleClass().add("process-table");
        frameTable.setPlaceholder(new Label("프레임 정보가 없습니다."));
    }

    private void buildPageTable() {
        pageTable.getColumns().add(column("페이지", 66, true, p -> "#" + p.pageNumber()));
        pageTable.getColumns().add(column("상태", 74, false, ActivityMonitorView::pageState));
        pageTable.getColumns().add(column("프레임", 66, true,
                p -> p.present() ? "#" + p.frameNumber() : "—"));
        pageTable.getColumns().add(column("스왑", 62, true,
                p -> p.swapSlot() < 0 ? "—" : "#" + p.swapSlot()));
        pageTable.getColumns().add(column("권한", 58, false, p -> p.writable() ? "rw" : "r-"));
        pageTable.getColumns().add(column("COW", 58, false, p -> p.copyOnWrite() ? "공유" : "—"));
        pageTable.getColumns().add(column("REF", 52, true, p -> String.valueOf(p.refCount())));

        pageTable.getStyleClass().add("process-table");
        pageTable.setPlaceholder(new Label("프로세스를 고르면 그 주소 공간이 보입니다."));
    }

    private TabPane buildTabs() {
        tabs.getStyleClass().add("monitor-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        queueStrip.getStyleClass().add("queue-strip");

        BorderPane processPane = new BorderPane();
        processPane.getStyleClass().add("monitor-pane");
        processPane.setCenter(table);
        processPane.setBottom(queueStrip);

        Tab processTab = new Tab("프로세스", processPane);
        Tab memoryTab = new Tab("메모리", buildMemoryPane());

        tabs.getTabs().addAll(processTab, memoryTab);
        // 메모리 탭으로 옮기는 순간 바로 채워져야 한다. 다음 펄스(최대 1초)를 기다리게 하지 않는다.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refresh());
        return tabs;
    }

    private VBox buildMemoryPane() {
        Label frameTitle = new Label("프레임 테이블 — 물리 메모리 한 장 한 장");
        frameTitle.getStyleClass().add("monitor-section");
        pageTableTitle.getStyleClass().add("monitor-section");

        VBox.setVgrow(frameTable, Priority.ALWAYS);
        VBox.setVgrow(pageTable, Priority.ALWAYS);

        VBox pane = new VBox(frameTitle, frameTable, pageTableTitle, pageTable);
        pane.getStyleClass().addAll("monitor-pane", "monitor-memory");
        return pane;
    }

    private HBox buildToolbar() {
        processNameField.setPromptText("프로세스 이름");
        processNameField.getStyleClass().add("field");
        burstTimeField.setPromptText("버스트");
        burstTimeField.getStyleClass().addAll("field", "field-narrow");
        priorityField.setPromptText("우선");
        priorityField.getStyleClass().addAll("field", "field-narrow");

        Button exec = new Button("실행", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        exec.getStyleClass().add("toolbar-button");
        exec.setOnAction(e -> execProcess());

        Button fork = new Button("복제", Glyphs.stroked(Glyphs.FORK, 14, "button-glyph"));
        fork.getStyleClass().add("toolbar-button");
        fork.setOnAction(e -> forkSelected());
        fork.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button kill = new Button("강제 종료", Glyphs.stroked(Glyphs.STOP, 14, "button-glyph"));
        kill.getStyleClass().add("toolbar-button");
        kill.pseudoClassStateChanged(Styles.DANGER, true);
        kill.setOnAction(e -> killSelected());
        kill.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        schedulerBox.getItems().setAll(
                new Algorithm("fcfs", "FCFS", "FCFS · 도착 순서"),
                new Algorithm("rr", "Round Robin", "Round Robin · 퀀텀"),
                new Algorithm("sjf", "SJF", "SJF · 총 실행 시간"),
                new Algorithm("srtf", "SRTF", "SRTF · 남은 시간"),
                new Algorithm("priority", "Priority", "Priority · 우선순위"),
                new Algorithm("mlfq", "MLFQ", "MLFQ · 다단계 피드백"));
        schedulerBox.setOnAction(e -> onSchedulerPicked());

        Label caption = new Label("스케줄러");
        caption.getStyleClass().add("toolbar-caption");

        HBox bar = new HBox(processNameField, burstTimeField, priorityField,
                exec, fork, kill, spacer, caption, schedulerBox);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ContextMenu buildRowMenu() {
        MenuItem forkItem = new MenuItem("복제 (fork)");
        forkItem.setOnAction(e -> forkSelected());

        MenuItem raise = new MenuItem("우선순위 올리기 (값 −1)");
        raise.setOnAction(e -> nudgePriority(-1));

        MenuItem lower = new MenuItem("우선순위 내리기 (값 +1)");
        lower.setOnAction(e -> nudgePriority(1));

        MenuItem kill = new MenuItem("강제 종료");
        kill.setOnAction(e -> killSelected());

        return new ContextMenu(forkItem, new SeparatorMenuItem(), raise, lower,
                new SeparatorMenuItem(), kill);
    }

    private ScrollPane buildSidebar() {
        policyBox.getItems().setAll(PageReplacementPolicy.values());
        policyBox.setOnAction(e -> onPolicyPicked());

        Label policyCaption = new Label("페이지 교체 정책");
        policyCaption.getStyleClass().add("toolbar-caption");

        VBox policyBoxRow = new VBox(policyCaption, policyBox);
        policyBoxRow.getStyleClass().add("policy-box");

        faultLabel.getStyleClass().add("sidebar-note");

        VBox sidebar = new VBox(frameChart, heapChart, tlbChart, swapChart,
                policyBoxRow, faultLabel);
        sidebar.getStyleClass().add("monitor-sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);

        // 게이지 넷과 정책 상자를 합치면 작은 창에서는 세로가 모자란다. 잘리는 대신 스크롤한다.
        ScrollPane scroller = new ScrollPane(sidebar);
        scroller.getStyleClass().add("monitor-sidebar-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setMinWidth(Region.USE_PREF_SIZE);
        return scroller;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("toolbar-status");
        HBox bar = new HBox(statusLabel);
        bar.getStyleClass().add("monitor-status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void execProcess() {
        String name = processNameField.getText().isBlank()
                ? "proc" + (processes.size() + 1)
                : processNameField.getText().trim();

        List<String> args = new java.util.ArrayList<>();
        args.add(name);
        if (!burstTimeField.getText().isBlank()) {
            args.add(burstTimeField.getText().trim());
        }
        if (!priorityField.getText().isBlank()) {
            // 우선순위는 세 번째 인자다. 버스트를 비워 두고 우선순위만 줄 수는 없으므로 기본값을 채운다.
            if (args.size() == 1) {
                args.add(String.valueOf(defaultBurst()));
            }
            args.add(priorityField.getText().trim());
        }

        SystemCallResult result = kernelService.call(
                SystemCallType.EXEC, args.toArray(new String[0]));
        if (result.isSuccess()) {
            processNameField.clear();
            burstTimeField.clear();
            priorityField.clear();
        }
        status(result.getMessage());
        refresh();
    }

    private void forkSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.FORK, String.valueOf(selected.pid()));
        if (result.isSuccess()) {
            ForkResultDto fork = result.dataAs(ForkResultDto.class);
            status("PID %d → PID %d 복제 완료. 페이지 %d장을 COW 로 공유합니다 — 프레임은 늘지 않았습니다."
                    .formatted(fork.parentPid(), fork.childPid(), fork.sharedPages()));
        } else {
            status(result.getMessage());
        }
        refresh();
    }

    private void nudgePriority(int delta) {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int next = Math.max(ProcessControlBlock.MIN_PRIORITY_VALUE,
                Math.min(ProcessControlBlock.MAX_PRIORITY_VALUE, selected.priority() + delta));
        if (next == selected.priority()) {
            status("우선순위는 %d ~ %d 범위입니다.".formatted(
                    ProcessControlBlock.MIN_PRIORITY_VALUE, ProcessControlBlock.MAX_PRIORITY_VALUE));
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.PRIORITY,
                String.valueOf(selected.pid()), String.valueOf(next));
        if (result.isSuccess()) {
            PriorityResultDto changed = result.dataAs(PriorityResultDto.class);
            status("PID %d 우선순위 %d → %d%s".formatted(
                    changed.pid(), changed.oldPriority(), changed.newPriority(),
                    changed.requeued() ? " (준비 큐에 다시 넣어 정렬을 유지했습니다)" : ""));
        } else {
            status(result.getMessage());
        }
        refresh();
    }

    private void killSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.KILL, String.valueOf(selected.pid()));
        status(result.getMessage());
        refresh();
    }

    private void onSchedulerPicked() {
        Algorithm picked = schedulerBox.getValue();
        if (syncing || picked == null) {
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER, picked.token());
        status(result.getMessage());
        refresh();
    }

    private void onPolicyPicked() {
        PageReplacementPolicy picked = policyBox.getValue();
        if (syncing || picked == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.SWAPINFO, "policy", picked.name().toLowerCase(java.util.Locale.ROOT));
        status(result.getMessage());
        refresh();
    }

    // ────────────────────────────── 갱신 ──────────────────────────────

    private void refresh() {
        refreshProcesses();
        refreshMemory();
        refreshScheduler();
        if (tabs.getSelectionModel().getSelectedIndex() == 1) {
            refreshFrameTable();
            refreshPageTable();
        }
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

        int swapTotal = snapshot.swapTotalSlots();
        double swapRatio = swapTotal == 0 ? 0 : (double) snapshot.swapUsedSlots() / swapTotal;
        swapChart.setValue(swapRatio, swapTotal == 0
                ? "스왑이 꺼져 있음"
                : "%d / %d 슬롯".formatted(snapshot.swapUsedSlots(), swapTotal));

        syncing = true;
        policyBox.setValue(snapshot.replacementPolicy());
        syncing = false;

        faultLabel.setText("""
                페이지 폴트 %d · 스왑 인 %d · 스왑 아웃 %d
                COW 폴트 %d

                폴트는 고장이 아니라 정상 동작입니다. 프레임이 모자라면 커널이
                가장 쓸모없어 보이는 페이지를 밀어내고, 그 페이지에 다시 손이
                닿는 순간 폴트로 되찾아 옵니다."""
                .formatted(snapshot.pageFaults(), snapshot.swapIns(),
                        snapshot.swapOuts(), snapshot.cowFaults()));
    }

    private void refreshScheduler() {
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER);
        if (!result.isSuccess()) {
            return;
        }
        SchedulerDto dto = result.dataAs(SchedulerDto.class);

        syncing = true;
        for (Algorithm option : schedulerBox.getItems()) {
            if (dto.name().startsWith(option.kernelPrefix())) {
                schedulerBox.setValue(option);
                break;
            }
        }
        syncing = false;

        queueStrip.getChildren().clear();
        for (SchedulerQueueDto queue : dto.queues()) {
            queueStrip.getChildren().add(queueChip(dto, queue));
        }
    }

    private void refreshFrameTable() {
        SystemCallResult result = kernelService.call(SystemCallType.FRAMETABLE);
        if (result.isSuccess()) {
            frames.setAll(result.dataAsList(FrameInfo.class));
        }
    }

    private void refreshPageTable() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            pages.clear();
            pageTableTitle.setText("페이지 테이블 — 프로세스를 고르세요");
            return;
        }
        pageTableTitle.setText("페이지 테이블 — PID %d (%s)".formatted(selected.pid(), selected.name()));

        SystemCallResult result = kernelService.call(
                SystemCallType.PAGETABLE, String.valueOf(selected.pid()));
        if (result.isSuccess()) {
            pages.setAll(result.dataAsList(PageInfo.class));
        } else {
            pages.clear();
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private Label queueChip(SchedulerDto scheduler, SchedulerQueueDto queue) {
        String title = scheduler.queues().size() > 1
                ? "Q%d (q=%d)".formatted(queue.level(), queue.timeQuantum())
                : "준비 큐 (q=%d)".formatted(queue.timeQuantum());
        String body = queue.pids().isEmpty()
                ? "비어 있음"
                : queue.pids().stream().map(String::valueOf)
                        .reduce((a, b) -> a + " → " + b).orElse("");

        Label chip = new Label(title + "  " + body);
        chip.getStyleClass().add("queue-chip");
        chip.pseudoClassStateChanged(Styles.ON, !queue.pids().isEmpty());
        return chip;
    }

    private long defaultBurst() {
        var kernel = kernelService.kernel();
        return kernel == null ? 10 : kernel.getConfig().defaultBurstTime();
    }

    private void status(String message) {
        if (message != null && !message.isBlank()) {
            statusLabel.setText(message.lines().findFirst().orElse(message));
        }
    }

    private static <T> TableColumn<T, String> column(String title, double width, boolean numeric,
                                                     java.util.function.Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(text.apply(cell.getValue())));
        column.setPrefWidth(width);
        if (numeric) {
            column.getStyleClass().add("column-numeric");
        }
        return column;
    }

    private static String frameFlags(FrameInfo frame) {
        if (!frame.allocated()) {
            return "—";
        }
        String bits = (frame.dirty() ? "D" : "-") + (frame.referenced() ? "R" : "-");
        return frame.refCount() >= 2 ? bits + "  COW" : bits;
    }

    private static String pageState(PageInfo page) {
        if (!page.valid()) {
            return "—";
        }
        return page.present() ? "MEM" : "SWAP";
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

    /**
     * 스케줄러 선택지 하나.
     *
     * <p>{@code kernelPrefix}가 따로 있는 이유는, 커널이 돌려주는 이름
     * ({@code "Round Robin (RR)"})이 인자({@code "rr"})로 시작하지 않는 경우가
     * 있기 때문이다. 인자를 접두사로 삼아 맞춰 보는 방식은 그 하나에서 조용히 깨진다.</p>
     *
     * @param token        커널이 알아듣는 인자 (예: {@code mlfq})
     * @param kernelPrefix 커널이 돌려주는 이름의 앞부분 (예: {@code Round Robin})
     * @param label        사람이 읽는 이름
     */
    private record Algorithm(String token, String kernelPrefix, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** 상태를 색 배지로 보여 주는 셀. 색은 CSS의 {@code .state-*} 클래스가 정한다. */
    private static final class StateCell extends TableCell<ProcessDto, ProcessState> {

        StateCell() {
        }

        @Override
        protected void updateItem(ProcessState state, boolean empty) {
            super.updateItem(state, empty);
            if (empty || state == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(state.name());
            badge.getStyleClass().addAll("state-badge", "state-" + state.name().toLowerCase(
                    java.util.Locale.ROOT));
            setGraphic(badge);
            setText(null);
        }
    }
}
```

---

# 18. DonutChart.java

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

    /** 고리 두께(px). 지름이 달라져도 두께는 유지한다 — 얇아지면 색이 안 읽힌다. */
    private static final double THICKNESS = 11;

    private final Arc progress = new Arc();
    private final Label valueLabel = new Label("0%");
    private final Label captionLabel = new Label();
    private final Label detailLabel = new Label();

    private final SpringValue sweep = new SpringValue(value -> progress.setLength(value))
            .tune(Motion.RESPONSE_STANDARD, Motion.DAMPING_STANDARD);

    /**
     * 게이지 하나를 만든다.
     *
     * @param caption          고리 아래 제목
     * @param accentStyleClass 고리 색을 정하는 CSS 클래스
     * @param size             고리의 바깥 지름(px). 사이드바에 몇 개가 들어가는지에 따라 다르다
     */
    DonutChart(String caption, String accentStyleClass, double size) {
        getStyleClass().add("donut");
        setAlignment(Pos.CENTER);

        final double diameter = size;
        double radius = (diameter - THICKNESS) / 2;

        Arc track = new Arc(diameter / 2, diameter / 2, radius, radius, 0, 360);
        track.setType(ArcType.OPEN);
        track.setFill(null);
        track.setStrokeWidth(THICKNESS);
        track.getStyleClass().add("donut-track");

        progress.setCenterX(diameter / 2);
        progress.setCenterY(diameter / 2);
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
        ring.setPrefSize(diameter, diameter);
        ring.setMinSize(diameter, diameter);
        ring.setMaxSize(diameter, diameter);

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

# 19. NotepadApp.java

**Path**
`src/main/java/forgeos/app/notepad/NotepadApp.java`

```java
package forgeos.app.notepad;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * 메모장 — 커널 파일 시스템 위에서 도는 텍스트 편집기.
 *
 * <h2>왜 호스트 디스크가 아니라 커널 디스크인가</h2>
 * <p>맥의 실제 폴더에 {@code .txt}를 떨구는 편이 구현은 훨씬 쉽다. 그렇게 하지
 * 않은 이유는 이 앱이 하는 일이 "글을 적는 것"이 아니라 <b>커널의 파일 시스템을
 * 손으로 만져 보는 것</b>이기 때문이다. 메모장에서 저장한 파일은 Finder 의 컬럼에
 * 곧바로 나타나고, 터미널의 {@code cat} 으로 읽히며, {@code sync} 를 누르면
 * {@code disk.img} 에 내려앉아 재부팅을 견딘다. 1.1.0 이 들여온 영속화를
 * 명령어 없이 눈으로 확인할 수 있는 자리가 바로 여기다.</p>
 */
public final class NotepadApp implements ForgeApp {

    @Override
    public String id() {
        return "notepad";
    }

    @Override
    public String title() {
        return "메모장";
    }

    @Override
    public String iconPath() {
        return Glyphs.NOTEPAD;
    }

    @Override
    public double preferredWidth() {
        return 880;
    }

    @Override
    public double preferredHeight() {
        return 560;
    }

    @Override
    public AppInstance launch(AppContext context) {
        NotepadView view = new NotepadView(context);
        return new AppInstance(view, view::dispose);
    }
}
```

---

# 20. NotepadView.java

**Path**
`src/main/java/forgeos/app/notepad/NotepadView.java`

```java
package forgeos.app.notepad;

import forgeframework.filesystem.DirectoryEntryDto;
import forgeframework.filesystem.FileContentDto;
import forgeframework.filesystem.FileListDto;
import forgeframework.filesystem.SyncResultDto;
import forgeframework.filesystem.WriteResultDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 메모장 화면.
 *
 * <h2>대화상자를 하나도 쓰지 않는다</h2>
 * <p>"이름을 입력하세요", "저장하지 않고 닫을까요?" 같은 질문을 {@code Dialog}로
 * 띄우면 그때마다 진짜 네이티브 창이 하나 열린다. ForgeOS 는 "데스크탑 안의
 * 모든 것은 노드"라는 원칙 위에 서 있고, 실제로 네이티브 창 해제는 종료 시
 * 크래시의 원인으로 지목된 자리이기도 하다. 그래서 새 파일 이름은 툴바의
 * 입력칸으로 받고, 저장하지 않은 변경은 질문 대신 <b>초안(draft)</b>으로 들고 있는다.</p>
 *
 * <h2>초안은 잃어버리지 않는다</h2>
 * <p>{@link #drafts}가 경로별 편집 중인 내용을, {@link #saved}가 마지막으로
 * 디스크에서 읽거나 디스크로 쓴 내용을 들고 있다. 둘이 다르면 저장되지 않은
 * 변경이 있는 것이고, 목록에 점(•)이 붙는다. 파일을 옮겨 다녀도 초안은 그대로
 * 남으므로 "저장할까요?"를 물을 이유 자체가 없어진다.</p>
 */
final class NotepadView extends BorderPane {

    /** 루트 경로. */
    private static final String ROOT = "/";

    private final KernelService kernelService;

    private final ListView<DirectoryEntryDto> fileList = new ListView<>();
    private final TextArea editor = new TextArea();
    private final TextField newNameField = new TextField();

    private final Label directoryLabel = new Label(ROOT);
    private final Label openFileLabel = new Label("열린 파일 없음");
    private final Label countLabel = new Label();
    private final Label statusLabel = new Label();

    private final Button saveButton = new Button("저장", Glyphs.stroked(Glyphs.SAVE, 14, "button-glyph"));
    private final Button revertButton =
            new Button("되돌리기", Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));

    /** 편집 중인 내용. 키는 절대경로. 창을 닫기 전까지 살아 있다. */
    private final Map<String, String> drafts = new HashMap<>();

    /** 마지막으로 디스크와 일치했던 내용. {@link #drafts}와 다르면 저장되지 않은 변경이다. */
    private final Map<String, String> saved = new HashMap<>();

    private String directory = ROOT;
    private String openFile;

    /** 편집기에 프로그램이 값을 넣는 중인지. 리스너가 그것을 사용자 입력으로 오해하지 않도록. */
    private boolean loading;

    NotepadView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("notepad");

        setLeft(buildSidebar());
        setCenter(buildEditorPane());

        editor.textProperty().addListener((obs, old, text) -> onEdited(text));
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        reloadDirectory();
        updateEditorState();
        status("커널 파일 시스템의 " + ROOT + " 를 열었습니다. 저장한 파일은 Finder 와 터미널에서도 보입니다.");
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private VBox buildSidebar() {
        directoryLabel.getStyleClass().add("notepad-path");

        Button up = new Button("상위 폴더", Glyphs.stroked(Glyphs.ARROW_LEFT, 13, "button-glyph"));
        up.getStyleClass().add("toolbar-button");
        up.setOnAction(e -> goUp());

        Button refresh = new Button(null, Glyphs.stroked(Glyphs.REFRESH, 13, "button-glyph"));
        refresh.getStyleClass().add("toolbar-button");
        refresh.setOnAction(e -> reloadDirectory());

        HBox header = new HBox(up, refresh);
        header.getStyleClass().add("notepad-sidebar-header");
        header.setAlignment(Pos.CENTER_LEFT);

        fileList.getStyleClass().add("notepad-file-list");
        fileList.setCellFactory(view -> new EntryCell());
        fileList.setPlaceholder(new Label("비어 있음"));
        VBox.setVgrow(fileList, Priority.ALWAYS);
        fileList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, entry) -> {
                    if (entry != null) {
                        onEntrySelected(entry);
                    }
                });

        VBox sidebar = new VBox(directoryLabel, header, fileList);
        sidebar.getStyleClass().add("notepad-sidebar");
        return sidebar;
    }

    private BorderPane buildEditorPane() {
        editor.getStyleClass().add("notepad-editor");
        editor.setWrapText(true);
        editor.setPromptText("왼쪽에서 파일을 고르거나, 위에서 새 파일을 만드세요.");

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("notepad-main");
        pane.setTop(buildToolbar());
        pane.setCenter(editor);
        pane.setBottom(buildStatusBar());
        return pane;
    }

    private HBox buildToolbar() {
        newNameField.setPromptText("새 파일 이름");
        newNameField.getStyleClass().add("field");
        newNameField.setOnAction(e -> createFile());

        Button create = new Button("만들기", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        create.getStyleClass().add("toolbar-button");
        create.setOnAction(e -> createFile());

        saveButton.getStyleClass().add("toolbar-button");
        saveButton.setOnAction(e -> save());

        revertButton.getStyleClass().add("toolbar-button");
        revertButton.setOnAction(e -> revert());

        Button sync = new Button("디스크에 기록", Glyphs.stroked(Glyphs.DISK, 14, "button-glyph"));
        sync.getStyleClass().add("toolbar-button");
        sync.setOnAction(e -> syncDisk());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        openFileLabel.getStyleClass().add("notepad-open-file");

        HBox bar = new HBox(newNameField, create, saveButton, revertButton, sync, spacer, openFileLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox buildStatusBar() {
        countLabel.getStyleClass().add("notepad-count");
        statusLabel.getStyleClass().add("notepad-status");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(statusLabel, spacer, countLabel);
        bar.getStyleClass().add("notepad-status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 탐색 ──────────────────────────────

    private void reloadDirectory() {
        SystemCallResult result = kernelService.call(SystemCallType.LS, directory, ".");
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        FileListDto listing = result.dataAs(FileListDto.class);
        directoryLabel.setText(directory);
        fileList.getItems().setAll(listing.entries());
    }

    private void goUp() {
        if (ROOT.equals(directory)) {
            return;
        }
        int cut = directory.lastIndexOf('/');
        directory = (cut <= 0) ? ROOT : directory.substring(0, cut);
        reloadDirectory();
    }

    private void onEntrySelected(DirectoryEntryDto entry) {
        if ("DIRECTORY".equals(entry.type())) {
            directory = join(directory, entry.name());
            reloadDirectory();
            return;
        }
        open(entry.name());
    }

    private void open(String name) {
        String path = join(directory, name);

        // 이 파일의 초안을 이미 들고 있으면 디스크를 다시 읽지 않는다.
        // 읽어 버리면 사용자가 방금 친 것이 조용히 사라진다.
        if (!drafts.containsKey(path)) {
            SystemCallResult result = kernelService.call(SystemCallType.CAT, directory, name);
            if (!result.isSuccess()) {
                status(result.getMessage());
                return;
            }
            String content = result.dataAs(FileContentDto.class).content();
            saved.put(path, content);
            drafts.put(path, content);
        }

        openFile = name;
        loading = true;
        editor.setText(drafts.get(path));
        loading = false;
        editor.positionCaret(editor.getText().length());
        updateEditorState();
        status(path + " 를 열었습니다.");
    }

    // ────────────────────────────── 편집 ──────────────────────────────

    private void onEdited(String text) {
        if (loading || openFile == null) {
            return;
        }
        drafts.put(currentPath(), text);
        updateEditorState();
        // 목록의 점(•)을 다시 그리게 한다. 항목 자체는 그대로이므로 셀만 새로 만든다.
        fileList.refresh();
    }

    private void createFile() {
        String name = newNameField.getText().trim();
        if (name.isEmpty()) {
            status("만들 파일 이름을 입력하세요.");
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.TOUCH, directory, name);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        newNameField.clear();
        reloadDirectory();
        open(name);
        selectInList(name);
        editor.requestFocus();
    }

    private void save() {
        if (openFile == null) {
            return;
        }
        String path = currentPath();
        String text = editor.getText();

        SystemCallResult result = kernelService.call(SystemCallType.WRITE, directory, openFile, text);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        WriteResultDto written = result.dataAs(WriteResultDto.class);
        saved.put(path, text);
        drafts.put(path, text);

        reloadDirectory();
        selectInList(openFile);
        updateEditorState();
        status("%s 에 %d바이트를 저장했습니다. 재부팅까지 남기려면 '디스크에 기록'을 누르세요."
                .formatted(path, written.bytesWritten()));
    }

    private void revert() {
        if (openFile == null) {
            return;
        }
        String path = currentPath();
        drafts.remove(path);
        saved.remove(path);
        open(openFile);
        fileList.refresh();
        status(path + " 를 디스크의 내용으로 되돌렸습니다.");
    }

    private void syncDisk() {
        SystemCallResult result = kernelService.call(SystemCallType.SYNC);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        SyncResultDto sync = result.dataAs(SyncResultDto.class);
        if (sync.persisted()) {
            status("%s 에 %d바이트를 기록했습니다. (블록 %d/%d · inode %d/%d)".formatted(
                    sync.imagePath(), sync.bytesWritten(),
                    sync.usedBlocks(), sync.totalBlocks(),
                    sync.usedInodes(), sync.totalInodes()));
        } else {
            status("디스크 이미지가 설정되어 있지 않아 메모리에만 남습니다. "
                    + "(--disk 옵션으로 커널을 띄우면 disk.img 에 영속됩니다)");
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.S && event.isShortcutDown()) {
            save();
            event.consume();
        }
    }

    private void updateEditorState() {
        boolean opened = openFile != null;
        editor.setDisable(!opened);
        saveButton.setDisable(!opened || !isDirty(currentPath()));
        revertButton.setDisable(!opened);

        if (!opened) {
            openFileLabel.setText("열린 파일 없음");
            countLabel.setText("");
            return;
        }
        String path = currentPath();
        openFileLabel.setText(isDirty(path) ? path + " •" : path);
        countLabel.setText("%d자 · %d바이트".formatted(
                editor.getText().length(),
                editor.getText().getBytes(StandardCharsets.UTF_8).length));
    }

    private void selectInList(String name) {
        for (DirectoryEntryDto entry : fileList.getItems()) {
            if (entry.name().equals(name) && !"DIRECTORY".equals(entry.type())) {
                fileList.getSelectionModel().select(entry);
                return;
            }
        }
    }

    private boolean isDirty(String path) {
        if (path == null) {
            return false;
        }
        String draft = drafts.get(path);
        return draft != null && !draft.equals(saved.get(path));
    }

    private String currentPath() {
        return openFile == null ? null : join(directory, openFile);
    }

    private void status(String message) {
        statusLabel.setText(message);
    }

    private static String join(String parent, String name) {
        return ROOT.equals(parent) ? ROOT + name : parent + "/" + name;
    }

    void dispose() {
        // 메모장은 주기 갱신을 구독하지 않는다. 파일 시스템은 사용자가 바꿀 때만 바뀐다.
    }

    /** 아이콘 + 이름 + 저장되지 않은 변경 표시로 구성된 항목 셀. */
    private final class EntryCell extends ListCell<DirectoryEntryDto> {

        EntryCell() {
        }

        @Override
        protected void updateItem(DirectoryEntryDto entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean isDirectory = "DIRECTORY".equals(entry.type());

            Label name = new Label(entry.name());
            name.getStyleClass().add("entry-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(
                    Glyphs.stroked(isDirectory ? Glyphs.FOLDER : Glyphs.NOTEPAD, 15, "entry-icon"),
                    name,
                    spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("entry-row");

            if (isDirectory) {
                row.getChildren().add(Glyphs.stroked(Glyphs.CHEVRON_RIGHT, 13, "entry-chevron"));
            } else {
                Label mark = new Label(isDirty(join(directory, entry.name()))
                        ? "•"
                        : entry.size() + "B");
                mark.getStyleClass().add("entry-size");
                mark.pseudoClassStateChanged(Styles.ON, isDirty(join(directory, entry.name())));
                row.getChildren().add(mark);
            }

            setGraphic(row);
            setText(null);
        }
    }
}
```

---

# 21. TerminalApp.java

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

# 22. TerminalView.java

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

# 23. BootConsole.java

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

# 24. BootSequence.java

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

# 25. BootVideo.java

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

# 26. KernelService.java

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

# 27. DesktopPane.java

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

# 28. DockView.java

**Path**
`src/main/java/forgeos/desktop/DockView.java`

```java
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
```

---

# 29. MenuBarView.java

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
 * "지금 커널이 어떤 상태인가"(프로세스 수, 메모리, 스왑, 가동 시간, 시계)다.
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
    private final Label swapChip = chip("스왑 —");
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
                processChip, memoryChip, swapChip, uptimeChip, themeToggle(theme), clockLabel);

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

            // 스왑이 꺼진 커널(swapSlots=0)에서는 칩 자체를 감춘다. 항상 "0"인 숫자는
            // 자리만 먹고 아무것도 알려 주지 않는다.
            boolean swapOn = snapshot.swapTotalSlots() > 0;
            swapChip.setVisible(swapOn);
            swapChip.setManaged(swapOn);
            if (swapOn) {
                swapChip.setText("스왑 %d/%d · 폴트 %d".formatted(
                        snapshot.swapUsedSlots(), snapshot.swapTotalSlots(), snapshot.pageFaults()));
            }
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

# 30. Wallpaper.java

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
```

---

# 31. ForgeMark.java

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

# 32. Glyphs.java

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
```

---

# 33. Motion.java

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

# 34. SpringValue.java

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

# 35. Styles.java

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

# 36. ThemeManager.java

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

# 37. ToggleSwitch.java

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

# 38. ForgeWindow.java

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

# 39. TrafficLights.java

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

# 40. WindowManager.java

**Path**
`src/main/java/forgeos/wm/WindowManager.java`

```java
package forgeos.wm;

import forgeos.app.AppContext;
import forgeos.app.AppProcessTable;
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

    /** 열려 있는 창과 커널 프로세스를 묶어 두는 표. */
    private final AppProcessTable processes;
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
        this.processes = new AppProcessTable(kernelService, this::closeByAppId);
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

        // 창을 화면에 올린 다음에 프로세스를 만든다. 커널이 없거나 내려간 상태라도
        // 창은 떠야 하기 때문이다 — 종료 화면에서 앱을 여는 것이 예외로 터지면 안 된다.
        processes.launch(app);

        focus(window);
        Motion.materialize(window, OPEN_SCALE, Duration.millis(240));
    }

    /**
     * 프로세스가 사라진 앱의 창을 닫는다.
     *
     * <p>활성 상태 보기에서 앱 프로세스를 강제 종료했을 때 {@code AppProcessTable}이
     * 부른다. 표에서 죽인 것이 화면에도 반영되어야 표가 장식이 아니게 된다.</p>
     */
    private void closeByAppId(String appId) {
        ForgeWindow window = openWindows.get(appId);
        if (window != null) {
            close(window);
        }
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
            processes.terminate(window.appId());

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

# 41. module-info.java

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

    /**
     * Firefox 앱의 렌더링 엔진(WebView). JavaFX 가 품는 엔진은 Gecko 가 아니라
     * WebKit 이므로 "Firefox 를 띄운다"가 아니라 "Firefox 를 닮은 브라우저를
     * ForgeOS 안에서 돌린다"가 정확한 표현이다.
     */
    requires javafx.web;

    /** 커널. ForgeOS의 모든 상태는 여기서 나온다. */
    requires forgeframework;

    /** 명령어 계층 — Terminal 앱이 CLI와 같은 명령어 세트를 그대로 쓴다. */
    requires forgeframework.cli;

    /** JavaFX 런타임이 {@code ForgeOsApp}을 리플렉션으로 생성할 수 있도록 한정 공개. */
    exports forgeos to javafx.graphics;
}
```

---

# 42. apps.css

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
    -fx-padding: 18px 18px 22px 18px;
    -fx-spacing: 18px;
    -fx-pref-width: 250px;
}

/* 게이지 넷 + 정책 상자는 작은 창에서 세로가 모자란다. 잘리는 대신 스크롤한다.
   폭은 스크롤 창이 잡아 준다 — 안쪽 VBox 에만 pref 를 주면 스크롤바가 겹쳐 나온다. */
.monitor-sidebar-scroll {
    -fx-background-color: transparent;
    -fx-pref-width: 268px;
}

.monitor-sidebar-scroll > .viewport {
    -fx-background-color: transparent;
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

/* 스왑은 "메모리가 디스크로 밀려났다"는 다른 층위의 사건이라 브랜드 3색 밖의
   보라를 쓴다. 로고의 불티와 같은 색이므로 시스템 밖으로 튀지는 않는다. */
.accent-violet {
    -fx-stroke: -forge-violet;
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

/* ── 탭 안쪽 ── */

.monitor-tabs {
    -fx-background-color: transparent;
}

.monitor-pane {
    -fx-background-color: transparent;
}

.monitor-memory {
    -fx-spacing: 0;
}

/* 표 위에 붙는 작은 제목. 표가 둘 겹쳐 있으면 어느 쪽이 무엇인지 반드시 써 줘야 한다. */
.monitor-section {
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-secondary;
    -fx-padding: 10px 14px 6px 14px;
}

.monitor-status-bar {
    -fx-background-color: -fill-subtle;
    -fx-border-color: -edge-line transparent transparent transparent;
    -fx-border-width: 1px 0 0 0;
    -fx-padding: 6px 14px;
}

/* ── 준비 큐 ── */
/*
 * MLFQ 는 큐가 셋, 나머지 스케줄러는 하나다. 개수가 달라지므로 FlowPane 에
 * 얹어 넘치면 다음 줄로 흘리게 둔다. 비어 있는 큐도 감추지 않는다 —
 * "Q2 가 비어 있다"는 것 자체가 MLFQ 를 이해하는 데 필요한 정보다.
 */

.queue-strip {
    -fx-background-color: -fill-subtle;
    -fx-border-color: -edge-line transparent transparent transparent;
    -fx-border-width: 1px 0 0 0;
    -fx-padding: 9px 14px;
    -fx-hgap: 8px;
    -fx-vgap: 6px;
}

.queue-chip {
    -fx-background-color: -fill-soft;
    -fx-background-radius: 6px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 6px;
    -fx-padding: 4px 10px;
    -fx-font-size: 11px;
    -fx-text-fill: -text-dim;
}

/* 프로세스가 들어 있는 큐만 살아난다. 눈이 먼저 가야 할 곳이 거기다. */
.queue-chip:on {
    -fx-background-color: -accent-tint;
    -fx-border-color: -accent-border;
    -fx-text-fill: -accent-text;
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

/* ─────────────────────────── 메모장 ─────────────────────────── */

.notepad {
    -fx-background-color: transparent;
}

.notepad-sidebar {
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent -edge-line transparent transparent;
    -fx-border-width: 0 1px 0 0;
    -fx-pref-width: 220px;
    -fx-spacing: 0;
}

.notepad-path {
    -fx-font-size: 11.5px;
    -fx-font-weight: bold;
    -fx-text-fill: -text-secondary;
    -fx-padding: 12px 14px 4px 14px;
}

.notepad-sidebar-header {
    -fx-spacing: 6px;
    -fx-padding: 4px 12px 10px 12px;
}

.notepad-file-list {
    -fx-background-color: transparent;
}

.notepad-main {
    -fx-background-color: transparent;
}

/*
 * 편집기만 고정폭 글꼴이다. 커널 파일 시스템에 들어가는 것은 대부분 설정·로그·
 * 짧은 메모라, 비례 글꼴보다 자릿수가 맞는 편이 읽기 쉽다.
 */
.notepad-editor {
    -fx-font-family: "SF Mono", "JetBrains Mono", "D2Coding", "Menlo", monospace;
    -fx-font-size: 13px;
}

.notepad-editor .content {
    -fx-background-color: -fill-sunken;
}

.notepad-open-file {
    -fx-font-size: 11.5px;
    -fx-text-fill: -text-dim;
}

.notepad-status-bar {
    -fx-background-color: -fill-subtle;
    -fx-border-color: -edge-line transparent transparent transparent;
    -fx-border-width: 1px 0 0 0;
    -fx-padding: 6px 14px;
    -fx-spacing: 12px;
}

.notepad-status {
    -fx-font-size: 11px;
    -fx-text-fill: -text-dim;
}

.notepad-count {
    -fx-font-size: 11px;
    -fx-text-fill: -text-dim;
}

/* 저장되지 않은 변경이 있는 파일. 크기 대신 점이 뜬다. */
.entry-size:on {
    -fx-text-fill: -molten-gold;
    -fx-font-weight: bold;
}

/* ─────────────────────────── Firefox ─────────────────────────── */
/*
 * 브라우저의 내용물은 우리 스타일시트가 닿지 않는 남의 문서다. 그래서 여기서
 * 칠할 수 있는 것은 크롬(도구 모음·탭·주소창)뿐이고, 그 크롬은 최대한 얇아야
 * 한다 — 페이지가 주인공인 앱에서 크롬이 두꺼우면 창이 작아 보인다.
 */

.browser {
    -fx-background-color: transparent;
}

.browser-toolbar {
    -fx-spacing: 4px;
    -fx-padding: 7px 10px;
}

.browser-nav {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-padding: 5px 8px;
    -fx-cursor: hand;
}

.browser-nav:hover {
    -fx-background-color: -fill-medium;
}

.browser-nav:disabled {
    -fx-opacity: 0.3;
}

.browser-address-box {
    -fx-background-color: -fill-sunken;
    -fx-background-radius: 9px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 9px;
    -fx-padding: 0 10px;
    -fx-spacing: 6px;
}

/* 주소창은 상자 안의 상자다. 자기 배경과 테두리를 지워야 한 겹으로 보인다. */
.browser-address {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-pref-width: 100;
    -fx-padding: 7px 2px;
    -fx-font-size: 12.5px;
}

.browser-address:focused {
    -fx-border-color: transparent;
}

.browser-lock {
    -fx-stroke: -accent-text;
    -fx-stroke-width: 1.7;
}

.browser-tabs {
    -fx-background-color: transparent;
}

.browser-progress > .bar {
    -fx-background-color: -forge-ember;
}
```

---

# 43. theme.css

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
    /* 아이콘 사이 간격. 좁으면 하나를 겨냥하다 옆을 누르게 된다. */
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

/*
 * 가리킨 앱의 이름표. Dock 바깥(위)에 떠서 바탕화면이나 창 위에 얹히므로,
 * 가장 불투명한 유리와 또렷한 그림자를 써야 어떤 배경에서도 읽힌다.
 */
.dock-name {
    -fx-background-color: -glass-thick;
    -fx-background-radius: 8px;
    -fx-border-color: -edge-line;
    -fx-border-radius: 8px;
    -fx-text-fill: -text-primary;
    -fx-font-size: 12px;
    -fx-padding: 4px 11px;
    -fx-effect: dropshadow(gaussian, -shadow-popup, 18, 0, 0, 6);
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

/* ── 탭 ── */
/*
 * 탭은 창을 더 만들지 않고 화면을 나누는 유일한 수단이다. 활성 상태 보기의
 * 프로세스/메모리, 브라우저의 페이지들이 모두 여기 얹힌다. macOS 의 탭이
 * 그렇듯 선택된 탭만 밝고, 나머지는 배경으로 물러난다.
 */

.tab-pane > .tab-header-area {
    -fx-padding: 0;
}

.tab-pane > .tab-header-area > .tab-header-background {
    -fx-background-color: -fill-subtle;
    -fx-border-color: transparent transparent -edge-line transparent;
    -fx-border-width: 0 0 1px 0;
}

.tab-pane > .tab-header-area > .headers-region > .tab {
    -fx-background-color: transparent;
    -fx-background-radius: 0;
    -fx-padding: 7px 16px;
    -fx-cursor: hand;
}

.tab-pane > .tab-header-area > .headers-region > .tab:hover {
    -fx-background-color: -fill-soft;
}

/* 선택된 탭은 배경이 아니라 밑줄로 표시한다. 배경을 칠하면 탭 줄이
   두 겹으로 보이고, 창 제목 표시줄과 색이 겹쳐 어디까지가 창인지 흐려진다. */
.tab-pane > .tab-header-area > .headers-region > .tab:selected {
    -fx-background-color: -fill-medium;
    -fx-border-color: transparent transparent -electric-cyan transparent;
    -fx-border-width: 0 0 2px 0;
}

.tab-pane > .tab-header-area > .headers-region > .tab .tab-label {
    -fx-text-fill: -text-secondary;
    -fx-font-size: 12px;
}

.tab-pane > .tab-header-area > .headers-region > .tab:selected .tab-label {
    -fx-text-fill: -text-primary;
}

.tab-pane > .tab-header-area > .headers-region > .tab .tab-close-button {
    -fx-background-color: -text-dim;
}

.tab-pane > .tab-header-area > .headers-region > .tab:hover .tab-close-button {
    -fx-background-color: -text-primary;
}

/* JavaFX 는 선택된 탭에 점선 초점 테두리를 그린다. 우리 디자인에는 없는 물건이다. */
.tab-pane > .tab-header-area > .headers-region > .tab:selected .focus-indicator {
    -fx-border-color: transparent;
}

.tab-pane > .tab-content-area {
    -fx-background-color: transparent;
}

/* ── 여러 줄 입력 ── */

.text-area {
    -fx-background-color: transparent;
    -fx-background-radius: 0;
    -fx-text-fill: -text-primary;
    -fx-prompt-text-fill: -text-dim;
    -fx-highlight-fill: -accent-tint-strong;
    -fx-padding: 0;
}

.text-area > .scroll-pane {
    -fx-background-color: transparent;
}

.text-area .content {
    -fx-background-color: transparent;
    -fx-padding: 14px 16px;
}

/* ── 진행 막대 ── */
/*
 * 브라우저의 로딩 표시. 두께 3px 에 트랙이 없다 — 진행 중일 때만 나타나고
 * 끝나면 자리째 사라지므로, 비어 있는 트랙을 보여 줄 이유가 없다.
 */

.progress-bar {
    -fx-pref-height: 3px;
    -fx-min-height: 3px;
    -fx-max-height: 3px;
    -fx-padding: 0;
}

.progress-bar > .track {
    -fx-background-color: transparent;
    -fx-background-radius: 0;
    -fx-background-insets: 0;
}

.progress-bar > .bar {
    -fx-background-color: -electric-cyan;
    -fx-background-radius: 0;
    -fx-background-insets: 0;
    -fx-padding: 0;
}

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

# 44. tokens-dark.css

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
    /* 로고의 불꽃에서 튀는 보라 불티. 1.1.0 에서 게이지가 넷이 되며 이름을 얻었다 —
       프레임(Ember)·힙(Gold)·TLB(Cyan) 옆에서 스왑이 자기 색을 가져야 했다. */
    -forge-violet:       #d946ef;

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

# 45. tokens-light.css

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
    /* 로고의 불꽃에서 튀는 보라 불티. 1.1.0 에서 게이지가 넷이 되며 이름을 얻었다 —
       프레임(Ember)·힙(Gold)·TLB(Cyan) 옆에서 스왑이 자기 색을 가져야 했다. */
    -forge-violet:       #d946ef;

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

