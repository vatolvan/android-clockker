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
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button mButton;
    EditText mText;
    Button mResetButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);
        mText = (EditText) findViewById(R.id.editText);
        mResetButton = (Button) findViewById(R.id.button7);

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

        try {
            readLocations();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to write file!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(MainActivity.this, "Failed to parse JSON!", Toast.LENGTH_SHORT).show();
        } finally {
            if (mLocations.size() > 0) {
                Toast.makeText(MainActivity.this, "Read " + mLocations.size() + " locations from file!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void createLocation(String name, List<ScanResult> wifiList, double latitude, double longitude) {
        mLocations.add(new ClockkerLocation(name, wifiList, latitude, longitude));

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
            mWifiList = mainWifi.getScanResults();
            mNumberOfWifis++;

            checkIfShouldCreateLocation();
        }
    }

    public void checkIfShouldCreateLocation() {
        if (mAddingLocation && mNumberOfLocations > 0 && mNumberOfWifis > 0) {
            mAddingLocation = false;
            createLocation(mName, mWifiList, mLatitude, mLongitude);

            try {
                exportLocations();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Failed to write file!", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Failed to parse JSON!", Toast.LENGTH_SHORT).show();
            } finally {
                Toast.makeText(MainActivity.this, "Wrote locations to file!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void exportLocations() throws IOException, JSONException {
        // Check if we can write to external storage
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        File path = getExternalFilesDir(null);
        File file = new File(path, "list_of_locations.txt");

        FileOutputStream stream = new FileOutputStream(file);
        for (ClockkerLocation loc : mLocations) {
            stream.write(loc.toJSON().toString().getBytes());
            stream.write(("\n").getBytes());
        }

        stream.close();
    }

    public void readLocations() throws IOException, JSONException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        File path = getExternalFilesDir(null);
        File file = new File(path, "list_of_locations.txt");

        int length = (int) file.length();

        byte[] bytes = new byte[length];

        FileInputStream in = new FileInputStream(file);
        try {
            in.read(bytes);
        } finally {
            in.close();
        }

        String contents = new String(bytes);

        String lines[] = contents.split("\\r?\\n");

        mLocations = new ArrayList<>();

        for (String line : lines) {
            if (line.length() > 0) {
                mLocations.add(ClockkerLocation.fromJSON(new JSONObject(line)));
            }
        }
    }

    public void clearLocations() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        File dir = getExternalFilesDir(null);
        File file = new File(dir, "list_of_locations.txt");
        boolean deleted = file.delete();
        
        mLocations = new ArrayList<>();
        
        if (deleted && mLocations.size() == 0) {
            Toast.makeText(this, "Resetted locations!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to reset locations!", Toast.LENGTH_SHORT).show();
        }
    }


}
