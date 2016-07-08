package com.threesixtyentertainment.nesn;

import android.content.BroadcastReceiver;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.Locale;


public class HomeFragment extends NESNBaseFragment implements OnClickListener, NESNLocationListener, GoogleMapsNPEHandler.MapHost {
    private static final String SCREEN_NAME = "Main Screen";
    private static final String BUTTON_PRESS_POLICE_ASSIST = "Police Assist button";
    private static final String BUTTON_PRESS_000 = "000 button";
    private static final String BUTTON_PRESS_SES = "SES button";

    private static final String LANGUAGE_JAPANESE = "jpn";
    private static final String LANGUAGE_ENGLISH = "eng";
    private static String TAG = "NESN-HomeFragment";
    private static View mView;

    GoogleMap mGoogleMap;
    TextView mLatValueTextView;
    TextView mLongValueTextView;
    Location mLatestLocation;
    String mLatestAddress;
    TextView mAddressTextView;
    private int newHeight;
    private LinearLayout llHome, llTopSection;
    private RelativeLayout rlThumbMapArea;
    private Handler mHandler;
    private ImageButton ibtnCall000, ibtnCallSES, ibtnCallPolice;
    private long mLastNotified;
    private BroadcastReceiver npeReceiver;
    private boolean mViewDestroyed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mViewDestroyed = false;

        if (mView != null) {
            ViewGroup parent = (ViewGroup) mView.getParent();
            if (parent != null) {
                parent.removeView(mView);
            }
        }
        try {
            mView = inflater.inflate(R.layout.home_fragment, container, false);
        } catch (InflateException ie) {
    		/* map is already there, just return mView as it is */
        }

        llHome = (LinearLayout) mView.findViewById(R.id.llHome);
        llTopSection = (LinearLayout) mView.findViewById(R.id.llTopSection);

        mLatValueTextView = (TextView) mView.findViewById(R.id.latValueTextView);
        mLongValueTextView = (TextView) mView.findViewById(R.id.longValueTextView);
        mAddressTextView = (TextView) mView.findViewById(R.id.addressTextView);

        String langCode = Locale.getDefault().getISO3Language();
        if (!LANGUAGE_ENGLISH.equals(langCode)) {
            mAddressTextView.setTextSize(mAddressTextView.getTextSize() - 14);
        }

        npeReceiver = GoogleMapsNPEHandler.register(this);

        //Getting reference to the SupportMapFragment
        SupportMapFragment mSupportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.thumb_map);
        initMap(mSupportMapFragment);

        ibtnCallPolice = (ImageButton) mView.findViewById(R.id.call_police_btn);
        ibtnCallPolice.setOnClickListener(this);

        ibtnCallSES = (ImageButton) mView.findViewById(R.id.call_ses_btn);
        ibtnCallSES.setOnClickListener(this);

        ibtnCall000 = (ImageButton) mView.findViewById(R.id.call_000_btn);
        ibtnCall000.setOnClickListener(this);

        rlThumbMapArea = (RelativeLayout) mView.findViewById(R.id.rlThumMapArea);
        rlThumbMapArea.setOnClickListener(this);

        updateLocationView(LocationHelper.isProviderAvailable());

        // Update the cached fragment with a possible new one.
        MainActivity.setHomeFragment(this);

        Log.d(TAG, "onCreateView - " + this);

        return mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewDestroyed = true;
        GoogleMapsNPEHandler.unregister(npeReceiver);
    }

    private GoogleMap.OnMyLocationChangeListener getListenerOnMyLocationChanged() {
        return new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {

                long cur = System.currentTimeMillis();

                // Update every 5 secs used as a backup location provider, due to a bug in
                // Network provider (Android >= 4)
                if (cur - mLastNotified > 5000) {
                    mLastNotified = cur;
                    ((MainActivity) getActivity()).onLocationChanged(location);
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - " + this);

        if (mLatestLocation != null && LocationHelper.isProviderAvailable())
            onLocationChanged(mLatestLocation);

        adjustButtonSize();
    }

    @Override
    public void initMap(final SupportMapFragment mapFragment) {

        if (mViewDestroyed)
            return;

//        mGoogleMap = mapFragment.getMap();
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
            }
        });

        if (mGoogleMap == null) {
            if (mHandler == null)
                mHandler = new Handler();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initMap(mapFragment);
                }
            }, 100);

            return;
        }

        mGoogleMap.setOnMyLocationChangeListener(getListenerOnMyLocationChanged());

        //Enable MyLocation Layer of Google Map
        mGoogleMap.setMyLocationEnabled(true);

        Location location = LocationHelper.getLatestLocation();
        if (location == null)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-25, 135)));
        else
            onLocationChanged(location);

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.selectTab(1);
            }
        });

        //remove zoom controls
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setAllGesturesEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
    }

    @Override
    protected String getScreenName() {
        return SCREEN_NAME;
    }

    public void adjustButtonSize() {

        if (llHome.getMeasuredHeight() == newHeight)
            return;

        int width; //, height;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
            //height = size.y;
        }
        else {
            width = display.getWidth();
            //height = display.getHeight();
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float inches = width / (float) metrics.xdpi;

        if (inches > 2) {
            float ratio = (inches > 3? 1.1f: 1.3f);

            newHeight = (int) (width * ratio);
            if (newHeight < llHome.getMeasuredHeight()) {
                LinearLayout.LayoutParams paramsHome = (LinearLayout.LayoutParams) llHome.getLayoutParams();
                paramsHome.height = newHeight;
            }
        }
        else
            newHeight = llHome.getMeasuredHeight();
    }

    public void callPoliceBtnOnClick(View view) {
        PhoneService.callPolice(getActivity());
    }

    public void call000BtnOnClick(View view) {
        PhoneService.callEmergency(getActivity());
    }

    public void callSesBtnOnClick(View view) {
        PhoneService.callSES(getActivity());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.call_police_btn:
                Tracker.trackButtonPressAction(BUTTON_PRESS_POLICE_ASSIST);
                callPoliceBtnOnClick(view);
                break;

            case R.id.call_000_btn:
                Tracker.trackButtonPressAction(BUTTON_PRESS_000);
                call000BtnOnClick(view);
                break;

            case R.id.call_ses_btn:
                Tracker.trackButtonPressAction(BUTTON_PRESS_SES);
                callSesBtnOnClick(view);
                break;

            case R.id.rlThumMapArea:
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.selectTab(1);
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged - Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());

        mLatestLocation = location;

        updateLocationView(true);
        updateMapWithLocation(location);

        DecimalFormat decimalFormat = new DecimalFormat("#.00000");

        if (mLatValueTextView != null) {
            mLatValueTextView.setText(decimalFormat.format(location.getLatitude()));
        }
        if (mLongValueTextView != null) {
            mLongValueTextView.setText(decimalFormat.format(location.getLongitude()));
        }
    }

    @Override
    public void onLocationServiceStatusChanged(boolean enabled) {
        updateLocationView(enabled);
    }

    @Override
    public void onAddressChanged(String address) {

        Log.d(TAG, "onAddressChanged - " + this);
        Log.d(TAG, "onAddressChanged: " + address);
        Log.d(TAG, "mAddressTextView: " + mAddressTextView);

        mLatestAddress = address;

        if (mAddressTextView != null)
            mAddressTextView.setText(mLatestAddress);
    }

    private void updateMapWithLocation(Location location) {
        if (location != null) {
            //Creating a LatLng object for the current location
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (mGoogleMap != null) {
                //Show the current location in Google Map
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                //Zoom in the Google Map
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        }
    }

    private void updateLocationView(boolean providerAvailable) {
        if (mView != null) {
            View llNoGPS = mView.findViewById(R.id.llNoGPS);

            if (llNoGPS != null && rlThumbMapArea != null)
            {
                if (providerAvailable) {
                    llNoGPS.setVisibility(View.GONE);
                    rlThumbMapArea.setVisibility(View.VISIBLE);
                }
                else {
                    llNoGPS.setVisibility(View.VISIBLE);
                    rlThumbMapArea.setVisibility(View.GONE);
                }
            }
        }
    }
}
