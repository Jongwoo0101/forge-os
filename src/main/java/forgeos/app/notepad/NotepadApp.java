package forgeos.app.notepad;

import forgeos.app.AppContext;
import forgeos.app.AppInstance;
import forgeos.app.ForgeApp;
import forgeos.ui.Glyphs;

/**
 * 메모장 — 커널 파일 시스템 위에서 도는 텍스트 편집기.
 *
 * <h2>왜 호스트 디스크가 아니라 커널 디스크인가</h2>
 * <p>맥의 실제 폴더에 {@code .txt}를 떨구는 편이 구현은 훨씬 쉽다. 그렇게 하지
 * 않은 이유는 이 앱이 하는 일이 "글을 적는 것"이 아니라 <b>커널의 파일 시스템을
 * 손으로 만져 보는 것</b>이기 때문이다. 메모장에서 저장한 파일은 Finder 의 컬럼에
 * 곧바로 나타나고, 터미널의 {@code cat} 으로 읽히며, {@code sync} 를 누르면
 * {@code disk.img} 에 내려앉아 재부팅을 견딘다. 1.1.0 이 들여온 영속화를
 * 명령어 없이 눈으로 확인할 수 있는 자리가 바로 여기다.</p>
 */
public final class NotepadApp implements ForgeApp {

    @Override
    public String id() {
        return "notepad";
    }

    @Override
    public String title() {
        return "메모장";
    }

    @Override
    public String iconPath() {
        return Glyphs.NOTEPAD;
    }

    @Override
    public double preferredWidth() {
        return 880;
    }

    @Override
    public double preferredHeight() {
        return 560;
    }

    @Override
    public AppInstance launch(AppContext context) {
        NotepadView view = new NotepadView(context);
        return new AppInstance(view, view::dispose);
    }
}
