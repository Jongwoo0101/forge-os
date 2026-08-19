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
