package com.jef.citysearch;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Entity;
import android.util.Log;

public class WebAccessTools {
	private Context context;
	
	public WebAccessTools(Context context) {
		this.context = context;
	}
	
	public String getWebContent(String url) {
		if (!Utils.isNetworkAvailable(context)) {
			return null;
		}
		
		@SuppressWarnings("deprecation")
		HttpGet request = new HttpGet(url);
		HttpParams params = new BasicHttpParams();
		
		HttpConnectionParams.setConnectionTimeout(params, 6000);
		HttpConnectionParams.setSoTimeout(params, 6000);
		
		HttpClient httpClient = new DefaultHttpClient(params);
		try {
			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String content = EntityUtils.toString(response.getEntity());
				return content;
			} else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return null;
	}
}



































