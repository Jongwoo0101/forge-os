package forgeos.app;

import forgeos.core.KernelService;
import forgeos.wm.WindowManager;

/**
 * 앱이 바깥 세계에 닿는 유일한 통로.
 *
 * <p>앱이 커널이나 창 관리자를 전역 정적 변수로 집어 오면 테스트도 못 하고,
 * 어느 앱이 무엇을 건드리는지도 알 수 없게 된다. 필요한 것만 생성자로 준다.</p>
 *
 * @param kernel  커널 서비스 (시스템 콜, 로그, 주기 갱신)
 * @param windows 창 관리자 (다른 앱 열기, 작업 영역 조회)
 */
public record AppContext(KernelService kernel, WindowManager windows) {
}
