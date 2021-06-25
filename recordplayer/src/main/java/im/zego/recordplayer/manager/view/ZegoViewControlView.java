package im.zego.recordplayer.manager.view;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import im.zego.commonlibs.utils.Logger;
import im.zego.recordplayer.R;
import im.zego.recordplayer.manager.utils.CommonUtil;


/**
 * 播放UI的显示、逻辑控制、手势处理等等
 */
public abstract class ZegoViewControlView extends ZegoVideoView
        implements View.OnClickListener, View.OnTouchListener, SeekBar.OnSeekBarChangeListener {

    //手势的阈值，滑动大于这个值才会做对应的操作（改变音量/进度）
    private static final int THRESHOLD = 80;

    //触摸显示后隐藏的时间
    private static final long DISMISS_CONTROL_TIME = 3 * 1000L;
    private static final String TAG = "ZegoViewControlView";

    //手指放下的位置
    protected int mDownPosition;

    //手势调节音量的大小
    protected int mGestureDownVolume;

    //手动改变滑动的位置
    protected int mSeekTimePosition;

    //手动滑动的起始偏移位置
    protected int mSeekEndOffset;

    //触摸的X
    protected float mDownX;

    //触摸的Y
    protected float mDownY;

    //移动的Y
    protected float mMoveY;

    //是否改变音量
    protected boolean mChangeVolume = false;

    //是否改变播放进度
    protected boolean mChangePosition = false;

    //触摸滑动进度的比例系数
    protected float mSeekRatio = 1;

    //触摸的是否进度条
    protected boolean mTouchingProgressBar = false;

    //是否首次触摸
    protected boolean mFirstTouch = false;

    //seek touch
    protected boolean mHadSeekTouch = false;

    protected boolean mPostProgress = false;
    protected boolean mPostDismiss = false;

    //播放按键
    protected ImageView mStartButton;

    //进度条
    protected SeekBar mProgressBar;

    //进度条时间显示
    protected TextView mCurrentTimeTextView, mTotalTimeTextView;

    //返回按键
    protected ImageView mBackButton;

    //title
    protected TextView mTitleTextView;

    //顶部 TopBar 和底部 BottomBar 区域，播放中动态展示和隐藏
    protected ViewGroup mTopContainer, mBottomContainer;

    public ZegoViewControlView(@NonNull Context context) {
        super(context);
    }

    public ZegoViewControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ZegoViewControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_control_layout;
    }

    protected void init(Context context) {
        super.init(context);

        mStartButton = findViewById(R.id.start);
        mTitleTextView = findViewById(R.id.title);
        mBackButton = findViewById(R.id.back);
        mProgressBar = findViewById(R.id.progress);
        mCurrentTimeTextView = findViewById(R.id.current);
        mTotalTimeTextView = findViewById(R.id.total);
        mBottomContainer = findViewById(R.id.layout_bottom);
        mTopContainer = findViewById(R.id.layout_top);

        if (isInEditMode())
            return;

        if (mStartButton != null) {
            mStartButton.setOnClickListener(this);
        }

        if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(this);
        }

        if (mBottomContainer != null) {
            mBottomContainer.setOnClickListener(this);
        }

        if (mTouchView != null) {
            mTouchView.setOnClickListener(this);
            mTouchView.setOnTouchListener(this);
        }

        if (mBackButton != null)
            mBackButton.setOnClickListener(this);

        if (getActivityContext() != null) {
            mSeekEndOffset = CommonUtil.dip2px(getActivityContext(), 50);
        }
    }

    @Override
    protected void setStateAndUi(int state) {
        mCurrentState = state;

        switch (mCurrentState) {
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                mProgressBar.setEnabled(false);
                cancelProgressTimer();
                break;
            case CURRENT_STATE_NORMAL:
            case CURRENT_STATE_PAUSE:
            case CURRENT_STATE_ERROR:
                mProgressBar.setEnabled(true);
                cancelProgressTimer();
                break;
            case CURRENT_STATE_PREPAREING:
                mProgressBar.setEnabled(false);
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
                mProgressBar.setEnabled(true);
                startProgressTimer();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                mProgressBar.setEnabled(false);
                cancelProgressTimer();
                if (mProgressBar != null) {
                    mProgressBar.setProgress(100);
                }
                if (mCurrentTimeTextView != null && mTotalTimeTextView != null) {
                    mCurrentTimeTextView.setText(mTotalTimeTextView.getText());
                }
                break;
        }
        resolveUIState(state);
    }

    /**
     * 处理控制显示
     *
     * @param state
     */
    protected void resolveUIState(int state) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_PREPAREING:
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPreparingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            clickStart();
        } else if (i == R.id.touch_view) {
            startDismissControlViewTimer();
        }
    }

    /**
     * 双击
     */
    protected GestureDetector gestureDetector = new GestureDetector(getContext().getApplicationContext(),
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                        clickStart();
                    }
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!mChangePosition && !mChangeVolume) {
                        onClickUiToggle(e);
                    }
                    return true;
                }

            });

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        float x = event.getX();
        float y = event.getY();

        if (id == R.id.touch_view) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    touchSurfaceDown(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (!mChangePosition && !mChangeVolume) {
                        //判断当前滑动是想要改变音量还是改变视频进度
                        touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
                    }
                    touchSurfaceMove(deltaX, deltaY, y);

                    break;
                case MotionEvent.ACTION_UP:

                    startDismissControlViewTimer();

                    touchSurfaceUp();

                    startProgressTimer();

                    break;
            }
            gestureDetector.onTouchEvent(event);
        }

        return true;
    }

    protected void touchSurfaceDown(float x, float y) {
        mTouchingProgressBar = true;
        mDownX = x;
        mDownY = y;
        mMoveY = 0;
        mChangeVolume = false;
        mChangePosition = false;
        mFirstTouch = true;
    }

    /**
     * 判断当前滑动是想要改变音量还是改变视频进度
     *
     * @param absDeltaX
     * @param absDeltaY
     */
    protected void touchSurfaceMoveFullLogic(float absDeltaX, float absDeltaY) {
        if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
            cancelProgressTimer();
            if (absDeltaX >= THRESHOLD) {
                //防止全屏虚拟按键
                int screenWidth = CommonUtil.getScreenWidth(getContext());
                if (Math.abs(screenWidth - mDownX) > mSeekEndOffset) {
                    mChangePosition = true;
                    mDownPosition = getCurrentPositionWhenPlaying();
                }
            } else {
                int screenHeight = CommonUtil.getScreenHeight(getContext());
                boolean noEnd = Math.abs(screenHeight - mDownY) > mSeekEndOffset;
                if (mFirstTouch) {
                    mFirstTouch = false;
                }
                mChangeVolume = noEnd;
                mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        }
    }

    protected void touchSurfaceMove(float deltaX, float deltaY, float y) {
        int curWidth = 0;
        int curHeight = 0;
        if (getActivityContext() != null) {
            curWidth = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenHeight : mScreenWidth;
            curHeight = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenWidth : mScreenHeight;
        }
        if (mChangePosition) {
            showAllWidget();
            int totalTimeDuration = getDuration();
            mSeekTimePosition = (int) (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio);
            if (mSeekTimePosition > totalTimeDuration)
                mSeekTimePosition = totalTimeDuration;
            String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
            String totalTime = CommonUtil.stringForTime(totalTimeDuration);
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        } else if (mChangeVolume) {
            deltaY = -deltaY;
            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int deltaV = (int) (max * deltaY * 3 / curHeight);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / curHeight);

            showVolumeDialog(-deltaY, volumePercent);
        }
    }

    protected void touchSurfaceUp() {
        mTouchingProgressBar = false;
        dismissProgressDialog();
        dismissVolumeDialog();
        if (mChangePosition && getVideoManager() != null && (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE)) {
            try {
                seekTo(mSeekTimePosition);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int duration = getDuration();
            int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
        }
    }

    protected void seekTo(long time) {
        if (mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START
                || mCurrentState == CURRENT_STATE_PREPAREING) {
            Logger.i(TAG, "seekTo need filter - is = " + mCurrentState);
            return;
        }
        getVideoManager().seekTo(time);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        showDragProgressTextOnSeekBar(fromUser, progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHadSeekTouch = true;
        cancelDismissControlViewTimer();
        cancelProgressTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startDismissControlViewTimer();
        startProgressTimer();

        if (getVideoManager() != null && (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE)) {
            try {
                int time = seekBar.getProgress() * getDuration() / 100;
                seekTo(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mHadSeekTouch = false;
    }

    protected void showDragProgressTextOnSeekBar(boolean fromUser, int progress) {
        if (fromUser) {
            int duration = getDuration();
            if (mCurrentTimeTextView != null)
                mCurrentTimeTextView.setText(CommonUtil.stringForTime(progress * duration / 100));
        }
    }

    /**
     * 播放按键点击
     */
    protected void clickStart() {
        if (mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START
                || mCurrentState == CURRENT_STATE_PREPAREING) {
            Logger.i(TAG, "clickStart need filter - is = " + mCurrentState);
            return;
        }
        if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR) {
            startButtonLogic();
        } else if (mCurrentState == CURRENT_STATE_PLAYING) {
            getVideoManager().pause();
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            getVideoManager().resume();
        } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            setStateAndUi(CURRENT_STATE_PLAYING);
            getVideoManager().start();
        }
    }

    private void startButtonLogic() {
        prepareVideo();
    }

    /**
     * 视频播放前的一些准备工作
     * 注册监听，设置状态等等
     */
    private void prepareVideo() {
        setStateAndUi(CURRENT_STATE_PREPAREING);
        if (getVideoManager() != null) {
            getVideoManager().prepare();
        }
    }

    protected void startProgressTimer() {
        cancelProgressTimer();
        mPostProgress = true;
        postDelayed(progressTask, 300);
    }

    protected void cancelProgressTimer() {
        mPostProgress = false;
        removeCallbacks(progressTask);
    }

    protected void setTextAndProgress(boolean forceChange) {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        setProgressAndTime(progress, position, duration, forceChange);
    }

    protected void setProgressAndTime(int progress, int currentTime, int totalTime, boolean forceChange) {

        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        if (mHadSeekTouch) {
            return;
        }
        if (!mTouchingProgressBar) {
            if (progress != 0 || forceChange) {
                mProgressBar.setProgress(progress);
            }
        }
        mTotalTimeTextView.setText(CommonUtil.stringForTime(totalTime));
        if (currentTime > 0) {
            mCurrentTimeTextView.setText(CommonUtil.stringForTime(currentTime));
        }
    }

    protected void resetProgressAndTime() {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        mProgressBar.setProgress(0);
        mCurrentTimeTextView.setText(CommonUtil.stringForTime(0));
        mTotalTimeTextView.setText(CommonUtil.stringForTime(0));
    }

    protected void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        mPostDismiss = true;
//        postDelayed(dismissControlTask, DISMISS_CONTROL_TIME);
    }

    protected void cancelDismissControlViewTimer() {
        mPostDismiss = false;
        removeCallbacks(dismissControlTask);
    }

    Runnable progressTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE) {
                setTextAndProgress(false);
            }
            if (mPostProgress) {
                postDelayed(this, 1000);
            }
        }
    };

    Runnable dismissControlTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState != CURRENT_STATE_NORMAL
                    && mCurrentState != CURRENT_STATE_ERROR
                    && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                if (getActivityContext() != null) {
                    hideAllWidget();
                    CommonUtil.hideNavKey(mContext);
                }
                if (mPostDismiss) {
                    postDelayed(this, DISMISS_CONTROL_TIME);
                }
            }
        }
    };

    protected void hideAllWidget() {
        CommonUtil.setViewState(mBottomContainer, INVISIBLE);
        CommonUtil.setViewState(mTopContainer, INVISIBLE);
    }

    protected void showAllWidget() {
        CommonUtil.setViewState(mBottomContainer, VISIBLE);
        CommonUtil.setViewState(mTopContainer, VISIBLE);
    }

    protected void changeUiToNormal() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
        }
        dismissLoadingDialog();
    }

    protected void changeUiToPreparingShow() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
        }
        showLoadingDialog();
    }

    protected void changeUiToPlayingShow() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_pause));
        }
        dismissLoadingDialog();
    }

    protected void changeUiToPauseShow() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
        }
        dismissLoadingDialog();
    }

    protected void changeUiToError() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
        }
        dismissLoadingDialog();
    }

    protected void changeUiToCompleteShow() {
        if (mStartButton != null) {
            mStartButton.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
        }
        dismissLoadingDialog();
    }

    protected void onClickUiToggle(MotionEvent e) {
        if (mBottomContainer != null) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                hideAllWidget();
            } else {
                showAllWidget();
            }
        }
    }

    public ImageView getBackButton() {
        return mBackButton;
    }

    public TextView getTitleTextView() {
        return mTitleTextView;
    }

    protected abstract void showProgressDialog(float deltaX,
                                               String seekTime, int seekTimePosition,
                                               String totalTime, int totalTimeDuration);

    protected abstract void dismissProgressDialog();

    protected abstract void showVolumeDialog(float deltaY, int volumePercent);

    protected abstract void dismissVolumeDialog();

    protected abstract void showLoadingDialog();

    protected abstract void dismissLoadingDialog();
}
