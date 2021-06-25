package im.zego.recordplayer.manager;

import android.content.Context;
import android.util.Log;

import com.zego.recorderplayback.IZegoPlaybackPlayerEventCallback;
import com.zego.recorderplayback.ZegoPlaybackState;
import com.zego.recorderplayback.ZegoPlaybackStreamInfo;
import com.zego.recorderplayback.ZegoRecorderPlayback;

import java.io.File;

import im.zego.commonlibs.constants.AppConstants;
import im.zego.commonlibs.utils.ToastUtils;
import im.zego.recordplayer.manager.utils.CommonUtil;


/**
 * 播放器管理类，包括：
 * 1. 注册了回放SDK相关的回调监听，并分发给对应的对象
 * 2. 实现并维护播放器相关状态改变的逻辑
 */
public class VideoManager implements IVideoManager {
    private static final VideoManager ourInstance = new VideoManager();

    public static VideoManager getInstance() {
        return ourInstance;
    }

    private VideoManager() {
    }

    private static final String TAG = "VideoManager";
    private static final String CACHE_FOLDER = "cache";

    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    private ZegoRecorderPlayback recorderPlayback = new ZegoRecorderPlayback();

    private IMediaViewListener mediaViewListener;
    private IPlayerListener playerListener;

    private Context mContext;

    private boolean isPlay;
    private long mDuration = 0L;
    private long mCurrentTime = 0L;
    private String metaJsonInfo = "";

    public void init(Context context, String metaJsonInfo) {
        this.metaJsonInfo = metaJsonInfo;
        mContext = context;
        /** 初始化SDK */
        recorderPlayback.init();
        /** 设置SDK日志输出路径，以下代码示例为输出到 sdcard/Android/data/您的应用package名称/files/zegologs 路径下 */
        recorderPlayback.setLogFolderPath(mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator +
                AppConstants.LOG_SUBFOLDER);

        /** 监听播放器事件，包含文件白板流信息，音视频流信息 */
        recorderPlayback.setPlaybackPlayerEventCallback(new IZegoPlaybackPlayerEventCallback() {

            /**
             * 监听播放器播放状态改变的通知
             * @param state 状态码 (未播放 = 0; 播放中 = 1; 暂停播放 = 2; 播放结束 = 3;)
             */
            @Override
            public void onPlayStateUpdate(int state) {
                Log.i(TAG, "onPlayStateUpdate: state=" + state);
                switch (state) {
                    case ZegoPlaybackState.kZegoPlaybackStatePausing:
                        playerListener.onVideoPause();
                        break;
                    case ZegoPlaybackState.kZegoPlaybackStatePlaying:
                        isPlay = true;
                        playerListener.onVideoResume();
                        break;
                    case ZegoPlaybackState.kZegoPlaybackStatePlayEnded:
                    case ZegoPlaybackState.kZegoPlaybackStateNoPlay:
                        isPlay = false;
                        playerListener.onCompletion();
                        break;
                    default:
                        isPlay = false;
                        break;
                }
            }

            /**
             * 播放异常
             * @param errorCode 错误码
             */
            @Override
            public void onPlayError(int errorCode) {
                Log.i(TAG, "onPlayError: errorCode=" + errorCode);
                playerListener.onError(errorCode);
            }

            /**
             * 快进到指定时刻
             * @param timestamp 时间戳，单位毫秒
             */
            @Override
            public void onSeekComplete(int state, long timestamp) {
                Log.i(TAG, String.format("onSeekComplete: state=%s, timestamp=%s", state, timestamp));
                mCurrentTime = timestamp;
                playerListener.onSeekComplete();
            }

            /**
             * 播放进度通知
             * @param timestamp 时间戳，单位毫秒
             */
            @Override
            public void onPlayingProgress(long timestamp) {
                Log.i(TAG, "onPlayingProgress: timestamp=" + timestamp);
                mCurrentTime = timestamp;
            }

            /**
             * 加载完成回调
             * @param error 0 -> 代表成功
             */
            @Override
            public void onLoadComplete(int error, long duration) {
                Log.i(TAG, String.format("onLoadComplete: error=%s, duration=%s", error, duration));
                mDuration = duration;
                mediaViewListener.onLoaded();
                playerListener.onPrepared();
            }

            /**
             * 收到白板流的切换通知
             * @param whiteboardId 白板ID
             */
            @Override
            public void onWhiteboardSwitch(long whiteboardId) {
                Log.i(TAG, "onWhiteboardSwitch: whiteboardId=" + whiteboardId);
                mediaViewListener.onWhiteboardSwitch(whiteboardId);
            }

            /**
             * 音视频流开始通知
             * @param info 流信息相关的对象model
             */
            @Override
            public void onMediaStreamStart(ZegoPlaybackStreamInfo info) {
                Log.i(TAG, "onMediaStreamStart: info=" + CommonUtil.printInfo(info));
                mediaViewListener.onVideoViewCreate(info);
            }

            /**
             * 音视频流结束通知
             * @param info 流信息相关的对象model
             */
            @Override
            public void onMediaStreamStop(ZegoPlaybackStreamInfo info) {
                Log.i(TAG, "onMediaStreamStop: info=" + CommonUtil.printInfo(info));
                mediaViewListener.onVideoViewDelete(info);
            }

            /**
             * 回放布局变化通知，返回每个流的布局
             * @param infoList 每个流的布局model
             */
            @Override
            public void onLayoutChange(ZegoPlaybackStreamInfo[] infoList) {
                Log.i(TAG, "onLayoutChange: infos=" + CommonUtil.printInfos(infoList));
                mediaViewListener.onLayoutChange(infoList);
            }

            /**
             * 下载缓存进度回调
             */
            @Override
            public void onDownloadCacheProgress(int errorCode, boolean isFinish, float progress) {
                Log.i(TAG, String.format("onDownloadCacheProgress: errorCode=%s, isFinish=%s, progress=%s", errorCode, isFinish, progress));
            }
        });

        /**
         * 监听自定义消息：包含用户录制过程中的自定义消息
         */
        recorderPlayback.setPlaybackCustomCommandCallback(cmd -> {
            String customCmd = CommonUtil.printCustomCommand(cmd);
            Log.i(TAG, "onCustomCmd: " + customCmd);
            ToastUtils.showCenterToast(customCmd);
        });
    }

    public void setMediaViewListener(IMediaViewListener mediaViewListener) {
        this.mediaViewListener = mediaViewListener;
    }

    public void setPlayerListener(IPlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    /**
     * 反初始化SDK
     */
    public void unInit() {
        isPlay = false;
        mDuration = 0L;
        mCurrentTime = 0L;
        metaJsonInfo = "";
        recorderPlayback.setPlaybackPlayerEventCallback(null);
        recorderPlayback.setPlaybackCustomCommandCallback(null);
        recorderPlayback.uninit();
    }

    @Override
    public void prepare() {
        /** 设置SDK缓存路径，以下代码示例为输出到 sdcard/Android/data/您的应用package名称/files/cache 路径下 */
        recorderPlayback.setCacheFolderPath(mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator + CACHE_FOLDER);
        /** 设置回放的相对宽高 */
        recorderPlayback.setDeviceSize(MAX_WIDTH, MAX_HEIGHT);
        /** 用拿到的json加载资源，比如视频流、白板流等 */
        recorderPlayback.load(metaJsonInfo);
    }

    /**
     * 开始播放
     */
    @Override
    public void start() {
        recorderPlayback.start();
    }

    /**
     * 停止播放
     */
    @Override
    public void stop() {
        recorderPlayback.stop();
    }

    /**
     * 暂停播放
     */
    @Override
    public void pause() {
        recorderPlayback.pause();
    }

    /**
     * 继续播放
     */
    @Override
    public void resume() {
        recorderPlayback.resume();
    }

    @Override
    public boolean isPlaying() {
        return isPlay;
    }

    /**
     * 跳转到指定时间戳播放，单位毫秒
     */
    @Override
    public void seekTo(long time) {
        //时间戳修正
        long correctionTime = time;
        if (time <= 0)
            correctionTime = 0;
        playerListener.onSeeking();
        Log.i(TAG, "seekTo: time=" + time + ", correctionTime=" + correctionTime);
        recorderPlayback.seekTo(correctionTime);
    }

    /**
     * 获取当前播放时间戳
     */
    @Override
    public long getCurrentPosition() {
        return mCurrentTime;
    }

    /**
     * 获取当前视频总时长
     */
    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getMaxHeight() {
        return MAX_HEIGHT;
    }
}
