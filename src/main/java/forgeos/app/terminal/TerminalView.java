package forgeos.app.terminal;

import forgeframework.cli.command.Command;
import forgeframework.cli.command.CommandRegistry;
import forgeframework.cli.command.StandardCommands;
import forgeframework.cli.shell.ShellContext;
import forgeframework.cli.shell.ShellPrompt;
import forgeframework.kernel.Kernel;
import forgeframework.syscall.SystemCallResult;
import forgeos.app.AppContext;
import forgeos.core.KernelService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * 터미널 화면.
 *
 * <h2>출력 한도</h2>
 * <p>{@code tree}나 {@code help}를 반복 호출하면 {@link TextFlow}의 자식 노드가
 * 수만 개로 불어나고, 그때부터 스크롤이 눈에 띄게 버벅인다. TextFlow는 자식
 * 개수에 선형으로 느려지므로 상한을 두고 오래된 줄부터 버린다.</p>
 *
 * <h2>왜 TextArea 가 아닌가</h2>
 * <p>실패한 명령을 붉게, 프롬프트를 청록으로 칠하려면 조각마다 다른 스타일이
 * 필요하다. {@code TextArea}는 통짜 텍스트라 그게 안 된다.</p>
 */
final class TerminalView extends BorderPane {

    /** 화면에 유지하는 최대 텍스트 조각 수. */
    private static final int MAX_SEGMENTS = 4000;

    /** 되짚을 수 있는 명령 기록 수. */
    private static final int MAX_HISTORY = 200;

    private final KernelService kernelService;
    private final CommandRegistry registry;
    private final ShellContext shellContext = new ShellContext();
    private final ShellPrompt shellPrompt = new ShellPrompt(shellContext);

    private final TextFlow output = new TextFlow();
    private final ScrollPane scroller = new ScrollPane(output);
    private final TextField input = new TextField();
    private final Label promptLabel = new Label();

    private final List<String> history = new ArrayList<>();
    private int historyCursor;

    TerminalView(AppContext context) {
        this.kernelService = context.kernel();
        this.registry = StandardCommands.createRegistry(shellContext);

        getStyleClass().add("terminal");

        output.getStyleClass().add("terminal-output");
        scroller.getStyleClass().add("terminal-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        promptLabel.getStyleClass().add("terminal-prompt");
        input.getStyleClass().add("terminal-input");
        HBox.setHgrow(input, Priority.ALWAYS);

        HBox inputRow = new HBox(promptLabel, input);
        inputRow.getStyleClass().add("terminal-input-row");

        setCenter(scroller);
        setBottom(inputRow);

        input.setOnAction(e -> submit());
        input.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        // 창이 열리면 곧바로 입력을 받을 수 있어야 한다. 한 번 더 클릭하게 만들지 않는다.
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                Platform.runLater(input::requestFocus);
            }
        });

        printWelcome();
        refreshPrompt();
    }

    private void printWelcome() {
        append("ForgeFramework Shell에 오신 것을 환영합니다. 'help'를 입력해보세요.\n", "terminal-notice");
        append("↑/↓ 로 이전 명령, Tab 으로 명령어 자동 완성, clear 로 화면 정리.\n\n", "terminal-dim");
    }

    private void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> {
                recallHistory(-1);
                event.consume();
            }
            case DOWN -> {
                recallHistory(1);
                event.consume();
            }
            case TAB -> {
                complete();
                event.consume();
            }
            case L -> {
                if (event.isShortcutDown()) {
                    clear();
                    event.consume();
                }
            }
            default -> {
                // 나머지는 TextField 기본 동작.
            }
        }
    }

    private void submit() {
        String line = input.getText().trim();
        input.clear();
        if (line.isEmpty()) {
            return;
        }

        remember(line);
        append(shellPrompt.render(), "terminal-prompt-echo");
        append(line + "\n", "terminal-echo");

        if ("clear".equals(line)) {
            clear();
            return;
        }

        Kernel kernel = kernelService.kernel();
        if (kernel == null) {
            append("커널이 아직 준비되지 않았습니다.\n", "terminal-error");
            return;
        }

        SystemCallResult result = registry.dispatch(kernel, line);
        String message = result.getMessage();
        if (message != null && !message.isBlank()) {
            append(message + "\n", result.isSuccess() ? "terminal-result" : "terminal-error");
        }

        // shutdown 은 이 경로로도 실행될 수 있다. 데스크탑이 그 사실을 알아야 한다.
        kernelService.syncRunningState();
        refreshPrompt();
        scrollToBottom();
    }

    private void remember(String line) {
        if (history.isEmpty() || !history.get(history.size() - 1).equals(line)) {
            history.add(line);
            if (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
        historyCursor = history.size();
    }

    private void recallHistory(int direction) {
        if (history.isEmpty()) {
            return;
        }
        int next = historyCursor + direction;
        if (next < 0) {
            next = 0;
        }
        if (next >= history.size()) {
            historyCursor = history.size();
            input.clear();
            return;
        }
        historyCursor = next;
        input.setText(history.get(next));
        input.positionCaret(input.getText().length());
    }

    private void complete() {
        String typed = input.getText();
        if (typed.isBlank() || typed.contains(" ")) {
            return;
        }
        List<String> matches = registry.getAll().stream()
                .map(Command::name)
                .filter(name -> name.startsWith(typed))
                .toList();

        if (matches.size() == 1) {
            input.setText(matches.get(0) + " ");
            input.positionCaret(input.getText().length());
            return;
        }
        if (matches.size() > 1) {
            append(String.join("  ", matches) + "\n", "terminal-dim");
            scrollToBottom();
        }
    }

    private void clear() {
        output.getChildren().clear();
    }

    private void refreshPrompt() {
        promptLabel.setText(shellPrompt.render());
    }

    private void append(String text, String styleClass) {
        Text segment = new Text(text);
        segment.getStyleClass().add(styleClass);
        output.getChildren().add(segment);

        if (output.getChildren().size() > MAX_SEGMENTS) {
            output.getChildren().remove(0, output.getChildren().size() - MAX_SEGMENTS);
        }
    }

    private void scrollToBottom() {
        // 레이아웃이 끝난 뒤라야 vvalue 가 새 높이를 기준으로 계산된다.
        Platform.runLater(() -> scroller.setVvalue(1));
    }

    void dispose() {
        // 터미널은 주기 갱신을 구독하지 않는다. 정리할 것이 없다는 사실을
        // 명시적으로 남겨 둔다 — 나중에 구독을 추가하면 여기에 해지를 넣어야 한다.
    }
}
