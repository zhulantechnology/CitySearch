package com.jef.citysearch;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.R.integer;
import android.content.Context;
import android.content.SharedPreferences;

public class WeatherDataUtil {
	public static final int INVALID_WEATHER_RESOURCE = -1;
	
	private static WeatherDataUtil mWeatherDataUtil;
	public final static String WEATHER_SP = "gweather";
	public static final String DEFAULT_WOEID_GPS = "woeid_gps";
	
	public static final int DEFAULT_STATE_NEED_CHECK = 0;
	public static final int DEFAULT_STATE_NEED_UPDATE = 1;
	public static final int DEFAULT_STATE_FINISHED = 2;

		
	public static final int CODE_THUNDERSTORMS = 4;
	public static final int CODE_RAIN_AND_SNOW = 5;
	public static final int CODE_SHOWERS = 11;
	public static final int CODE_RAIN = 12;
	public static final int CODE_SNOW_SHOWERS = 14;
	public static final int CODE_BREEZY = 23;//有微风
	public static final int CODE_WINDY = 24;//有风
	public static final int CODE_CLOUDY = 26;
	public static final int CODE_MOSTLY_CLOUDY = 28;
	public static final int CODE_PARTLY_CLOUDY = 29;
	public static final int CODE_PARTLY_CLOUDY_2 = 30;
	public static final int CODE_CLEAR = 31;
	public static final int CODE_SUNNY = 32;
	public static final int CODE_MOSTLY_CLEAR = 33;
	public static final int CODE_MOSTLY_SUNNY = 34;
	public static final int CODE_SCATTERED_SHOWERS = 39;
	//public static final int CODE_SCATTERED_SHOWERS = 45;//零星阵雨
	public static final int CODE_SCATTERED_THUNDERSTORMS = 47;//零星雷雨
	
	private WeatherDataUtil() {}
	
	public static WeatherDataUtil getInstance() {
		if (null == mWeatherDataUtil) {
			mWeatherDataUtil = new WeatherDataUtil();
		}
		return mWeatherDataUtil;
	}
	
	public void updateDefaultCityWoeid(Context context, String woeid) {
		SharedPreferences.Editor editor = context.getSharedPreferences(
										WEATHER_SP, Context.MODE_PRIVATE).edit();
		editor.putString("woeid", woeid);
		editor.commit();
	}
	
	public int getWeatherTextResByCode(int code) {
		switch (code) {
		case CODE_CLOUDY:
		case CODE_MOSTLY_CLOUDY:
		case CODE_PARTLY_CLOUDY:
		case CODE_PARTLY_CLOUDY_2:
			return R.string.weather_cloudy;
		case CODE_BREEZY:
			return R.string.weather_breezy;
		case CODE_WINDY:
			return R.string.weather_windy;
		case CODE_SUNNY:
		case CODE_MOSTLY_SUNNY:
			return R.string.weather_sunny;
		case CODE_CLEAR:
		case CODE_MOSTLY_CLEAR:
			return R.string.weather_clear;
		case CODE_SCATTERED_SHOWERS:
		case CODE_SHOWERS:
			return R.string.weather_showers;
		case CODE_SNOW_SHOWERS:
			return R.string.weather_snow_showers;
		case CODE_THUNDERSTORMS:
		case CODE_SCATTERED_THUNDERSTORMS:
			return R.string.weather_thunderstorms;
		case CODE_RAIN:
			return R.string.weather_rain;
		case CODE_RAIN_AND_SNOW:
			return R.string.weather_rain_and_snow;
		default:
			return INVALID_WEATHER_RESOURCE;
		}

	}
	
	public int getDefaultState(Context context) {
		SharedPreferences sp = context.getSharedPreferences(WEATHER_SP, Context.MODE_PRIVATE);
		return sp.getInt("default_state", DEFAULT_STATE_NEED_CHECK);
	}
	
	public void setDefaultState(Context context, int state) {
		SharedPreferences.Editor editor = context.getSharedPreferences(WEATHER_SP,
					Context.MODE_PRIVATE).edit();
		editor.putInt("default_state", state);
		editor.commit();
	}
	
	public long getRefreshTime(Context context) {
		long defaultRefreshTime = getDefaultRefreshTime(context);
		SharedPreferences sp = context.getSharedPreferences(WEATHER_SP, context.MODE_PRIVATE);
		return sp.getLong("refresh_time", defaultRefreshTime);
	}
	
	private long getDefaultRefreshTime(Context context) {
		String defaultTimeString = context.getResources().getString(R.string.default_refresh_time);
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(new SimpleDateFormat("yyyyMMddHHmmss").parse(defaultTimeString));
		} catch (Exception e) {
			return 01;
		}
		return calendar.getTimeInMillis();
	}
	
}







































