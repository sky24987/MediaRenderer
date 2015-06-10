package com.kakapp.receiver.upnp;

import java.util.concurrent.ConcurrentHashMap;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.TransportState;

import android.content.Context;
import android.util.Log;

public class DefMediaControls extends ConcurrentHashMap<UnsignedIntegerFourBytes, DefMediaControl> {

	private static final long serialVersionUID = 1L;
	final private static String TAG = "DefMediaControls";

	final protected LastChange avTransportLastChange;
	final protected LastChange renderingControlLastChange;

	public DefMediaControls(Context context, int numberOfPlayers, LastChange avTransportLastChange, LastChange renderingControlLastChange) {
		super(numberOfPlayers);
		this.avTransportLastChange = avTransportLastChange;
		this.renderingControlLastChange = renderingControlLastChange;

		for (int i = 0; i < numberOfPlayers; i++) {
			DefMediaControl player = new DefMediaControl(context) {
				@Override
				public void transportStateChanged(TransportState newState) {
					super.transportStateChanged(newState);
					if (newState.equals(TransportState.PLAYING)) {
						onPlayerPlay(this);
					} else if (newState.equals(TransportState.STOPPED)) {
						onPlayerStop(this);
					} else if (newState.equals(TransportState.PAUSED_PLAYBACK)) {
						onPlayerPaused(this);
					}
				}

			};

			player.init(new UnsignedIntegerFourBytes(i), avTransportLastChange, renderingControlLastChange);

			put(player.getInstanceId(), player);

		}
	}

	protected void onPlayerPlay(DefMediaControl player) {
		Log.v(TAG, "Player is playing: " + player.getInstanceId());
	}

	protected void onPlayerStop(DefMediaControl player) {
		Log.v(TAG, "Player is stopping: " + player.getInstanceId());
	}

	protected void onPlayerPaused(DefMediaControl player) {
		Log.v(TAG, "Player is pausing: " + player.getInstanceId());
	}
}
