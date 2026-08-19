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
