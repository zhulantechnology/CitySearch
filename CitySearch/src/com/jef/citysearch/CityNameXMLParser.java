package com.jef.citysearch;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Notification.Style;

public class CityNameXMLParser extends DefaultHandler {
	
	private final static String TAG_CITY = "place";
	private final static String TAG_WOEID = "woeid";
	private final static String TAG_COUNTRY = "country";

	private final static String TAG_ADMIN1 = "admin1";

	private final static String TAG_ADMIN2 = "admin2";

	private final static String TAG_ADMIN3 = "admin3";
	private final static String TAG_NAME = "name";

	private final static String QNAME_TYPE = "type";
	
	private final static String TAG_SOUTHWEST = "southWest";
	private final static String TAG_NORTHEAST = "northEast";
	private final static String TAG_LAT = "latitude";
	private final static String TAG_LON = "longitude";

	private List<CityInfo> mCityInfoLit;
	private CityInfo cityInfo;
	private String content;
	private boolean skip = false;
	
	private boolean isSouthWest = false;
	private boolean isNorthEast = false;
	
	public CityNameXMLParser(List<CityInfo> cityInfos) {
		mCityInfoLit = cityInfos;
	}
	
	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		super.characters(ch, start, length);
	}

}
