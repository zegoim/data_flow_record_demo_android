package im.zego.recordplayer.manager.view;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import im.zego.recordplayer.R;
import im.zego.recordplayer.manager.IVideoManager;
import im.zego.recordplayer.manager.VideoManager;

/**
 * 播放器下展示的控件，包括音量、加载中控件等等的控制展示逻辑
 */
public class ZegoVideoPlayer extends ZegoViewControlView {

    //音量dialog
    protected Dialog mVolumeDialog;

    //触摸进度dialog
    protected Dialog mProgressDialog;

    //加载中dialog
    protected Dialog mLoadingDialog;

    //音量进度条的progress
    protected ProgressBar mDialogVolumeProgressBar;

    //音量进度条旁边的icon
    protected ImageView mDialogVolumeIv;

    //触摸移动进度时，显示当前时间的文本
    protected TextView mDialogSeekTime;

    //触摸移动进度时，显示全部时间的文本
    protected TextView mDialogTotalTime;

    public ZegoVideoPlayer(Context context) {
        super(context);
    }

    public ZegoVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepare(String metaJsonInfo) {
        VideoManager.getInstance().init(mContext, metaJsonInfo);
        VideoManager.getInstance().setPlayerListener(this);
        clickStart();
    }

    public void pause() {
        VideoManager.getInstance().pause();
    }

    public void unInit() {
        VideoManager.getInstance().stop();
        VideoManager.getInstance().unInit();
    }

    @Override
    public IVideoManager getVideoManager() {
        return VideoManager.getInstance();
    }

    @Override
    protected void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(R.layout.video_progress_dialog, null);
            mDialogSeekTime = localView.findViewById(R.id.tv_current);
            mDialogTotalTime = localView.findViewById(R.id.tv_duration);
            mProgressDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mProgressDialog.setContentView(localView);
            mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mProgressDialog.getWindow().setLayout(getWidth(), getHeight());
            WindowManager.LayoutParams localLayoutParams = mProgressDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.CENTER;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int[] location = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
            mProgressDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
        if (mDialogSeekTime != null) {
            mDialogSeekTime.setText(seekTime);
        }
        if (mDialogTotalTime != null) {
            mDialogTotalTime.setText(" / " + totalTime);
        }
    }

    @Override
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void showVolumeDialog(float deltaY, int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(R.layout.video_volume_dialog, null);
            mDialogVolumeProgressBar = localView.findViewById(R.id.volume_progressbar);
            mDialogVolumeIv = localView.findViewById(R.id.volume_iv);
            mVolumeDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            mVolumeDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            mVolumeDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mVolumeDialog.getWindow().setLayout(getWidth(), getHeight());
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.CENTER;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int[] location = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
            mVolumeDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (mDialogVolumeProgressBar != null) {
            mDialogVolumeProgressBar.setProgress(volumePercent);
        }
        if (volumePercent > 0) {
            mDialogVolumeIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_music_on));
        } else {
            mDialogVolumeIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_music_off));
        }
    }

    @Override
    protected void dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
            mVolumeDialog = null;
        }
    }

    @Override
    protected void showLoadingDialog() {
        post(() -> {
            if (mLoadingDialog == null) {
                View localView = LayoutInflater.from(getActivityContext()).inflate(R.layout.video_loading_dialog, null);
                mLoadingDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
                mLoadingDialog.setContentView(localView);
                mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                mLoadingDialog.getWindow().setLayout(getWidth(), getHeight());
                WindowManager.LayoutParams localLayoutParams = mLoadingDialog.getWindow().getAttributes();
                localLayoutParams.gravity = Gravity.CENTER;
                localLayoutParams.width = getWidth();
                localLayoutParams.height = getHeight();
                int[] location = new int[2];
                getLocationOnScreen(location);
                localLayoutParams.x = location[0];
                localLayoutParams.y = location[1];
                mLoadingDialog.getWindow().setAttributes(localLayoutParams);
            }
            if (!mLoadingDialog.isShowing()) {
                mLoadingDialog.show();
            }
        });
    }

    @Override
    protected void dismissLoadingDialog() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
