package com.clockker.clockker;

import android.net.wifi.ScanResult;

/**
 * Created by villet on 27/05/2017.
 */

public class ClockkerWifiScan {
    public String BSSID;
    public int RSSI;

    public ClockkerWifiScan(String bssid, int rssi) {
        BSSID = bssid;
        RSSI = rssi;
    }

    public static ClockkerWifiScan fromScanResult(ScanResult scan) {
        return new ClockkerWifiScan(scan.BSSID, scan.level);
    }

}
