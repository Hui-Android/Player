package com.app.player.view;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

import com.app.player.R;


public class BottomPopDialog {
	public static Dialog getDialog(Context context, View contentView,
                                   Dialog tmpDialog) {
		if (tmpDialog != null && tmpDialog.isShowing()) {
			return tmpDialog;
		}
		Dialog dialog = new Dialog(context, R.style.popup_dialog_anim);
		Window window = dialog.getWindow();
		WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		windowParams.x = 0;
		windowParams.y = dm.heightPixels;
		window.setAttributes(windowParams);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.setContentView(contentView);
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		return dialog;
	}

	public static Dialog getDialog2(Context context, View contentView,
                                    Dialog tmpDialog) {
		if (tmpDialog != null && tmpDialog.isShowing()) {
			return tmpDialog;
		}
		Dialog dialog = new Dialog(context, R.style.popup_dialog_anim);
		Window window = dialog.getWindow();
		WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		windowParams.x = 0;
		windowParams.y = dm.heightPixels;
		window.setAttributes(windowParams);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(contentView);
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		return dialog;
	}
}
