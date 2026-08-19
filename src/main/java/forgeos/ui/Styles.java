package forgeos.ui;

import javafx.css.PseudoClass;

/**
 * ForgeOS가 쓰는 CSS 의사 클래스 모음.
 *
 * <p>상태를 자바에서 {@code setStyle}로 칠하지 않고 의사 클래스만 토글한다.
 * 그래야 "활성 창은 어떻게 보이는가", "실행 중인 Dock 아이콘은 어떻게
 * 보이는가"를 CSS 파일 한 줄로 바꿀 수 있다. 색과 치수가 자바 코드로 새어
 * 들어가는 순간 디자인 변경은 컴파일이 필요한 일이 된다.</p>
 */
public final class Styles {

    /** 활성(최상단) 창. 비활성 창은 그림자와 채도를 낮춰 뒤로 물러나게 한다. */
    public static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    /** Dock 아이콘이 가리키는 앱이 실행 중일 때. */
    public static final PseudoClass RUNNING = PseudoClass.getPseudoClass("running");

    /** 위험한 동작(프로세스 강제 종료, 교착 복구)을 하는 버튼. */
    public static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");

    /** 교착 상태에 얽힌 프로세스 노드. */
    public static final PseudoClass DEADLOCKED = PseudoClass.getPseudoClass("deadlocked");

    /** 켜짐 상태의 토글 스위치. */
    public static final PseudoClass ON = PseudoClass.getPseudoClass("on");

    private Styles() {
    }
}
