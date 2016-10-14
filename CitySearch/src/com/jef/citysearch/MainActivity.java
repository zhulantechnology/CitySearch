package com.jef.citysearch;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class MainActivity extends Activity implements View.OnClickListener{
	
	private View mainContentView;
	private View menuView;
	private ImageView refresh;
	private ImageView setting;
	
	private ScrollControlLayout weatherInfoMainContainer;
	private WeatherInfoMainView weatherInfoMainView;
	
	private View loadProgressView;
	private TextView progressText;
	
	private LinearLayout indicatorBar;
	private int defScreen;
	private TextView refreshTimeText;
	private List<WeatherInfo> mWeatherInfoList;
	
	public static final int FORECAST_DAY = 5;
	
	private View menuAutoRefresh;
	private ImageView menuCheckAutoRefresh;
	private View menuAuto6;
	private ImageView menuCheckAuto6h;
	private View menuAuto12;
	private ImageView menuCheckAuto12h;
	private View menuAuto24;
	private ImageView menuCheckAuto24h;
	private View menuWifiOnly;
	private ImageView menuCheckWifiOnly;
	private View menuSetCity;
	
	public static final String SETTINGS_SP = "settings_sp";
	public static final String SETTINGS_AUTO_REFRESH_ENABLE = "settings_auto_enable";
	public static final String SETTINGS_AUTO_REFRESH = "settings_auto_refresh";
	public static final String SETTINGS_WIFI_ONLY = "settings_wifi_only";
	public static final int SETTINGS_AUTO_REFRESH_INVALID = -1;
	public static final int SETTINGS_AUTO_REFRESH_6H = 6;
	public static final int SETTINGS_AUTO_REFRESH_12H = 12;
	public static final int SETTINGS_AUTO_REFRESH_24H = 24;
	private WeatherRefreshedReceiver mWeatherRefreshedReceiver;
	
	public enum MenuState {
		OPEN,CLOSE
	}
	
	private MenuState menuState = MenuState.CLOSE;
	
	private class WeatherRefreshedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) {
				return;
			}
			String action = intent.getAction();
			if (WeatherAction.ACTION_WEATHER_REFRESHED.equals(action)) {
				setWeatherFromDB();
				showLoadingProgress(false, R.string.progress_refresh);
			}
			
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		WeatherApp.mModel = WeatherModel.getInstance(getApplicationContext());
		initUI();
		
		setWeatherFromDB();
		
		mWeatherRefreshedReceiver = new WeatherRefreshedReceiver();
		IntentFilter filter = new IntentFilter(
				WeatherAction.ACTION_WEATHER_REFRESHED);
		filter.addAction(WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
		registerReceiver(mWeatherRefreshedReceiver, filter);
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		if (WeatherDataUtil.getInstance()
				.getNeedUpdateMainUI(MainActivity.this)) {
			showLoadingProgress(true, R.string.progress_refresh);
			WeatherDataUtil.getInstance().setNeedUpdateMainUI(
					MainActivity.this, false);
			setWeatherFromBD();
			showLoadingProgress(false, R.string.progress_refresh);
		}
		
		if (menuState == MenuState.CLOSE) {
			if(null == mWeatherInfoList || mWeatherInfoList.isEmpty()) {
				refresh.setEnabled(false);
				finish();
			} else if(!refresh.isEnabled()) {
				if (loadProgressView.getVisibility() != View.VISIBLE) {
					refresh.setEnabled(true);
				} else {
				}
			}
		} else {
			if(null == mWeatherInfoList || mWeatherInfoList.isEmpty()) {
				refresh.setEnabled(false);
				finish();
			}
		}
	}



	@Override
	protected void onDestroy() {
		unregisterReceiver(mWeatherRefreshedReceiver);
		mWeatherRefreshedReceiver = null;
		super.onDestroy();
	}



	private void initUI() {
		mainContentView = findViewById(R.id.main_content);
		menuView = findViewById(R.id.menu);
		menuView.setOnClickListener(this);
		
		indicatorBar = (LinearLayout) findViewById(R.id.indicator_bar);
		refreshTimeText = (TextView) findViewById(R.id.latest_refresh_time);

		refresh = (ImageView) findViewById(R.id.refresh);
		refresh.setOnClickListener(this);
		
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
		
		setting = (ImageView) findViewById(R.id.settings);
		setting.setOnClickListener(this);
		
		loadProgressView = findViewById(R.id.loading_progress_view);
		progressText = (TextView) findViewById(R.id.progress_text);
		
		initMenu();
	}
	
	private void initMenu() {
		menuAutoRefresh = findViewById(R.id.menu_auto_refresh);
		menuAutoRefresh.setOnClickListener(menuItemOnClickListener);
		menuCheckAutoRefresh = (ImageView) findViewById(R.id.menu_check_auto_refresh);

		menuAuto6 = findViewById(R.id.menu_auto_6h);
		menuAuto6.setOnClickListener(menuItemOnClickListener);
		menuCheckAuto6h = (ImageView) findViewById(R.id.menu_check_auto_6h);

		menuAuto12 = findViewById(R.id.menu_auto_12h);
		menuAuto12.setOnClickListener(menuItemOnClickListener);
		menuCheckAuto12h = (ImageView) findViewById(R.id.menu_check_auto_12h);

		menuAuto24 = findViewById(R.id.menu_auto_24h);
		menuAuto24.setOnClickListener(menuItemOnClickListener);
		menuCheckAuto24h = (ImageView) findViewById(R.id.menu_check_auto_24h);

		menuWifiOnly = findViewById(R.id.menu_wifi_only);
		menuWifiOnly.setOnClickListener(menuItemOnClickListener);
		menuCheckWifiOnly = (ImageView) findViewById(R.id.menu_check_wifi_only);

		menuSetCity = findViewById(R.id.menu_set_city);
		menuSetCity.setOnClickListener(menuItemOnClickListener);

		SharedPreferences sp = getSharedPreferences(SETTINGS_SP,
				Context.MODE_PRIVATE);
		boolean isAutoRefreshEnable = sp.getBoolean(
				SETTINGS_AUTO_REFRESH_ENABLE,
				getResources().getBoolean(R.bool.config_auto_refresh_enable));
		if (isAutoRefreshEnable) {
			menuCheckAutoRefresh.setImageResource(R.drawable.checkbox_checked);
		} else {
			menuCheckAutoRefresh.setImageResource(R.drawable.checkbox_normal);
		}

		switch (sp.getInt(SETTINGS_AUTO_REFRESH,
				getResources().getInteger(R.integer.config_auto_refresh))) {
		case SETTINGS_AUTO_REFRESH_6H:
			if (isAutoRefreshEnable) {
				menuCheckAuto6h.setImageResource(R.drawable.checkbox_checked);
				menuCheckAuto12h.setImageResource(R.drawable.checkbox_normal);
				menuCheckAuto24h.setImageResource(R.drawable.checkbox_normal);
			} else {
				menuCheckAuto6h
						.setImageResource(R.drawable.checkbox_checked_disable);
				menuCheckAuto12h
						.setImageResource(R.drawable.checkbox_normal_disable);
				menuCheckAuto24h
						.setImageResource(R.drawable.checkbox_normal_disable);
			}
			break;
		case SETTINGS_AUTO_REFRESH_12H:
			if (isAutoRefreshEnable) {
				menuCheckAuto6h.setImageResource(R.drawable.checkbox_normal);
				menuCheckAuto12h.setImageResource(R.drawable.checkbox_checked);
				menuCheckAuto24h.setImageResource(R.drawable.checkbox_normal);
			} else {
				menuCheckAuto6h
						.setImageResource(R.drawable.checkbox_normal_disable);
				menuCheckAuto12h
						.setImageResource(R.drawable.checkbox_checked_disable);
				menuCheckAuto24h
						.setImageResource(R.drawable.checkbox_normal_disable);
			}
			break;
		case SETTINGS_AUTO_REFRESH_24H:
			if (isAutoRefreshEnable) {
				menuCheckAuto6h.setImageResource(R.drawable.checkbox_normal);
				menuCheckAuto12h.setImageResource(R.drawable.checkbox_normal);
				menuCheckAuto24h.setImageResource(R.drawable.checkbox_checked);
			} else {
				menuCheckAuto6h
						.setImageResource(R.drawable.checkbox_normal_disable);
				menuCheckAuto12h
						.setImageResource(R.drawable.checkbox_normal_disable);
				menuCheckAuto24h
						.setImageResource(R.drawable.checkbox_checked_disable);
			}
			break;
		default:
			break;
		}

		if (sp.getBoolean(SETTINGS_WIFI_ONLY,
				getResources().getBoolean(R.bool.config_wifi_only_enable))) {
			menuCheckWifiOnly.setImageResource(R.drawable.checkbox_checked);
		} else {
			menuCheckWifiOnly.setImageResource(R.drawable.checkbox_normal);
		}
	}

	private void setWeatherFromDB() {
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
		
		if (mWeatherInfoList.size() > 0 
				&& mWeatherInfoList.get(0).getForecasts().size() >= FORECAST_DAY) {
				updateUI();
		}
	}
	
	private void updateUI() {
		if (mWeatherInfoList.size() <1 
				&& mWeatherInfoList.get(0).getForecasts().size() < FORECAST_DAY) {
			return;
		}
		String temperature = "";
		String tmp = "";
		WeatherInfo info = null;
		defScreen = 0;
		String defWoeid = WeatherDataUtil.getInstance().getDefaultCityWoeid(MainActivity.this);
		
		weatherInfoMainContainer.removeAllViews();
		for (int i = 0; i < mWeatherInfoList.size(); i++) {
			info = mWeatherInfoList.get(i);
			if (defWoeid.equals(info.getWoeid())) {
				defScreen = i;
			}
			
			String date = info.getForecasts().get(0).getDate() + "("
					+ info.getForecasts().get(0).getDay() + ")";
			
			temperature = info.getCondition().getTemp() + "¡æ";
			tmp = info.getForecasts().get(0).getLow() + "¡æ /"
					+ info.getForecasts().get(0).getHigh() + "¡æ";
			String text = info.getCondition().getText();
			int code = Integer.parseInt(info.getCondition().getCode());
			
			
			int resId;
			boolean isnight = WeatherDataUtil.getInstance().isNight();
			resId = WeatherDataUtil.getInstance().getWeatherImageResourceByCode(code, isnight, false);
			if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE == resId) {
				resId = WeatherDataUtil.getInstance()
						.getWeatherImageResourceByText(info.getCondition().getText(),isnight, false);
			}
			weatherInfoMainView = new WeatherInfoMainView(MainActivity.this);
			weatherInfoMainView.bindView(date, info.getName(), resId,
											text, temperature, tmp, info.isGps());
			for (int j = 0; j < FORECAST_DAY; j++) {
				weatherInfoMainView.updateForecastItem(j, info.getForecasts().get(j));
			}
			weatherInfoMainContainer.addView(weatherInfoMainView);
		}
		weatherInfoMainContainer.setDefaultScreen(defScreen);
		
		updateIndicatorBar();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date mDate = new Date(mWeatherInfoList.get(defScreen).getUpdateTime());
		String refreshTime = getResources().getString(R.string.refresh_time,format.format(mDate));
		refreshTimeText.setText(refreshTime);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.settings:
				showMenu(true);
			break;
		case R.id.refresh:
			SharedPreferences sp = getSharedPreferences(SETTINGS_SP,
					Context.MODE_PRIVATE);
			if (sp.getBoolean(SETTINGS_WIFI_ONLY, getResources()
					.getBoolean(R.bool.config_wifi_only_enable))) {
				if (Utils.isNetworkTypeWifi(MainActivity.this)) {
					setWeatherFromInternet();
				} else {
					Toast.makeText(MainActivity.this,
							R.string.toast_wifi_only_mode,
							Toast.LENGTH_SHORT).show();
				}
			} else {
				if (Utils.isNetworkAvailable(MainActivity.this)) {
					setWeatherFromInternet();
				} else {
					Toast.makeText(MainActivity.this,
							R.string.toast_net_inavailable,
							Toast.LENGTH_SHORT).show();
				}
			}
			break;

		default:
			break;
		}
		
	}
	
	private void showMenu(boolean show) {
		final int mainContentWidth = mainContentView.getWidth();
		final int menuWidth = getResources().getDimensionPixelSize(
				R.dimen.menu_width);
		if (show) {
			refresh.setEnabled(false);
			setting.setEnabled(false);
			weatherInfoMainContainer.setEnabled(false);
			weatherInfoMainContainer.setTouchMove(false);
			menuView.setVisibility(View.VISIBLE);
			
			mainContentView.setTranslationX(-menuWidth);
			Animation mTranslateAnimation = new TranslateAnimation(menuWidth, 0, 0, 0);
			mTranslateAnimation.setDuration(350);
			mainContentView.startAnimation(mTranslateAnimation);
			
			menuView.setTranslationX(mainContentWidth - menuWidth);
			Animation mTranslateAnimation2 = new TranslateAnimation(menuWidth, 0, 0, 0);
			mTranslateAnimation2.setDuration(350);
			menuView.startAnimation(mTranslateAnimation2);
			menuState = MenuState.OPEN;
		} else {
			mainContentView.setTranslationX(0);
			menuView.setTranslationX(0);
			menuView.setVisibility(View.GONE);

			Animation mTranslateAnimation = new TranslateAnimation(-menuWidth,
					0, 0, 0);
			mTranslateAnimation.setDuration(350);
			mainContentView.startAnimation(mTranslateAnimation);
			Animation mTranslateAnimation2 = new TranslateAnimation(
					mainContentWidth - menuWidth, mainContentWidth, 0, 0);
			mTranslateAnimation2.setDuration(350);
			menuView.startAnimation(mTranslateAnimation2);

			refresh.setEnabled(true);
			setting.setEnabled(true);
			weatherInfoMainContainer.setEnabled(true);
			weatherInfoMainContainer.setTouchMove(true);

			menuState = MenuState.CLOSE;
		}
	}
	
	private View.OnClickListener menuItemOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.menu_auto_refresh: {
				SharedPreferences sp = getSharedPreferences(SETTINGS_SP,
						Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sp.edit();
				boolean isAutoRefreshEnable = sp.getBoolean(
						SETTINGS_AUTO_REFRESH_ENABLE, getResources()
								.getBoolean(R.bool.config_auto_refresh_enable));
				if (isAutoRefreshEnable) {
					editor.putBoolean(SETTINGS_AUTO_REFRESH_ENABLE, false);
					menuCheckAutoRefresh
							.setImageResource(R.drawable.checkbox_normal);
				} else {
					menuCheckAutoRefresh
							.setImageResource(R.drawable.checkbox_checked);
					editor.putBoolean(SETTINGS_AUTO_REFRESH_ENABLE, true);
				}
				int time = sp.getInt(SETTINGS_AUTO_REFRESH, getResources()
						.getInteger(R.integer.config_auto_refresh));
				switch (time) {
				case SETTINGS_AUTO_REFRESH_6H:
					if (!isAutoRefreshEnable) {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_checked);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_normal);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_normal);
					} else {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_checked_disable);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_normal_disable);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_normal_disable);
					}
					break;
				case SETTINGS_AUTO_REFRESH_12H:
					if (!isAutoRefreshEnable) {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_normal);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_checked);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_normal);
					} else {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_normal_disable);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_checked_disable);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_normal_disable);
					}
					break;
				case SETTINGS_AUTO_REFRESH_24H:
					if (!isAutoRefreshEnable) {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_normal);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_normal);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_checked);
					} else {
						menuCheckAuto6h
								.setImageResource(R.drawable.checkbox_normal_disable);
						menuCheckAuto12h
								.setImageResource(R.drawable.checkbox_normal_disable);
						menuCheckAuto24h
								.setImageResource(R.drawable.checkbox_checked_disable);
					}
					break;
				default:
					break;
				}

				editor.commit();
				if (isAutoRefreshEnable) {
					setAutoRefreshAlarm(MainActivity.this,
							SETTINGS_AUTO_REFRESH_INVALID);
				} else {
					setAutoRefreshAlarm(MainActivity.this, time);
				}

				break;
			}
			case R.id.menu_auto_6h: {
				menuAutoReflashTimeChecked(SETTINGS_AUTO_REFRESH_6H);
				break;
			}
			case R.id.menu_auto_12h: {
				menuAutoReflashTimeChecked(SETTINGS_AUTO_REFRESH_12H);
				break;
			}
			case R.id.menu_auto_24h: {
				menuAutoReflashTimeChecked(SETTINGS_AUTO_REFRESH_24H);
				break;
			}
			case R.id.menu_wifi_only: {
				SharedPreferences sp = getSharedPreferences(SETTINGS_SP,
						Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sp.edit();

				if (sp.getBoolean(SETTINGS_WIFI_ONLY, getResources()
						.getBoolean(R.bool.config_wifi_only_enable))) {
					editor.putBoolean(SETTINGS_WIFI_ONLY, false);
					menuCheckWifiOnly
							.setImageResource(R.drawable.checkbox_normal);
				} else {
					menuCheckWifiOnly
							.setImageResource(R.drawable.checkbox_checked);
					editor.putBoolean(SETTINGS_WIFI_ONLY, true);
				}
				editor.commit();
				break;
			}

			case R.id.menu_set_city:
				Intent intent = new Intent(MainActivity.this,
						CityMangerActivity.class);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	};
	
	private void setAutoRefreshAlarm(Context context, int time) {
		
	}
	
	private void menuAutoReflashTimeChecked(int checkedTime) {
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && menuState == MenuState.OPEN) {
			showMenu(false);
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private void setWeatherFromInternet() {
		showLoadingProgress(true,R.string.progress_refresh);
		if (!WeatherApp.mModel.refreshWeather(mWeatherInfoList.get(defScreen))) {
			showLoadingProgress(false, R.string.progress_refresh);
		}

	//	WeatherApp.mModel.refreshWeather(mWeatherInfoList.get(defScreen));
	}
	
	private void showLoadingProgress(boolean show, int textId) {
		if (show) {
			loadProgressView.setVisibility(View.VISIBLE);
			progressText.setText(textId);
			refresh.setEnabled(false);
			setting.setEnabled(false);
			weatherInfoMainContainer.setEnabled(false);
			weatherInfoMainContainer.setTouchMove(false);
		} else {
			loadProgressView.setVisibility(View.GONE);
			refresh.setEnabled(true);
			setting.setEnabled(true);
			weatherInfoMainContainer.setEnabled(true);
			weatherInfoMainContainer.setTouchMove(true);
		}
	}
	
	private void updateIndicatorBar() {
		indicatorBar.removeAllViews();
		ImageView indicatorImage;
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
									LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		lp.setMargins(5, 5, 5, 5);
		for (int i = 0; i < mWeatherInfoList.size(); i++) {
			indicatorImage = new ImageView(MainActivity.this);
			indicatorImage.setLayoutParams(lp);
			if (defScreen == i) {
				indicatorImage.setImageResource(R.drawable.point_current);
			} else {
				indicatorImage.setImageResource(R.drawable.point);
			}
			indicatorBar.addView(indicatorImage);
		}
	}
	
	private void setWeatherFromBD() {
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
		//Log.e("XXX", "wangjun------setWeatherFromBD-------mWeatherInfoList.size()=" + mWeatherInfoList.size());
		//Log.e("XXX", "wangjun--------setWeatherFromBD-----mWeatherInfoList.get(0).getForecasts().size()=" + mWeatherInfoList.get(0).getForecasts().size());
		if (mWeatherInfoList.size() > 0
				&& mWeatherInfoList.get(0).getForecasts().size() >= FORECAST_DAY) {
			updateUI();
			/*startUpdateService(MainActivity.this, WeatherWidget.ACTION_UPDATE,
					AppWidgetManager.INVALID_APPWIDGET_ID);*/
		}
	}
	
}



































