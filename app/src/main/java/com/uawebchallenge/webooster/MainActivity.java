package com.uawebchallenge.webooster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VPN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO move UI to fragment
        initViews();
    }

    private void initViews() {
        SwitchCompat vpnSwitch = (SwitchCompat) findViewById(R.id.sw_activate_vpn);
        vpnSwitch.setChecked(ProxyVpnService.isProxyRunning());
        vpnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableProxy();
                } else {
                    stopProxy();
                }
            }
        });

        boolean enableCompression = ConfigHelper.getUseGoogleProxy(this);
        SwitchCompat googleCompressionSwitch = (SwitchCompat) findViewById(
                R.id.sw_use_google_compression);
        googleCompressionSwitch.setChecked(enableCompression);
        googleCompressionSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        ConfigHelper.setUseGoogleProxy(MainActivity.this, isChecked);
                        Intent intent = new Intent(ProxyVpnService.PREFS_UPDATE_ACTION);
                        sendBroadcast(intent);
                    }
                });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VPN && resultCode == RESULT_OK) {
            startProxyVpnService();
        }
    }


    //region proxy control
    private void enableProxy() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_VPN);
        } else {
            startProxyVpnService();
        }
    }

    public void stopProxy() {
        Intent intent = new Intent(ProxyVpnService.DISCONNECT_ACTION);
        sendBroadcast(intent);
    }

    private void startProxyVpnService() {
        Intent serviceIntent = new Intent(this, ProxyVpnService.class);
        startService(serviceIntent);
    }
    //endregion
}
