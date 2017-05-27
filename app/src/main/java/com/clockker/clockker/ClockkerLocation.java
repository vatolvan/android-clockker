package com.clockker.clockker;

import android.net.wifi.ScanResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by villet on 27/05/2017.
 */

public class ClockkerLocation {

    String mName;

    List<ClockkerWifiScan> mWifiScan;

    double mLatitude;
    double mLongitude;

    public ClockkerLocation(String name, List<ScanResult> wifiScan, double lat, double lon) {
        mName = name;

        mWifiScan = new ArrayList<>();
        for (ScanResult scan : wifiScan) {
            mWifiScan.add(ClockkerWifiScan.fromScanResult(scan));
        }

        mLatitude = lat;
        mLongitude = lon;
    }

    public ClockkerLocation(String name, ArrayList<ClockkerWifiScan> wifiScan, double lat, double lon) {
        mName = name;

        mWifiScan = new ArrayList<>(wifiScan);

        mLatitude = lat;
        mLongitude = lon;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", mName);
        json.put("latitude", mLatitude);
        json.put("longitude", mLongitude);

        JSONArray wifis = new JSONArray();
        for (ClockkerWifiScan scan : mWifiScan) {
            JSONObject scanJson = new JSONObject();
            scanJson.put("BSSID", scan.BSSID);
            scanJson.put("RSSI", scan.RSSI);
            wifis.put(scanJson);
        }
        json.put("wifi_scan", wifis);

        return json;
    }

    public static ClockkerLocation fromJSON(JSONObject json) throws JSONException {
        String name = json.getString("name");
        double latitude = json.getDouble("latitude");
        double longitude = json.getDouble("longitude");

        JSONArray wifiJson = json.getJSONArray("wifi_scan");
        ArrayList<ClockkerWifiScan> wifis = new ArrayList<>();
        for (int i = 0; i < wifiJson.length(); i++) {
            JSONObject scan = wifiJson.getJSONObject(i);
            wifis.add(new ClockkerWifiScan(scan.getString("BSSID"), scan.getInt("RSSI")));
        }

        return new ClockkerLocation(name, wifis, latitude, longitude);
    }

}
