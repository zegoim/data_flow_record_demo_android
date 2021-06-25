package im.zego.recorder.demo.ui;

import android.Manifest;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.NetworkUtils;
import com.permissionx.guolindev.PermissionX;

import im.zego.commonlibs.KeyCenter;
import im.zego.commonlibs.utils.ToastUtils;
import im.zego.recorder.demo.BaseActivity;
import im.zego.recorder.demo.R;
import im.zego.recorder.demo.api.PlayInfo;
import im.zego.recorder.demo.api.Result;
import im.zego.recorder.demo.api.TokenInfo;
import im.zego.recorder.demo.api.ZegoApiClient;
import im.zego.recorder.demo.helper.Constant;

public class LoginActivity extends BaseActivity {

    private Button mJoinBtn;
    private TextView mSettingsTv;
    private EditText mTaskIdEt;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected void initView() {
        mJoinBtn = findViewById(R.id.join_entrance_main);
        mSettingsTv = findViewById(R.id.setting_tv);
        mTaskIdEt = findViewById(R.id.edit_text);
    }

    @Override
    protected void initData() {
        super.initData();

        PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .request((allGranted, grantedList, deniedList) -> {
                    if (!allGranted) {
                        Toast.makeText(this, "These permissions are denied: " + deniedList, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void setListener() {
        super.setListener();

        mJoinBtn.setOnClickListener((v ->
                NetworkUtils.isAvailableAsync(isAvailable -> {
                    if (isAvailable) {
                        showLoadingDialog();
                        ZegoApiClient.getInstance().getToken(KeyCenter.APP_ID, KeyCenter.APP_SECRET, new ZegoApiClient.RequestCallback<TokenInfo>() {
                            @Override
                            public void onResult(Result result, TokenInfo tokenInfo) {
                                dismissLoadingDialog();
                                if (tokenInfo != null) {
                                    String taskId = mTaskIdEt.getText().toString();
                                    startPlay(taskId, tokenInfo.getTokenData().getAccessToken());
                                }
                            }
                        });
                    } else {
                        ToastUtils.showCenterToast("网络连接异常");
                    }
                })));

        mSettingsTv.setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
    }

    private void startPlay(String taskId, String accessToken) {
        showLoadingDialog();
        ZegoApiClient.getInstance().getPlayInfo(KeyCenter.APP_ID, taskId, accessToken, (ZegoApiClient.RequestCallback<PlayInfo>) (result, playInfo) -> {
            dismissLoadingDialog();
            Log.d(Constant.TAG, "getPlayInfo: result = " + result + ", playInfo = " + playInfo);
            if (result.getCode() == 0) {
                MainActivity.start(LoginActivity.this, playInfo.getPlayInfo());
            } else {
                ToastUtils.showCenterToast("获取数据异常: msg=" + result.getMessage() + ", playInfo = " + playInfo);
            }
        });
    }
}