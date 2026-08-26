package forgeos.boot;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 부팅 2단계 — MP4 부팅 애니메이션.
 *
 * <h2>jar 안의 미디어 문제</h2>
 * <p>JavaFX {@link Media}는 {@code jar:} URL을 재생하지 못한다. IDE에서 돌릴 때는
 * 리소스가 디렉터리(build/resources/main)에 풀려 있어 잘 되다가, 배포용 jar로
 * 묶는 순간 조용히 실패한다. 그래서 리소스 URL의 프로토콜이 {@code file}이
 * 아니면 임시 파일로 한 번 복사해서 재생한다. 배포 시점에 발견하는 것보다
 * 지금 다섯 줄 더 쓰는 편이 싸다.</p>
 *
 * <h2>1.1.1 — 미리 준비하고, 앞부분만 쓴다</h2>
 * <p>두 가지가 달라졌다.</p>
 * <ul>
 *   <li><b>준비를 앞당긴다.</b> 1.1.0 은 콘솔이 다 찍힌 <i>뒤에야</i> 2.4MB 짜리
 *       리소스를 임시 파일로 복사하고 디코더를 세웠다. 그 일이 FX 스레드에서
 *       벌어졌기 때문에 화면이 한 박자 굳었다가 영상이 시작됐다. 이제
 *       {@link #prepare}가 부팅과 <b>동시에</b> 백그라운드에서 그 일을 끝낸다 —
 *       콘솔이 로그를 찍는 동안 어차피 비어 있던 시간이다.</li>
 *   <li><b>끝까지 틀지 않는다.</b> 영상은 10초짜리인데, 부팅 연출에서 그것은
 *       "분위기"가 아니라 "기다림"이다. {@code stopTime}으로 앞부분만 쓰고
 *       넘어간다 — 재생 종료 이벤트는 그 지점에서도 똑같이 온다.</li>
 * </ul>
 *
 * <h2>실패해도 부팅은 계속된다</h2>
 * <p>코덱이 없거나 파일이 빠져 있으면 그냥 다음 단계로 넘어간다. 부팅
 * 애니메이션 때문에 운영체제가 못 뜨는 것은 말이 안 된다.</p>
 */
final class BootVideo extends StackPane {

    /**
     * 실제로 재생할 길이.
     *
     * <p>원본은 10초다. 로고가 다 여물고 화면이 밝아지는 데까지가 앞 3.6초이므로,
     * 그 뒤는 이미 본 것을 다시 보는 시간이다. 부팅 연출에서 가장 비싼 자원은
     * 사용자의 인내심이다.</p>
     */
    private static final Duration PLAY_LENGTH = Duration.seconds(3.6);

    private final MediaView view = new MediaView();

    /** 백그라운드에서 미리 세워 둔 재생기. 준비 전이면 {@code null}. */
    private volatile MediaPlayer prepared;

    /** 준비가 끝나기 전에 {@link #dispose}가 불렸는지. 늦게 도착한 재생기를 바로 버리기 위한 표식. */
    private volatile boolean disposed;

    private MediaPlayer player;

    BootVideo() {
        getStyleClass().add("boot-video");
        view.setPreserveRatio(false);
        view.setSmooth(true);
        getChildren().add(view);

        // 영상은 화면을 꽉 채운다. MediaView 는 부모 크기를 따라가지 않으므로 직접 묶는다.
        widthProperty().addListener((ObservableValue<? extends Number> obs, Number old, Number now) ->
                view.setFitWidth(now.doubleValue()));
        heightProperty().addListener((ObservableValue<? extends Number> obs, Number old, Number now) ->
                view.setFitHeight(now.doubleValue()));
    }

    /**
     * 재생 준비를 백그라운드에서 미리 해 둔다. 즉시 반환한다.
     *
     * <p>커널이 부팅하고 콘솔이 로그를 찍는 동안 함께 돌리라고 만든 메서드다.
     * 실패해도 조용히 넘어간다 — {@link #play}가 그때 다시 시도한다.</p>
     *
     * @param resourcePath 클래스패스 리소스 경로
     */
    void prepare(String resourcePath) {
        Thread loader = new Thread(() -> {
            MediaPlayer built = build(resourcePath);
            if (built == null) {
                return;
            }
            if (disposed) {
                built.dispose();
                return;
            }
            prepared = built;
        }, "forgeos-boot-video");
        loader.setDaemon(true);
        loader.start();
    }

    /**
     * 애니메이션을 재생한다.
     *
     * @param resourcePath 클래스패스 리소스 경로
     * @param onFinished   재생이 끝났거나 재생할 수 없을 때 호출될 콜백 (항상 정확히 한 번)
     */
    void play(String resourcePath, Runnable onFinished) {
        // 준비가 끝났으면 그것을 쓴다. 아직이면(아주 빠른 기계) 여기서 직접 세운다.
        MediaPlayer ready = prepared;
        prepared = null;
        player = ready != null ? ready : build(resourcePath);

        if (player == null) {
            onFinished.run();
            return;
        }

        OneShot guard = new OneShot(onFinished);
        // stopTime 에 닿아도 재생 종료로 취급된다. 두 경로 모두 막아 둔다.
        player.setOnEndOfMedia(guard::fire);
        player.setOnError(guard::fire);
        player.setOnHalted(guard::fire);
        player.setOnStopped(guard::fire);

        view.setMediaPlayer(player);
        player.play();
    }

    /** 재생을 중단하고 자원을 정리한다. 건너뛰기와 창 종료에서 호출한다. */
    void dispose() {
        disposed = true;
        release(player);
        player = null;
        release(prepared);
        prepared = null;
    }

    /**
     * 재생기를 세운다. FX 스레드가 아니어도 된다 —
     * {@link MediaView}에 붙이는 것만 FX 스레드의 몫이다.
     */
    private MediaPlayer build(String resourcePath) {
        String source = resolveSource(resourcePath);
        if (source == null) {
            return null;
        }
        try {
            MediaPlayer built = new MediaPlayer(new Media(source));
            built.setStopTime(PLAY_LENGTH);
            return built;
        } catch (RuntimeException e) {
            // 코덱 미지원 등. 부팅을 막을 이유가 없다.
            return null;
        }
    }

    private static void release(MediaPlayer target) {
        if (target == null) {
            return;
        }
        target.setOnEndOfMedia(null);
        target.setOnError(null);
        target.setOnHalted(null);
        target.setOnStopped(null);
        // 정리도 FX 스레드에서 해야 안전하다. 준비 스레드가 늦게 도착한 경우를 위해 감싼다.
        if (Platform.isFxApplicationThread()) {
            target.stop();
            target.dispose();
        } else {
            Platform.runLater(() -> {
                target.stop();
                target.dispose();
            });
        }
    }

    private String resolveSource(String resourcePath) {
        URL url = BootVideo.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        if ("file".equals(url.getProtocol())) {
            return url.toExternalForm();
        }
        return extractToTempFile(resourcePath);
    }

    private String extractToTempFile(String resourcePath) {
        try (InputStream in = BootVideo.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
            Path temp = Files.createTempFile("forgeos-boot", suffix);
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toUri().toString();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** 콜백이 두 번 불리지 않도록 막는 래퍼. onError 와 onHalted 가 겹쳐 오는 경우가 있다. */
    private static final class OneShot {
        private final Runnable action;
        private boolean fired;

        OneShot(Runnable action) {
            this.action = action;
        }

        void fire() {
            if (fired) {
                return;
            }
            fired = true;
            action.run();
        }
    }
}
