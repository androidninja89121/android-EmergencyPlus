package com.threesixtyentertainment.nesn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DisclaimerActivity extends Activity {
    public static final String PARAM_VIEW_ONLY = "ViewOnly";
    private static final String DISCLAIMER_RES_PREFIX = "DISCLAIMER_";

    public static String USER_ACCEPTED = "user_accepted";
    private static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted";
    private static final String PREFERENCES_EULA = "eula";
    private static final String LOG_TAG = "DisclaimerActivity";
    private static final String DISCLAIMER_FILE_NAME = "disclaimer.html";

    private Boolean viewOnly = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(LOG_TAG, "onCreate()");
		
		setContentView(R.layout.disclaimer);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
          viewOnly = (Boolean) bundle.getBoolean(PARAM_VIEW_ONLY);

        WebView wvDisclaimer = (WebView) findViewById(R.id.wvDisclaimer);
        wvDisclaimer.setVerticalFadingEdgeEnabled(true);

        String disclaimer = readAssetAsString(DISCLAIMER_FILE_NAME);
        disclaimer = disclaimer.replace("{content}", readDisclaimer(true));
        wvDisclaimer.loadData(disclaimer, "text/html; charset=utf-8", "utf-8");

        String description = readDisclaimer(false);
        wvDisclaimer.setContentDescription(description);

        Button disclaimerAgreeBtn = (Button) findViewById(R.id.disclaimerAgreeBtn);
        if (viewOnly) {
            disclaimerAgreeBtn.setText(R.string.Done);
            disclaimerAgreeBtn.setTextColor(getResources().getColor(R.color.navy));
        }
        else {
            disclaimerAgreeBtn.setText(R.string.IAgree);
            disclaimerAgreeBtn.setTextColor(getResources().getColor(R.color.dark_red));
        }
	}

	@Override
	public void onBackPressed() {
		//do nothing on back button pressed as we don't want to be able to exit this
		//screen unless the cancel or accept button is pressed
	}
	
	/*
	public void onCancelPressed(View view) {
		Log.i(LOG_TAG, "onCancelPressed()");
		
		//Google Analytics Tracking
        GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
        tracker.trackEvent("android", "disclaimer_canceled", "", -1);
        
        setResult(RESULT_CANCELED);
        finish();
	}
	*/
	
	public void onAcceptPressed(View view) {
		Log.i(LOG_TAG, "onAcceptPressed()");
		
        if (viewOnly)
            finish();
        else
            alert();

	}

	
	
	@Override
	protected void onStart() {
//    	GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
//    	tracker.trackPageView("/disclaimer_screen_android");
		super.onStart();
	}
	
	public static boolean eulaHasBeenAccepted (Context context) {
		boolean bEulaHasBeenAccepted = false;
		final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_EULA,
                Activity.MODE_PRIVATE);
        
        bEulaHasBeenAccepted = preferences.getBoolean(PREFERENCE_EULA_ACCEPTED, false);
        
        Log.i(LOG_TAG, "eulaHasBeenAccepted() = "+bEulaHasBeenAccepted);
        
        return bEulaHasBeenAccepted;
	}
	
	public void alert() {
	
		new AlertDialog.Builder(this)
	    //.setTitle("Important Information")
	    .setMessage(R.string.ReadAccept)
	    .setNegativeButton(R.string.IAgree, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            //continue with app
	        	goToNextPage();
	        }
	     })
	    .setPositiveButton(R.string.Cancel, null)
	    		
	    		
	    		/* new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 

	        } 
	     }) */
	     .show();
	}
	
	public void goToNextPage() {
		//Google Analytics Tracking
//        GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
//        tracker.trackEvent("android", "disclaimer_accepted", "", -1);
        
        final SharedPreferences preferences = getSharedPreferences(PREFERENCES_EULA, Activity.MODE_PRIVATE);
        preferences.edit().putBoolean(PREFERENCE_EULA_ACCEPTED, true).commit();
        
        setResult(RESULT_OK);
        finish();
        
	}

    public String readDisclaimer(boolean html) {
        StringBuilder builder = new StringBuilder();

        String headerTemplate = (html? "<h1>%s</h1>\n": "\n");
        String clauseTemplate = (html? "<p>%s</p>\n": "\n");

        builder.append(String.format(headerTemplate, getString(R.string.iosAgreement)));

        for (int i=1; i<=20; i++) {
            int resId = getResources().getIdentifier(DISCLAIMER_RES_PREFIX + i, "string", getPackageName());
            builder.append(String.format(clauseTemplate, getString(resId)));
        }

        return builder.toString();
    }

    public String readAssetAsString(String name) {
        InputStream ins = null;

        try {
            ins = getAssets().open(name);

            StringBuilder buf = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
            String str;

            while ((str = reader.readLine()) != null) {
                buf.append(str + "\n");
            }

            reader.close();
            return buf.toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (ins != null) {
                try {
                    ins.close();
                }
                catch (IOException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
            }
        }
    }

}
