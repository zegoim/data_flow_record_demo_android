package im.zego.commonlibs.sdk.rtc

import android.app.Application
import android.util.Log
import im.zego.commonlibs.constants.AppConstants
import im.zego.commonlibs.sdk.ISDKInitCallback
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoRoomState
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.entity.ZegoEngineConfig
import im.zego.zegoexpress.entity.ZegoLogConfig
import org.json.JSONObject
import java.io.File

/**
 * 不要写 双斜线 注释，编译脚本处理的时候会删掉所有的 双斜线
 */
internal class ZegoExpressWrapper : IZegoVideoSDKProxy {

    private val TAG = "ZegoExpressWrapper"
    private lateinit var expressEngine: ZegoExpressEngine
    private var zegoRoomStateListener: IZegoRoomStateListener? = null
    private var isLoginRoom = false
    private var loginResult: (Int) -> Unit = {}

    override fun initSDK(
        application: Application,
        appID: Long,
        appSign: String,
        testEnv: Boolean,
        sdkInitCallback: ISDKInitCallback
    ) {
        Log.d(TAG, "init ZegoExpressEngine, version:${ZegoExpressEngine.getVersion()}")
        val config = ZegoEngineConfig()
        val zegoLogConfig = ZegoLogConfig()
        zegoLogConfig.logPath = application.getExternalFilesDir(null)!!.absolutePath + File.separator + AppConstants.LOG_SUBFOLDER
        zegoLogConfig.logSize = 5*1024*1024
        config.logConfig = zegoLogConfig
        ZegoExpressEngine.setEngineConfig(config)
        val engine = ZegoExpressEngine.createEngine(
            appID, appSign, testEnv,
            ZegoScenario.LIVE, application, null
        )
        if (engine == null) {
            sdkInitCallback.onInit(false)
            return
        }

        expressEngine = engine
        expressEngine.setEventHandler(object : IZegoEventHandler() {
            override fun onRoomStateUpdate(
                roomID: String,
                state: ZegoRoomState,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                Log.d(TAG, "onRoomStateUpdate:state :${state},errorCode:${errorCode}")
                when (state) {
                    ZegoRoomState.DISCONNECTED -> {
                        if (isLoginRoom) {
                            loginResult.invoke(errorCode)
                            isLoginRoom = false
                        } else {
                            zegoRoomStateListener?.onDisconnect(errorCode, roomID)
                        }
                    }
                    ZegoRoomState.CONNECTED -> {
                        if (isLoginRoom) {
                            loginResult.invoke(errorCode)
                            isLoginRoom = false
                        } else {
                            zegoRoomStateListener?.onConnected(errorCode, roomID)
                        }
                    }
                    ZegoRoomState.CONNECTING -> {
                        zegoRoomStateListener?.connecting(errorCode, roomID)
                    }
                }
            }
        })

        sdkInitCallback.onInit(true)
    }

    override fun setZegoRoomCallback(stateCallback: IZegoRoomStateListener?) {
        this.zegoRoomStateListener = stateCallback
    }

    override fun unInitSDK() {
        ZegoExpressEngine.destroyEngine(null)
    }

//    override fun loginRoom(
//        userID: String,
//        userName: String,
//        roomID: String,
//        function: (Int) -> Unit
//    ) {
//        val user = ZegoUser(userID, userName)
//        val roomConfig = ZegoRoomConfig()
//        roomConfig.isUserStatusNotify = true
//        expressEngine.loginRoom(roomID, user, roomConfig)
//        this.loginResult = function
//        isLoginRoom = true
//    }
//
//    override fun logoutRoom(roomID: String) {
//        expressEngine.logoutRoom(roomID)
//        isLoginRoom = false
//    }

    override fun getVersion(): String {
        return ZegoExpressEngine.getVersion()
    }

//    override fun uploadLog() {
//        Logger.d(TAG, "uploadLog() called")
//        expressEngine.uploadLog()
//    }
}