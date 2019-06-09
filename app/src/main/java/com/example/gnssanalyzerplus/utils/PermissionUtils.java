package com.example.gnssanalyzerplus.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

public class PermissionUtils {

    /**
     * Returns true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     * @param activity
     * @param requiredPermissions
     * @return true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     */
    public static boolean hasGrantedPermissions(Activity activity, String[] requiredPermissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time
            return true;
        }
        for (String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
