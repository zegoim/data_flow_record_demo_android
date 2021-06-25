package im.zego.commonlibs.sdk.rtc;

import android.app.Application;

import im.zego.commonlibs.KeyCenter;
import im.zego.commonlibs.constants.AppConstants;
import im.zego.commonlibs.sdk.ISDKInitCallback;
import im.zego.commonlibs.utils.Logger;

/**
 * 房间服务 SDK 管理
 */
public class VideoSDKManager {
    private static final String TAG = "VideoSDKManager";

    private static final VideoSDKManager ourInstance = new VideoSDKManager();

    public static VideoSDKManager getInstance() {
        return ourInstance;
    }

    private VideoSDKManager() {
    }

    private IZegoVideoSDKProxy zegoVideoSDKProxy = new ZegoExpressWrapper();  //replace_with_content

    // 用于判断是否成功初始化房间服务
    public Boolean initRoomResult = null;

    public void init(Application application, ISDKInitCallback initCallback) {
        zegoVideoSDKProxy.initSDK(application, KeyCenter.APP_ID, KeyCenter.APP_SIGN, AppConstants.IS_TEST_ENV, success -> {
            Logger.i(TAG, "init zegoLiveRoomSDK result: " + success);
            initRoomResult = success;
            initCallback.onInit(success);
        });
    }

    public void unInitSDK() {
        initRoomResult = null;
        zegoVideoSDKProxy.unInitSDK();
    }

    public String getVersion() {
        return zegoVideoSDKProxy.getVersion();
    }
}
