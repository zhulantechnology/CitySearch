package com.jef.citysearch;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.amap.api.location.AMapLocation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

public class Utils {
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null) {
				if (info.getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isNetworkTypeWifi(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null && ConnectivityManager.TYPE_WIFI == info.getType()) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 *  ��ʼ��λ
	 */
	public final static int MSG_LOCATION_START = 0;
	/**
	 * ��λ���
	 */
	public final static int MSG_LOCATION_FINISH = 1;
	/**
	 * ֹͣ��λ
	 */
	public final static int MSG_LOCATION_STOP= 2;
	
	public final static String KEY_URL = "URL";
	public final static String URL_H5LOCATION = "file:///android_asset/location.html";
	/**
	 * ���ݶ�λ������ض�λ��Ϣ���ַ���
	 * @param loc
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public synchronized static String getLocationStr(AMapLocation location){
		if(null == location){
			return null;
		}
		StringBuffer sb = new StringBuffer();
		//errCode����0����λ�ɹ���������Ϊ��λʧ�ܣ�����Ŀ��Բ��չ�����λ������˵��
		if(location.getErrorCode() == 0){
			sb.append("��λ�ɹ�" + "\n");
			sb.append("��λ����: " + location.getLocationType() + "\n");
			sb.append("��    ��    : " + location.getLongitude() + "\n");
			sb.append("γ    ��    : " + location.getLatitude() + "\n");
			sb.append("��    ��    : " + location.getAccuracy() + "��" + "\n");
			sb.append("�ṩ��    : " + location.getProvider() + "\n");
			
			if (location.getProvider().equalsIgnoreCase(
					android.location.LocationManager.GPS_PROVIDER)) {
				// ������Ϣֻ���ṩ����GPSʱ�Ż���
				sb.append("��    ��    : " + location.getSpeed() + "��/��" + "\n");
				sb.append("��    ��    : " + location.getBearing() + "\n");
				// ��ȡ��ǰ�ṩ��λ��������Ǹ���
				sb.append("��    ��    : "
						+ location.getSatellites() + "\n");
			} else {
				// �ṩ����GPSʱ��û��������Ϣ��
				sb.append("��    ��    : " + location.getCountry() + "\n");
				sb.append("ʡ            : " + location.getProvince() + "\n");
				sb.append("��            : " + location.getCity() + "\n");
				sb.append("���б��� : " + location.getCityCode() + "\n");
				sb.append("��            : " + location.getDistrict() + "\n");
				sb.append("���� ��   : " + location.getAdCode() + "\n");
				sb.append("��    ַ    : " + location.getAddress() + "\n");
				sb.append("��Ȥ��    : " + location.getPoiName() + "\n");
				sb.append("·    : " + location.getRoad() + "\n");
				
				//��λ��ɵ�ʱ��
				sb.append("��λʱ��: " + formatUTC(location.getTime(), "yyyy-MM-dd HH:mm:ss:sss") + "\n");
			}
		} else {
			//��λʧ��
			sb.append("��λʧ��" + "\n");
			sb.append("������:" + location.getErrorCode() + "\n");
			sb.append("������Ϣ:" + location.getErrorInfo() + "\n");
			sb.append("��������:" + location.getLocationDetail() + "\n");
		}
		//��λ֮��Ļص�ʱ��
		sb.append("�ص�ʱ��: " + formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss:sss") + "\n");
		return sb.toString();
	}
	
	private static SimpleDateFormat sdf = null;
	public synchronized static String formatUTC(long l, String strPattern) {
		if (TextUtils.isEmpty(strPattern)) {
			strPattern = "yyyy-MM-dd HH:mm:ss";
		}
		if (sdf == null) {
			try {
				sdf = new SimpleDateFormat(strPattern, Locale.CHINA);
			} catch (Throwable e) {
			}
		} else {
			sdf.applyPattern(strPattern);
		}
		if (l <= 0l) {
			l = System.currentTimeMillis();
		}
		return sdf == null ? "NULL" : sdf.format(l);
	}
}
