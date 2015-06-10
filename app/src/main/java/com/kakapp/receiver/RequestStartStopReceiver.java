package com.kakapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RequestStartStopReceiver extends BroadcastReceiver {

	static final String TAG = RequestStartStopReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "Received: " + intent.getAction());
		try {
			if (intent.getAction().equals(MediaRendererService.ACTION_START_RENDER)) {
				Intent serverService = new Intent(context, MediaRendererService.class);
				if (!MediaRendererService.isRunning()) {
					context.startService(serverService);
				}
			} else if (intent.getAction().equals(MediaRendererService.ACTION_STOP_RENDER)) {
				Intent serverService = new Intent(context, MediaRendererService.class);
				context.stopService(serverService);
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to start/stop on intent " + e.getMessage());
		}
	}
}
