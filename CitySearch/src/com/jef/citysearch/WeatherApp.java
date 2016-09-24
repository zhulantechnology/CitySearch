package com.jef.citysearch;

import android.app.Application;
import android.util.Log;

public class WeatherApp extends Application {
	
	public static WeatherModel mModel = null;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.e("XXX", "WeatherApp------------onCreate");
		mModel = WeatherModel.getInstance(getApplicationContext());
		mModel.init();
		
	}
	
	

}
