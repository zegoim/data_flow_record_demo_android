package im.zego.recordplayer.manager;

/**
 * 控制播放器状态切换的接口
 */
public interface IVideoManager {

    void prepare();

    void start();

    void stop();

    void pause();

    void resume();

    boolean isPlaying();

    void seekTo(long time);

    long getCurrentPosition();

    long getDuration();

    default int getMaxWidth() {
        return 0;
    }

    default int getMaxHeight() {
        return 0;
    }
}
