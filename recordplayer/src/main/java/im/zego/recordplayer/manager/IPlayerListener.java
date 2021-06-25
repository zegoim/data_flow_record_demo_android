package im.zego.recordplayer.manager;

/**
 * 播放器状态改变listener
 */
public interface IPlayerListener {
    void onPrepared();

    void onCompletion();

    void onSeeking();

    void onSeekComplete();

    void onVideoPause();

    void onVideoResume();

    void onError(int errorCode);
}
