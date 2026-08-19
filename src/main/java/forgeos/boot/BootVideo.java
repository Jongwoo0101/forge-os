package forgeos.boot;

import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

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
 * <h2>실패해도 부팅은 계속된다</h2>
 * <p>코덱이 없거나 파일이 빠져 있으면 그냥 다음 단계로 넘어간다. 부팅
 * 애니메이션 때문에 운영체제가 못 뜨는 것은 말이 안 된다.</p>
 */
final class BootVideo extends StackPane {

    private final MediaView view = new MediaView();
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
     * 애니메이션을 재생한다.
     *
     * @param resourcePath 클래스패스 리소스 경로
     * @param onFinished   재생이 끝났거나 재생할 수 없을 때 호출될 콜백 (항상 정확히 한 번)
     */
    void play(String resourcePath, Runnable onFinished) {
        String source = resolveSource(resourcePath);
        if (source == null) {
            onFinished.run();
            return;
        }

        try {
            player = new MediaPlayer(new Media(source));
        } catch (RuntimeException e) {
            // 코덱 미지원 등. 부팅을 막을 이유가 없다.
            onFinished.run();
            return;
        }

        OneShot guard = new OneShot(onFinished);
        player.setOnEndOfMedia(guard::fire);
        player.setOnError(guard::fire);
        player.setOnHalted(guard::fire);
        player.setOnStopped(guard::fire);

        view.setMediaPlayer(player);
        player.play();
    }

    /** 재생을 중단하고 자원을 정리한다. 건너뛰기와 창 종료에서 호출한다. */
    void dispose() {
        if (player != null) {
            player.setOnEndOfMedia(null);
            player.setOnError(null);
            player.setOnHalted(null);
            player.setOnStopped(null);
            player.stop();
            player.dispose();
            player = null;
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
