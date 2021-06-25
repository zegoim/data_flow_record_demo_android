package im.zego.commonlibs.sdk.rtc;

public interface IZegoRoomStateListener {
    void onConnected(int errorCode, String roomID);

    void onDisconnect(int errorCode, String roomID);

    void connecting(int errorCode, String roomID);
}
