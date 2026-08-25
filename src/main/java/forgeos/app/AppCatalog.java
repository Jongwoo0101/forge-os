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
