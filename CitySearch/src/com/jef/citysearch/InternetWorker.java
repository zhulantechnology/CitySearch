package com.jef.citysearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.CalendarContract.Instances;
import android.util.Log;

public class InternetWorker {
	private static final String URL_QUERY_WEATHER_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+weather.forecast+where+woeid=";
	private static final String URL_QUERY_WEATHER_PART2 = "+and+u='c'";
	
	private static final String URL_QUERY_CITY_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+text='";
	//private static final String URL_QUERY_CITY_PART2 ="*'+and+lang='en-US'";
	private static final String URL_QUERY_CITY_PART2 ="*'+and+lang='zh-CN'";
	private Context mContext;
	
	private QueryWeatherTask mQueryWeatherTask;
	
	private List<CityInfo> locationCityInfos;
			
	enum State{
		IDLE, WORK_WEATHER, WORK_CITY, WORK_LOCATION
	};
	
	enum QueryWeatherType {
		CURRENT, ALL, ADD_NEW
	}
	
	private QueryWeatherType mQueryWeatherType = QueryWeatherType.CURRENT;
	private List<WeatherInfo> mWeatherInfoList;
	private WeatherInfo tempWeatherInfo;

	private State mState = State.IDLE;
	private QueryCityTask mQueryCityTask;
	private static InternetWorker INSTANCE;
	
	private String locationCityName = null;
	
	private int updateWeatherCount = 0;
	private int updateFinishedWeatherCount = 0;
	
	private boolean isNeedCheck = true;
	
	private AMapLocationClient locationClient = null;
	private AMapLocationClientOption locationOption = new AMapLocationClientOption();
	
	private InternetWorker(Context context) {
		mContext = context;
	}
	
	private CallBacks mCallBacks;
	
	public interface CallBacks {
		void queryCityFinished();
		void refreshWeatherFinished(WeatherInfo weatherInfo);
		void queryAddWeatherFinished(WeatherInfo weatherInfo, boolean isLocation);
		void refreshAllWeatherFinished();

		void queryLocationCityFinished();
	}
	
	public void setCallBacks(CallBacks callBacks) {
		mCallBacks = callBacks;
	}
	
	
	
	public String getLocationCityName() {
		return locationCityName;
	}



	public void setLocationCityName(String locationCityName) {
		this.locationCityName = locationCityName;
	}



	public static InternetWorker getInstance(Context context) {
		if (null == INSTANCE) {
			INSTANCE = new InternetWorker(context);
		}
		
		return INSTANCE;
	}
	
	public boolean queryCity(String name, List<CityInfo> cityInfos, boolean isLocation) {
		
		State tempState = mState;
		if (mState == State.IDLE) {
		//if (true) {
			if (isLocation) {
				mState = State.WORK_LOCATION;
			} else {
				mState = State.WORK_CITY;
			}
			
			if (null != mQueryCityTask && mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryCityTask.cancel(true);
			}
			Log.e("XXX", "WANGJUN-----queryCity------");
			mQueryCityTask = new QueryCityTask(cityInfos);
			mQueryCityTask.execute(name);
			return true;
		} else {
			return false;
		}
		
	}
	
	public void stopQueryCity() {
		if (null != mQueryCityTask && 
				mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
			mQueryCityTask.cancel(true);
		}
		if (mState == State.WORK_CITY) {
			mState = State.IDLE;
		}
	}
	
	class QueryCityTask extends AsyncTask<String, Void, Void> {
		private List<CityInfo> mCityInfos;
	
		public QueryCityTask(List<CityInfo> cityInfos) {
			mCityInfos = cityInfos;
		}
	
		@Override
		protected Void doInBackground(String... params) {
			String url =  URL_QUERY_CITY_PART1 + params[0] + URL_QUERY_CITY_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseCity(content,mCityInfos);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			
			super.onPostExecute(result);
			if (null != mCallBacks) {
				if (mState == State.WORK_LOCATION) {
					mState = State.IDLE;
					mCallBacks.queryLocationCityFinished();
				} else {
					mState = State.IDLE;
					mCallBacks.queryCityFinished();
				}
				
			}
		}
		
		
	}
	
	private void parseCity(String content, List<CityInfo> mCityInfos) {
		if (null == content || content.isEmpty()) {
			return;
		}
		mCityInfos.clear();
		SAXParserFactory mSaxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSaxParser = mSaxParserFactory.newSAXParser();
			XMLReader mXmlReader = mSaxParser.getXMLReader();
			CityNameXMLParser handler = new CityNameXMLParser(mCityInfos);
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean queryWeather(WeatherInfo weatherInfo, boolean isLocation) {
		if (mState == State.IDLE) {
		//if (true) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ADD_NEW;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask 
					&& mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo, isLocation);
			mQueryWeatherTask.execute();
			return true;
		} else {
			return false;
		}
	}
	
	class QueryWeatherTask extends AsyncTask<Void, Void, Void> {
		private WeatherInfo mWeatherInfo;
		private boolean mLocation;
		
		public QueryWeatherTask(WeatherInfo weatherInfo, boolean isLocation) {
			mWeatherInfo = weatherInfo;
			mLocation = isLocation;
		}
		@Override
		protected Void doInBackground(Void... params) {
			String url = URL_QUERY_WEATHER_PART1 + mWeatherInfo.getWoeid()
					+ URL_QUERY_WEATHER_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseWeather(content, mWeatherInfo);
			
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			mState = State.IDLE;
			super.onPostExecute(result);
			if (tempWeatherInfo.getForecasts().size() < 5) {
				//do nothing
			} else {
				tempWeatherInfo.setName(mWeatherInfo.getName());
				mWeatherInfo.copyInfo(tempWeatherInfo);

			}
			updateFinishedWeatherCount++;
			if (updateFinishedWeatherCount == updateWeatherCount) {
				
				if (QueryWeatherType.ALL == mQueryWeatherType) {
					mCallBacks.refreshAllWeatherFinished();
				} else if (QueryWeatherType.CURRENT == mQueryWeatherType) {
					mCallBacks.refreshWeatherFinished(mWeatherInfo);
					
				} else if (QueryWeatherType.ADD_NEW == mQueryWeatherType){

					if (mWeatherInfo.isGps()) {

						if (mCallBacks != null) {
							//mCallBacks.queryAddGpsWeatherFinished(mWeatherInfo);
						}
					} else {
						if (mCallBacks != null) {
							mCallBacks.queryAddWeatherFinished(mWeatherInfo, mLocation);
						}
					}
				}
				
				
			}
			
		}
		
		
		
	}
	
	private void parseWeather(String content, WeatherInfo mwWeatherInfo) {
		if (content == null || content.isEmpty()) {
			tempWeatherInfo = new WeatherInfo();
			tempWeatherInfo.getForecasts().clear();
			return;
		}
		
		tempWeatherInfo = new WeatherInfo();
		
		SAXParserFactory mSaxParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSaxParser = mSaxParserFactory.newSAXParser();
			XMLReader mXmlReader = mSaxParser.getXMLReader();
			WeatherXMLParser handler = new WeatherXMLParser(mContext, 
					tempWeatherInfo, mwWeatherInfo.getWoeid());
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			try {
				mXmlReader.parse(inputSource);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ParserConfigurationException e) {
			// TODO: handle exception
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void init() {
		/*
		initLocation();
		startLocation();
		if(getLocationCityName() != null) {
			queryCity(getLocationCityName(),locationCityInfos);

		}*/
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
	}
	
	public boolean updateWeather(WeatherInfo weatherInfo) {

		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.CURRENT;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo, false);
			mQueryWeatherTask.execute();
			return true;
		} else {
			return false;
		}
	}
	
	public boolean updateWeather() {
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ALL;
			updateWeatherCount = mWeatherInfoList.size();
			updateFinishedWeatherCount = 0;
			if (updateWeatherCount == 0) {
				Intent intent = new Intent(WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
				mContext.sendBroadcast(intent);
			} else {
				for(int i=0; i<updateWeatherCount; i++) {
					new QueryWeatherTask(mWeatherInfoList.get(i), false).execute();
				}
			}
			
			return true;
		} else {
			return false;
		}
	}
	

}






































