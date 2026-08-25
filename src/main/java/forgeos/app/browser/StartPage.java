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
