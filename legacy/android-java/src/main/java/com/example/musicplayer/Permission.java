package com.example.musicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;


public class Permission {
    private final String[] permission={
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private final List<String> permissionList = new ArrayList<>();
    public int CODE=1;
    public void checkerPermission(Activity activity)
    {
        Log.e("premission","checkerPermission");
        for (String s : permission) {
            if (ContextCompat.checkSelfPermission(activity, s) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(s);
            }
        }
        if (permissionList.size()>0){
            requestPermission(activity);
        }
        Log.e("premission","checkerPermission over");

    }
    private void requestPermission(Activity activity){

        ActivityCompat.requestPermissions(activity,permissionList.toArray(new String[permissionList.size()]),CODE);
        Log.e("premission","requestPermission");
    }




}
