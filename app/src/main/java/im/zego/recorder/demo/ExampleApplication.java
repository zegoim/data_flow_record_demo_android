package im.zego.recorder.demo;

import android.app.Application;

import im.zego.commonlibs.sdk.ZegoSDKManager;
import im.zego.commonlibs.utils.Logger;
import im.zego.commonlibs.utils.ToastUtils;
import im.zego.recorder.demo.helper.CrashHandler;

public class ExampleApplication extends Application {

    private static final String TAG = "ExampleApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        ToastUtils.setAppContext(this);
        CrashHandler.getInstance().init(this);

        ZegoSDKManager.getInstance().initSDKEnvironment(this, success -> Logger.i(TAG, "onInit() result: " + success));
    }
}