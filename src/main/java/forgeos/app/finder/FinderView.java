package forgeos.app.finder;

import forgeframework.filesystem.DirectoryEntryDto;
import forgeframework.filesystem.FileContentDto;
import forgeframework.filesystem.FileListDto;
import forgeframework.filesystem.SyncResultDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Motion;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finder 화면 — 다단 컬럼 뷰.
 *
 * <h2>컬럼은 곧 경로다</h2>
 * <p>{@link #columnPaths}의 <i>i</i>번째 원소가 <i>i</i>번째 컬럼이 보여 주는
 * 디렉터리의 절대경로다. 디렉터리를 고르면 그 뒤 컬럼을 전부 잘라내고 새
 * 컬럼을 붙인다. 화면 상태와 경로 상태를 따로 관리하면 반드시 어긋나므로
 * 하나만 두고 화면은 거기서 파생시킨다.</p>
 */
final class FinderView extends BorderPane {

    /** 컬럼 하나의 폭(px). */
    private static final double COLUMN_WIDTH = 232;

    /** 루트 경로. */
    private static final String ROOT = "/";

    private final KernelService kernelService;

    private final HBox columns = new HBox();
    private final ScrollPane columnScroller = new ScrollPane(columns);
    private final List<String> columnPaths = new ArrayList<>();
    private final Label pathLabel = new Label(ROOT);
    private final VBox preview = new VBox();

    FinderView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("finder");

        columns.getStyleClass().add("finder-columns");
        columnScroller.getStyleClass().add("finder-scroll");
        columnScroller.setFitToHeight(true);
        columnScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        preview.getStyleClass().add("finder-preview");
        preview.setVisible(false);
        preview.setManaged(false);

        setTop(buildToolbar());
        setCenter(columnScroller);
        setRight(preview);

        pushColumn(ROOT);
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private HBox buildToolbar() {
        pathLabel.getStyleClass().add("finder-path");

        Button newFolder = new Button("새 폴더", Glyphs.stroked(Glyphs.FOLDER, 14, "button-glyph"));
        newFolder.getStyleClass().add("toolbar-button");
        newFolder.setOnAction(e -> create(SystemCallType.MKDIR, "새 폴더", "폴더 이름"));

        Button newFile = new Button("새 파일", Glyphs.stroked(Glyphs.FILE, 14, "button-glyph"));
        newFile.getStyleClass().add("toolbar-button");
        newFile.setOnAction(e -> create(SystemCallType.TOUCH, "새 파일", "파일 이름"));

        Button refresh = new Button("새로고침", Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));
        refresh.getStyleClass().add("toolbar-button");
        refresh.setOnAction(e -> reloadLastColumn());

        // 1.1.0 의 disk.img. 여기까지 눌러야 파일이 재부팅을 견딘다는 사실을
        // 명령어 없이 알 수 있는 자리다.
        Button sync = new Button("디스크에 기록", Glyphs.stroked(Glyphs.DISK, 14, "button-glyph"));
        sync.getStyleClass().add("toolbar-button");
        sync.setOnAction(e -> syncDisk());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(pathLabel, spacer, newFolder, newFile, refresh, sync);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 컬럼 관리 ──────────────────────────────

    private void pushColumn(String path) {
        FileListDto listing = list(path);
        if (listing == null) {
            return;
        }

        ListView<DirectoryEntryDto> column = new ListView<>();
        column.getStyleClass().add("finder-column");
        column.setPrefWidth(COLUMN_WIDTH);
        column.setMinWidth(COLUMN_WIDTH);
        column.setCellFactory(view -> new EntryCell());
        column.getItems().setAll(listing.entries());
        column.setPlaceholder(new Label("비어 있음"));

        int index = columnPaths.size();
        column.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                onEntrySelected(index, path, entry);
            }
        });

        columnPaths.add(path);
        columns.getChildren().add(column);
        pathLabel.setText(path);

        Motion.fadeIn(column, Duration.millis(160), null);
        // 새 컬럼이 화면 밖에 생기면 사용자가 스크롤을 찾아야 한다. 따라가 준다.
        columnScroller.setHvalue(1);
    }

    private void onEntrySelected(int columnIndex, String parentPath, DirectoryEntryDto entry) {
        truncateColumnsAfter(columnIndex);

        String childPath = join(parentPath, entry.name());
        if ("DIRECTORY".equals(entry.type())) {
            hidePreview();
            pushColumn(childPath);
            return;
        }
        pathLabel.setText(childPath);
        showPreview(parentPath, entry);
    }

    private void truncateColumnsAfter(int columnIndex) {
        while (columnPaths.size() > columnIndex + 1) {
            columnPaths.remove(columnPaths.size() - 1);
            columns.getChildren().remove(columns.getChildren().size() - 1);
        }
    }

    private void reloadLastColumn() {
        if (columnPaths.isEmpty()) {
            return;
        }
        String path = columnPaths.get(columnPaths.size() - 1);
        truncateColumnsAfter(columnPaths.size() - 2);
        pushColumn(path);
    }

    // ────────────────────────────── 미리보기 ──────────────────────────────

    private void showPreview(String parentPath, DirectoryEntryDto entry) {
        preview.getChildren().clear();

        Node icon = Glyphs.stroked(Glyphs.FILE, 52, "preview-icon");
        Label name = new Label(entry.name());
        name.getStyleClass().add("preview-name");
        Label meta = new Label("%s · %d바이트".formatted(entry.type(), entry.size()));
        meta.getStyleClass().add("preview-meta");

        Label body = new Label(readContent(parentPath, entry.name()));
        body.getStyleClass().add("preview-body");
        body.setWrapText(true);

        preview.getChildren().addAll(icon, name, meta, body);
        preview.setVisible(true);
        preview.setManaged(true);
        Motion.fadeIn(preview, Duration.millis(180), null);
    }

    private void hidePreview() {
        preview.setVisible(false);
        preview.setManaged(false);
    }

    private String readContent(String parentPath, String name) {
        SystemCallResult result = kernelService.call(SystemCallType.CAT, parentPath, name);
        if (!result.isSuccess()) {
            return result.getMessage();
        }
        FileContentDto dto = result.dataAs(FileContentDto.class);
        return dto.content().isEmpty() ? "(빈 파일)" : dto.content();
    }

    // ────────────────────────────── 생성 ──────────────────────────────

    private void create(SystemCallType type, String title, String prompt) {
        String parent = columnPaths.isEmpty() ? ROOT : columnPaths.get(columnPaths.size() - 1);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(parent + " 안에 만들 이름");
        dialog.setContentText(prompt);

        Optional<String> name = dialog.showAndWait();
        if (name.isEmpty() || name.get().isBlank()) {
            return;
        }
        SystemCallResult result = kernelService.call(type, parent, name.get().trim());
        if (result.isSuccess()) {
            reloadLastColumn();
        } else {
            pathLabel.setText(result.getMessage());
        }
    }

    // ────────────────────────────── 영속화 ──────────────────────────────

    /**
     * 파일 시스템을 {@code disk.img} 에 내려쓴다.
     *
     * <p>커널을 {@code --disk} 없이 띄웠으면 이미지가 없다. 그 경우를 실패로
     * 보고하지 않는 이유는, 이미지가 없는 것이 오류가 아니라 기본 설정이기
     * 때문이다({@code diskImagePath} 기본값 null). 대신 무엇을 해야 영속되는지를 말해 준다.</p>
     */
    private void syncDisk() {
        SystemCallResult result = kernelService.call(SystemCallType.SYNC);
        if (!result.isSuccess()) {
            pathLabel.setText(result.getMessage());
            return;
        }
        SyncResultDto sync = result.dataAs(SyncResultDto.class);
        pathLabel.setText(sync.persisted()
                ? "%s · %d바이트 · 블록 %d/%d · inode %d/%d".formatted(
                        sync.imagePath(), sync.bytesWritten(),
                        sync.usedBlocks(), sync.totalBlocks(),
                        sync.usedInodes(), sync.totalInodes())
                : "디스크 이미지가 없어 메모리에만 남습니다 (커널을 --disk 로 띄우면 영속됩니다)");
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private FileListDto list(String path) {
        SystemCallResult result = kernelService.call(SystemCallType.LS, path, ".");
        if (!result.isSuccess()) {
            pathLabel.setText(result.getMessage());
            return null;
        }
        return result.dataAs(FileListDto.class);
    }

    private static String join(String parent, String name) {
        return ROOT.equals(parent) ? ROOT + name : parent + "/" + name;
    }

    void dispose() {
        // Finder 는 주기 갱신을 구독하지 않는다. 파일 시스템은 사용자가 바꿀 때만 바뀐다.
    }

    /** 아이콘 + 이름 + (디렉터리면) 꺾쇠로 구성된 항목 셀. */
    private static final class EntryCell extends ListCell<DirectoryEntryDto> {
        @Override
        protected void updateItem(DirectoryEntryDto entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean directory = "DIRECTORY".equals(entry.type());

            Label name = new Label(entry.name());
            name.getStyleClass().add("entry-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(
                    Glyphs.stroked(directory ? Glyphs.FOLDER : Glyphs.FILE, 16, "entry-icon"),
                    name,
                    spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("entry-row");

            if (directory) {
                row.getChildren().add(Glyphs.stroked(Glyphs.CHEVRON_RIGHT, 14, "entry-chevron"));
            } else {
                Label size = new Label(entry.size() + "B");
                size.getStyleClass().add("entry-size");
                row.getChildren().add(size);
            }

            setGraphic(row);
            setText(null);
        }
    }
}
