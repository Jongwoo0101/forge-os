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
     * 앱 화면을 만든다. 창이 열릴 때마다 한 번 호출된다.
     *
     * @param context 커널 접근과 창 관리자
     * @return 화면과 정리 작업
     */
    AppInstance launch(AppContext context);
}
