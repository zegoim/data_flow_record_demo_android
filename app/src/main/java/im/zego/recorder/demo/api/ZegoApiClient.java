package im.zego.recorder.demo.api;

import android.util.Log;

import com.blankj.utilcode.util.EncodeUtils;
import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.StringUtils;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import im.zego.recorder.demo.R;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 封装了通过taskID去获取回放视频资源的metaInfo接口
 * 具体的调用可参考官方服务端API文档
 */
public class ZegoApiClient {
    private static final ZegoApiClient ourInstance = new ZegoApiClient();

    public static ZegoApiClient getInstance() {
        return ourInstance;
    }

    private ZegoApiClient() {
        init();
    }

    private static final String TAG = "ZegoApiClient";
    private ZegoPlaybackService playbackApi;

    private void init() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String rootUrl = SPStaticUtils.getString("api_env_url", StringUtils.getString(R.string.default_api_env_url));

        Retrofit retrofit = new Retrofit.Builder()
                // 域名需要替换成从API后台拿到的
                .baseUrl(rootUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        playbackApi = retrofit.create(ZegoPlaybackService.class);
    }

    public void getPlayInfo(long appId, String taskId, String accessToken, RequestCallback<PlayInfo> callback) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("app_id", appId);
        map.put("task_id", taskId);
        map.put("access_token", accessToken);
        playbackApi.getPlayInfo(map).enqueue(new Callback<PlayInfo>() {
            @Override
            public void onResponse(Call<PlayInfo> call, Response<PlayInfo> response) {
                PlayInfo body = response.body();
                if (body != null) {
                    Result result = new Result(body.getCode(), body.getMessage());
                    callback.onResult(result, body);
                } else {
                    Result result = new Result(-1, "error!");
                    callback.onResult(result, null);
                }
            }

            @Override
            public void onFailure(Call<PlayInfo> call, Throwable error) {
                Log.d(TAG, "onFailure() called with: call = " + call + ", error = " + error);

                Result result = new Result(-1, "error!");
                callback.onResult(result, null);
            }
        });
    }

    public void getToken(long appId, String appSecret, RequestCallback<TokenInfo> callback) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("app_id", appId);
        map.put("seq", 1);
        map.put("token", generateToken(appId, appSecret));
        map.put("version", 1);
        playbackApi.getToken(map).enqueue(new Callback<TokenInfo>() {
            @Override
            public void onResponse(Call<TokenInfo> call, Response<TokenInfo> response) {
                TokenInfo body = response.body();
                if (body != null && body.getTokenData() != null) {
                    Result result = new Result(body.getCode(), body.getMessage());
                    callback.onResult(result, body);
                } else {
                    Result result = new Result(-1, "error!");
                    callback.onResult(result, null);
                }
            }

            @Override
            public void onFailure(Call<TokenInfo> call, Throwable error) {
                Log.d(TAG, "onFailure() called with: call = " + call + ", error = " + error);

                Result result = new Result(-1, "error!");
                callback.onResult(result, null);
            }
        });
    }

    private String generateToken(long appId, String appSecret) {
        String nonce = UUID.randomUUID().toString().substring(0, 16);
        long expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.HOURS.toSeconds(72);
        String hash = EncryptUtils.encryptMD5ToString(appId + appSecret + nonce + expiredTime).toLowerCase();

        HashMap<String, Object> map = new HashMap<>();
        map.put("ver", 1);
        map.put("nonce", nonce);
        map.put("expired", expiredTime);
        map.put("hash", hash);

        return new String(EncodeUtils.base64Encode(GsonUtils.toJson(map)));
    }

    public interface RequestCallback<T> {
        void onResult(Result result, T t);
    }
}
