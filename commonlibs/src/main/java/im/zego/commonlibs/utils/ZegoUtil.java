package im.zego.commonlibs.utils;

import android.content.Context;
import android.content.Intent;

public class ZegoUtil {
    /**
     * 字符串转换成 byte 数组
     * 主要用于 appSign 的转换
     *
     * @param strSignKey 字符串格式的appSign，形如"0x01, 0x02, 0x03, 0x04"
     * @return 根据 strSignKey 转化而来的 byte[]
     * @throws NumberFormatException
     */
    public static byte[] parseSignKeyFromString(String strSignKey) throws NumberFormatException {
        if (!strSignKey.startsWith("0x")) {
            StringBuilder builder = new StringBuilder();
            char[] chars = strSignKey.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                if (i % 2 == 0) {
                    if (i == 0) {
                        builder.append("0x");
                    } else {
                        builder.append(",0x");
                    }
                }
                builder.append(chars[i]);
            }
            strSignKey = builder.toString();
        }

        // 解决客户有可能直接拷贝邮件上的appSign导致错误的问题。
        strSignKey = strSignKey.replaceAll("\\(byte\\)", "");

        String[] keys = strSignKey.split(",");
        if (keys.length != 32) {
            return null;
        }
        byte[] byteSignKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            int data = Integer.valueOf(keys[i].trim().replace("0x", ""), 16);
            byteSignKey[i] = (byte) data;
        }
        return byteSignKey;
    }

    public static void killSelfAndRestart(Context context, Class launcherClass) {
        Intent intent = new Intent(context, launcherClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
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
}
