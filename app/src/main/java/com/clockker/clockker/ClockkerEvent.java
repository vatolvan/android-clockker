package com.clockker.clockker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by villet on 27/05/2017.
 */

public class ClockkerEvent {
    String mType;
    long mTimestamp;
    ClockkerLocation mLocation;

    public ClockkerEvent(String type, long timestamp, ClockkerLocation loc) {
        mType = type;
        mTimestamp = timestamp;
        mLocation = loc;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("type", mType);
        json.put("timestamp", mTimestamp);

        json.put("name", mLocation.getLocation().getProvider());
        json.put("latitude", mLocation.getLocation().getLatitude());
        json.put("longitude", mLocation.getLocation().getLongitude());

        return json;
    }

}
