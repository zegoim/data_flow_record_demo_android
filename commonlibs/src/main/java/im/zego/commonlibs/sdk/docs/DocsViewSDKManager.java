package im.zego.commonlibs.sdk.docs;

import android.app.Application;
import android.os.Environment;

import java.io.File;

import im.zego.commonlibs.KeyCenter;
import im.zego.commonlibs.constants.AppConstants;
import im.zego.commonlibs.sdk.ISDKInitCallback;
import im.zego.commonlibs.utils.Logger;
import im.zego.zegodocs.ZegoDocsViewConfig;
import im.zego.zegodocs.ZegoDocsViewManager;

/**
 * 文档服务 SDK 管理
 */
public class DocsViewSDKManager {
    private static final String TAG = "DocsViewSDKManager";

    private static final DocsViewSDKManager ourInstance = new DocsViewSDKManager();

    public static DocsViewSDKManager getInstance() {
        return ourInstance;
    }

    private DocsViewSDKManager() {
    }

    // 用于判断是否成功初始化文档服务
    public Boolean initDocsResult = null;

    /**
     * 初始化文档服务
     */
    public void init(Application application, ISDKInitCallback sdkInitCallback) {
        Logger.i(TAG, "initDocSdk.... currentVersion: " + ZegoDocsViewManager.getInstance().getVersion());

        // 设置appID, appSign, 是否测试环境 isTestEnv
        Logger.i(TAG, "initDocSdk.... isDocsViewEnvTest: " + AppConstants.IS_TEST_ENV);
        ZegoDocsViewConfig config = new ZegoDocsViewConfig();
        config.setAppID(KeyCenter.APP_ID);
        config.setAppSign(KeyCenter.APP_SIGN);
        config.setTestEnv(AppConstants.IS_TEST_ENV);

        // 设置存储路径
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 可选设置
            config.setLogFolder(application.getExternalFilesDir(null).getAbsolutePath() + File.separator + AppConstants.LOG_SUBFOLDER);
            config.setDataFolder(application.getExternalFilesDir(null).getAbsolutePath() + File.separator + "zegodocs" + File.separator + "data");
            config.setCacheFolder(application.getExternalFilesDir(null).getAbsolutePath() + File.separator + "zegodocs" + File.separator + "cache");
        }

        // 初始化
        ZegoDocsViewManager.getInstance().init(application, config, errorCode -> {
            Logger.i(TAG, "init docsView result: " + errorCode);
            initDocsResult = errorCode == 0;
            sdkInitCallback.onInit(errorCode == 0);
        });
    }

    public void unInitSDK() {
        initDocsResult = null;
        ZegoDocsViewManager.getInstance().uninit();
    }
}
