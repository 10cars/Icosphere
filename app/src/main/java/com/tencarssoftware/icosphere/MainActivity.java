/*
 * Copyright (c) 2015 10cars Software
 */

package com.tencarssoftware.icosphere;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (supportsGlEs2()) {
            setContentView(R.layout.activity_main);
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
            }
        } else {
            Toast.makeText(this, getString(R.string.error_need_gles_20), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                new AboutDialogFragment().show(getSupportFragmentManager(), "aboutDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean supportsGlEs2() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

}
