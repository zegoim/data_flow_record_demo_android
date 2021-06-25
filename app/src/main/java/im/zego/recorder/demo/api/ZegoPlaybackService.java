package im.zego.recorder.demo.api;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ZegoPlaybackService {
    @POST("get_play_info")
    Call<PlayInfo> getPlayInfo(@Body HashMap<String, Object> body);

    @POST("access_token")
    Call<TokenInfo> getToken(@Body HashMap<String, Object> body);
}
