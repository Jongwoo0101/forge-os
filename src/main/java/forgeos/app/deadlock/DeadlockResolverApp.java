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
