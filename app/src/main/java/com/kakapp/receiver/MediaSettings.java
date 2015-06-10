package com.kakapp.receiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MediaSettings {

	private final static String TAG = MediaSettings.class.getSimpleName();

	public static String getDeviceName() {
		final SharedPreferences sp = getSharedPreferences();
		String nameString = sp.getString("deName", android.os.Build.MODEL + " Renderer");
		Log.v(TAG, "Using Name: " + nameString);
		return nameString;
	}

	private static SharedPreferences getSharedPreferences() {
		final Context context = AppController.getAppContext();
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

}
