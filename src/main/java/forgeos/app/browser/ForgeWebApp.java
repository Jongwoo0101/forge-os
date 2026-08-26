package forgeos.app.browser;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * ForgeWeb — ForgeOS의 기본 웹 브라우저.
 *
 * <h2>1.1.1 — 이름을 제 것으로 바꿨다</h2>
 * <p>1.1.0 에서 이 앱의 이름은 {@code Firefox} 였다. 그러나 이 앱은 Mozilla 의
 * Gecko 를 품고 있지 않다. JavaFX 가 들고 있는 렌더링 엔진은 {@code javafx.web}
 * 의 <b>WebKit</b> 하나뿐이고, 자바 프로세스 안에서 Gecko 를 띄울 방법은 없다.
 * 남의 이름을 빌린 채로 "사실 그 엔진이 아니다"를 각주로 다는 것보다, 자기
 * 이름을 갖는 편이 정직하다 — ForgeOS 의 다른 앱들이 전부 제 이름인 것과도 맞다.</p>
 *
 * <p>앱 식별자도 {@code firefox} 에서 {@code forgeweb} 으로 바뀌었다. 활성 상태
 * 보기의 프로세스 표에 뜨는 이름이 이 값이므로, 표에서도 같은 이름으로 보인다.</p>
 *
 * <h2>왜 호스트 브라우저를 띄우지 않는가</h2>
 * <p>호스트에 설치된 진짜 브라우저를 {@code Desktop.browse()} 로 여는 길도 있었다.
 * 다섯 줄이면 됐을 것이다. 그러나 그러면 창이 ForgeOS 바깥으로 튀어나가
 * 가상 데스크탑이라는 전제가 그 순간 깨진다.</p>
 */
public final class ForgeWebApp implements ForgeApp {

    @Override
    public String id() {
        return "forgeweb";
    }

    @Override
    public String title() {
        return "ForgeWeb";
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
     * ForgeWeb 하나에 눈에 띄게 움직이는 것은 그래서 옳은 그림이다.</p>
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
