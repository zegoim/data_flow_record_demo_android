package im.zego.recorder.demo;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    //加载中dialog
    protected Dialog mLoadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        initView();
        initData();
        setListener();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void initView();

    protected void initData() {

    }

    protected void setListener() {
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            View localView = LayoutInflater.from(this).inflate(im.zego.recordplayer.R.layout.video_loading_dialog, null);
            mLoadingDialog = new Dialog(this, im.zego.recordplayer.R.style.video_style_dialog_progress);
            mLoadingDialog.setContentView(localView);
            mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            mLoadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mLoadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    protected void dismissLoadingDialog() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
