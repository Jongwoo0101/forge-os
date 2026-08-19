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
