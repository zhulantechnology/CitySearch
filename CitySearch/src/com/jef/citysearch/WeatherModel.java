package com.jef.citysearch;

import java.util.ArrayList;
import java.util.List;

import android.R.bool;
import android.content.Context;
import android.util.Log;

public class WeatherModel {
	
	private Context mApp;
	private boolean inited = false;
	private InternetWorker mInternetWorker;
	
	private static WeatherModel INSTANCE;
	
	private List<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	
	public static WeatherModel  getInstance(Context app) {
		if(null == INSTANCE) {
			INSTANCE = new WeatherModel(app);
		}
		return INSTANCE;
	}
	
	private WeatherModel(Context app) {
		mApp = app;
		mInternetWorker = InternetWorker.getInstance(mApp);

		mInternetWorker.setCallBacks(mInternetCallBacks);
	}
	
	private InternetWorker.CallBacks mInternetCallBacks = new InternetWorker.CallBacks() {

		@Override
		public void queryCityFinished() {
			if (mCityInfoUpdatedListener != null) {
				mCityInfoUpdatedListener.updated();
			}
			
		}
		
	};
	
	public void stopQueryCity() {
		mInternetWorker.stopQueryCity();
	}
	
	public boolean addWeatherByCity(CityInfo info, boolean isGps) {
		WeatherInfo weatherInfo = new WeatherInfo();
		weatherInfo.setWoeid(info.getWoeid());
		weatherInfo.setName(info.getName());
		weatherInfo.setGps(isGps);
		return mInternetWorker.queryWeather(weatherInfo);
	}
	
	public void init() {
/*		Log.d(TAG, "init");
		loadWeatherInfos();
		loadGpsCityInfos();
		mInternetWorker.init();*/
		inited = true;
	}
	
	public List<CityInfo> getCityInfos() {
		return mCityInfos;
	}

	interface OnCityInfoUpdatedListener {
		void updated();
	}
	
	private OnCityInfoUpdatedListener mCityInfoUpdatedListener;
	public void setOnCityInfoUpdatedListener(OnCityInfoUpdatedListener listener) {
		mCityInfoUpdatedListener = listener;
	}
	
	public boolean getCityInfosByNameFromInternet(String cityName) {
		mCityInfos.clear();
		return mInternetWorker.queryCity(cityName,mCityInfos);
	}
	
	
}



























