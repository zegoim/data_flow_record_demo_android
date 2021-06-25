package im.zego.recorder.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.Utils;

import im.zego.recorder.demo.BaseActivity;
import im.zego.recorder.demo.R;
import im.zego.recorder.demo.helper.Constant;
import im.zego.recordplayer.manager.view.ZegoVideoPlayer;

public class MainActivity extends BaseActivity {

    public static void start(Context context, String info) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("info", info);
        context.startActivity(intent);
    }

    private ZegoVideoPlayer mVideoPlayer;

    private final Utils.OnAppStatusChangedListener appStatusChangedListener = new Utils.OnAppStatusChangedListener() {
        @Override
        public void onForeground(Activity activity) {

        }

        @Override
        public void onBackground(Activity activity) {
            if (mVideoPlayer != null) {
                mVideoPlayer.pause();
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mVideoPlayer = findViewById(R.id.video_player);
    }

    @Override
    protected void initData() {
        super.initData();
        AppUtils.registerAppStatusChangedListener(appStatusChangedListener);
        String info = getIntent().getStringExtra("info");
        Log.d(Constant.TAG, "playInfo = " + info);

        // 通过taskId获取到录制回放meta文件内容，传给回放SDK进行预加载准备工作
        mVideoPlayer.prepare(info);
        mVideoPlayer.getTitleTextView().setText(R.string.app_name);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mVideoPlayer.getBackButton().setOnClickListener((view) -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppUtils.unregisterAppStatusChangedListener(appStatusChangedListener);
        // 释放回放SDK的资源
        if (mVideoPlayer != null) {
            mVideoPlayer.unInit();
        }
    }
}