package im.zego.recordplayer.manager;


import com.zego.recorderplayback.ZegoPlaybackStreamInfo;

/**
 * 来自回放SDK回调的转发listener
 */
public interface IMediaViewListener {

    void onLoaded();

    void onWhiteboardSwitch(long whiteboardId);

    void onVideoViewCreate(ZegoPlaybackStreamInfo info);

    void onVideoViewDelete(ZegoPlaybackStreamInfo info);

    void onLayoutChange(ZegoPlaybackStreamInfo[] infoList);
}
