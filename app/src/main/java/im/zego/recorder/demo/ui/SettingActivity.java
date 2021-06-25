package im.zego.recorder.demo.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.StringUtils;
import com.zego.recorderplayback.ZegoRecorderPlayback;

import im.zego.commonlibs.sdk.rtc.VideoSDKManager;
import im.zego.commonlibs.utils.ToastUtils;
import im.zego.commonlibs.utils.ZegoUtil;
import im.zego.recorder.demo.BaseActivity;
import im.zego.recorder.demo.BuildConfig;
import im.zego.recorder.demo.R;
import im.zego.zegodocs.ZegoDocsViewManager;
import im.zego.zegowhiteboard.ZegoWhiteboardManager;

public class SettingActivity extends BaseActivity {

    private ImageView mSettingBack;
    private TextView mSettingSave;
    private TextView mVersionsTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void initView() {
        mSettingBack = (ImageView) findViewById(R.id.setting_back);
        mSettingSave = (TextView) findViewById(R.id.setting_save);
        mVersionsTv = (TextView) findViewById(R.id.versions_tv);
    }

    @Override
    protected void initData() {
        super.initData();

        ZegoRecorderPlayback playback = new ZegoRecorderPlayback();
        playback.init();

        String versionStr =
                "playback: " + playback.getVersion() +
                        "\n" +
                        "video: " + VideoSDKManager.getInstance().getVersion() +
                        "\n" +
                        "app: " + BuildConfig.VERSION_NAME +
                        "\n" +
                        "docs: " + ZegoDocsViewManager.getInstance().getVersion() +
                        "\n" +
                        "whiteboard: " + ZegoWhiteboardManager.getInstance().getVersion() +
                        "\n";
        mVersionsTv.setText(versionStr);
    }

    @Override
    protected void setListener() {
        super.setListener();
        // 返回上一页
        mSettingBack.setOnClickListener(v -> onBackPressed());

        // 保存
        mSettingSave.setOnClickListener(v -> {
            // 重启使环境生效
            int time = 2;
            mSettingSave.postDelayed(() -> ZegoUtil.killSelfAndRestart(SettingActivity.this, LoginActivity.class), time * 1000L);
            ToastUtils.showCenterToast(time + "秒后重启应用");
        });
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            String url = sp.getString("api_env_url", StringUtils.getString(R.string.default_api_env_url));

            Preference preference = findPreference("api_env_url");
            if (preference != null) {
                preference.setSummary(url);

                preference.setOnPreferenceChangeListener((preference1, newValue) -> {
                    String newUrl = (String) newValue;
                    preference.setSummary(newUrl);
                    SPStaticUtils.put("api_env_url", newUrl);
                    return true;
                });
            }
        }
    }
}