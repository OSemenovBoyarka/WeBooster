package com.uawebchallenge.webooster;

import android.content.Intent;
import android.net.VpnService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat vpnSwitch;

    private static final int REQUEST_CODE_VPN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO move UI to fragment
        vpnSwitch = (SwitchCompat) findViewById(R.id.sw_activate_vpn);
        vpnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    enableProxy();
                } else {
                    stopProxy();
                }
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
        if (intent != null){
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
