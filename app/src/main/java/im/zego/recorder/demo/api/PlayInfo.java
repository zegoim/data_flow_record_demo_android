package im.zego.recorder.demo.api;

import com.google.gson.annotations.SerializedName;

public class PlayInfo {
    private int code;
    private String message;
    @SerializedName("play_info")
    private String playInfo;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPlayInfo() {
        return playInfo;
    }

    public void setPlayInfo(String playInfo) {
        this.playInfo = playInfo;
    }

    @Override
    public String toString() {
        return "PlayInfo{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", playInfo='" + playInfo + '\'' +
                '}';
    }
}