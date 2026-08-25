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
