package forgeos.app.monitor;

import forgeframework.memory.FrameInfo;
import forgeframework.memory.HeapSnapshot;
import forgeframework.memory.MemorySnapshot;
import forgeframework.memory.PageInfo;
import forgeframework.memory.PageReplacementPolicy;
import forgeframework.process.ForkResultDto;
import forgeframework.process.PriorityResultDto;
import forgeframework.process.ProcessControlBlock;
import forgeframework.process.ProcessDto;
import forgeframework.process.ProcessState;
import forgeframework.process.SchedulerDto;
import forgeframework.process.SchedulerQueueDto;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
 *
 * <h2>1.1.0 — 커널이 넓어진 만큼 화면도 넓어졌다</h2>
 * <p>커널이 스케줄러 6종·스왑·fork(COW)를 갖게 되면서, 이 앱이 답해야 할 질문이
 * 늘었다. "누가 CPU를 기다리는가"만이 아니라 <b>"왜 저 프로세스가 먼저 뽑혔는가"</b>
 * (스케줄러·우선순위·큐 등급), <b>"메모리가 어디로 갔는가"</b>(프레임·스왑·COW 공유)
 * 까지다. 그래서 표는 탭 둘로 나뉜다 — 프로세스 탭이 앞의 질문을, 메모리 탭이
 * 뒤의 질문을 맡는다. 게이지는 두 탭에 공통이라 사이드바에 남는다.</p>
 *
 * <h2>주기 갱신과 사용자 조작이 부딪히지 않게</h2>
 * <p>스케줄러·교체 정책 콤보 상자는 커널 값을 되비추면서 동시에 사용자 입력을
 * 받는다. 되비추는 순간에도 {@code setValue}는 변경 이벤트를 쏘므로, 그대로 두면
 * 1초마다 커널에 "스케줄러를 지금 값으로 바꿔라"는 시스템 콜이 날아간다.
 * {@link #syncing} 이 그 순환을 끊는다.</p>
 */
final class ActivityMonitorView extends BorderPane {

    /** 게이지 지름. 사이드바에 넷이 세로로 들어가야 해서 기본값보다 작다. */
    private static final double GAUGE_SIZE = 96;

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ObservableList<ProcessDto> processes = FXCollections.observableArrayList();
    private final TableView<ProcessDto> table = new TableView<>(processes);

    private final ObservableList<FrameInfo> frames = FXCollections.observableArrayList();
    private final TableView<FrameInfo> frameTable = new TableView<>(frames);

    private final ObservableList<PageInfo> pages = FXCollections.observableArrayList();
    private final TableView<PageInfo> pageTable = new TableView<>(pages);

    private final DonutChart frameChart = new DonutChart("물리 프레임", "accent-ember", GAUGE_SIZE);
    private final DonutChart heapChart = new DonutChart("힙 사용량", "accent-gold", GAUGE_SIZE);
    private final DonutChart tlbChart = new DonutChart("TLB 적중률", "accent-cyan", GAUGE_SIZE);
    private final DonutChart swapChart = new DonutChart("스왑 슬롯", "accent-violet", GAUGE_SIZE);

    private final ComboBox<Algorithm> schedulerBox = new ComboBox<>();
    private final ComboBox<PageReplacementPolicy> policyBox = new ComboBox<>();

    private final TabPane tabs = new TabPane();
    private final FlowPane queueStrip = new FlowPane();
    private final Label pageTableTitle = new Label("페이지 테이블");
    private final Label faultLabel = new Label();
    private final Label statusLabel = new Label();

    private final TextField processNameField = new TextField();
    private final TextField burstTimeField = new TextField();
    private final TextField priorityField = new TextField();

    /** 커널 값을 콤보에 되비추는 중. 그동안 변경 이벤트는 무시한다. */
    private boolean syncing;

    ActivityMonitorView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("activity-monitor");

        buildProcessTable();
        buildFrameTable();
        buildPageTable();

        BorderPane left = new BorderPane();
        left.getStyleClass().add("monitor-left");
        left.setTop(buildToolbar());
        left.setCenter(buildTabs());
        left.setBottom(buildStatusBar());

        setCenter(left);
        setRight(buildSidebar());

        this.subscription = kernelService.onRefresh(this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private void buildProcessTable() {
        table.getColumns().add(column("PID", 56, true, p -> String.valueOf(p.pid())));
        table.getColumns().add(column("이름", 128, false, ProcessDto::name));
        // 부모 PID 는 fork 가 생기면서 의미를 얻은 열이다. 부모가 없으면 -1 이 온다.
        table.getColumns().add(column("PPID", 58, true,
                p -> p.parentPid() < 0 ? "—" : String.valueOf(p.parentPid())));
        table.getColumns().add(column("우선", 54, true, p -> String.valueOf(p.priority())));
        // 큐 등급은 MLFQ 에서만 뜻이 있다. 다른 스케줄러에서는 전부 같은 값이라 가로줄만 보여 준다.
        table.getColumns().add(column("큐", 48, true,
                p -> p.queueLevel() < 0 ? "—" : "Q" + p.queueLevel()));

        TableColumn<ProcessDto, ProcessState> state = new TableColumn<>("상태");
        state.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().state()));
        state.setCellFactory(col -> new StateCell());
        state.setPrefWidth(104);
        table.getColumns().add(state);

        table.getColumns().add(column("CPU 사용", 96, true,
                p -> p.cpuTimeUsed() + " / " + p.burstTime()));
        table.getColumns().add(column("진행", 124, false, ActivityMonitorView::formatProgress));

        table.getStyleClass().add("process-table");
        table.setPlaceholder(new Label("실행 중인 프로세스가 없습니다.\n위에서 새 프로세스를 만들어 보세요."));
        table.setContextMenu(buildRowMenu());
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> refreshPageTable());
        // 리사이즈 정책은 기본값(UNCONSTRAINED)을 그대로 둔다. CONSTRAINED_RESIZE_POLICY 는
        // JavaFX 20에서 deprecated 되었고, 이 프로젝트는 -Werror 기준선을 지킨다.
    }

    private void buildFrameTable() {
        frameTable.getColumns().add(column("프레임", 66, true, f -> "#" + f.frameNumber()));
        frameTable.getColumns().add(column("상태", 68, false, f -> f.allocated() ? "USED" : "FREE"));
        frameTable.getColumns().add(column("PID", 56, true,
                f -> f.allocated() ? String.valueOf(f.ownerPid()) : "—"));
        frameTable.getColumns().add(column("페이지", 62, true,
                f -> f.allocated() ? "#" + f.pageNumber() : "—"));
        // 참조 수가 2 이상이면 fork 로 공유 중인 프레임이다. 그 프레임은 스왑 대상에서도 빠진다.
        frameTable.getColumns().add(column("REF", 52, true,
                f -> f.allocated() ? String.valueOf(f.refCount()) : "—"));
        frameTable.getColumns().add(column("플래그", 120, false, ActivityMonitorView::frameFlags));

        frameTable.getStyleClass().add("process-table");
        frameTable.setPlaceholder(new Label("프레임 정보가 없습니다."));
    }

    private void buildPageTable() {
        pageTable.getColumns().add(column("페이지", 66, true, p -> "#" + p.pageNumber()));
        pageTable.getColumns().add(column("상태", 74, false, ActivityMonitorView::pageState));
        pageTable.getColumns().add(column("프레임", 66, true,
                p -> p.present() ? "#" + p.frameNumber() : "—"));
        pageTable.getColumns().add(column("스왑", 62, true,
                p -> p.swapSlot() < 0 ? "—" : "#" + p.swapSlot()));
        pageTable.getColumns().add(column("권한", 58, false, p -> p.writable() ? "rw" : "r-"));
        pageTable.getColumns().add(column("COW", 58, false, p -> p.copyOnWrite() ? "공유" : "—"));
        pageTable.getColumns().add(column("REF", 52, true, p -> String.valueOf(p.refCount())));

        pageTable.getStyleClass().add("process-table");
        pageTable.setPlaceholder(new Label("프로세스를 고르면 그 주소 공간이 보입니다."));
    }

    private TabPane buildTabs() {
        tabs.getStyleClass().add("monitor-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        queueStrip.getStyleClass().add("queue-strip");

        BorderPane processPane = new BorderPane();
        processPane.getStyleClass().add("monitor-pane");
        processPane.setCenter(table);
        processPane.setBottom(queueStrip);

        Tab processTab = new Tab("프로세스", processPane);
        Tab memoryTab = new Tab("메모리", buildMemoryPane());

        tabs.getTabs().addAll(processTab, memoryTab);
        // 메모리 탭으로 옮기는 순간 바로 채워져야 한다. 다음 펄스(최대 1초)를 기다리게 하지 않는다.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refresh());
        return tabs;
    }

    private VBox buildMemoryPane() {
        Label frameTitle = new Label("프레임 테이블 — 물리 메모리 한 장 한 장");
        frameTitle.getStyleClass().add("monitor-section");
        pageTableTitle.getStyleClass().add("monitor-section");

        VBox.setVgrow(frameTable, Priority.ALWAYS);
        VBox.setVgrow(pageTable, Priority.ALWAYS);

        VBox pane = new VBox(frameTitle, frameTable, pageTableTitle, pageTable);
        pane.getStyleClass().addAll("monitor-pane", "monitor-memory");
        return pane;
    }

    private HBox buildToolbar() {
        processNameField.setPromptText("프로세스 이름");
        processNameField.getStyleClass().add("field");
        burstTimeField.setPromptText("버스트");
        burstTimeField.getStyleClass().addAll("field", "field-narrow");
        priorityField.setPromptText("우선");
        priorityField.getStyleClass().addAll("field", "field-narrow");

        Button exec = new Button("실행", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        exec.getStyleClass().add("toolbar-button");
        exec.setOnAction(e -> execProcess());

        Button fork = new Button("복제", Glyphs.stroked(Glyphs.FORK, 14, "button-glyph"));
        fork.getStyleClass().add("toolbar-button");
        fork.setOnAction(e -> forkSelected());
        fork.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button kill = new Button("강제 종료", Glyphs.stroked(Glyphs.STOP, 14, "button-glyph"));
        kill.getStyleClass().add("toolbar-button");
        kill.pseudoClassStateChanged(Styles.DANGER, true);
        kill.setOnAction(e -> killSelected());
        kill.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        schedulerBox.getItems().setAll(
                new Algorithm("fcfs", "FCFS", "FCFS · 도착 순서"),
                new Algorithm("rr", "Round Robin", "Round Robin · 퀀텀"),
                new Algorithm("sjf", "SJF", "SJF · 총 실행 시간"),
                new Algorithm("srtf", "SRTF", "SRTF · 남은 시간"),
                new Algorithm("priority", "Priority", "Priority · 우선순위"),
                new Algorithm("mlfq", "MLFQ", "MLFQ · 다단계 피드백"));
        schedulerBox.setOnAction(e -> onSchedulerPicked());

        Label caption = new Label("스케줄러");
        caption.getStyleClass().add("toolbar-caption");

        HBox bar = new HBox(processNameField, burstTimeField, priorityField,
                exec, fork, kill, spacer, caption, schedulerBox);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ContextMenu buildRowMenu() {
        MenuItem forkItem = new MenuItem("복제 (fork)");
        forkItem.setOnAction(e -> forkSelected());

        MenuItem raise = new MenuItem("우선순위 올리기 (값 −1)");
        raise.setOnAction(e -> nudgePriority(-1));

        MenuItem lower = new MenuItem("우선순위 내리기 (값 +1)");
        lower.setOnAction(e -> nudgePriority(1));

        MenuItem kill = new MenuItem("강제 종료");
        kill.setOnAction(e -> killSelected());

        return new ContextMenu(forkItem, new SeparatorMenuItem(), raise, lower,
                new SeparatorMenuItem(), kill);
    }

    private ScrollPane buildSidebar() {
        policyBox.getItems().setAll(PageReplacementPolicy.values());
        policyBox.setOnAction(e -> onPolicyPicked());

        Label policyCaption = new Label("페이지 교체 정책");
        policyCaption.getStyleClass().add("toolbar-caption");

        VBox policyBoxRow = new VBox(policyCaption, policyBox);
        policyBoxRow.getStyleClass().add("policy-box");

        faultLabel.getStyleClass().add("sidebar-note");

        VBox sidebar = new VBox(frameChart, heapChart, tlbChart, swapChart,
                policyBoxRow, faultLabel);
        sidebar.getStyleClass().add("monitor-sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);

        // 게이지 넷과 정책 상자를 합치면 작은 창에서는 세로가 모자란다. 잘리는 대신 스크롤한다.
        ScrollPane scroller = new ScrollPane(sidebar);
        scroller.getStyleClass().add("monitor-sidebar-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setMinWidth(Region.USE_PREF_SIZE);
        return scroller;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("toolbar-status");
        HBox bar = new HBox(statusLabel);
        bar.getStyleClass().add("monitor-status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void execProcess() {
        String name = processNameField.getText().isBlank()
                ? "proc" + (processes.size() + 1)
                : processNameField.getText().trim();

        List<String> args = new java.util.ArrayList<>();
        args.add(name);
        if (!burstTimeField.getText().isBlank()) {
            args.add(burstTimeField.getText().trim());
        }
        if (!priorityField.getText().isBlank()) {
            // 우선순위는 세 번째 인자다. 버스트를 비워 두고 우선순위만 줄 수는 없으므로 기본값을 채운다.
            if (args.size() == 1) {
                args.add(String.valueOf(defaultBurst()));
            }
            args.add(priorityField.getText().trim());
        }

        SystemCallResult result = kernelService.call(
                SystemCallType.EXEC, args.toArray(new String[0]));
        if (result.isSuccess()) {
            processNameField.clear();
            burstTimeField.clear();
            priorityField.clear();
        }
        status(result.getMessage());
        refresh();
    }

    private void forkSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.FORK, String.valueOf(selected.pid()));
        if (result.isSuccess()) {
            ForkResultDto fork = result.dataAs(ForkResultDto.class);
            status("PID %d → PID %d 복제 완료. 페이지 %d장을 COW 로 공유합니다 — 프레임은 늘지 않았습니다."
                    .formatted(fork.parentPid(), fork.childPid(), fork.sharedPages()));
        } else {
            status(result.getMessage());
        }
        refresh();
    }

    private void nudgePriority(int delta) {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int next = Math.max(ProcessControlBlock.MIN_PRIORITY_VALUE,
                Math.min(ProcessControlBlock.MAX_PRIORITY_VALUE, selected.priority() + delta));
        if (next == selected.priority()) {
            status("우선순위는 %d ~ %d 범위입니다.".formatted(
                    ProcessControlBlock.MIN_PRIORITY_VALUE, ProcessControlBlock.MAX_PRIORITY_VALUE));
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.PRIORITY,
                String.valueOf(selected.pid()), String.valueOf(next));
        if (result.isSuccess()) {
            PriorityResultDto changed = result.dataAs(PriorityResultDto.class);
            status("PID %d 우선순위 %d → %d%s".formatted(
                    changed.pid(), changed.oldPriority(), changed.newPriority(),
                    changed.requeued() ? " (준비 큐에 다시 넣어 정렬을 유지했습니다)" : ""));
        } else {
            status(result.getMessage());
        }
        refresh();
    }

    private void killSelected() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.KILL, String.valueOf(selected.pid()));
        status(result.getMessage());
        refresh();
    }

    private void onSchedulerPicked() {
        Algorithm picked = schedulerBox.getValue();
        if (syncing || picked == null) {
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER, picked.token());
        status(result.getMessage());
        refresh();
    }

    private void onPolicyPicked() {
        PageReplacementPolicy picked = policyBox.getValue();
        if (syncing || picked == null) {
            return;
        }
        SystemCallResult result = kernelService.call(
                SystemCallType.SWAPINFO, "policy", picked.name().toLowerCase(java.util.Locale.ROOT));
        status(result.getMessage());
        refresh();
    }

    // ────────────────────────────── 갱신 ──────────────────────────────

    private void refresh() {
        refreshProcesses();
        refreshMemory();
        refreshScheduler();
        if (tabs.getSelectionModel().getSelectedIndex() == 1) {
            refreshFrameTable();
            refreshPageTable();
        }
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

        int swapTotal = snapshot.swapTotalSlots();
        double swapRatio = swapTotal == 0 ? 0 : (double) snapshot.swapUsedSlots() / swapTotal;
        swapChart.setValue(swapRatio, swapTotal == 0
                ? "스왑이 꺼져 있음"
                : "%d / %d 슬롯".formatted(snapshot.swapUsedSlots(), swapTotal));

        syncing = true;
        policyBox.setValue(snapshot.replacementPolicy());
        syncing = false;

        faultLabel.setText("""
                페이지 폴트 %d · 스왑 인 %d · 스왑 아웃 %d
                COW 폴트 %d

                폴트는 고장이 아니라 정상 동작입니다. 프레임이 모자라면 커널이
                가장 쓸모없어 보이는 페이지를 밀어내고, 그 페이지에 다시 손이
                닿는 순간 폴트로 되찾아 옵니다."""
                .formatted(snapshot.pageFaults(), snapshot.swapIns(),
                        snapshot.swapOuts(), snapshot.cowFaults()));
    }

    private void refreshScheduler() {
        SystemCallResult result = kernelService.call(SystemCallType.SCHEDULER);
        if (!result.isSuccess()) {
            return;
        }
        SchedulerDto dto = result.dataAs(SchedulerDto.class);

        syncing = true;
        for (Algorithm option : schedulerBox.getItems()) {
            if (dto.name().startsWith(option.kernelPrefix())) {
                schedulerBox.setValue(option);
                break;
            }
        }
        syncing = false;

        queueStrip.getChildren().clear();
        for (SchedulerQueueDto queue : dto.queues()) {
            queueStrip.getChildren().add(queueChip(dto, queue));
        }
    }

    private void refreshFrameTable() {
        SystemCallResult result = kernelService.call(SystemCallType.FRAMETABLE);
        if (result.isSuccess()) {
            frames.setAll(result.dataAsList(FrameInfo.class));
        }
    }

    private void refreshPageTable() {
        ProcessDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            pages.clear();
            pageTableTitle.setText("페이지 테이블 — 프로세스를 고르세요");
            return;
        }
        pageTableTitle.setText("페이지 테이블 — PID %d (%s)".formatted(selected.pid(), selected.name()));

        SystemCallResult result = kernelService.call(
                SystemCallType.PAGETABLE, String.valueOf(selected.pid()));
        if (result.isSuccess()) {
            pages.setAll(result.dataAsList(PageInfo.class));
        } else {
            pages.clear();
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private Label queueChip(SchedulerDto scheduler, SchedulerQueueDto queue) {
        String title = scheduler.queues().size() > 1
                ? "Q%d (q=%d)".formatted(queue.level(), queue.timeQuantum())
                : "준비 큐 (q=%d)".formatted(queue.timeQuantum());
        String body = queue.pids().isEmpty()
                ? "비어 있음"
                : queue.pids().stream().map(String::valueOf)
                        .reduce((a, b) -> a + " → " + b).orElse("");

        Label chip = new Label(title + "  " + body);
        chip.getStyleClass().add("queue-chip");
        chip.pseudoClassStateChanged(Styles.ON, !queue.pids().isEmpty());
        return chip;
    }

    private long defaultBurst() {
        var kernel = kernelService.kernel();
        return kernel == null ? 10 : kernel.getConfig().defaultBurstTime();
    }

    private void status(String message) {
        if (message != null && !message.isBlank()) {
            statusLabel.setText(message.lines().findFirst().orElse(message));
        }
    }

    private static <T> TableColumn<T, String> column(String title, double width, boolean numeric,
                                                     java.util.function.Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(text.apply(cell.getValue())));
        column.setPrefWidth(width);
        if (numeric) {
            column.getStyleClass().add("column-numeric");
        }
        return column;
    }

    private static String frameFlags(FrameInfo frame) {
        if (!frame.allocated()) {
            return "—";
        }
        String bits = (frame.dirty() ? "D" : "-") + (frame.referenced() ? "R" : "-");
        return frame.refCount() >= 2 ? bits + "  COW" : bits;
    }

    private static String pageState(PageInfo page) {
        if (!page.valid()) {
            return "—";
        }
        return page.present() ? "MEM" : "SWAP";
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

    /**
     * 스케줄러 선택지 하나.
     *
     * <p>{@code kernelPrefix}가 따로 있는 이유는, 커널이 돌려주는 이름
     * ({@code "Round Robin (RR)"})이 인자({@code "rr"})로 시작하지 않는 경우가
     * 있기 때문이다. 인자를 접두사로 삼아 맞춰 보는 방식은 그 하나에서 조용히 깨진다.</p>
     *
     * @param token        커널이 알아듣는 인자 (예: {@code mlfq})
     * @param kernelPrefix 커널이 돌려주는 이름의 앞부분 (예: {@code Round Robin})
     * @param label        사람이 읽는 이름
     */
    private record Algorithm(String token, String kernelPrefix, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** 상태를 색 배지로 보여 주는 셀. 색은 CSS의 {@code .state-*} 클래스가 정한다. */
    private static final class StateCell extends TableCell<ProcessDto, ProcessState> {

        StateCell() {
        }

        @Override
        protected void updateItem(ProcessState state, boolean empty) {
            super.updateItem(state, empty);
            if (empty || state == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(state.name());
            badge.getStyleClass().addAll("state-badge", "state-" + state.name().toLowerCase(
                    java.util.Locale.ROOT));
            setGraphic(badge);
            setText(null);
        }
    }
}
