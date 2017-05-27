package com.clockker.clockker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button mButton;
    EditText mText;

    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    int mNumberOfWifis = 0;

    private final Handler handler = new Handler();

    StringBuilder sb = new StringBuilder();

    String mName;

    LocationManager locationManager;

    double mLatitude;
    double mLongitude;
    int mNumberOfLocations = 0;

    List<ScanResult> mWifiList;

    LocationListener mLocationListener;

    boolean mAddingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);
        mText = (EditText) findViewById(R.id.editText);

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
        } else {
            getWifi();
            getLocation();
        }

        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if (!mAddingLocation) {
                            mName = mText.getText().toString();

                            mAddingLocation = true;
                            sb = new StringBuilder();
                            if (mNumberOfLocations > 0) {
                                sb.append("LatLon = " + mLatitude + "," + mLongitude + ", ");
                            }
                            if (mNumberOfWifis > 0) {
                                for (ScanResult s : mWifiList) {
                                    sb.append(s.BSSID + " : " + s.level + ",");
                                }
                            }
                            Toast.makeText(MainActivity.this, mName + ", " + sb.toString(), Toast.LENGTH_LONG).show();

                            checkIfShouldCreateLocation();
                        }
                    }
                });

    }

    public void createLocation(String name, List<ScanResult> wifiList, double latitude, double longitude) {
        Toast.makeText(this, "Creating location!", Toast.LENGTH_SHORT).show();
    }

    public void getWifi() {
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (!mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(true);
        }
        doInback();
    }

    public void getLocation() {
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
                mNumberOfLocations++;

                checkIfShouldCreateLocation();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocationListener != null) {
            locationManager.removeUpdates(mLocationListener);
        }
        if (receiverWifi != null) {
            mainWifi = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
        } else {
            getWifi();
            getLocation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationListener != null) {
            locationManager.removeUpdates(mLocationListener);
        }
        if (receiverWifi != null) {
            mainWifi = null;
        }
    }


    public void doInback()
    {
        handler.postDelayed(new Runnable() {

            @Override
            public void run()
            {
                // TODO Auto-generated method stub
                mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                if (receiverWifi == null) {
                    receiverWifi = new WifiReceiver();
                }
                registerReceiver(receiverWifi, new IntentFilter(
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                mainWifi.startScan();
                doInback();
            }
        }, 1000);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0x12345) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            getWifi();
            getLocation();
        }
    }

    class WifiReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            mWifiList = mainWifi.getScanResults();
            mNumberOfWifis++;

            checkIfShouldCreateLocation();
        }
    }

    public void checkIfShouldCreateLocation() {
        if (mAddingLocation && mNumberOfLocations > 0 && mNumberOfWifis > 0) {
            mAddingLocation = false;
            createLocation(mName, mWifiList, mLatitude, mLongitude);
        }
    }


}
