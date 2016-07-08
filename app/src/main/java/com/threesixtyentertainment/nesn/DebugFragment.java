package com.threesixtyentertainment.nesn;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class DebugFragment extends NESNBaseFragment {
    private static final String SCREEN_NAME = "Debug Screen";

    private TextView tvLogs;
    private Handler handler;
    private Runnable runnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.debug_fragment, container, false);

        tvLogs = (TextView) view.findViewById(R.id.tvLogs);

        Button btnEmail = (Button) view.findViewById(R.id.btnEmail);
        btnEmail.setOnClickListener(getListenerOnClickBtnEmail());

        runnable = getRunnable();

        handler = new Handler();
        handler.postDelayed(runnable, 1000);
        return view;
    }

    @Override
    protected String getScreenName() {
        return SCREEN_NAME;
    }

    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                Location location = LocationHelper.getLatestLocation();
                if (location != null) {
                    StringBuilder b = new StringBuilder();

                    b.append(location.toString() + "\n");

                    String address = LocationHelper.getLatestAddress();
                    if (address != null) {
                        b.append("Address updated " + LocationHelper.fuzzyTimeIntervalSinceNow() + "\n");
                        b.append(LocationHelper.getLatestAddress() + "\n");
                    }

                    tvLogs.setText(b);
                }

                handler.postDelayed(runnable, 1000);
            }
        };
    }

    private View.OnClickListener getListenerOnClickBtnEmail() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        };
    }
}
