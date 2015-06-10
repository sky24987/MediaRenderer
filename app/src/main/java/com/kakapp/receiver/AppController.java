package com.kakapp.receiver;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class AppController extends Application {

	private static final String TAG = AppController.class.getSimpleName();
	private static String url = "";
	private static Context sContext;
	private static Boolean playMode = false;

	@Override
	public void onCreate() {
		super.onCreate();
		sContext = getApplicationContext();
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
	}

	public static Context getAppContext() {
		if (sContext == null) {
			Log.e(TAG, "Global context not set");
		}
		return sContext;
	}

	public static String getUrl() {
		return url;
	}

	public static void setUrl(String uri) {
		AppController.url = uri;
	}

	public static Boolean getPlayMode() {
		return playMode;
	}

	public static void setPlayMode(Boolean playMode) {
		AppController.playMode = playMode;
	}

}
