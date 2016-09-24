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
import android.os.AsyncTask;
import android.provider.CalendarContract.Instances;

public class InternetWorker {
	
	private static final String URL_QUERY_CITY_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+text='";
	private static final String URL_QUERY_CITY_PART2 ="*'+and+lang='en-US'";
	private Context mContext;
			
	enum State{
		IDLE, WORK_WEATHER, WORK_CITY, WORK_LOCATION
	};

	private State mState = State.IDLE;
	private QueryCityTask mQueryCityTask;
	private static InternetWorker INSTANCE;
	
	private InternetWorker(Context context) {
		mContext = context;
	}
	
	private CallBacks mCallBacks;
	
	public interface CallBacks {
		void queryCityFinished();
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
	
}






































