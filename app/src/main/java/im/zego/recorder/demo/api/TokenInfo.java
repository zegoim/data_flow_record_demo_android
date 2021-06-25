package im.zego.recorder.demo.api;

import com.google.gson.annotations.SerializedName;

/**
 * Created by rocket_wang on 2021/6/21.
 */
public class TokenInfo {
    private int code;
    private String message;
    @SerializedName("data")
    private TokenData tokenData;

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

    public TokenData getTokenData() {
        return tokenData;
    }

    public void setTokenData(TokenData tokenData) {
        this.tokenData = tokenData;
    }

    @Override
    public String toString() {
        return "TokenInfo{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", tokenData=" + tokenData +
                '}';
    }

    public class TokenData {
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("expires_in")
        private long expiresTimeIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public long getExpiresTimeIn() {
            return expiresTimeIn;
        }

        public void setExpiresTimeIn(long expiresTimeIn) {
            this.expiresTimeIn = expiresTimeIn;
        }

        @Override
        public String toString() {
            return "TokenData{" +
                    "accessToken='" + accessToken + '\'' +
                    ", expiresTimeIn=" + expiresTimeIn +
                    '}';
        }
    }
}
