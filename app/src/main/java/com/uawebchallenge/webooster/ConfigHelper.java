package com.uawebchallenge.webooster;

import android.content.Context;

/**
 * @author Alexander Semenov
 */
public class ConfigHelper {

    public static boolean getUseGoogleProxy(Context context){
        return context.getSharedPreferences(ProxyVpnService.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(ProxyVpnService.PREFS_ENABLE_GOOGLE_COMPRESSION, true);
    }

    public static void setUseGoogleProxy(Context context, boolean useProxy){
        context.getSharedPreferences(ProxyVpnService.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ProxyVpnService.PREFS_ENABLE_GOOGLE_COMPRESSION, useProxy);
    }


}
