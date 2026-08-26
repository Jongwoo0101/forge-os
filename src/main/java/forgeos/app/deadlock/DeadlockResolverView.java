package forgeos.app.deadlock;

import forgeframework.deadlock.BankerConfigDto;
import forgeframework.deadlock.DeadlockDetectDto;
import forgeframework.deadlock.DeadlockRecoverDto;
import forgeframework.deadlock.ResourceRowDto;
import forgeframework.deadlock.ResourceSnapshotDto;
import forgeframework.deadlock.VictimPolicy;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Styles;
import forgeos.ui.ToggleSwitch;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 교착 상태 관리자 화면.
 *
 * <h2>화면 구성의 근거</h2>
 * <p>왼쪽은 <b>숫자</b>(Allocation / Max / Need), 오른쪽은 <b>관계</b>(누가 누구를
 * 기다리는가)다. 은행원 알고리즘은 행렬을 보는 알고리즘이고 교착 탐지는 그래프를
 * 보는 알고리즘이라, 둘을 나란히 두면 같은 상황을 두 방식으로 동시에 볼 수 있다.</p>
 *
 * <h2>토글은 커널의 값을 되비춘다</h2>
 * <p>스위치를 UI 상태로 들고 있으면 터미널에서 {@code banker off}를 쳤을 때
 * 화면과 커널이 어긋난다. 스위치는 명령을 보내기만 하고, 표시는 항상 커널이
 * 돌려준 {@link BankerConfigDto}를 따른다.</p>
 */
final class DeadlockResolverView extends BorderPane {

    private final KernelService kernelService;
    private final KernelService.Subscription subscription;

    private final ToggleSwitch bankerSwitch = new ToggleSwitch();
    private final ComboBox<VictimPolicy> policyBox = new ComboBox<>();
    private final Label availableLabel = new Label();
    private final Label statusLabel = new Label("대기 중");

    private final ObservableList<ResourceRowDto> rows = FXCollections.observableArrayList();
    private final TableView<ResourceRowDto> table = new TableView<>(rows);
    private final WaitForGraphView graph = new WaitForGraphView();

    private final TextField pidField = new TextField();
    private final TextField r1Field = new TextField();
    private final TextField r2Field = new TextField();
    private final TextField r3Field = new TextField();

    /** 스위치·콤보를 코드로 되돌릴 때 리스너가 다시 커널을 부르지 않도록 막는 빗장. */
    private boolean syncingSwitch;
    private boolean syncingPolicy;

    DeadlockResolverView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("deadlock-resolver");

        buildTable();

        setTop(buildToolbar());
        setLeft(buildLeftPanel());
        setCenter(graph);
        setBottom(buildRequestBar());

        // 창이 최소화돼 있으면 그래프를 다시 계산할 이유가 없다.
        this.subscription = kernelService.onRefresh(this, this::refresh);
        refresh();
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private HBox buildToolbar() {
        Label bankerCaption = new Label("은행원 알고리즘 (회피)");
        bankerCaption.getStyleClass().add("toolbar-caption");

        bankerSwitch.selectedProperty().addListener((obs, was, now) -> {
            if (syncingSwitch) {
                return;
            }
            SystemCallResult result = kernelService.call(
                    SystemCallType.BANKER, now ? "on" : "off");
            report(result, now ? "회피를 켰습니다." : "회피를 껐습니다.");
            refresh();
        });

        policyBox.getItems().setAll(VictimPolicy.values());
        policyBox.setConverter(new PolicyConverter());
        policyBox.getStyleClass().add("policy-box");
        policyBox.setOnAction(e -> {
            VictimPolicy policy = policyBox.getValue();
            if (policy == null || syncingPolicy) {
                return;
            }
            SystemCallResult result = kernelService.call(
                    SystemCallType.BANKER, "policy", policy.name().toLowerCase());
            report(result, "희생자 선정 정책을 바꿨습니다.");
        });

        Button detect = new Button("탐지");
        detect.getStyleClass().add("toolbar-button");
        detect.setOnAction(e -> runDetect());

        Button recover = new Button("복구");
        recover.getStyleClass().add("toolbar-button");
        recover.pseudoClassStateChanged(Styles.DANGER, true);
        recover.setOnAction(e -> runRecover());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel.getStyleClass().add("toolbar-status");

        HBox bar = new HBox(bankerSwitch, bankerCaption,
                new Label("희생자"), policyBox, detect, recover, spacer, statusLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTable() {
        TableColumn<ResourceRowDto, String> process = new TableColumn<>("프로세스");
        process.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                "P%d %s".formatted(cell.getValue().pid(), cell.getValue().name())));
        process.setPrefWidth(120);

        table.getColumns().add(process);
        table.getColumns().add(vectorColumn("Allocation", ResourceRowDto::allocation));
        table.getColumns().add(vectorColumn("Max", ResourceRowDto::max));
        table.getColumns().add(vectorColumn("Need", ResourceRowDto::need));

        TableColumn<ResourceRowDto, String> pending = new TableColumn<>("대기 요청");
        pending.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().blocked() ? format(cell.getValue().pendingRequest()) : "—"));
        pending.setPrefWidth(96);

        table.getColumns().add(pending);
        table.getStyleClass().add("resource-table");
        table.setPlaceholder(new Label("등록된 프로세스가 없습니다.\n터미널에서 exec 로 프로세스를 만들어 보세요."));
    }

    private TableColumn<ResourceRowDto, String> vectorColumn(
            String title, java.util.function.Function<ResourceRowDto, List<Integer>> extractor) {
        TableColumn<ResourceRowDto, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(format(extractor.apply(cell.getValue()))));
        column.getStyleClass().add("column-numeric");
        column.setPrefWidth(92);
        return column;
    }

    private VBox buildLeftPanel() {
        availableLabel.getStyleClass().add("available-chip");
        availableLabel.setWrapText(true);

        VBox panel = new VBox(availableLabel, table);
        panel.getStyleClass().add("deadlock-left");
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    private HBox buildRequestBar() {
        pidField.setPromptText("PID");
        pidField.getStyleClass().addAll("field", "field-narrow");
        r1Field.setPromptText("R1");
        r1Field.getStyleClass().addAll("field", "field-narrow");
        r2Field.setPromptText("R2");
        r2Field.getStyleClass().addAll("field", "field-narrow");
        r3Field.setPromptText("R3");
        r3Field.getStyleClass().addAll("field", "field-narrow");

        Button max = new Button("최대 선언");
        max.getStyleClass().add("toolbar-button");
        max.setOnAction(e -> sendVector(SystemCallType.RES_MAX, "최대 요구량을 선언했습니다."));

        Button request = new Button("요청");
        request.getStyleClass().add("toolbar-button");
        request.setOnAction(e -> sendVector(SystemCallType.RES_REQ, "자원을 요청했습니다."));

        Button release = new Button("반납");
        release.getStyleClass().add("toolbar-button");
        release.setOnAction(e -> sendVector(SystemCallType.RES_FREE, "자원을 반납했습니다."));

        Label hint = new Label("은행원 알고리즘은 Max 선언이 있어야 의미가 있습니다. 최대 선언 → 요청 순서로 시도해 보세요.");
        hint.getStyleClass().add("request-hint");

        HBox bar = new HBox(new Label("PID"), pidField,
                new Label("벡터"), r1Field, r2Field, r3Field,
                max, request, release, hint);
        bar.getStyleClass().addAll("toolbar", "request-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 동작 ──────────────────────────────

    private void sendVector(SystemCallType type, String successMessage) {
        String pid = pidField.getText().trim();
        if (pid.isEmpty()) {
            statusLabel.setText("PID를 입력하세요.");
            return;
        }
        List<String> args = new ArrayList<>();
        args.add(pid);
        args.add(valueOrZero(r1Field));
        args.add(valueOrZero(r2Field));
        args.add(valueOrZero(r3Field));

        SystemCallResult result = kernelService.call(type, args.toArray(new String[0]));
        report(result, successMessage);
        refresh();
    }

    private void runDetect() {
        SystemCallResult result = kernelService.call(SystemCallType.DETECT);
        if (!result.isSuccess()) {
            report(result, "");
            return;
        }
        DeadlockDetectDto dto = result.dataAs(DeadlockDetectDto.class);
        statusLabel.setText(dto.hasDeadlock()
                ? "교착 발견 — 얽힌 프로세스 " + dto.deadlockedPids()
                : "교착 없음 (검사한 프로세스 %d개)".formatted(dto.inspected()));
        applyDetection(dto);
    }

    private void runRecover() {
        SystemCallResult result = kernelService.call(SystemCallType.RECOVER);
        if (!result.isSuccess()) {
            report(result, "");
            return;
        }
        DeadlockRecoverDto dto = result.dataAs(DeadlockRecoverDto.class);
        if (dto.victims().isEmpty()) {
            statusLabel.setText("복구할 교착이 없습니다.");
        } else {
            statusLabel.setText("희생 %s · 회수 %s · 깨어남 %s".formatted(
                    dto.victims(), dto.reclaimed(), dto.wokenPids()));
        }
        refresh();
    }

    private void refresh() {
        SystemCallResult info = kernelService.callCached(SystemCallType.RES_INFO);
        if (!info.isSuccess()) {
            return;
        }
        ResourceSnapshotDto snapshot = info.dataAs(ResourceSnapshotDto.class);
        // 내용이 그대로면 표를 갈아 끼우지 않는다 — setAll 은 셀 전부를 다시 만든다.
        if (!rows.equals(snapshot.rows())) {
            rows.setAll(snapshot.rows());
        }

        availableLabel.setText("가용 %s / 전체 %s  ·  자원 %s".formatted(
                format(snapshot.available()),
                format(snapshot.total()),
                String.join(" ", snapshot.labels())));

        syncingSwitch = true;
        bankerSwitch.syncSilently(snapshot.bankerEnabled());
        syncingSwitch = false;

        if (policyBox.getValue() != snapshot.victimPolicy()) {
            syncingPolicy = true;
            policyBox.setValue(snapshot.victimPolicy());
            syncingPolicy = false;
        }

        SystemCallResult detect = kernelService.callCached(SystemCallType.DETECT);
        if (detect.isSuccess()) {
            applyDetection(detect.dataAs(DeadlockDetectDto.class));
        }
    }

    private void applyDetection(DeadlockDetectDto dto) {
        Set<Integer> deadlocked = new HashSet<>(dto.deadlockedPids());
        graph.update(rows, dto.waitForEdges(), deadlocked);
    }

    private void report(SystemCallResult result, String successMessage) {
        statusLabel.setText(result.isSuccess()
                ? (result.getMessage() == null || result.getMessage().isBlank()
                        ? successMessage : result.getMessage())
                : result.getMessage());
    }

    private static String valueOrZero(TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? "0" : text;
    }

    private static String format(List<Integer> vector) {
        return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(" ")) + "]";
    }

    void dispose() {
        subscription.cancel();
        graph.dispose();
    }

    /** 정책 enum 을 사람이 읽는 말로 바꾼다. {@code LOWEST_ALLOCATION} 은 설명이 아니다. */
    private static final class PolicyConverter extends StringConverter<VictimPolicy> {
        @Override
        public String toString(VictimPolicy policy) {
            if (policy == null) {
                return "";
            }
            return switch (policy) {
                case LOWEST_ALLOCATION -> "적게 가진 쪽";
                case HIGHEST_ALLOCATION -> "많이 가진 쪽";
                case YOUNGEST_FIRST -> "가장 나중에 만들어진 쪽";
            };
        }

        @Override
        public VictimPolicy fromString(String text) {
            return VictimPolicy.parse(text);
        }
    }
}
