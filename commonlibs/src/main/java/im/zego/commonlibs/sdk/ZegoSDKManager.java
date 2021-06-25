package im.zego.commonlibs.sdk;

import android.app.Application;

import im.zego.commonlibs.sdk.docs.DocsViewSDKManager;
import im.zego.commonlibs.sdk.rtc.VideoSDKManager;
import im.zego.commonlibs.sdk.whiteboard.WhiteboardSDKManager;

/**
 * 对 express、白板 SDK、DocsView SDK 的封装
 */
public class ZegoSDKManager {
    private static final ZegoSDKManager ourInstance = new ZegoSDKManager();

    public static ZegoSDKManager getInstance() {
        return ourInstance;
    }

    private ZegoSDKManager() {
    }

    public void initSDKEnvironment(Application application, ISDKInitCallback sdkInitCallback) {
        // 该处需要先初始化 VideoSDKManager 再初始化 WhiteboardSDKManager，顺序不能乱
        VideoSDKManager.getInstance().init(application, videoSDKSuccess -> {
            if (videoSDKSuccess) {
                WhiteboardSDKManager.getInstance().init(application, whiteboardSDKSuccess -> {
                    if (whiteboardSDKSuccess) {
                        notifyInitResult(sdkInitCallback);
                    } else {
                        sdkInitCallback.onInit(false);
                    }
                });
            } else {
                sdkInitCallback.onInit(false);
            }
        });

        DocsViewSDKManager.getInstance().init(application, success -> {
            if (success) {
                notifyInitResult(sdkInitCallback);
            } else {
                sdkInitCallback.onInit(false);
            }
        });
    }

    private void notifyInitResult(ISDKInitCallback callback) {
        if (VideoSDKManager.getInstance().initRoomResult != null
                && DocsViewSDKManager.getInstance().initDocsResult != null
                && WhiteboardSDKManager.getInstance().initWhiteboardResult != null) {

            callback.onInit(
                    VideoSDKManager.getInstance().initRoomResult
                            && DocsViewSDKManager.getInstance().initDocsResult
                            && WhiteboardSDKManager.getInstance().initWhiteboardResult
            );
        }
    }
}
