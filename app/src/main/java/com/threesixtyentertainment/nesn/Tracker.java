package com.threesixtyentertainment.nesn;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;

import static com.google.android.gms.analytics.Logger.LogLevel;

public class Tracker {
    private static final String CATEGORY_UI_ACTION = "ui_action";
    private static final String ACTION_BUTTON_PRESS = "button_press";

    private static com.google.android.gms.analytics.Tracker tracker;

    synchronized public static com.google.android.gms.analytics.Tracker getTracker() {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(MyApp.getContext());
            tracker = analytics.newTracker(R.xml.app_tracker);
        }
        return tracker;
    }

    public static void trackPage(String screenName) {
        com.google.android.gms.analytics.Tracker t = getTracker();
        t.setScreenName(screenName);
        t.enableExceptionReporting(true);

        t.send(new HitBuilders.AppViewBuilder().build());
    }

    public static void trackButtonPressAction(String label) {
        // Get tracker.
        com.google.android.gms.analytics.Tracker t = getTracker();
        t.send(new HitBuilders.EventBuilder()
            .setCategory(CATEGORY_UI_ACTION)
            .setAction(ACTION_BUTTON_PRESS)
            .setLabel(label)
            .build());
    }
}
