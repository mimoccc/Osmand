package net.osmand.plus.api;

import net.osmand.plus.OsmandApplication;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public class ExternalServiceAPIImpl implements ExternalServiceAPI {

	private OsmandApplication app;

	public ExternalServiceAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public boolean isWifiConnected() {
		ConnectivityManager mgr =  (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	@Override
	public boolean isInternetConnected() {
		ConnectivityManager mgr = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo active = mgr.getActiveNetworkInfo();
		if(active == null){
			return false;
		} else {
			NetworkInfo.State state = active.getState();
			return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
		}
	}

	@Override
	public boolean isLightSensorEnabled() {
		SensorManager mSensorManager = (SensorManager)app.getSystemService(Context.SENSOR_SERVICE);         
	    Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		return mLight != null;
	}

	@Override
	public String getExternalStorageDirectory() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

}