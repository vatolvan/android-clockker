package com.clockker.clockker.util;

import android.content.Context;
import android.os.Environment;

import com.clockker.clockker.ClockkerEvent;
import com.clockker.clockker.ClockkerLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by villet on 28/05/2017.
 */

public class IOUtil {

    public static void writeEvent(Context context, ClockkerEvent event) throws IOException, JSONException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        File path = context.getExternalFilesDir(null);
        File file = new File(path, "event_log.txt");

        FileOutputStream stream = new FileOutputStream(file, true);
        stream.write(event.toJSON().toString().getBytes());
        stream.write(("\n").getBytes());

        stream.close();
    }

    public static List<ClockkerLocation> readLocations(Context context) throws IOException, JSONException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return null;
        }

        File path = context.getExternalFilesDir(null);
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

        List<ClockkerLocation> mLocations = new ArrayList<>();

        for (String line : lines) {
            if (line.length() > 0) {
                mLocations.add(ClockkerLocation.fromJSON(new JSONObject(line)));
            }
        }
        return mLocations;
    }


    public static void exportLocations(Context context, List<ClockkerLocation> locations) throws IOException, JSONException {
        // Check if we can write to external storage
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        File path = context.getExternalFilesDir(null);
        File file = new File(path, "list_of_locations.txt");

        FileOutputStream stream = new FileOutputStream(file);
        for (ClockkerLocation loc : locations) {
            stream.write(loc.toJSON().toString().getBytes());
            stream.write(("\n").getBytes());
        }

        stream.close();
    }

    public static boolean clearLocations(Context context) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }

        File dir = context.getExternalFilesDir(null);
        File file = new File(dir, "list_of_locations.txt");
        return file.delete();

    }

}
