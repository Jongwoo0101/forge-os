package forgeos.app;

import forgeframework.process.ExecResultDto;
import forgeframework.process.ProcessControlBlock;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.core.KernelService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 열려 있는 앱 창과 커널 프로세스를 1:1로 묶어 두는 표.
 *
 * <h2>왜 필요했나</h2>
 * <p>{@code 1.1.0}까지 ForgeOS의 앱 창은 순수 JavaFX 노드였다. ForgeWeb을 여섯 개
 * 열어도 커널은 그 사실을 몰랐고, 활성 상태 보기의 프로세스 표는 비어 있었으며
 * 메모리 게이지도 0이었다. <b>운영체제 시뮬레이터의 데스크탑에서 앱을 실행했는데
 * 그 운영체제가 모른다</b>는 것은 설명하기 어려운 상태다. 창을 열면 프로세스가
 * 생기고 메모리를 먹는다 — 여기서부터가 데스크탑이다.</p>
 *
 * <h2>앱 프로세스는 CPU를 두고 다투지 않는다</h2>
 * <p>커널의 프로세스는 전부 "버스트 시간만큼 CPU를 쓰고 끝나는 배치 작업"이다.
 * GUI 앱은 그런 물건이 아니다 — 끝나지 않고, 대부분의 시간을 <b>입력을 기다리며</b>
 * 보낸다. 그래서 앱 프로세스는 만들자마자 {@code io_req … keyboard}로 키보드
 * 대기열에 넣어 {@code WAITING}으로 재운다.</p>
 *
 * <p>이것은 편법이 아니라 실제 모델과 같다. 그리고 재우지 않으면 FCFS에서
 * <b>첫 번째로 열린 앱이 CPU를 영원히 붙들고</b> 사용자가 만든 프로세스가 하나도
 * 진행되지 않는다. 비선점 스케줄러에 끝나지 않는 프로세스를 올리면 그렇게 된다.</p>
 *
 * <p>터미널에서 {@code type} 을 치면 키보드 인터럽트가 대기열의 맨 앞을 깨우므로
 * 앱 프로세스 하나가 READY로 돌아올 수 있다. 그래서 매 갱신 펄스마다
 * {@link #sweep()}이 <b>다시 재운다</b> — 입력을 처리하고 다시 기다림으로
 * 돌아가는 GUI 앱의 실제 동작과 같다.</p>
 *
 * <h2>프로세스를 죽이면 창이 닫힌다</h2>
 * <p>활성 상태 보기에서 {@code forgeweb} 프로세스를 강제 종료하면 ForgeWeb 창이
 * 닫힌다. 표와 화면이 같은 사실을 가리키게 하려면 방향이 양쪽으로 다 통해야 한다.
 * 한쪽으로만 통하면 표는 장식이 된다.</p>
 */
public final class AppProcessTable {

    /**
     * 앱 프로세스의 버스트 시간.
     *
     * <p>GUI 앱은 끝나지 않으므로 실제로는 아무 값이나 상관없다. 그럼에도 큰 값을
     * 두는 이유는, 표의 진행 막대가 거의 비어 있는 채로 남아 "이건 끝나려고 도는
     * 일이 아니다"를 보여 주기 때문이다.</p>
     */
    private static final long APP_BURST_TIME = 999;

    /** 앱 프로세스의 우선순위. 가장 낮게 둬서 어쩌다 깨어나도 사용자 프로세스를 밀지 않는다. */
    private static final int APP_PRIORITY = ProcessControlBlock.MAX_PRIORITY_VALUE;

    /** 앱 프로세스를 재워 둘 장치. 키보드를 고른 이유는 GUI 앱이 실제로 기다리는 것이 입력이기 때문이다. */
    private static final String PARK_DEVICE = "keyboard";

    private final KernelService kernelService;
    private final Consumer<String> onProcessLost;

    /** 앱 id → 커널 PID. 창이 열려 있는 동안만 항목이 존재한다. */
    private final Map<String, Integer> pidByAppId = new HashMap<>();

    /**
     * 표를 만들고 갱신 펄스에 붙는다.
     *
     * @param kernelService 시스템 콜 통로
     * @param onProcessLost 창은 열려 있는데 프로세스가 사라졌을 때 호출된다 (창을 닫으라는 뜻)
     */
    public AppProcessTable(KernelService kernelService, Consumer<String> onProcessLost) {
        this.kernelService = kernelService;
        this.onProcessLost = onProcessLost;
        kernelService.onRefresh(this::sweep);
    }

    /**
     * 앱 창이 열렸다. 커널에 프로세스를 만들고 메모리를 할당한 뒤 재운다.
     *
     * <p>커널이 아직 없거나 이미 내려갔으면 조용히 아무것도 하지 않는다. 데스크탑은
     * 커널 없이도 떠 있어야 한다 — {@code shutdown} 뒤에 창을 여는 것이 예외로
     * 터지면 종료 화면에서 앱이 죽는다.</p>
     *
     * @param app 열린 앱
     */
    public void launch(ForgeApp app) {
        if (pidByAppId.containsKey(app.id())) {
            return;
        }
        SystemCallResult exec = kernelService.call(SystemCallType.EXEC,
                app.id(), String.valueOf(APP_BURST_TIME), String.valueOf(APP_PRIORITY));
        if (!exec.isSuccess()) {
            return;
        }
        int pid = exec.dataAs(ExecResultDto.class).pid();
        pidByAppId.put(app.id(), pid);

        if (app.memoryFootprint() > 0) {
            // 실패해도(프레임이 꽉 찼어도) 프로세스는 그대로 둔다. 메모리가 모자라
            // 앱이 안 열리는 것보다, 열리되 힙이 비어 있는 편이 낫다.
            kernelService.call(SystemCallType.MALLOC,
                    String.valueOf(pid), String.valueOf(app.memoryFootprint()));
        }
        park(pid);
    }

    /**
     * 앱 창이 닫혔다. 프로세스를 종료한다.
     *
     * <p>메모리는 따로 반납하지 않는다 — 커널이 프로세스 종료 리스너에서
     * {@code releaseProcess}로 주소 공간을 통째로 회수한다.</p>
     *
     * @param appId 닫힌 앱의 id
     */
    public void terminate(String appId) {
        Integer pid = pidByAppId.remove(appId);
        if (pid != null) {
            kernelService.call(SystemCallType.KILL, String.valueOf(pid));
        }
    }

    /**
     * 이 앱이 지금 쓰고 있는 PID.
     *
     * @param appId 앱 id
     * @return PID. 열려 있지 않으면 {@code -1}
     */
    public int pidOf(String appId) {
        return pidByAppId.getOrDefault(appId, -1);
    }

    /**
     * 매 갱신 펄스마다 앱 프로세스의 상태를 살핀다.
     *
     * <ul>
     *   <li>깨어나 있으면 다시 재운다 (입력을 처리하고 대기로 돌아가는 것과 같다)</li>
     *   <li>사라졌으면 창을 닫으라고 알린다 (사용자가 표에서 강제 종료한 경우)</li>
     * </ul>
     */
    private void sweep() {
        if (pidByAppId.isEmpty()) {
            return;
        }
        // 같은 펄스에 메뉴바와 활성 상태 보기도 PS 를 본다. 한 번만 묻는다.
        SystemCallResult ps = kernelService.callCached(SystemCallType.PS);
        if (!ps.isSuccess()) {
            return;
        }
        Map<Integer, ProcessState> stateByPid = new HashMap<>();
        for (ProcessDto process : ps.dataAsList(ProcessDto.class)) {
            stateByPid.put(process.pid(), process.state());
        }

        List<String> lost = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : Map.copyOf(pidByAppId).entrySet()) {
            ProcessState state = stateByPid.get(entry.getValue());
            if (state == null || state == ProcessState.TERMINATED) {
                lost.add(entry.getKey());
            } else if (state != ProcessState.WAITING) {
                park(entry.getValue());
            }
        }

        for (String appId : lost) {
            pidByAppId.remove(appId);
            onProcessLost.accept(appId);
        }
    }

    private void park(int pid) {
        kernelService.call(SystemCallType.IO_REQ, String.valueOf(pid), PARK_DEVICE);
    }
}
