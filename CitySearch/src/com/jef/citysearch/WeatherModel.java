package com.jef.citysearch;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import com.jef.citysearch.WeatherInfo.Forecast;

import android.R.bool;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class WeatherModel {
	
	private Context mApp;
	private boolean inited = false;
	private InternetWorker mInternetWorker;
	
	private static WeatherModel INSTANCE;
	
	private static final String URI_GWEATHER = "content://com.gweather.app.weather/gweather";
	private static final String URI_GCITY = "content://com.gweather.app.weather/gcity";
	
	private List<WeatherInfo> mWeatherInfoList = new ArrayList<WeatherInfo>();
	private List<CityInfo> mGpsCityInfoList = new ArrayList<CityInfo>();
	private List<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	
	private static final int GPS_CITY_COUNT_MAX = 32;
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
		//Log.e("XXX","addWeatherByCity-----info.getWoeid()---" + info.getWoeid());
		//Log.e("XXX","addWeatherByCity-----info.getName()---" + info.getName());
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
	
	public void addDefaultData() {
		WeatherInfo info = new WeatherInfo();
		info.setWoeid(mApp.getResources().getString(R.string.default_woeid));
		info.setName(mApp.getResources().getString(R.string.default_city_name));
		info.setGps(false);
		info.setUpdateTime(WeatherDataUtil.getInstance().getRefreshTime(mApp));
		info.getCondition().setCode("32");
		info.getCondition().setDate("Fri, 1 Jan 2016 11:00 AM PKT");
		info.getCondition().setIndex(WeatherProvider.CONDITION_INDEX);
		info.getCondition().setTemp("22");
		info.getCondition().setText("sunny");
		
		info.getForecasts().clear();
		for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
			WeatherInfo.Forecast forecast = info.new Forecast();
			forecast.setCode("32");
			switch (i) {
			case 0:
				forecast.setDate("1 Jan 2016");
				forecast.setDay("Fri");
				break;
			case 1:
				forecast.setDate("2 Jan 2016");
				forecast.setDay("Sat");
				break;
			case 2:
				forecast.setDate("3 Jan 2016");
				forecast.setDay("Sun");
				break;
			case 3:
				forecast.setDate("4 Jan 2016");
				forecast.setDay("Mon");
				break;
			case 4:
				forecast.setDate("5 Jan 2016");
				forecast.setDay("Tue");
				break;
			}
			
			forecast.setHigh("26");
			forecast.setIndex(i);
			forecast.setLow("20");
			forecast.setText("sunny");
			info.getForecasts().add(forecast);
		}
		
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GWEATHER);
		ContentValues values = new ContentValues();
		values.put(WeatherProvider.INDEX,
				WeatherProvider.CONDITION_INDEX);
		values.put(WeatherProvider.WOEID, info.getWoeid());
		values.put(WeatherProvider.NAME, info.getName());
		values.put(WeatherProvider.CODE, info.getCondition().getCode());
		values.put(WeatherProvider.DATE, info.getCondition().getDate());
		values.put(WeatherProvider.TEMP, info.getCondition().getTemp());
		values.put(WeatherProvider.TEXT, info.getCondition().getText());
		values.put(WeatherProvider.UPDATE_TIME, info.getUpdateTime());
		values.put(WeatherProvider.GPS, info.isGps()?WeatherProvider.FLAG_GPS:0);
		mContentResolver.insert(uri, values);
		
		for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
			values = new ContentValues();
			values.put(WeatherProvider.INDEX, i);
			values.put(WeatherProvider.WOEID, info.getWoeid());
			values.put(WeatherProvider.CODE, info.getForecasts().get(i).getCode());
			values.put(WeatherProvider.DATE, info.getForecasts().get(i).getDate());
			values.put(WeatherProvider.DAY, info.getForecasts().get(i).getDay());
			values.put(WeatherProvider.HIGH, info.getForecasts().get(i).getHigh());
			values.put(WeatherProvider.LOW, info.getForecasts().get(i).getLow());
			values.put(WeatherProvider.TEXT, info.getForecasts().get(i).getText());
			values.put(WeatherProvider.GPS, info.isGps()?WeatherProvider.FLAG_GPS:0);
			mContentResolver.insert(uri, values);	
		}
		
		mWeatherInfoList.add(info);
		WeatherDataUtil.getInstance().updateDefaultCityWoeid(mApp, info.getWoeid());
		WeatherDataUtil.getInstance().setDefaultState(mApp, WeatherDataUtil.DEFAULT_STATE_NEED_UPDATE);
	}
	
	public List<WeatherInfo> getWeatherInfos() {
		//if (!isInited() || mWeatherInfoList.isEmpty()) {
		if (mWeatherInfoList.isEmpty()) {
			loadWeatherInfos();
		}
		return mWeatherInfoList;
	}
	
	private void loadWeatherInfos() {
		WeatherInfo.Forecast forecast = null;
		mWeatherInfoList.clear();
		
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GWEATHER);
		Cursor cursor = mContentResolver.query(uri, null, "gIndex=?",
						new String[] { Integer.toString(WeatherProvider.CONDITION_INDEX)}, null);
	}
	
}



























