package com.app.player.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    /***
     * 展示toast
     * @param context context
     * @param msgResId msgResId
     */
    public static void showDefaultToast(Context context, int msgResId) {
        Toast.makeText(context, context.getString(msgResId), Toast.LENGTH_SHORT).show();
    }

    /***
     * 展示toast
     * @param context context
     * @param msg msg
     */
    public static void showDefaultToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
