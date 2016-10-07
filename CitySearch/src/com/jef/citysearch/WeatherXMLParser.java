package com.jef.citysearch;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jef.citysearch.WeatherInfo.Forecast;

import android.content.Context;
import android.util.Log;

public class WeatherXMLParser extends DefaultHandler {
	
	private final static String TAG_CONDITION = "condition";
	private final static String TAG_FORCAST = "forecast";
	private final static String QNAME_CODE = "code";
	private final static String QNAME_DATE = "date";
	private final static String QNAME_DAY = "day";
	private final static String QNAME_TMP = "temp";
	private final static String QNAME_HIGH = "high";
	private final static String QNAME_LOW = "low";
	private final static String QNAME_TEXT = "text";
	
	private Context mContext;
	private WeatherInfo mWeatherInfo;
	private Forecast forecast;
	private String woeid;
	
	public WeatherXMLParser(Context context, WeatherInfo info, String woeid) {
		mContext = context;
		mWeatherInfo = info;
		this.woeid = woeid;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		super.characters(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		int weatherCode;
		int textRes;
		
		if (null == mWeatherInfo.getCondition().getCode()) {
			Log.e("XXX", "endDocument-code NULL");
		} else {
			weatherCode = Integer.valueOf(mWeatherInfo.getCondition().getCode());
			textRes = WeatherDataUtil.getInstance().getWeatherTextResByCode(weatherCode);
			if (WeatherDataUtil.INVALID_WEATHER_RESOURCE != textRes) {
				mWeatherInfo.getCondition().setText(mContext.getResources().getString(textRes));
			}
			
			for (WeatherInfo.Forecast forecast : mWeatherInfo.getForecasts()) {
				weatherCode = Integer.valueOf(forecast.getCode());
				textRes = WeatherDataUtil.getInstance().getWeatherTextResByCode(weatherCode);
				if (WeatherDataUtil.INVALID_WEATHER_RESOURCE != textRes) {
					forecast.setText(mContext.getResources().getString(textRes));
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, qName);
	}

	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.startDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, qName, attributes);
		
		if (TAG_CONDITION.equals(localName)) {
			mWeatherInfo.setWoeid(woeid);
			for (int i = 0; i < attributes.getLength(); i++) {
				String qn = attributes.getQName(i);
				
		//Log.e("XXX","TAG_CONDITION-----------attributes.getQName(i)----"+ attributes.getQName(i));
		//Log.e("XXX","TAG_CONDITION-----------attributes.getValue(i)----"+ attributes.getValue(i));
				if (QNAME_CODE.equals(qn)) {
					mWeatherInfo.getCondition().setCode(attributes.getValue(i));
				} else if (QNAME_DATE.equals(qn)) {
					mWeatherInfo.getCondition().setDate(attributes.getValue(i));
				} else if (QNAME_TMP.equals(qn)) {
					mWeatherInfo.getCondition().setTemp(attributes.getValue(i));
				} else if (QNAME_TEXT.equals(qn)) {
					mWeatherInfo.getCondition().setText(attributes.getValue(i));
				}
			}
		} else if (TAG_FORCAST.equals(localName)) {
			forecast = mWeatherInfo.new Forecast();
			for (int i = 0; i < attributes.getLength(); i++) {
				String qn = attributes.getQName(i);
		//Log.e("XXX","TAG_FORCAST-----------attributes.getQName(i)----"+ attributes.getQName(i));
		//Log.e("XXX","TAG_FORCAST-----------attributes.getValue(i)----"+ attributes.getValue(i));
				
				if (QNAME_CODE.equals(qn)) {
					forecast.setCode(attributes.getValue(i));
				} else if (QNAME_DAY.equals(qn)) {
					forecast.setDay(attributes.getValue(i));
				} else if (QNAME_HIGH.equals(qn)) {
					forecast.setHigh(attributes.getValue(i));
				} else if (QNAME_LOW.equals(qn)) {
					forecast.setLow(attributes.getValue(i));
				} else if (QNAME_TEXT.equals(qn)) {
					forecast.setText(attributes.getValue(i));
				}
			}
			mWeatherInfo.getForecasts().add(forecast);
		}
	}
	
	
	
	
/*  the original code
 * 
<link>
http://us.rd.yahoo.com/dailynews/rss/weather/Country__Country/*https://weather.yahoo.com/country/state/city-2151849/
</link>
<pubDate>Sun, 02 Oct 2016 09:00 AM CST</pubDate>
<yweather:condition xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="30" date="Sun, 02 Oct 2016 09:00 AM CST" temp="27" text="Partly Cloudy"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="47" date="02 Oct 2016" day="Sun" high="30" low="23" text="Scattered Thunderstorms"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="39" date="03 Oct 2016" day="Mon" high="25" low="21" text="Scattered Showers"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="30" date="04 Oct 2016" day="Tue" high="26" low="20" text="Partly Cloudy"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="30" date="05 Oct 2016" day="Wed" high="26" low="19" text="Partly Cloudy"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="30" date="06 Oct 2016" day="Thu" high="25" low="22" text="Partly Cloudy"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="47" date="07 Oct 2016" day="Fri" high="27" low="22" text="Scattered Thunderstorms"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="4" date="08 Oct 2016" day="Sat" high="26" low="23" text="Thunderstorms"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="47" date="09 Oct 2016" day="Sun" high="22" low="17" text="Scattered Thunderstorms"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="26" date="10 Oct 2016" day="Mon" high="19" low="17" text="Cloudy"/>
<yweather:forecast xmlns:yweather="http://xml.weather.yahoo.com/ns/rss/1.0" code="26" date="11 Oct 2016" day="Tue" high="20" low="17" text="Cloudy"/>
 * 
 */
}
