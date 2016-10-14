package com.jef.citysearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;
import com.jef.citysearch.WeatherInfo.Forecast;

import android.R.bool;
import android.R.integer;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.session.PlaybackState.CustomAction;
import android.net.Uri;
import android.util.Log;

public class WeatherModel {
	
	private Context mApp;
	private boolean inited = false;
	private InternetWorker mInternetWorker;
	
	private static WeatherModel INSTANCE;
	
	private static final String URI_GWEATHER = "content://com.jef.citysearch.weather/gweather";
	private static final String URI_GCITY = "content://com.jef.citysearch.weather/gcity";
	
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

		@Override
		public void refreshWeatherFinished(WeatherInfo weatherInfo) {
		
			updateWeatherInfo(weatherInfo);
			Intent intent = new Intent(WeatherAction.ACTION_WEATHER_REFRESHED);
			mApp.sendBroadcast(intent);
			
		}

		@Override
		public void queryAddWeatherFinished(WeatherInfo weatherInfo) {
			if (weatherInfo.getForecasts().size() < MainActivity.FORECAST_DAY) {
			} else {
				if(addWeatherInfoToDb(weatherInfo)) {
					mWeatherInfoList.add(weatherInfo);
				}
			}
			Intent intent = new Intent(WeatherAction.ACTION_ADD_WEATHER_FINISH);
			mApp.sendBroadcast(intent);
			//setAutoRefreshAlarm(mApp);
		}

		@Override
		public void refreshAllWeatherFinished() {
		Log.e("XXX", "wangjun-------refreshAllWeatherFinished----0000");
			for(WeatherInfo info:mWeatherInfoList) {
				Log.e("XXX", "wangjun-------refreshAllWeatherFinished----1111");
				updateWeatherInfo(info);
			}
			WeatherDataUtil.getInstance().setRefreshTime(mApp, System.currentTimeMillis());
			Intent intent = new Intent(WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
			mApp.sendBroadcast(intent);
			Log.e("XXX", "wangjun-------refreshAllWeatherFinished----2222");
			//setAutoRefreshAlarm(mApp);
		}
		
	};
	
	public boolean addWeatherInfoToDb(WeatherInfo newInfo) {
		boolean isNew = true;
		
		for(WeatherInfo info:mWeatherInfoList) {
			if (newInfo.isGps() && info.isGps()) {
				//GPS info exist
				isNew = false;
				info.copyInfo(newInfo);
				updateWeatherInfo(info);
				return isNew;
			} else if (!newInfo.isGps() && !info.isGps()
					&& (newInfo.getWoeid().equals(info.getWoeid()))) {
				//Normal info exist
				isNew = false;
				info.copyInfo(newInfo);
				updateWeatherInfo(info);
				return isNew;
			}
		}
		
		//Add to DB
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GWEATHER);
		ContentValues values = new ContentValues();
		values.put(WeatherProvider.INDEX,
				WeatherProvider.CONDITION_INDEX);
		values.put(WeatherProvider.WOEID, newInfo.getWoeid());
		values.put(WeatherProvider.NAME, newInfo.getName());
		values.put(WeatherProvider.CODE, newInfo.getCondition().getCode());
		values.put(WeatherProvider.DATE, newInfo.getCondition().getDate());
		values.put(WeatherProvider.TEMP, newInfo.getCondition().getTemp());
		values.put(WeatherProvider.TEXT, newInfo.getCondition().getText());
		values.put(WeatherProvider.UPDATE_TIME, newInfo.getUpdateTime());
		values.put(WeatherProvider.GPS, newInfo.isGps()?WeatherProvider.FLAG_GPS:0);
		mContentResolver.insert(uri, values);
		
		for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
			values = new ContentValues();
			values.put(WeatherProvider.INDEX, i);
			values.put(WeatherProvider.WOEID, newInfo.getWoeid());
			values.put(WeatherProvider.CODE, newInfo.getForecasts()
					.get(i).getCode());
			values.put(WeatherProvider.DATE, newInfo.getForecasts()
					.get(i).getDate());
			values.put(WeatherProvider.DAY, newInfo.getForecasts().get(i)
					.getDay());
			values.put(WeatherProvider.HIGH, newInfo.getForecasts()
					.get(i).getHigh());
			values.put(WeatherProvider.LOW, newInfo.getForecasts().get(i)
					.getLow());
			values.put(WeatherProvider.TEXT, newInfo.getForecasts()
					.get(i).getText());
			values.put(WeatherProvider.GPS, newInfo.isGps()?WeatherProvider.FLAG_GPS:0);
			mContentResolver.insert(uri, values);
		}
		
		return isNew;
	}
	
	public boolean isInited() {
		return inited;
	}
	
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

		loadWeatherInfos();
		mInternetWorker.init();
	/*	loadGpsCityInfos();
		*/
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
		if (!isInited() || mWeatherInfoList.isEmpty()) {
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
		
		WeatherInfo info;
		String woeid = "";
		if (cursor != null) {
			while (cursor.moveToNext()) {
				int cursorIndex = cursor.getColumnIndex(WeatherProvider.WOEID);
				woeid = cursor.getString(cursorIndex);
				info = new WeatherInfo();
				info.setWoeid(woeid);
				cursorIndex = cursor.getColumnIndex(WeatherProvider.GPS);
				info.setGps(WeatherProvider.FLAG_GPS == cursor.getInt(cursorIndex));
				mWeatherInfoList.add(info);
			}
			cursor.close();
		}
		
		if (mWeatherInfoList.size() == 0) {
			return;
		}
		
		for (WeatherInfo mInfo : mWeatherInfoList) {
			if (mInfo.isGps()) {
				cursor = mContentResolver.query(uri, null,
						WeatherProvider.GPS + "=?",
						new String[] {String.valueOf(WeatherProvider.FLAG_GPS)},
						WeatherProvider.INDEX);
			} else {
				cursor = mContentResolver.query(uri, null,
						"woeid=? AND " + WeatherProvider.GPS + "!=?",
						new String[] {mInfo.getWoeid(), String.valueOf(WeatherProvider.FLAG_GPS)},
						WeatherProvider.INDEX);
			}
			if (cursor != null) {
				while (cursor.moveToNext()) {
					int cursorIndex = cursor.getColumnIndex(WeatherProvider.INDEX);
					int index = cursor.getInt(cursorIndex);
					
					if (WeatherProvider.CONDITION_INDEX == index) {
					
						mInfo.getCondition().setIndex(index);
						cursorIndex = cursor.getColumnIndex(WeatherProvider.NAME);
						mInfo.setName(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.CODE);
						mInfo.getCondition().setCode(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.DATE);
						mInfo.getCondition().setDate(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.TEMP);
						mInfo.getCondition().setTemp(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.TEXT);
						mInfo.getCondition().setText(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.GPS);
						mInfo.setGps(WeatherProvider.FLAG_GPS == cursor.getInt(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.UPDATE_TIME);
						mInfo.setUpdateTime(cursor.getLong(cursorIndex));
					} else {
						forecast = mInfo.new Forecast();
						forecast.setIndex(index);
						cursorIndex = cursor.getColumnIndex(WeatherProvider.CODE);
						forecast.setCode(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.DATE);
						forecast.setDate(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.TEXT);
						forecast.setText(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.DAY);
						forecast.setDay(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.HIGH);
						forecast.setHigh(cursor.getString(cursorIndex));
						
						cursorIndex = cursor.getColumnIndex(WeatherProvider.LOW);
						forecast.setLow(cursor.getString(cursorIndex));
						
						mInfo.getForecasts().add(forecast);
					}
				}
				cursor.close();
			}
			}
		}
	
	public boolean refreshWeather(WeatherInfo weatherInfo) {
		return mInternetWorker.updateWeather(weatherInfo);
	}
	
	public void updateWeatherInfo(WeatherInfo info) {
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
		mContentResolver.update(
				uri,
				values,
				WeatherProvider.INDEX + " = ? AND "
						+ WeatherProvider.GPS + " = ? AND "
						+ WeatherProvider.WOEID + " = ?",
				new String[] { Integer.toString(WeatherProvider.CONDITION_INDEX),
						String.valueOf(info.isGps()?WeatherProvider.FLAG_GPS:0),
						info.getWoeid() });
		
		for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
			values = new ContentValues();
			values.put(WeatherProvider.INDEX, i);
			values.put(WeatherProvider.WOEID, info.getWoeid());
			values.put(WeatherProvider.CODE, info.getForecasts()
					.get(i).getCode());
			values.put(WeatherProvider.DATE, info.getForecasts()
					.get(i).getDate());
			values.put(WeatherProvider.DAY, info.getForecasts().get(i)
					.getDay());
			values.put(WeatherProvider.HIGH, info.getForecasts()
					.get(i).getHigh());
			values.put(WeatherProvider.LOW, info.getForecasts().get(i)
					.getLow());
			values.put(WeatherProvider.TEXT, info.getForecasts()
					.get(i).getText());
			values.put(WeatherProvider.GPS, info.isGps()?WeatherProvider.FLAG_GPS:0);
			mContentResolver.update(
					uri,
					values,
					WeatherProvider.INDEX + " = ? AND "
							+ WeatherProvider.GPS + " = ? AND "
							+ WeatherProvider.WOEID + " = ?",
					new String[] { Integer.toString(i),
							String.valueOf(info.isGps()?WeatherProvider.FLAG_GPS:0),
							info.getWoeid() });
		}
	}
	
	public boolean refreshAllWeather() {
		return mInternetWorker.updateWeather();
	}
	
	
}



























