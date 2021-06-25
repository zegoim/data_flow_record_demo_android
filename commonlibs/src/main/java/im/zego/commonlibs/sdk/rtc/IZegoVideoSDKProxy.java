package im.zego.commonlibs.sdk.rtc;

import android.app.Application;

import im.zego.commonlibs.sdk.ISDKInitCallback;

interface IZegoVideoSDKProxy {
    /**
     * 初始化 SDK
     */
    void initSDK(Application application, long appID, String appSign, boolean testEnv, ISDKInitCallback initCallback);

    /**
     * 反初始化 SDK
     */
    void unInitSDK();

    /**
     * 设置房间回调
     */
    void setZegoRoomCallback(IZegoRoomStateListener stateCallback);

    /**
     * liveroom/express 的 SDK 版本号
     */
    String getVersion();
}
