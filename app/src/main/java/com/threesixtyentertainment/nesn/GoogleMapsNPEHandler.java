package com.threesixtyentertainment.nesn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * Workaround for a Google Maps bug that crashes the app sometimes.
 */
public class GoogleMapsNPEHandler extends BroadcastReceiver {

    private final static String ACTION_MAPS_NPE = "android.googlemapsnpe.new_crash";

    public static interface MapHost {
        void initMap(SupportMapFragment mapFragment);
        FragmentManager getChildFragmentManager();
    }

    private final MapHost mMapHost;

    public GoogleMapsNPEHandler(MapHost mapHost) {
        mMapHost = mapHost;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // TxODO: Dev only
        //UIHelper.show("NPE Raised");

        recreateMap();
    }

    private void recreateMap() {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();

        mMapHost.getChildFragmentManager().beginTransaction()
                .replace(R.id.map_holder, mapFragment)
                .commit();

        mMapHost.initMap(mapFragment);
    }

    public static void init() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {

                // Detect an exception thrown by Google Maps
                if (ex instanceof NullPointerException && thread.getName().startsWith("GLThread")) {
                    for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
                        if (stackTraceElement.getClassName().contains("maps")) {
                            MyApp.getContext().sendBroadcast(new Intent(ACTION_MAPS_NPE));
                            return;
                        }
                    }
                }
                // xTODO: Remove this test
//                else {
//                    MyApp.getContext().sendBroadcast(new Intent(ACTION_MAPS_NPE));
//                    return;
//                }

                defaultHandler.uncaughtException(thread, ex);
            }
        });
    }

    public static BroadcastReceiver register(MapHost mapHost) {
        BroadcastReceiver receiver = new GoogleMapsNPEHandler(mapHost);
        IntentFilter intentFilter = new IntentFilter(ACTION_MAPS_NPE);
        MyApp.getContext().registerReceiver(receiver, intentFilter);

        return receiver;
    }

    public static void unregister(BroadcastReceiver receiver) {
        MyApp.getContext().unregisterReceiver(receiver);
    }
}