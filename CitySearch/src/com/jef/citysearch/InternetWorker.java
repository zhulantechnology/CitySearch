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
	
	private int updateWeatherCount = 0;
	private int updateFinishedWeatherCount = 0;
	
	private InternetWorker(Context context) {
		mContext = context;
	}
	
	private CallBacks mCallBacks;
	
	public interface CallBacks {
		void queryCityFinished();
		void refreshWeatherFinished(WeatherInfo weatherInfo);
		void queryAddWeatherFinished(WeatherInfo weatherInfo);
		void refreshAllWeatherFinished();
	}
	
	public void setCallBacks(CallBacks callBacks) {
		mCallBacks = callBacks;
	}
	
	public static InternetWorker getInstance(Context context) {
		if (null == INSTANCE) {
			INSTANCE = new InternetWorker(context);
		}
		
		return INSTANCE;
	}
	
	public boolean queryCity(String name, List<CityInfo> cityInfos) {
		if (mState == State.IDLE) {
			mState = State.WORK_CITY;
			if (null != mQueryCityTask && mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryCityTask.cancel(true);
			}
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
				mCallBacks.queryCityFinished();
			}
			mState = State.IDLE;
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
	
	public boolean queryWeather(WeatherInfo weatherInfo) {
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ADD_NEW;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask 
					&& mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo);
			mQueryWeatherTask.execute();
			return true;
		} else {
			return false;
		}
	}
	
	class QueryWeatherTask extends AsyncTask<Void, Void, Void> {
		private WeatherInfo mWeatherInfo;
		
		public QueryWeatherTask(WeatherInfo weatherInfo) {
			mWeatherInfo = weatherInfo;
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
			super.onPostExecute(result);
			
			if (tempWeatherInfo.getForecasts().size() < 5) {
				//do nothing
			} else {
				tempWeatherInfo.setName(mWeatherInfo.getName());
				mWeatherInfo.copyInfo(tempWeatherInfo);

			}
			
			updateFinishedWeatherCount++;
			Log.e("XXX", "wangjun-------updateFinishedWeatherCount----" + updateFinishedWeatherCount);
			Log.e("XXX", "wangjun-------updateWeatherCount----" + updateWeatherCount);
			if (updateFinishedWeatherCount == updateWeatherCount) {
				
				Log.e("XXX", "wangjun-------mQueryWeatherType----" + mQueryWeatherType);
				if (QueryWeatherType.ALL == mQueryWeatherType) {
					Log.e("XXX", "wangjun-------refresh finish----");
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
							mCallBacks.queryAddWeatherFinished(mWeatherInfo);
						}
					}
				}
				
				mState = State.IDLE;
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
			
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo);
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
					new QueryWeatherTask(mWeatherInfoList.get(i)).execute();
				}
			}
			
			return true;
		} else {
			return false;
		}
	}
}






































