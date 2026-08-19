package forgeos.app;

import javafx.scene.Node;

/**
 * 실행된 앱 하나 — 화면과 뒷정리 한 쌍.
 *
 * <p>{@code dispose}를 옵션으로 두지 않은 이유가 있다. 앱은 거의 예외 없이
 * {@code KernelService.onRefresh}를 구독하는데, 창을 닫을 때 해지하지 않으면
 * 이미 화면에서 사라진 표를 계속 갱신하게 된다. 창을 스무 번 여닫으면 초당
 * 스무 번의 유령 갱신이 남는다. 반환 타입에 강제로 넣어 두면 잊을 수가 없다.</p>
 *
 * @param view    창 안에 들어갈 화면
 * @param dispose 창이 닫힐 때 실행할 정리 작업 (할 일이 없으면 빈 람다)
 */
public record AppInstance(Node view, Runnable dispose) {
}
