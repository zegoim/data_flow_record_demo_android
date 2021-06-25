package im.zego.recordplayer.manager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.widget.TintContextWrapper;

import com.zego.recorderplayback.ZegoCustomCommand;
import com.zego.recorderplayback.ZegoPlaybackMediaStreamInfo;
import com.zego.recorderplayback.ZegoPlaybackStreamInfo;
import com.zego.recorderplayback.ZegoPlaybackWhiteboardStreamInfo;

import java.util.Formatter;
import java.util.Locale;

/**
 * 公共类
 */
public class CommonUtil {
    public static String stringForTime(int timeMs) {
        if (timeMs <= 0) {
            return "00:00";
        }
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * Get activity from context object
     *
     * @param context something
     * @return object of Activity or null if it is not Activity
     */
    public static Activity scanForActivity(Context context) {
        if (context == null) return null;

        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof TintContextWrapper) {
            return scanForActivity(((TintContextWrapper) context).getBaseContext());
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }

    public static void hideNavKey(Context context) {
        if (Build.VERSION.SDK_INT >= 29) {
            //       设置屏幕始终在前面，不然点击鼠标，重新出现虚拟按键
            ((Activity) context).getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                            // bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //       设置屏幕始终在前面，不然点击鼠标，重新出现虚拟按键
            ((Activity) context).getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                            // bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            ((Activity) context).getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
            );
        }
    }

    public static void setViewState(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * dip转为PX
     */
    public static int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的高度px
     *
     * @param context 上下文
     * @return 屏幕高px
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    public static Activity getActivityContext(Context context) {
        if (context == null)
            return null;
        else if (context instanceof Activity)
            return (Activity) context;
        else if (context instanceof TintContextWrapper)
            return scanForActivity(((TintContextWrapper) context).getBaseContext());
        else if (context instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) context).getBaseContext());

        return null;
    }


    public static boolean getCurrentScreenLand(Activity context) {
        return context.getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90 ||
                context.getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270;

    }

    public static String printCustomCommand(ZegoCustomCommand customCommand) {
        return "ZegoCustomCommand{timestamp=" + customCommand.timestamp + "\n"
                + "data=" + customCommand.data + "\n}";
    }

    public static String printInfos(ZegoPlaybackStreamInfo[] infos) {
        StringBuilder sb = new StringBuilder();
        sb.append("streamInfos:\n");
        for (ZegoPlaybackStreamInfo info : infos) {
            sb.append(printInfo(info));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String printInfo(ZegoPlaybackStreamInfo info) {
        return "ZegoPlaybackStreamInfo{" +
                "whiteboardInfo=" + printWhiteboardStreamInfo(info.whiteboardInfo) +
                ", mediaInfo=" + printMediaStreamInfo(info.mediaInfo) +
                ", begin_time=" + info.begin_time +
                ", end_time=" + info.end_time +
                ", title='" + info.title + '\'' +
                ", width=" + info.width +
                ", height=" + info.height +
                ", x=" + info.x +
                ", y=" + info.y +
                ", zorder=" + info.zorder +
                ", display=" + info.display +
                '}';
    }

    private static String printWhiteboardStreamInfo(ZegoPlaybackWhiteboardStreamInfo info) {
        if (info != null) {
            return String.valueOf(info.whiteboardId);
        }
        return null;
    }

    private static String printMediaStreamInfo(ZegoPlaybackMediaStreamInfo info) {
        if (info != null) {
            return "ZegoPlaybackMediaStreamInfo{" +
                    "streamId='" + info.streamId + '\'' +
                    ", path='" + info.path + '\'' +
                    ", play=" + info.play +
                    '}';
        }
        return null;
    }
}
