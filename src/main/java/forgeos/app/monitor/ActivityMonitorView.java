package forgeos.app.monitor;

import forgeframework.memory.HeapSnapshot;
import forgeframework.memory.MemorySnapshot;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.process.SchedulerDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Styles;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 활성 상태 보기 화면.
 *
 * <h2>선택을 잃지 않기</h2>
 * <p>1초마다 표를 통째로 갈아 끼우면 사용자가 고른 행이 매초 풀린다. 프로세스를
 * 고르고 종료 버튼으로 손을 옮기는 사이에 선택이 사라지는 표는 쓸 수가 없다.
 * 그래서 갱신 전에 PID를 기억했다가 갱신 후 같은 PID를 다시 고른다.</p>
 */
final class ActivityMonitorView extends BorderPane {

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ObservableList<ProcessDto> processes = FXCollections.observableArrayList();
    private final TableView<ProcessDto> table = new TableView<>(processes);

    private final DonutChart frameChart = new DonutChart("물리 프레임", "accent-ember");
    private final DonutChart heapChart = new DonutChart("힙 사용량", "accent-gold");
    private final DonutChart tlbChart = new DonutChart("TLB 적중률", "accent-cyan");

    private final Label schedulerLabel = new Label("스케줄러 —");
    private final Label tlbDetailLabel = new Label();
    private final TextField processNameField = new TextField();
    private final TextField burstTimeField = new TextField();

    ActivityMonitorView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("activity-monitor");

        buildTable();

        BorderPane left = new BorderPane();
        left.getStyleClass().add("monitor-left");
        left.setTop(buildToolbar());
        left.setCenter(table);

        setCenter(left);
        setRight(buildSidebar());

        this.subscription = kernelService.onRefresh(this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private void buildTable() {
        TableColumn<ProcessDto, Integer> pid = new TableColumn<>("PID");
        pid.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().pid()));
        pid.getStyleClass().add("column-numeric");
        pid.setPrefWidth(64);

        TableColumn<ProcessDto, String> name = new TableColumn<>("이름");
        name.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().name()));
        name.setPrefWidth(160);

        TableColumn<ProcessDto, ProcessState> state = new TableColumn<>("상태");
        state.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().state()));
        state.setCellFactory(column -> new StateCell());
        state.setPrefWidth(110);

        TableColumn<ProcessDto, String> cpu = new TableColumn<>("CPU 사용");
        cpu.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().cpuTimeUsed() + " / " + cell.getValue().burstTime()));
        cpu.getStyleClass().add("column-numeric");
        cpu.setPrefWidth(110);

        TableColumn<ProcessDto, String> progress = new TableColumn<>("진행");
        progress.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(formatProgress(cell.getValue())));
        progress.setPrefWidth(130);

        table.getColumns().add(pid);
        table.getColumns().add(name);
        table.getColumns().add(state);
        table.getColumns().add(cpu);
        table.getColumns().add(progress);

        table.getStyleClass().add("process-table");
        table.setPlaceholder(new Label("실행 중인 프로세스가 없습니다.\n위에서 새 프로세스를 만들어 보세요."));
        // 리사이즈 정책은 기본값(UNCONSTRAINED)을 그대로 둔다. CONSTRAINED_RESIZE_POLICY 는
        // JavaFX 20에서 deprecated 되었고, 이 프로젝트는 -Werror 기준선을 지킨다.
        // 대신 마지막 컬럼이 남는 폭을 먹도록 넉넉한 pref 를 준다.
    }

    private HBox buildToolbar() {
        processNameField.setPromptText("프로세스 이름");
        processNameField.getStyleClass().add("field");
        burstTimeField.setPromptText("버스트");
        burstTimeField.getStyleClass().addAll("field", "field-narrow");

        Button exec = new Button("실행", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        exec.getStyleClass().add("toolbar-button");
        exec.setOnAction(e -> execProcess());

        Button kill = new Button("강제 종료", Glyphs.stroked(Glyphs.STOP, 14, "button-glyph"));
        kill.getStyleClass().add("toolbar-button");
        kill.pseudoClassStateChanged(Styles.DANGER, true);
        kill.setOnAction(e -> killSelected());
        kill.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        schedulerLabel.getStyleClass().add("toolbar-status");

        HBox bar = new HBox(processNameField, burstTimeField, exec, kill, spacer, schedulerLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildSidebar() {
        tlbDetailLabel.getStyleClass().add("sidebar-note");

        VBox sidebar = new VBox(frameChart, heapChart, tlbChart, tlbDetailLabel);
        sidebar.getStyleClass().add("monitor-sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);
        return sidebar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void execProcess() {
        String name = processNameField.getText().isBlank()
                ? "proc" + (processes.size() + 1)
                : processNameField.getText().trim();

        SystemCallResult result = burstTimeField.getText().isBlank()
                ? kernelService.call(SystemCallType.EXEC, name)
                : kernelService.call(SystemCallType.EXEC, name, burstTimeField.getText().trim());

        if (result.isSuccess()) {
            processNameField.clear();
            burstTimeField.clear();
        }
        refresh();
    }

    private void killSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        kernelService.call(SystemCallType.KILL, String.valueOf(selected.pid()));
        refresh();
    }

    private void refresh() {
        refreshProcesses();
        refreshMemory();
        refreshScheduler();
    }

    private void refreshProcesses() {
        SystemCallResult result = kernelService.call(SystemCallType.PS);
        if (!result.isSuccess()) {
            return;
        }
        List<ProcessDto> snapshot = result.dataAsList(ProcessDto.class);

        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        Integer selectedPid = selected == null ? null : selected.pid();

        processes.setAll(snapshot);

        if (selectedPid != null) {
            for (ProcessDto process : processes) {
                if (process.pid() == selectedPid) {
                    table.getSelectionModel().select(process);
                    break;
                }
            }
        }
    }

    private void refreshMemory() {
        SystemCallResult result = kernelService.call(SystemCallType.MEMINFO);
        if (!result.isSuccess()) {
            return;
        }
        MemorySnapshot snapshot = result.dataAs(MemorySnapshot.class);

        int totalFrames = snapshot.totalFrames();
        double frameRatio = totalFrames == 0 ? 0 : (double) snapshot.usedFrames() / totalFrames;
        frameChart.setValue(frameRatio,
                "%d / %d 프레임 · %dKB".formatted(
                        snapshot.usedFrames(), totalFrames, snapshot.frameSize()));

        long heapCapacity = 0;
        long heapUsed = 0;
        for (HeapSnapshot heap : snapshot.heapByPid().values()) {
            heapCapacity += heap.capacity();
            heapUsed += heap.used();
        }
        double heapRatio = heapCapacity == 0 ? 0 : (double) heapUsed / heapCapacity;
        heapChart.setValue(heapRatio,
                heapCapacity == 0
                        ? "할당된 힙 없음"
                        : "%d / %d · 프로세스 %d개".formatted(
                                heapUsed, heapCapacity, snapshot.heapByPid().size()));

        tlbChart.setValue(snapshot.tlbHitRatio(),
                "적중 %d · 실패 %d".formatted(snapshot.tlbHits(), snapshot.tlbMisses()));

        tlbDetailLabel.setText("TLB는 주소 변환 캐시입니다. 같은 페이지를 반복 접근할수록\n"
                + "적중률이 오릅니다 — 터미널에서 translate 를 여러 번 실행해 보세요.");
    }

    private void refreshScheduler() {
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER);
        if (!result.isSuccess()) {
            return;
        }
        SchedulerDto dto = result.dataAs(SchedulerDto.class);
        schedulerLabel.setText(dto.preemptive()
                ? "스케줄러 %s · 퀀텀 %d".formatted(dto.name(), dto.timeQuantum())
                : "스케줄러 %s · 비선점".formatted(dto.name()));
    }

    private static String formatProgress(ProcessDto process) {
        if (process.burstTime() <= 0) {
            return "—";
        }
        double ratio = Math.min(1, (double) process.cpuTimeUsed() / process.burstTime());
        int filled = (int) Math.round(ratio * 10);
        return "▰".repeat(filled) + "▱".repeat(10 - filled);
    }

    void dispose() {
        subscription.cancel();
    }

    /** 상태를 색 배지로 보여 주는 셀. 색은 CSS의 {@code .state-*} 클래스가 정한다. */
    private static final class StateCell extends TableCell<ProcessDto, ProcessState> {
        @Override
        protected void updateItem(ProcessState state, boolean empty) {
            super.updateItem(state, empty);
            if (empty || state == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(state.name());
            badge.getStyleClass().addAll("state-badge", "state-" + state.name().toLowerCase());
            setGraphic(badge);
            setText(null);
        }
    }
}
