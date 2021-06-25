package im.zego.commonlibs.sdk.whiteboard;

import android.content.Context;

import java.io.File;

import im.zego.commonlibs.constants.AppConstants;
import im.zego.commonlibs.sdk.ISDKInitCallback;
import im.zego.commonlibs.utils.Logger;
import im.zego.zegowhiteboard.ZegoWhiteboardConfig;
import im.zego.zegowhiteboard.ZegoWhiteboardManager;
import im.zego.zegowhiteboard.callback.IZegoWhiteboardGetListListener;
import im.zego.zegowhiteboard.callback.IZegoWhiteboardManagerListener;

/**
 * 白板服务 SDK 管理
 */
public class WhiteboardSDKManager {
    private static final String TAG = "WhiteboardSDKManager";

    private static final WhiteboardSDKManager ourInstance = new WhiteboardSDKManager();

    public static WhiteboardSDKManager getInstance() {
        return ourInstance;
    }

    private WhiteboardSDKManager() {
    }

    // 用于判断是否成功初始化白板服务
    public Boolean initWhiteboardResult = null;

    public void init(Context context, ISDKInitCallback sdkInitCallback) {
        Logger.i(TAG, String.format("initWhiteboardSDK.... currentVersion: %s", ZegoWhiteboardManager.getInstance().getVersion()));

        ZegoWhiteboardConfig config = new ZegoWhiteboardConfig();
        // 设置日志存储路径
        config.setLogPath(context.getExternalFilesDir(null).getAbsolutePath() + File.separator + AppConstants.LOG_SUBFOLDER);
        // 设置图片存储路径
        config.setCacheFolder(context.getExternalFilesDir(null).getAbsolutePath() + File.separator + AppConstants.IMAGE_SUBFOLDER);
        ZegoWhiteboardManager.getInstance().setConfig(config);

        // 初始化
        ZegoWhiteboardManager.getInstance().init(context, errorCode -> {
            Logger.i(TAG, "init Whiteboard errorCode: " + errorCode);
            if (errorCode == 0) {
                // 设置默认字体为系统
                ZegoWhiteboardManager.getInstance().setCustomFontFromAsset("", "");
            }
            initWhiteboardResult = errorCode == 0;
            sdkInitCallback.onInit(errorCode == 0);
        });
    }

    public void unInitSDK() {
        initWhiteboardResult = null;
        ZegoWhiteboardManager.getInstance().uninit();
    }

    public void setWhiteboardCountListener(IZegoWhiteboardManagerListener listener) {
        ZegoWhiteboardManager.getInstance().setWhiteboardManagerListener(listener);
    }

    public void getWhiteboardViewList(IZegoWhiteboardGetListListener listListener) {
        ZegoWhiteboardManager.getInstance().getWhiteboardViewList(listListener);
    }
}
