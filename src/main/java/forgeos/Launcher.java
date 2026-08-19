package forgeos;

import javafx.application.Application;

/**
 * ForgeOS 진입점.
 *
 * <p>{@link ForgeOsApp}을 직접 {@code main} 클래스로 지정하지 않는 이유가 있다.
 * JVM은 메인 클래스가 {@code Application}의 서브클래스이면 JavaFX 런타임을 먼저
 * 확인하는데, 모듈 경로에 JavaFX가 없으면 "JavaFX runtime components are missing"
 * 한 줄만 남기고 죽는다. 한 겹 감싸 두면 그 검사를 우회해서 클래스패스 실행도
 * 가능해지고, 실패하더라도 실제 스택 트레이스를 볼 수 있다.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * ForgeOS를 기동한다.
     *
     * @param args JavaFX에 그대로 전달되는 실행 인자
     */
    public static void main(String[] args) {
        Application.launch(ForgeOsApp.class, args);
    }
}
