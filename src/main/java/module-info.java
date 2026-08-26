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
     * ForgeWeb 앱의 렌더링 엔진(WebView). JavaFX 가 품는 엔진은 Gecko 도 Blink 도
     * 아닌 <b>WebKit</b> 하나뿐이다. 1.1.0 에서 이 앱의 이름이 {@code Firefox} 였던
     * 것을 1.1.1 에서 ForgeWeb 으로 바꾼 이유가 여기 있다 — 엔진이 그 엔진이 아닌데
     * 이름만 빌리면, 그 사실을 매번 각주로 달아야 한다.
     */
    requires javafx.web;

    /** 커널. ForgeOS의 모든 상태는 여기서 나온다. */
    requires forgeframework;

    /** 명령어 계층 — Terminal 앱이 CLI와 같은 명령어 세트를 그대로 쓴다. */
    requires forgeframework.cli;

    /** JavaFX 런타임이 {@code ForgeOsApp}을 리플렉션으로 생성할 수 있도록 한정 공개. */
    exports forgeos to javafx.graphics;
}
