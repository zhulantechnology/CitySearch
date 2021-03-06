package com.jef.citysearch;

import java.util.List;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class CitySearchActivity extends Activity {

	private EditText cityName;
	private ImageButton searchCity;
	private ListView cityList;
	private View loadProgressView;

	private List<CityInfo> mCityInfos;
	private ArrayAdapter<String> cityInfoAdapter;
	private WeatherRefreshedReceiver mWeatherRefreshedReceiver;
	
	private class WeatherRefreshedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == intent) {
				return;
			}
			String action = intent.getAction();
			if (WeatherAction.ACTION_ADD_WEATHER_FINISH.equals(action)) {
				Log.e("XXX", "CitySearchActivity-------------------onreceive");
				CitySearchActivity.this.setResult(
						CityMangerActivity.REQUEST_CODE_CITY_ADD, null);
				
				CitySearchActivity.this.finish();
				showLoadingProgress(false);
			}
		}

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_city_search);
		mCityInfos = WeatherApp.mModel.getCityInfos();
		
		cityName = (EditText) findViewById(R.id.city_name);
		searchCity = (ImageButton) findViewById(R.id.search_city);
		
		
		searchCity.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Utils.isNetworkAvailable(CitySearchActivity.this)) {
					searchCity();
					cityList.setVisibility(View.VISIBLE);// wangjun add
				} else {
					Toast.makeText(CitySearchActivity.this,
							R.string.toast_net_inavailable, Toast.LENGTH_SHORT)
							.show();
					//Log.d(TAG, "Search city BTN, network NOT available");
					return;
				}

			}
		});

		loadProgressView = findViewById(R.id.loading_progress_view);
		cityList = (ListView) findViewById(R.id.city_list);
		cityList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int length = mCityInfos.size();
				if (position < length) {
					addCity(mCityInfos.get(position));
				}
				
			}
		});
		
		mWeatherRefreshedReceiver = new WeatherRefreshedReceiver();
		IntentFilter filter = new IntentFilter(WeatherAction.ACTION_ADD_WEATHER_FINISH);
		registerReceiver(mWeatherRefreshedReceiver, filter);
	}
	
	
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		WeatherApp.mModel.stopQueryCity();
		super.onStop();
	}



	private void addCity(CityInfo info) {
		WeatherApp.mModel.stopQueryCity();
		cityList.setVisibility(View.GONE);
		
		showLoadingProgress(true);

		if (WeatherApp.mModel.addWeatherByCity(info, false, false)) {
			showLoadingProgress(false);
		}

	}
	
	private void searchCity() {
		String name = cityName.getText().toString();
		if (name.isEmpty()) {
			// Any toast?
		} else {
			showLoadingProgress(true);
			WeatherApp.mModel.setOnCityInfoUpdatedListener(onCityInfoUpdatedListener);
			if (!WeatherApp.mModel.getCityInfosByNameFromInternet(name, false)) {
				showLoadingProgress(false);
			}
		}
	}
	
	WeatherModel.OnCityInfoUpdatedListener onCityInfoUpdatedListener = 
			new WeatherModel.OnCityInfoUpdatedListener() {
		
		@Override
		public void updated() {
			if(!mCityInfos.isEmpty()) {
				final int count = mCityInfos.size();
				String[] cityInfosStrings = new String[count];
				for (int i = 0; i < count; i++) {
					cityInfosStrings[i] = mCityInfos.get(i).toString();
				}
				cityInfoAdapter = new ArrayAdapter<String>(
						CitySearchActivity.this,
						R.layout.simple_list_item, cityInfosStrings);
				cityList.setAdapter(cityInfoAdapter);
				cityList.setVisibility(View.VISIBLE);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.city_not_found),
						Toast.LENGTH_SHORT).show();
			}
			showLoadingProgress(false);
		}
	};
	
	private void showLoadingProgress(boolean show) {
		if (show) {
			loadProgressView.setVisibility(View.VISIBLE);
			searchCity.setEnabled(false);
		} else {
			loadProgressView.setVisibility(View.GONE);
			searchCity.setEnabled(true);
		}
	}

	
	
}
