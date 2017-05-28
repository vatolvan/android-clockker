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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;

import com.clockker.clockker.util.IOUtil;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    double MAX_DIST = 1e10;
    double DISTANCE_LIMIT = 100;

    Button mButton;
    EditText mText;
    Button mResetButton;
    Button mCheckInButton;
    Button mCheckOutButton;

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

    List<ClockkerLocation> mLocations = new ArrayList<>();

    ClockkerLocation checkedInLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);
        mText = (EditText) findViewById(R.id.editText);
        mResetButton = (Button) findViewById(R.id.button7);
        mCheckInButton = (Button) findViewById(R.id.button4);
        mCheckOutButton = (Button) findViewById(R.id.button2);

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

        mResetButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        clearLocations();
                    }
                }
        );

        mCheckInButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if (mLocations.size() == 0) {
                            Toast.makeText(MainActivity.this, "Cant check in, no locations!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (checkedInLocation != null) {
                            Toast.makeText(MainActivity.this, "Cant check in, already checked in to a location! Check out first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ClockkerLocation loc = new ClockkerLocation(mName, mWifiList, mLatitude, mLongitude);

                        double minDist = MAX_DIST;
                        double secondMinDist = MAX_DIST;
                        int distIndex = -1;
                        double maxRatio = -1;
                        int ratioIndex = -1;

                        for (int i = 0; i < mLocations.size(); i++) {
                            double ratio = loc.baseStationRatio(mLocations.get(i));
                            double dist = loc.distanceTo(mLocations.get(i));
                            if (maxRatio < ratio) {
                                maxRatio = ratio;
                                ratioIndex = i;
                            }
                            if (minDist > dist) {
                                secondMinDist = minDist;
                                minDist = dist;
                                distIndex = i;
                            } else if (secondMinDist > dist) {
                                secondMinDist = dist;
                            }
                        }
                        Log.d("TESTI", "RatioIndex = " + ratioIndex + ", ratio = " + maxRatio + ", distIndex = " + distIndex + ", dist = " + minDist);
                        if (minDist < DISTANCE_LIMIT && secondMinDist < DISTANCE_LIMIT) {
                            checkedInLocation = mLocations.get(ratioIndex);
                        } else {
                            checkedInLocation = mLocations.get(distIndex);
                        }
                        ClockkerEvent event = new ClockkerEvent("check_in", System.currentTimeMillis() / 1000, checkedInLocation);

                        try {
                            IOUtil.writeEvent(MainActivity.this, event);
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "Failed to write event to file!", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Failed to parse event JSON!", Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(MainActivity.this, "Checked in to location with name = " +
                                checkedInLocation.getLocation().getProvider(), Toast.LENGTH_SHORT).show();
                        
                    }
                }
        );

        mCheckOutButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if (checkedInLocation == null) {
                            Toast.makeText(MainActivity.this, "Can't check out, havent checked in to a location!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ClockkerEvent event = new ClockkerEvent("check_out", System.currentTimeMillis() / 1000, checkedInLocation);

                        try {
                            IOUtil.writeEvent(MainActivity.this, event);
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "Failed to write event to file!", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Failed to parse event JSON!", Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(MainActivity.this, "Checked out of location with name = " +
                                checkedInLocation.getLocation().getProvider(), Toast.LENGTH_SHORT).show();

                        checkedInLocation = null;
                    }
                }
        );

        try {
            mLocations = IOUtil.readLocations(MainActivity.this);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to read file!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(MainActivity.this, "Failed to parse JSON!", Toast.LENGTH_SHORT).show();
        } finally {
            if (mLocations.size() > 0) {
                Toast.makeText(MainActivity.this, "Read " + mLocations.size() + " locations from file!", Toast.LENGTH_SHORT).show();
            }
        }

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
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
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
            //mWifiList = mainWifi.getScanResults();
            mergeWifiScans(mainWifi.getScanResults());
            mNumberOfWifis++;

            checkIfShouldCreateLocation();
        }
    }

    public void checkIfShouldCreateLocation() {
        if (mAddingLocation && mNumberOfLocations > 0 && mNumberOfWifis > 0) {
            mAddingLocation = false;
            mLocations.add(new ClockkerLocation(mName, mWifiList, mLatitude, mLongitude));

            try {
                IOUtil.exportLocations(MainActivity.this, mLocations);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Failed to write file!", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Failed to parse JSON!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void clearLocations() {

        boolean deleted = IOUtil.clearLocations(this);

        if (deleted) {
            mLocations = new ArrayList<>();

            Toast.makeText(this, "Resetted locations!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to reset locations!", Toast.LENGTH_SHORT).show();
        }
    }

    public void mergeWifiScans(List<ScanResult> scan) {
        boolean found = false;
        if (mWifiList != null) {
            for (ScanResult s : scan) {
                for (ScanResult s2 : mWifiList) {
                    if (s.BSSID.equals(s2.BSSID)) {
                        s2.level = s.level;
                        found = true;
                    }
                }
                if (!found) {
                    mWifiList.add(s);
                }
            }
        } else {
            mWifiList = scan;
        }
    }




}
