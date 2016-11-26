package com.jef.citysearch;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CityMangerActivity extends Activity {

	private ImageView addCity;
	private ImageView location;
	private ImageView allRefresh;
	private TextView allRefreshTimeText;
	private ListView cityList;
	private View loadProgressView;

	public static final int REQUEST_CODE_CITY_ADD = 1001;
	public static final int REQUEST_CODE_PERMISSION = 101;
	public static final int CITY_COUNT_MAX = 10;

	private CityListAdapter mCityListAdapter;
	private CityListItem gpsItem;
	private List<CityListItem> items = new ArrayList<CityListItem>();
	private ArrayList<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	private CityInfo gpsCityInfo = new CityInfo();
	private List<WeatherInfo> mWeatherInfoList;
	private List<CityInfo> mGpsCityInfoList;

	private int deletePosition;
	
	private CityManagerReceiver mCityManagerReceiver;

	private class CityManagerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == intent) {
				return;
			}

			String action = intent.getAction();

			if (WeatherAction.ACTION_WEATHER_REFRESHED_ALL.equals(action)) {
				refreshCityList();
				/*startUpdateService(CityMangerActivity.this,
						WeatherWidget.ACTION_UPDATE,
						AppWidgetManager.INVALID_APPWIDGET_ID);*/
				showLoadingProgress(false);
			}
			/*
			else if (WeatherAction.ACTION_QUERT_LOCATION_FINISH
					.equals(action)) {
				if (!mCityInfos.isEmpty()) {
					CityInfo city = mCityInfos.get(0);

					Log.d(TAG, "CityInfo, "
							+ city.getLocationInfo().getSouthWestLat() + ", "
							+ city.getLocationInfo().getSouthWestLon() + ", "
							+ city.getLocationInfo().getNorthEastLat() + ", "
							+ city.getLocationInfo().getNorthEastLon());
					if (null == gpsCityInfo) {
						gpsCityInfo = city;
						WeatherApp.mModel.saveGpsCityInfoToDB(gpsCityInfo);
					}
				}

				isAutoGps = false;

				if (isStoped) {
					Log.w(TAG, "City Activity has Stoped");
				} else if (gpsCityInfo == null) {
					Log.w(TAG, "gpsCityInfo is NULL");
				} else {
					getLocationFindDialog(false).show();
				}

				clearLocationThing();
				showLoadingProgress(false);
			} else if (WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH
					.equals(action)) {
				refreshCityList();
				String defaultWoeid = WeatherDataUtil.getInstance()
						.getDefaultCityWoeid(CityMangerActivity.this);
				if (defaultWoeid.isEmpty() && !mWeatherInfoList.isEmpty()) {
					WeatherDataUtil
							.getInstance()
							.updateDefaultCityWoeid(
									CityMangerActivity.this,
									mWeatherInfoList.get(0).isGps() ? WeatherDataUtil.DEFAULT_WOEID_GPS
											: mWeatherInfoList.get(0)
													.getWoeid());

					startUpdateService(CityMangerActivity.this,
							WeatherWidget.ACTION_UPDATE,
							AppWidgetManager.INVALID_APPWIDGET_ID);
				}
				if (mWeatherInfoList.size() == 1) {
					WeatherDataUtil.getInstance().setRefreshTime(CityMangerActivity.this, System.currentTimeMillis());
					setRefreshTime();
				}
				WeatherDataUtil.getInstance().setNeedUpdateMainUI(
						CityMangerActivity.this, true);
				
				showLoadingProgress(false);
			}*/

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.city_manager);
		WeatherApp.mModel = WeatherModel.getInstance(getApplicationContext());

		initUI();
		refreshCityList();
		
		mCityManagerReceiver = new CityManagerReceiver();
		IntentFilter filter = new IntentFilter(
				WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
		filter.addAction(WeatherAction.ACTION_QUERT_LOCATION_FINISH);
		filter.addAction(WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH);
		registerReceiver(mCityManagerReceiver, filter);
	}

	private void initUI() {
		addCity = (ImageView) findViewById(R.id.add_city);
		addCity.setOnClickListener(onClickListener);

		location = (ImageView) findViewById(R.id.location);
		location.setOnClickListener(onClickListener);

		allRefresh = (ImageView) findViewById(R.id.all_refresh);
		allRefresh.setOnClickListener(onClickListener);
		allRefreshTimeText = (TextView) findViewById(R.id.latest_all_refresh_time);
		cityList = (ListView) findViewById(R.id.city_list);

		loadProgressView = findViewById(R.id.loading_progress_view);
	}

	View.OnClickListener onClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.all_refresh:
				SharedPreferences sp = getSharedPreferences(
						MainActivity.SETTINGS_SP, Context.MODE_PRIVATE);
				if (sp.getBoolean(
						MainActivity.SETTINGS_WIFI_ONLY,
						getResources().getBoolean(
								R.bool.config_wifi_only_enable))) {
					if (Utils.isNetworkTypeWifi(CityMangerActivity.this)) {
						showLoadingProgress(true);
						if (!WeatherApp.mModel.refreshAllWeather()) {
							showLoadingProgress(false);
						}
					} else {
						Toast.makeText(CityMangerActivity.this,
								R.string.toast_wifi_only_mode,
								Toast.LENGTH_SHORT).show();
					}
				} else {
					if (Utils.isNetworkAvailable(CityMangerActivity.this)) {
						showLoadingProgress(true);
						if (!WeatherApp.mModel.refreshAllWeather()) {
							showLoadingProgress(false);
						}
					} else {
						Toast.makeText(CityMangerActivity.this,
								R.string.toast_net_inavailable,
								Toast.LENGTH_SHORT).show();
					}
				}

				break;
			case R.id.add_city:
				if (items.size() < CITY_COUNT_MAX) {
					Intent intent = new Intent(CityMangerActivity.this,
							CitySearchActivity.class);
					intent.putExtra("from_manager", true);
					startActivityForResult(intent, REQUEST_CODE_CITY_ADD);
				} else {
					Toast.makeText(
							CityMangerActivity.this,
							getResources().getString(R.string.city_max_toast,
									CITY_COUNT_MAX), Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				break;
			}

		}
	};
	
	

	@Override
	protected void onDestroy() {
		unregisterReceiver(mCityManagerReceiver);
		mCityManagerReceiver = null;
		super.onDestroy();
	}

	class CityListItem {
		public String woeid;
		public String name;
		public int imgRes;
		public String text;
		public String weather;
		public boolean isGps;

		public CityListItem(String woeid, String name) {
			super();
			this.woeid = woeid;
			this.name = name;
		}

		public void setImgRes(int imgRes) {
			this.imgRes = imgRes;
		}

		public void setWeather(String weather) {
			this.weather = weather;
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setGPs(boolean isGps) {
			this.isGps = isGps;
		}
	}

	class CityListAdapter extends BaseAdapter {
		private List<CityListItem> mList;
		private LayoutInflater mInflater;

		public CityListAdapter(Context context, List<CityListItem> mList) {
			super();
			this.mList = mList;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder mHolder;

			if (convertView == null || convertView.getTag() == null) {
				// Time consuming 1 -- inflate
				convertView = mInflater.inflate(R.layout.city_list_item, null);
				mHolder = new ViewHolder();
				// Time consuming 2 -- findViewById
				mHolder.name = (TextView) convertView.findViewById(R.id.name);
				mHolder.image = (ImageView) convertView
						.findViewById(R.id.image);
				mHolder.text = (TextView) convertView.findViewById(R.id.text);
				mHolder.weather = (TextView) convertView
						.findViewById(R.id.weather);
				mHolder.gpsIcon = (ImageView) convertView
						.findViewById(R.id.ic_gps);
				convertView.setTag(mHolder);
			} else {
				mHolder = (ViewHolder) convertView.getTag();
			}
			CityListItem bean = mList.get(position);
			mHolder.name.setText(bean.name);
			mHolder.image.setImageResource(bean.imgRes);
			mHolder.text.setText(bean.text);
			mHolder.weather.setText(bean.weather + "¡æ");
			mHolder.gpsIcon
					.setVisibility(bean.isGps ? View.VISIBLE : View.GONE);
			return convertView;
		}

		// Google I/O
		class ViewHolder {
			public TextView name;
			public ImageView image;
			public TextView text;
			public TextView weather;
			public ImageView gpsIcon;
		}
	}

	private void refreshCityList() {
		gpsItem = null;
		items.clear();
		CityListItem item;
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();

		for (WeatherInfo weatherInfo : mWeatherInfoList) {
			item = new CityListItem(weatherInfo.getWoeid(),
					weatherInfo.getName());
			item.setText(weatherInfo.getCondition().getText());
			item.setWeather(weatherInfo.getCondition().getTemp());
			item.setGPs(weatherInfo.isGps());
			if (weatherInfo.isGps()) {
				gpsItem = item;
			} else {
				items.add(item);
			}
		}
		if (null != gpsItem) {
			items.add(0, gpsItem);
		}

		if (items.size() < CITY_COUNT_MAX) {
			addCity.setImageResource(R.drawable.add_city);
		} else {
			addCity.setImageResource(R.drawable.add_city_disabled);
		}

		mCityListAdapter = new CityListAdapter(CityMangerActivity.this, items);
		cityList.setAdapter(mCityListAdapter);
		cityList.setOnItemLongClickListener(longClickListener);

		setRefreshTime();
	}
	
	AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			deletePosition = position;

			String title = getResources().getString(R.string.delete_city,
					items.get(position).name);
			final AlertDialog dialog = new AlertDialog.Builder(
					CityMangerActivity.this,
					AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
					.setTitle(title)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									CityListItem item = items.get(deletePosition);
									for(WeatherInfo info:mWeatherInfoList) {
										if (item.isGps && info.isGps()) {
											WeatherApp.mModel.deleteWeatherInfo(info);
											break;
										} else if (!item.isGps && !info.isGps() && (info.getWoeid().equals(item.woeid))) {
											WeatherApp.mModel.deleteWeatherInfo(info);
											break;
										}
									}
									
									String defaultWoeid = WeatherDataUtil
											.getInstance().getDefaultCityWoeid(
													CityMangerActivity.this);

									if ((!item.isGps
											&& defaultWoeid.equals(items
													.get(deletePosition).woeid) && !defaultWoeid
												.equals(WeatherDataUtil.DEFAULT_WOEID_GPS))
											|| (item.isGps && defaultWoeid
													.equals(WeatherDataUtil.DEFAULT_WOEID_GPS))) {
										WeatherDataUtil
												.getInstance()
												.updateDefaultCityWoeid(
														CityMangerActivity.this,
														"");

										String firstWoeid = WeatherApp.mModel
												.getFirstWeatherFromDB();
										if (null != firstWoeid) {
											WeatherDataUtil
													.getInstance()
													.updateDefaultCityWoeid(
															CityMangerActivity.this,
															firstWoeid);
										}

/*										startUpdateService(
												CityMangerActivity.this,
												WeatherWidget.ACTION_UPDATE,
												AppWidgetManager.INVALID_APPWIDGET_ID);*/
									}

									refreshCityList();
									WeatherDataUtil.getInstance()
											.setNeedUpdateMainUI(
													CityMangerActivity.this,
													true);
								}
							}).setNeutralButton(android.R.string.cancel, null)
					.create();
			dialog.show();
			return true;
		}
	};

	private void setRefreshTime() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date mDate = new Date(WeatherDataUtil.getInstance().getRefreshTime(
				CityMangerActivity.this));
		String refreshTime = getResources().getString(R.string.refresh_time,
				format.format(mDate));
		allRefreshTimeText.setText(refreshTime);
	}
	
	private void showLoadingProgress(boolean show) {
		if (show) {
			loadProgressView.setVisibility(View.VISIBLE);
			location.setEnabled(false);
			addCity.setEnabled(false);
			allRefresh.setEnabled(false);
		} else {
			loadProgressView.setVisibility(View.GONE);
			location.setEnabled(true);
			addCity.setEnabled(true);
			allRefresh.setEnabled(true);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CODE_CITY_ADD:
			refreshCityList();
			String defaultWoeid = WeatherDataUtil.getInstance()
					.getDefaultCityWoeid(CityMangerActivity.this);
			if (defaultWoeid.isEmpty() && !mWeatherInfoList.isEmpty()) {
				WeatherDataUtil
						.getInstance()
						.updateDefaultCityWoeid(
								CityMangerActivity.this,
								mWeatherInfoList.get(0).isGps() ? WeatherDataUtil.DEFAULT_WOEID_GPS
										: mWeatherInfoList.get(0).getWoeid());
				/*startUpdateService(CityMangerActivity.this,
						WeatherWidget.ACTION_UPDATE,
						AppWidgetManager.INVALID_APPWIDGET_ID);*/
			}
			if (mWeatherInfoList.size() == 1) {
			//	WeatherDataUtil.getInstance().setRefreshTime(CityMangerActivity.this, System.currentTimeMillis());
				setRefreshTime();
			}
			WeatherDataUtil.getInstance().setNeedUpdateMainUI(
					CityMangerActivity.this, true);
			
			break;

		default:
			break;
		}
	}
	
	
	
	

}
