package com.jef.citysearch;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.jef.citysearch.WeatherModel.OnLocationCityInfoUpdatedListener;



import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class MainActivity extends Activity implements View.OnClickListener,
   ActivityCompat.OnRequestPermissionsResultCallback
   ,OnCheckedChangeListener,
   CompoundButton.OnCheckedChangeListener{
	
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
	private LinearLayout noNetwork;
	
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

	private CityInfo locationCityInfo = new CityInfo();
	
	private static List<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	
	private static boolean needLocation = false;
	
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
				//deleteDefaultWeather();
				Log.e("XXX", "mainactivity-------------------onreceive");
				setWeatherFromDB();
				showLoadingProgress(false, R.string.progress_refresh);
			}
			
		}
		
	}

	/**
	 * 需要进行检测的权限数组
	 */
	protected String[] needPermissions = {
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.READ_PHONE_STATE
			};
	
	private static final int PERMISSON_REQUESTCODE = 0;
	
	/**
	 * 判断是否需要检测，防止不停的弹框
	 */
	private boolean isNeedCheck = true;
	
	private AMapLocationClient locationClient = null;
	private AMapLocationClientOption locationOption = new AMapLocationClientOption();
	
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
		
		if (Utils.isNetworkAvailable(MainActivity.this)) {
			Log.e("XXX", "-----------------------start location");
			needLocation = true;
			initLocation();
			startLocation();
		}
	}
	
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		
	}



	@Override
	protected void onResume() {
		super.onResume();
		if(isNeedCheck){
			checkPermissions(needPermissions);
		}
		if (WeatherDataUtil.getInstance()
				.getNeedUpdateMainUI(MainActivity.this)) {
			Log.e("XXX", "PROGRESS-----------------------11111");
			showLoadingProgress(true, R.string.progress_refresh);
			WeatherDataUtil.getInstance().setNeedUpdateMainUI(
					MainActivity.this, false);
			setWeatherFromBD();
			showLoadingProgress(false, R.string.progress_refresh);
		}
		
		if (menuState == MenuState.CLOSE) {
			if(null == mWeatherInfoList || mWeatherInfoList.isEmpty()) {
				refresh.setEnabled(false);
				//finish();
			} else if(!refresh.isEnabled()) {
				if (loadProgressView.getVisibility() != View.VISIBLE) {
					refresh.setEnabled(true);
				} else {
				}
			}
		} else {
			if(null == mWeatherInfoList || mWeatherInfoList.isEmpty()) {
				refresh.setEnabled(false);
				//finish();
			}
		}	
	}



	@Override
	protected void onDestroy() {
		unregisterReceiver(mWeatherRefreshedReceiver);
		mWeatherRefreshedReceiver = null;
		WeatherApp.mModel.stopLocationJob();
		super.onDestroy();
	}



	private void initUI() {
		mainContentView = findViewById(R.id.main_content);
		menuView = findViewById(R.id.menu);
		menuView.setOnClickListener(this);
		
		indicatorBar = (LinearLayout) findViewById(R.id.indicator_bar);
		refreshTimeText = (TextView) findViewById(R.id.latest_refresh_time);
		
		noNetwork = (LinearLayout) findViewById(R.id.no_network);

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
		
		if (mWeatherInfoList.size() == 0 && !Utils.isNetworkAvailable(MainActivity.this)) {
			mainContentView.setVisibility(View.GONE);
			noNetwork.setVisibility(View.VISIBLE);
		} else if (mWeatherInfoList.size() > 0 
				&& mWeatherInfoList.get(0).getForecasts().size() >= FORECAST_DAY) {
			mainContentView.setVisibility(View.VISIBLE);
			noNetwork.setVisibility(View.GONE);
			updateUI();
		} else if (mWeatherInfoList.size() > 0 && !Utils.isNetworkAvailable(MainActivity.this)) {
			showLoadingProgress(false, R.string.progress_refresh);
		}
	}
	
	private WeatherInfo deleWeatherInfo = new WeatherInfo();

	private void deleteDefaultWeather() {
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
		if (mWeatherInfoList.size() > 1) {
			for (WeatherInfo weatherInfo : mWeatherInfoList) {
				if (weatherInfo.getWoeid().equals("2132574")) {
					deleWeatherInfo = weatherInfo;
					//WeatherApp.mModel.deleteWeatherInfo(weatherInfo);
				}
			

			}
		}
		WeatherApp.mModel.deleteWeatherInfo(deleWeatherInfo);
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
			
			temperature = info.getCondition().getTemp() + "℃";
			tmp = info.getForecasts().get(0).getLow() + "℃ /"
					+ info.getForecasts().get(0).getHigh() + "℃";
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
		Log.e("XXX", "PROGRESS-----------------------2222");
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
		if (mWeatherInfoList.size() > 0
				&& mWeatherInfoList.get(0).getForecasts().size() >= FORECAST_DAY) {
			updateUI();
			/*startUpdateService(MainActivity.this, WeatherWidget.ACTION_UPDATE,
					AppWidgetManager.INVALID_APPWIDGET_ID);*/
		}
	}
	
	/**
	 * 
	 * @param needRequestPermissonList
	 * @since 2.5.0
	 *
	 */
	private void checkPermissions(String... permissions) {
		List<String> needRequestPermissonList = findDeniedPermissions(permissions);
		if (null != needRequestPermissonList
				&& needRequestPermissonList.size() > 0) {
			ActivityCompat.requestPermissions(this,
					needRequestPermissonList.toArray(
							new String[needRequestPermissonList.size()]),
					PERMISSON_REQUESTCODE);
		}
	}

	/**
	 * 获取权限集中需要申请权限的列表
	 * 
	 * @param permissions
	 * @return
	 * @since 2.5.0
	 *
	 */
	private List<String> findDeniedPermissions(String[] permissions) {
		List<String> needRequestPermissonList = new ArrayList<String>();
		for (String perm : permissions) {
			if (ContextCompat.checkSelfPermission(this,
					perm) != PackageManager.PERMISSION_GRANTED
					|| ActivityCompat.shouldShowRequestPermissionRationale(
							this, perm)) {
				needRequestPermissonList.add(perm);
			} 
		}
		return needRequestPermissonList;
	}

	/**
	 * 检测是否说有的权限都已经授权
	 * @param grantResults
	 * @return
	 * @since 2.5.0
	 *
	 */
	private boolean verifyPermissions(int[] grantResults) {
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String[] permissions, int[] paramArrayOfInt) {
		if (requestCode == PERMISSON_REQUESTCODE) {
			if (!verifyPermissions(paramArrayOfInt)) {
				showMissingPermissionDialog();
				isNeedCheck = false;
			}
		}
	}

	/**
	 * 显示提示信息
	 * 
	 * @since 2.5.0
	 *
	 */
	private void showMissingPermissionDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.notifyTitle);
		builder.setMessage(R.string.notifyMsg);

		// 拒绝, 退出应用
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});

		builder.setPositiveButton(R.string.setting,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startAppSettings();
					}
				});

		builder.setCancelable(false);

		builder.show();
	}

	/**
	 *  启动应用的设置
	 * 
	 * @since 2.5.0
	 *
	 */
	private void startAppSettings() {
		Intent intent = new Intent(
				Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		startActivity(intent);
	}



	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		
	}
	
	// 高德定位  begin
	private void initLocation(){
		//初始化client
		locationClient = new AMapLocationClient(MainActivity.this);
		//设置定位参数
		locationClient.setLocationOption(getDefaultOption());
		// 设置定位监听
		locationClient.setLocationListener(locationListener);
		
	}
	
	/**
	 * 默认的定位参数
	 * @since 2.8.0
	 * @author hongming.wang
	 *
	 */
	private AMapLocationClientOption getDefaultOption(){
		AMapLocationClientOption mOption = new AMapLocationClientOption();
		//mOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
		//mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
		mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
		mOption.setInterval(10000);//可选，设置定位间隔。默认为2秒
		//mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是ture
		//mOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
		//mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
		
		return mOption;
	}
	
	public static String getLocationCityName;
	/**
	 * 定位监听
	 */
	AMapLocationListener locationListener = new AMapLocationListener() {
		@Override
		public void onLocationChanged(AMapLocation loc) {
			if (null != loc) {
				if (loc.getCity().length() > 0 && needLocation) {
					getLocationCityName = loc.getCity();
					needLocation = false;
					stopLocation();
					searchLocationCity(getLocationCityName);
				}
			} else {
			}
		}
	};
	
	
	private void searchLocationCity(String locationCityName) {
		if (locationCityName.isEmpty()) {
			// Any toast?
		} else {
			Log.e("XXX", "PROGRESS-----------------------3333");
			showLoadingProgress(true, R.string.progress_refresh);
			WeatherApp.mModel.setOnLocationCityInfoUpdatedListener(locationSearchCityListener);
			
			boolean searchResult = WeatherApp.mModel.getCityInfosByNameFromInternet(locationCityName, true);
			if (searchResult) {
				//showLoadingProgress(false, R.string.progress_refresh);
			}
			
		}
	}
	
	WeatherModel.OnLocationCityInfoUpdatedListener locationSearchCityListener = 
			new WeatherModel.OnLocationCityInfoUpdatedListener() {
		
		@Override
		public void updated() {
			mCityInfos = WeatherApp.mModel.getCityInfos();
			
			if(!mCityInfos.isEmpty()) {
				final int count = mCityInfos.size();
				String[] cityInfosStrings = new String[count];
				for (int i = 0; i < count; i++) {
					cityInfosStrings[i] = mCityInfos.get(i).toString();
				}

			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.city_not_found),
						Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	
	
	public void startLocation(){
		//根据控件的选择，重新设置定位参数
		//resetOption();
		// 设置定位参数
		locationClient.setLocationOption(locationOption);
		// 启动定位
		locationClient.startLocation();
	}
	
	public void stopLocation(){

		locationClient.stopLocation();
	}
	// 高德定位 end

	
}

