package forgeos.boot;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 부팅 1단계 — 커널 로그가 한 글자씩 찍히는 터미널 화면.
 *
 * <h2>왜 큐인가</h2>
 * <p>줄이 들어오는 속도(커널 부팅 단계마다 한 줄)와 찍히는 속도(글자당 몇 ms)는
 * 서로 무관하다. 로그가 몰아서 들어오면 화면이 순간이동하고, 반대로 타이핑이
 * 빠르면 다음 줄을 기다리며 멈춘다. 그래서 유입과 출력을 큐로 분리하고,
 * 출력은 항상 일정한 리듬으로만 흐르게 한다. 부팅이 끝나도 큐가 빌 때까지는
 * 다음 단계로 넘어가지 않는다({@link #whenDrained}).</p>
 */
final class BootConsole extends StackPane {

    /** 글자 하나가 찍히는 간격(초). 너무 빠르면 로그가 아니라 깜빡임으로 보인다. */
    private static final double CHAR_INTERVAL = 0.0065;

    /** 줄이 끝난 뒤의 숨. 사람이 한 줄을 읽었다고 느끼는 최소 간격이다. */
    private static final double LINE_PAUSE = 0.055;

    private final VBox lines = new VBox();
    private final ScrollPane scroller = new ScrollPane();
    private final Deque<String> pending = new ArrayDeque<>();
    private final Label cursor = new Label("█");

    private Label currentLine;
    private String currentText = "";
    private int typedChars;
    private double accumulator;
    private long lastNanos;
    private boolean typing;
    private boolean drainRequested;
    private Runnable onDrained;

    private final AnimationTimer typewriter = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastNanos == 0) {
                lastNanos = now;
                return;
            }
            accumulator += Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
            lastNanos = now;
            pump();
        }
    };

    BootConsole() {
        getStyleClass().add("boot-console");

        lines.getStyleClass().add("boot-console-lines");

        HBox cursorRow = new HBox(cursor);
        cursorRow.getStyleClass().add("boot-console-cursor-row");

        VBox column = new VBox(lines, cursorRow);
        column.getStyleClass().add("boot-console-column");
        VBox.setVgrow(lines, Priority.NEVER);

        scroller.setContent(column);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("boot-console-scroll");
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        cursor.getStyleClass().add("boot-console-cursor");
        blink();

        setAlignment(Pos.TOP_LEFT);
        getChildren().add(scroller);
    }

    /** 커서가 살아 있다는 신호. 정확히 이것 하나 때문에 화면이 정지 화면으로 안 보인다. */
    private void blink() {
        FadeTransition fade = new FadeTransition(Duration.millis(560), cursor);
        fade.setFromValue(1);
        fade.setToValue(0.05);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    /**
     * 찍을 줄을 큐에 넣는다. 이미 타이핑 중이면 뒤에 붙는다.
     *
     * @param line 출력할 한 줄
     */
    void println(String line) {
        pending.add(line);
        startTyping();
    }

    private void startTyping() {
        if (typing) {
            return;
        }
        typing = true;
        lastNanos = 0;
        typewriter.start();
    }

    private void stopTyping() {
        typing = false;
        typewriter.stop();
    }

    /**
     * 큐가 모두 소진된 시점에 한 번 실행할 작업을 건다.
     *
     * @param action 소진 콜백
     */
    void whenDrained(Runnable action) {
        this.onDrained = action;
        this.drainRequested = true;
        if (isDrained()) {
            fireDrained();
        }
    }

    /** 남은 줄을 전부 즉시 출력하고 타이핑을 끝낸다. (건너뛰기) */
    void flush() {
        commitCurrentLine();
        while (!pending.isEmpty()) {
            appendLine(pending.poll());
        }
        stopTyping();
        scroller.setVvalue(1);
        if (drainRequested) {
            fireDrained();
        }
    }

    private void pump() {
        while (accumulator >= CHAR_INTERVAL) {
            if (currentLine == null) {
                if (pending.isEmpty()) {
                    accumulator = 0;
                    if (drainRequested) {
                        fireDrained();
                    }
                    return;
                }
                currentText = pending.poll();
                typedChars = 0;
                currentLine = newLine("");
            }

            if (typedChars >= currentText.length()) {
                accumulator -= LINE_PAUSE;
                commitCurrentLine();
                continue;
            }

            typedChars++;
            currentLine.setText(currentText.substring(0, typedChars));
            accumulator -= CHAR_INTERVAL;
        }
        scroller.setVvalue(1);
    }

    private void commitCurrentLine() {
        if (currentLine != null) {
            currentLine.setText(currentText);
            currentLine = null;
            currentText = "";
            typedChars = 0;
        }
    }

    private void appendLine(String text) {
        newLine(text);
    }

    private Label newLine(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("boot-console-line");
        lines.getChildren().add(label);
        return label;
    }

    private boolean isDrained() {
        return pending.isEmpty() && currentLine == null;
    }

    private void fireDrained() {
        if (onDrained == null) {
            return;
        }
        Runnable action = onDrained;
        onDrained = null;
        drainRequested = false;
        stopTyping();
        action.run();
    }
}
