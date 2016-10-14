package com.jef.citysearch;

import android.R.integer;
import android.app.Application;
import android.util.Log;

public class WeatherApp extends Application {
	
	public static WeatherModel mModel = null;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mModel = WeatherModel.getInstance(getApplicationContext());
		Log.e("XXX", "-----WeatherApp--------onCreate------");
		mModel.init();
		
		int defState = WeatherDataUtil.getInstance().getDefaultState(this);
		if (defState == WeatherDataUtil.DEFAULT_STATE_NEED_CHECK) {
			String defWoeid = getResources().getString(R.string.default_woeid);
			if (defWoeid.isEmpty()) {
				Log.e("XXX", "-----WeatherApp--------isEmpty------");
				WeatherDataUtil.getInstance().setDefaultState(this, WeatherDataUtil.DEFAULT_STATE_NEED_CHECK);
			} else {
				Log.e("XXX", "-----WeatherApp----NO----isEmpty------");
				mModel.addDefaultData();
			}
		}
		
	}
	
}







































