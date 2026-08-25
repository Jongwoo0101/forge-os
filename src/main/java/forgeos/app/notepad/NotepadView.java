package forgeos.app.notepad;

import forgeframework.filesystem.DirectoryEntryDto;
import forgeframework.filesystem.FileContentDto;
import forgeframework.filesystem.FileListDto;
import forgeframework.filesystem.SyncResultDto;
import forgeframework.filesystem.WriteResultDto;
import forgeframework.syscall.SystemCallResult;
import forgeframework.syscall.SystemCallType;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import forgeos.ui.Glyphs;
import forgeos.ui.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 메모장 화면.
 *
 * <h2>대화상자를 하나도 쓰지 않는다</h2>
 * <p>"이름을 입력하세요", "저장하지 않고 닫을까요?" 같은 질문을 {@code Dialog}로
 * 띄우면 그때마다 진짜 네이티브 창이 하나 열린다. ForgeOS 는 "데스크탑 안의
 * 모든 것은 노드"라는 원칙 위에 서 있고, 실제로 네이티브 창 해제는 종료 시
 * 크래시의 원인으로 지목된 자리이기도 하다. 그래서 새 파일 이름은 툴바의
 * 입력칸으로 받고, 저장하지 않은 변경은 질문 대신 <b>초안(draft)</b>으로 들고 있는다.</p>
 *
 * <h2>초안은 잃어버리지 않는다</h2>
 * <p>{@link #drafts}가 경로별 편집 중인 내용을, {@link #saved}가 마지막으로
 * 디스크에서 읽거나 디스크로 쓴 내용을 들고 있다. 둘이 다르면 저장되지 않은
 * 변경이 있는 것이고, 목록에 점(•)이 붙는다. 파일을 옮겨 다녀도 초안은 그대로
 * 남으므로 "저장할까요?"를 물을 이유 자체가 없어진다.</p>
 */
final class NotepadView extends BorderPane {

    /** 루트 경로. */
    private static final String ROOT = "/";

    private final KernelService kernelService;

    private final ListView<DirectoryEntryDto> fileList = new ListView<>();
    private final TextArea editor = new TextArea();
    private final TextField newNameField = new TextField();

    private final Label directoryLabel = new Label(ROOT);
    private final Label openFileLabel = new Label("열린 파일 없음");
    private final Label countLabel = new Label();
    private final Label statusLabel = new Label();

    private final Button saveButton = new Button("저장", Glyphs.stroked(Glyphs.SAVE, 14, "button-glyph"));
    private final Button revertButton =
            new Button("되돌리기", Glyphs.stroked(Glyphs.REFRESH, 14, "button-glyph"));

    /** 편집 중인 내용. 키는 절대경로. 창을 닫기 전까지 살아 있다. */
    private final Map<String, String> drafts = new HashMap<>();

    /** 마지막으로 디스크와 일치했던 내용. {@link #drafts}와 다르면 저장되지 않은 변경이다. */
    private final Map<String, String> saved = new HashMap<>();

    private String directory = ROOT;
    private String openFile;

    /** 편집기에 프로그램이 값을 넣는 중인지. 리스너가 그것을 사용자 입력으로 오해하지 않도록. */
    private boolean loading;

    NotepadView(AppContext context) {
        this.kernelService = context.kernel();
        getStyleClass().add("notepad");

        setLeft(buildSidebar());
        setCenter(buildEditorPane());

        editor.textProperty().addListener((obs, old, text) -> onEdited(text));
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        reloadDirectory();
        updateEditorState();
        status("커널 파일 시스템의 " + ROOT + " 를 열었습니다. 저장한 파일은 Finder 와 터미널에서도 보입니다.");
    }

    // ────────────────────────────── 구성 ──────────────────────────────

    private VBox buildSidebar() {
        directoryLabel.getStyleClass().add("notepad-path");

        Button up = new Button("상위 폴더", Glyphs.stroked(Glyphs.ARROW_LEFT, 13, "button-glyph"));
        up.getStyleClass().add("toolbar-button");
        up.setOnAction(e -> goUp());

        Button refresh = new Button(null, Glyphs.stroked(Glyphs.REFRESH, 13, "button-glyph"));
        refresh.getStyleClass().add("toolbar-button");
        refresh.setOnAction(e -> reloadDirectory());

        HBox header = new HBox(up, refresh);
        header.getStyleClass().add("notepad-sidebar-header");
        header.setAlignment(Pos.CENTER_LEFT);

        fileList.getStyleClass().add("notepad-file-list");
        fileList.setCellFactory(view -> new EntryCell());
        fileList.setPlaceholder(new Label("비어 있음"));
        VBox.setVgrow(fileList, Priority.ALWAYS);
        fileList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, entry) -> {
                    if (entry != null) {
                        onEntrySelected(entry);
                    }
                });

        VBox sidebar = new VBox(directoryLabel, header, fileList);
        sidebar.getStyleClass().add("notepad-sidebar");
        return sidebar;
    }

    private BorderPane buildEditorPane() {
        editor.getStyleClass().add("notepad-editor");
        editor.setWrapText(true);
        editor.setPromptText("왼쪽에서 파일을 고르거나, 위에서 새 파일을 만드세요.");

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("notepad-main");
        pane.setTop(buildToolbar());
        pane.setCenter(editor);
        pane.setBottom(buildStatusBar());
        return pane;
    }

    private HBox buildToolbar() {
        newNameField.setPromptText("새 파일 이름");
        newNameField.getStyleClass().add("field");
        newNameField.setOnAction(e -> createFile());

        Button create = new Button("만들기", Glyphs.stroked(Glyphs.PLUS, 14, "button-glyph"));
        create.getStyleClass().add("toolbar-button");
        create.setOnAction(e -> createFile());

        saveButton.getStyleClass().add("toolbar-button");
        saveButton.setOnAction(e -> save());

        revertButton.getStyleClass().add("toolbar-button");
        revertButton.setOnAction(e -> revert());

        Button sync = new Button("디스크에 기록", Glyphs.stroked(Glyphs.DISK, 14, "button-glyph"));
        sync.getStyleClass().add("toolbar-button");
        sync.setOnAction(e -> syncDisk());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        openFileLabel.getStyleClass().add("notepad-open-file");

        HBox bar = new HBox(newNameField, create, saveButton, revertButton, sync, spacer, openFileLabel);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox buildStatusBar() {
        countLabel.getStyleClass().add("notepad-count");
        statusLabel.getStyleClass().add("notepad-status");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(statusLabel, spacer, countLabel);
        bar.getStyleClass().add("notepad-status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ────────────────────────────── 탐색 ──────────────────────────────

    private void reloadDirectory() {
        SystemCallResult result = kernelService.call(SystemCallType.LS, directory, ".");
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        FileListDto listing = result.dataAs(FileListDto.class);
        directoryLabel.setText(directory);
        fileList.getItems().setAll(listing.entries());
    }

    private void goUp() {
        if (ROOT.equals(directory)) {
            return;
        }
        int cut = directory.lastIndexOf('/');
        directory = (cut <= 0) ? ROOT : directory.substring(0, cut);
        reloadDirectory();
    }

    private void onEntrySelected(DirectoryEntryDto entry) {
        if ("DIRECTORY".equals(entry.type())) {
            directory = join(directory, entry.name());
            reloadDirectory();
            return;
        }
        open(entry.name());
    }

    private void open(String name) {
        String path = join(directory, name);

        // 이 파일의 초안을 이미 들고 있으면 디스크를 다시 읽지 않는다.
        // 읽어 버리면 사용자가 방금 친 것이 조용히 사라진다.
        if (!drafts.containsKey(path)) {
            SystemCallResult result = kernelService.call(SystemCallType.CAT, directory, name);
            if (!result.isSuccess()) {
                status(result.getMessage());
                return;
            }
            String content = result.dataAs(FileContentDto.class).content();
            saved.put(path, content);
            drafts.put(path, content);
        }

        openFile = name;
        loading = true;
        editor.setText(drafts.get(path));
        loading = false;
        editor.positionCaret(editor.getText().length());
        updateEditorState();
        status(path + " 를 열었습니다.");
    }

    // ────────────────────────────── 편집 ──────────────────────────────

    private void onEdited(String text) {
        if (loading || openFile == null) {
            return;
        }
        drafts.put(currentPath(), text);
        updateEditorState();
        // 목록의 점(•)을 다시 그리게 한다. 항목 자체는 그대로이므로 셀만 새로 만든다.
        fileList.refresh();
    }

    private void createFile() {
        String name = newNameField.getText().trim();
        if (name.isEmpty()) {
            status("만들 파일 이름을 입력하세요.");
            return;
        }
        SystemCallResult result = kernelService.call(SystemCallType.TOUCH, directory, name);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        newNameField.clear();
        reloadDirectory();
        open(name);
        selectInList(name);
        editor.requestFocus();
    }

    private void save() {
        if (openFile == null) {
            return;
        }
        String path = currentPath();
        String text = editor.getText();

        SystemCallResult result = kernelService.call(SystemCallType.WRITE, directory, openFile, text);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        WriteResultDto written = result.dataAs(WriteResultDto.class);
        saved.put(path, text);
        drafts.put(path, text);

        reloadDirectory();
        selectInList(openFile);
        updateEditorState();
        status("%s 에 %d바이트를 저장했습니다. 재부팅까지 남기려면 '디스크에 기록'을 누르세요."
                .formatted(path, written.bytesWritten()));
    }

    private void revert() {
        if (openFile == null) {
            return;
        }
        String path = currentPath();
        drafts.remove(path);
        saved.remove(path);
        open(openFile);
        fileList.refresh();
        status(path + " 를 디스크의 내용으로 되돌렸습니다.");
    }

    private void syncDisk() {
        SystemCallResult result = kernelService.call(SystemCallType.SYNC);
        if (!result.isSuccess()) {
            status(result.getMessage());
            return;
        }
        SyncResultDto sync = result.dataAs(SyncResultDto.class);
        if (sync.persisted()) {
            status("%s 에 %d바이트를 기록했습니다. (블록 %d/%d · inode %d/%d)".formatted(
                    sync.imagePath(), sync.bytesWritten(),
                    sync.usedBlocks(), sync.totalBlocks(),
                    sync.usedInodes(), sync.totalInodes()));
        } else {
            status("디스크 이미지가 설정되어 있지 않아 메모리에만 남습니다. "
                    + "(--disk 옵션으로 커널을 띄우면 disk.img 에 영속됩니다)");
        }
    }

    // ────────────────────────────── 보조 ──────────────────────────────

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.S && event.isShortcutDown()) {
            save();
            event.consume();
        }
    }

    private void updateEditorState() {
        boolean opened = openFile != null;
        editor.setDisable(!opened);
        saveButton.setDisable(!opened || !isDirty(currentPath()));
        revertButton.setDisable(!opened);

        if (!opened) {
            openFileLabel.setText("열린 파일 없음");
            countLabel.setText("");
            return;
        }
        String path = currentPath();
        openFileLabel.setText(isDirty(path) ? path + " •" : path);
        countLabel.setText("%d자 · %d바이트".formatted(
                editor.getText().length(),
                editor.getText().getBytes(StandardCharsets.UTF_8).length));
    }

    private void selectInList(String name) {
        for (DirectoryEntryDto entry : fileList.getItems()) {
            if (entry.name().equals(name) && !"DIRECTORY".equals(entry.type())) {
                fileList.getSelectionModel().select(entry);
                return;
            }
        }
    }

    private boolean isDirty(String path) {
        if (path == null) {
            return false;
        }
        String draft = drafts.get(path);
        return draft != null && !draft.equals(saved.get(path));
    }

    private String currentPath() {
        return openFile == null ? null : join(directory, openFile);
    }

    private void status(String message) {
        statusLabel.setText(message);
    }

    private static String join(String parent, String name) {
        return ROOT.equals(parent) ? ROOT + name : parent + "/" + name;
    }

    void dispose() {
        // 메모장은 주기 갱신을 구독하지 않는다. 파일 시스템은 사용자가 바꿀 때만 바뀐다.
    }

    /** 아이콘 + 이름 + 저장되지 않은 변경 표시로 구성된 항목 셀. */
    private final class EntryCell extends ListCell<DirectoryEntryDto> {

        EntryCell() {
        }

        @Override
        protected void updateItem(DirectoryEntryDto entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            boolean isDirectory = "DIRECTORY".equals(entry.type());

            Label name = new Label(entry.name());
            name.getStyleClass().add("entry-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(
                    Glyphs.stroked(isDirectory ? Glyphs.FOLDER : Glyphs.NOTEPAD, 15, "entry-icon"),
                    name,
                    spacer);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("entry-row");

            if (isDirectory) {
                row.getChildren().add(Glyphs.stroked(Glyphs.CHEVRON_RIGHT, 13, "entry-chevron"));
            } else {
                Label mark = new Label(isDirty(join(directory, entry.name()))
                        ? "•"
                        : entry.size() + "B");
                mark.getStyleClass().add("entry-size");
                mark.pseudoClassStateChanged(Styles.ON, isDirty(join(directory, entry.name())));
                row.getChildren().add(mark);
            }

            setGraphic(row);
            setText(null);
        }
    }
}
