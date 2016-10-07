package com.jef.citysearch;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private View mainContentView;
	private ScrollControlLayout weatherInfoMainContainer;
	private LinearLayout indicatorBar;
	private int defScreen;
	private TextView refreshTimeText;
	private List<WeatherInfo> mWeatherInfoList;
	
	public static final int FORECAST_DAY = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		WeatherApp.mModel = WeatherModel.getInstance(getApplicationContext());
		initUI();
	}
	
	private void initUI() {
		mainContentView = findViewById(R.id.main_content);
		indicatorBar = (LinearLayout) findViewById(R.id.indicator_bar);
		refreshTimeText = (TextView) findViewById(R.id.latest_refresh_time);
		
		weatherInfoMainContainer = (ScrollControlLayout) findViewById(R.id.main_container);
		weatherInfoMainContainer
			.setOnScreenChangedListener(new ScrollControlLayout.OnScreenChangedListener() {
				
				@Override
				public void screenChange(int curScreen) {
					ImageView indicatorImage = (ImageView) indicatorBar.getChildAt(defScreen);
					indicatorImage.setImageResource(R.drawable.point);
					indicatorImage = (ImageView) indicatorBar.getChildAt(curScreen);
					indicatorImage.setImageResource(R.drawable.point_current);
					defScreen = curScreen;
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date mDate = new Date(mWeatherInfoList.get(defScreen).getUpdateTime());
					String refreshTime = getResources()
							.getString(R.string.refresh_time, format.format(mDate));
					refreshTimeText.setText(refreshTime);
					if (mWeatherInfoList.get(defScreen).isGps()) {
						WeatherDataUtil.getInstance()
						.updateDefaultCityWoeid(MainActivity.this, 
												WeatherDataUtil.DEFAULT_WOEID_GPS);
					} else {
						WeatherDataUtil.getInstance()
							.updateDefaultCityWoeid(
									MainActivity.this,
									mWeatherInfoList.get(defScreen).getWoeid());
					}
				//	startUpdateService(MainActivity.this,WeatherWidget.ACTION_UPDATE,
				//			AppWidgetManager.INVALID_APPWIDGET_ID);
					
				}
			});
	}

	private void setWeatherFromDB() {
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
	}
	
}



































