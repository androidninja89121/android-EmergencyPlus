/**
 * 
 */
package com.threesixtyentertainment.nesn;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class SplashActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        setTitle(getResources().getString(R.string.starting_app));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            finish();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);

            changeTitle();
          }
        }, 200);
    }

    private void changeTitle() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setTitle(getResources().getString(R.string.app_name));
                SplashActivity.this.finish();
            }
        }, 30000);
    }
}
