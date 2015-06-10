package com.kakapp.receiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.AndroidWifiSwitchableRouter;
import org.fourthline.cling.binding.LocalServiceBinder;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.ServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;

import com.kakapp.receiver.upnp.DefAVTransportService;
import com.kakapp.receiver.upnp.DefAudioRenderingControl;
import com.kakapp.receiver.upnp.DefConnectionManagerService;
import com.kakapp.receiver.upnp.DefMediaControl;
import com.kakapp.receiver.upnp.DefMediaControls;

public class MediaRendererService extends AndroidUpnpServiceImpl {

	static public final String ACTION_STARTED = "RENDER_STARTED";
	static public final String ACTION_STOPPED = "RENDER_STOPPED";
	static public final String ACTION_FAILEDTOSTART = "RENDER_FAILEDTOSTART";

	static public final String ACTION_START_RENDER = "ACTION_START_RENDER";
	static public final String ACTION_STOP_RENDER = "ACTION_STOP_RENDER";
	public static final String ACTION_SETURI = "ACTION_SETURI";
	public static final String ACTION_PLAY = "ACTION_PLAY";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_PAUSE = "ACTION_PAUSE";

	public static String TAG = "RenderService";

	public static final long LAST_CHANGE_FIRING_INTERVAL_MILLISECONDS = 2000;

	final protected LocalServiceBinder binder = new AnnotationLocalServiceBinder();

	final protected LastChange avTransportLastChange = new LastChange(new AVTransportLastChangeParser());
	final protected LastChange renderingControlLastChange = new LastChange(new RenderingControlLastChangeParser());

	protected static Map<UnsignedIntegerFourBytes, DefMediaControl> mediaPlayers;

	protected static ServiceManager<DefConnectionManagerService> connectionManager;
	protected static LastChangeAwareServiceManager<DefAVTransportService> avTransport;
	protected static LastChangeAwareServiceManager<DefAudioRenderingControl> renderingControl;
	private static boolean b = false;

	protected LocalDevice device;

	@Override
	protected AndroidUpnpServiceConfiguration createConfiguration(Object manager) {
		return new AndroidUpnpServiceConfiguration(manager) {

			@Override
			public int getRegistryMaintenanceIntervalMillis() {
				return 7000;
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		try {
			mediaPlayers = new DefMediaControls(this, 1, avTransportLastChange, renderingControlLastChange);

			LocalService<ConnectionManagerService> connectionManagerService = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

			connectionManagerService.setManager(new DefaultServiceManager<ConnectionManagerService>(connectionManagerService, null) {
				@Override
				protected ConnectionManagerService createServiceInstance() throws Exception {
					return new ConnectionManagerService();
				}
			});

			LocalService<DefAVTransportService> avTransportService = binder.read(DefAVTransportService.class);

			avTransport = new LastChangeAwareServiceManager<DefAVTransportService>(avTransportService, new AVTransportLastChangeParser()) {
				@Override
				protected DefAVTransportService createServiceInstance() throws Exception {
					return new DefAVTransportService(avTransportLastChange, mediaPlayers);
				}

				@Override
				protected int getLockTimeoutMillis() {
					return 2000;
				}

			};
			avTransportService.setManager(avTransport);

			LocalService<DefAudioRenderingControl> renderingControlService = binder.read(DefAudioRenderingControl.class);

			renderingControl = new LastChangeAwareServiceManager<DefAudioRenderingControl>(renderingControlService, new RenderingControlLastChangeParser()) {
				@Override
				protected DefAudioRenderingControl createServiceInstance() throws Exception {

					return new DefAudioRenderingControl(renderingControlLastChange, mediaPlayers);
				}
			};
			renderingControlService.setManager(renderingControl);

			try {
				device = new LocalDevice(new DeviceIdentity(UDN.uniqueSystemIdentifier("MediaRenderer")), new UDADeviceType("MediaRenderer", 1), new DeviceDetails(MediaSettings.getDeviceName(),
						new ManufacturerDetails(android.os.Build.MANUFACTURER), new ModelDetails("MediaRenderer", "MediaRenderer", "1")), new Icon[] { createDefaultDeviceIcon() }, new LocalService[] {
						avTransportService, renderingControlService, connectionManagerService });

			} catch (ValidationException ex) {
				throw new RuntimeException(ex);

			}

			runLastChangePushThread();
			upnpService.getRegistry().addDevice(device);

			b = true;
			sendBroadcast(new Intent(MediaRendererService.ACTION_STARTED));
		} catch (Exception e) {
			b = false;
			sendBroadcast(new Intent(MediaRendererService.ACTION_STOPPED));
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		b = false;
		sendBroadcast(new Intent(MediaRendererService.ACTION_STOPPED));
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (!ModelUtil.ANDROID_EMULATOR && isListeningForConnectivityChanges()) {
					unregisterReceiver(((AndroidWifiSwitchableRouter) upnpService.getRouter()).getBroadcastReceiver());
				}

				new Shutdown().execute(upnpService);

			}
		}).run();
	}

	class Shutdown extends AsyncTask<UpnpService, Void, Void> {
		@Override
		protected Void doInBackground(UpnpService... svcs) {
			UpnpService svc = svcs[0];
			if (null != svc) {
				try {
					svc.shutdown();
				} catch (java.lang.IllegalArgumentException ex) {
					ex.printStackTrace();
				}
			}
			return null;
		}
	}

	protected void runLastChangePushThread() {

		new Thread() {
			@Override
			public void run() {
				try {
					Log.d("runLastChangePushThread", "runLastChangePushThread is running");
					while (true) {
						avTransport.fireLastChange();
						renderingControl.fireLastChange();
						Thread.sleep(LAST_CHANGE_FIRING_INTERVAL_MILLISECONDS);
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}.start();
	}

	protected Icon createDefaultDeviceIcon() {

		BitmapDrawable bitDw = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher));
		Bitmap bitmap = bitDw.getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] imageInByte = stream.toByteArray();
		System.out.println("........length......" + imageInByte);
		ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);

		try {
			return new Icon("image/png", 48, 48, 8, URI.create("icon.png"), bis);
		} catch (IOException ex) {
			throw new RuntimeException("Could not load icon", ex);
		}
	}

	public static boolean isRunning() {
		return b;
	}

	public static Map<UnsignedIntegerFourBytes, DefMediaControl> getMediaPlayers() {
		return mediaPlayers;
	}

}
