package im.zego.commonlibs.sdk.rtc;
//
//import android.app.Application;
//import android.util.Log;
//
//import com.zego.zegoliveroom.ZegoLiveRoom;
//import com.zego.zegoliveroom.callback.IZegoLogHookCallback;
//import com.zego.zegoliveroom.callback.IZegoRoomCallback;
//import com.zego.zegoliveroom.entity.ZegoRoomInfo;
//import com.zego.zegoliveroom.entity.ZegoStreamInfo;
//
//import java.io.File;
//
//import im.zego.commonlibs.constants.AppConstants;
//import im.zego.commonlibs.sdk.ISDKInitCallback;
//import im.zego.commonlibs.utils.ZegoUtil;
//
///**
// *
// */
//class ZegoLiveRoomWrapper implements IZegoVideoSDKProxy {
//    private String TAG = "ZegoLiveRoomWrapper";
//    private ZegoLiveRoom zegoLiveRoomSDK = new ZegoLiveRoom();
//
//    private IZegoRoomStateListener zegoRoomStateListener = null;
//
//    @Override
//    public void initSDK(Application application, long appID, String appSign, boolean testEnv, ISDKInitCallback initCallback) {
//        ZegoLiveRoom.setSDKContext(new ZegoLiveRoom.SDKContextEx() {
//            @Override
//            public long getLogFileSize() {
//                return AppConstants.LOG_SIZE;
//            }
//
//            @Override
//            public String getSubLogFolder() {
//                return AppConstants.LOG_SUBFOLDER;
//            }
//
//            @Override
//            public IZegoLogHookCallback getLogHookCallback() {
//                return null;
//            }
//
//            @Override
//            public String getSoFullPath() {
//                return null;
//            }
//
//            @Override
//            public String getLogPath() {
//                return application.getExternalFilesDir(null).getAbsolutePath() + File.separator;
//            }
//
//            @Override
//            public Application getAppContext() {
//                return application;
//            }
//        });
//
//        ZegoLiveRoom.setConfig("room_retry_time=60");
//        if (testEnv) {
//            ZegoLiveRoom.setTestEnv(true);
//        }
//
//        boolean result  = zegoLiveRoomSDK.initSDK(appID, ZegoUtil.parseSignKeyFromString(appSign), errorCode -> {
//            initCallback.onInit(errorCode == 0);
//            if (errorCode == 0) {
//                initSDKCallbacks();
//                ZegoLiveRoom.requireHardwareDecoder(true);
//                ZegoLiveRoom.requireHardwareEncoder(true);
//            }
//        });
//        if (!result) {
//            initCallback.onInit(false);
//        }
//    }
//
//    private void initSDKCallbacks() {
//        zegoLiveRoomSDK.setZegoRoomCallback(new IZegoRoomCallback() {
//            @Override
//            public void onKickOut(int i, String s, String s1) {
//
//            }
//
//            @Override
//            public void onDisconnect(int errorCode, String roomID) {
//                Log.d(TAG, "onDisconnect:errorCode:" + errorCode);
//                if (zegoRoomStateListener != null) {
//                    zegoRoomStateListener.onDisconnect(errorCode, roomID);
//                }
//            }
//
//            @Override
//            public void onReconnect(int errorCode, String roomID) {
//                Log.d(TAG, "onReconnect:errorCode:" + errorCode);
//                if (zegoRoomStateListener != null) {
//                    zegoRoomStateListener.onConnected(errorCode, roomID);
//                }
//            }
//
//            @Override
//            public void onTempBroken(int errorCode, String roomID) {
//                if (zegoRoomStateListener != null) {
//                    zegoRoomStateListener.connecting(errorCode, roomID);
//                }
//            }
//
//            @Override
//            public void onRoomInfoUpdated(ZegoRoomInfo zegoRoomInfo, String s) {
//
//            }
//
//            @Override
//            public void onStreamUpdated(int i, ZegoStreamInfo[] zegoStreamInfos, String s) {
//
//            }
//
//            @Override
//            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {
//
//            }
//
//            @Override
//            public void onRecvCustomCommand(String s, String s1, String s2, String s3) {
//
//            }
//
//            @Override
//            public void onNetworkQuality(String s, int i, int i1) {
//
//            }
//        });
//    }
//
//    @Override
//    public void unInitSDK() {
//        zegoLiveRoomSDK.unInitSDK();
//    }
//
//    @Override
//    public void setZegoRoomCallback(IZegoRoomStateListener stateCallback) {
//        this.zegoRoomStateListener = stateCallback;
//    }
//
//    @Override
//    public String getVersion() {
//        return ZegoLiveRoom.version();
//    }
//}
