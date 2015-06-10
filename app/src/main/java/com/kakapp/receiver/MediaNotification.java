package com.kakapp.receiver;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MediaNotification extends BroadcastReceiver {
	private static final String TAG = MediaNotification.class.getSimpleName();

	private final int NOTIFICATIONID = 7891;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive broadcast: " + intent.getAction());
		if (intent.getAction().equals(MediaRendererService.ACTION_STARTED)) {
			setupNotification(context);
		} else if (intent.getAction().equals(MediaRendererService.ACTION_STOPPED)) {
			clearNotification(context);
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void setupNotification(Context context) {
		Log.d(TAG, "Setting up the notification");
		// Get NotificationManager reference
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) context.getSystemService(ns);

		// Instantiate a Notification
		int icon = R.drawable.ic_stat_done;
		CharSequence tickerText = String.format(context.getString(R.string.notif_server_starting), MediaSettings.getDeviceName());
		long when = System.currentTimeMillis();

		// Define Notification's message and Intent
		CharSequence contentTitle = context.getString(R.string.notif_title);
		CharSequence contentText = String.format(context.getString(R.string.notif_text), MediaSettings.getDeviceName());

		Intent notificationIntent = new Intent(context, MediaPreferenceActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
		CharSequence stopText = context.getString(R.string.notif_stop_text);
		Intent stopIntent = new Intent(MediaRendererService.ACTION_STOP_RENDER);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder nb = new NotificationCompat.Builder(context) //
				.setContentTitle(contentTitle) //
				.setContentText(contentText) //
				.setContentIntent(contentIntent) //
				.setSmallIcon(icon) //
				.setTicker(tickerText) //
				.setWhen(when) //
				.setOngoing(true);

		Notification notification = null;
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
			nb.addAction(stopIcon, stopText, stopPendingIntent);
			notification = nb.build();
		} else {
			notification = nb.getNotification();
		}

		// Pass Notification to NotificationManager
		nm.notify(NOTIFICATIONID, notification);

		Log.d(TAG, "Notication setup done");
	}

	private void clearNotification(Context context) {
		Log.d(TAG, "Clearing the notifications");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) context.getSystemService(ns);
		nm.cancelAll();
		Log.d(TAG, "Cleared notification");
	}
}
