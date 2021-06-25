package im.zego.recordplayer.manager.view;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import im.zego.recordplayer.R;
import im.zego.recordplayer.manager.IPlayerListener;
import im.zego.recordplayer.manager.IVideoManager;
import im.zego.recordplayer.manager.utils.CommonUtil;


/**
 * Created by rocket_wang on 4/19/21.
 */
public abstract class ZegoVideoView extends FrameLayout implements IPlayerListener {

    //正常
    public static final int CURRENT_STATE_NORMAL = 0;
    //准备中
    public static final int CURRENT_STATE_PREPAREING = 1;
    //播放中
    public static final int CURRENT_STATE_PLAYING = 2;
    //开始缓冲
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    //暂停
    public static final int CURRENT_STATE_PAUSE = 5;
    //自动播放结束
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    //错误状态
    public static final int CURRENT_STATE_ERROR = 7;

    //当前的播放状态
    protected int mCurrentState = CURRENT_STATE_NORMAL;

    //上下文
    protected Context mContext;

    //屏幕宽度
    protected int mScreenWidth;

    //屏幕高度
    protected int mScreenHeight;

    //渲染控件父类
    protected ViewGroup mViewContainer;

    //监听各种手势的View
    protected View mTouchView;

    //音频焦点的监听
    protected AudioManager mAudioManager;

    public ZegoVideoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZegoVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZegoVideoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {

        if (getActivityContext() != null) {
            this.mContext = getActivityContext();
        } else {
            this.mContext = context;
        }

        initInflate(mContext);

        mTouchView = findViewById(R.id.touch_view);

        mViewContainer = findViewById(R.id.view_container);
        ZegoMediaViewHolder viewHolder = new ZegoMediaViewHolder(mContext);
        mViewContainer.addView(viewHolder, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));

        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

    }

    protected void initInflate(Context context) {
        View.inflate(context, getLayoutId(), this);
    }

    protected Context getActivityContext() {
        return CommonUtil.getActivityContext(getContext());
    }

    @Override
    public void onPrepared() {
        startAfterPrepared();
    }

    /**
     * 在资源准备好之后，开始播放
     */
    protected void startAfterPrepared() {
        try {
            setStateAndUi(CURRENT_STATE_PLAYING);
            if (getVideoManager() != null) {
                getVideoManager().start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion() {
        setStateAndUi(CURRENT_STATE_AUTO_COMPLETE);
    }

    @Override
    public void onSeeking() {
        setStateAndUi(CURRENT_STATE_PLAYING_BUFFERING_START);
    }

    @Override
    public void onSeekComplete() {
        setStateAndUi(CURRENT_STATE_PLAYING);
    }

    @Override
    public void onError(int errorCode) {

    }

    public void onVideoPause() {
        setStateAndUi(CURRENT_STATE_PAUSE);
    }

    public void onVideoResume() {
        setStateAndUi(CURRENT_STATE_PLAYING);
    }

    public abstract int getLayoutId();

    protected abstract void setStateAndUi(int state);

    public abstract IVideoManager getVideoManager();

    /**
     * 获取当前播放进度
     */
    public int getCurrentPositionWhenPlaying() {
        return (int) getVideoManager().getCurrentPosition();
    }

    /**
     * 获取当前总时长
     */
    public int getDuration() {
        return (int) getVideoManager().getDuration();
    }
}
