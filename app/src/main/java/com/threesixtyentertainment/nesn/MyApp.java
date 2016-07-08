package com.threesixtyentertainment.nesn;

import android.app.Application;
import android.content.Context;

//import org.acra.ACRA;
//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;

// TODO: Dev only
/*@ReportsCrashes(formKey = "", // will not be used
        mailTo = "tomtanti@gmail.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)*/
public class MyApp extends Application
{
    private static Context appContext;

    public MyApp() {
        appContext = this;
    }

    public static Context getContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        GoogleMapsNPEHandler.init();

        //ACRA.init(this);      // TODO: Dev only
    }
}
