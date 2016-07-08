package com.threesixtyentertainment.nesn;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;


public class NESNMapFragment extends NESNBaseFragment implements NESNLocationListener, GoogleMapsNPEHandler.MapHost {
    private static final String SCREEN_NAME = "Map Screen";
    private static final String BUTTON_PRESS_000 = "map 000 button";
    private static String TAG = "NESNMapFragment";

    private static View mView;
    GoogleMap mGoogleMap;
    Location mLatestLocation;
    String mLatestAddress;
    long mAddressUpdatedTimeStamp;
    TextView mAddressTextView;
    TextView mLatValueTextView;
    TextView mLongValueTextView;
    TextView mAddressUpdatedTextView;
    TextView mAddressUpdatedOtherTextView;
    TextView mAccuracyTextView;
    TextView mGPSTitleTextView;
    private Handler mAddressUpdatedHandler = new Handler();
    DecimalFormat mDecimalFormat = new DecimalFormat("#.00000");
    private boolean firstFix;
    private ImageButton ibtnMyLocation;
    private boolean moveToCurrentLocation;
    private boolean mViewDestroyed;
    private Handler mHandler;
    private BroadcastReceiver npeReceiver;
    boolean isBig = false;

    /**
     * Need to be careful with the MapView as it lingers around and we don't want to have it inflated twice
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");
        mViewDestroyed = false;

        if (mView != null) {
            ViewGroup parent = (ViewGroup) mView.getParent();
            if (parent != null) {
                parent.removeView(mView);
            }
        }
        try {
            //inflate and return the layout
            mView = inflater.inflate(R.layout.map_fragment, container, false);
        } catch (InflateException ie) {
    		/* map is already there, just return mView as it is */
        }

        //getting text views from the layout
        mGPSTitleTextView = (TextView) mView.findViewById(R.id.gpsCoordinates);
        mAddressTextView = (TextView) mView.findViewById(R.id.addressTextView);
        mLatValueTextView = (TextView) mView.findViewById(R.id.latValueTextView);
        mLongValueTextView = (TextView) mView.findViewById(R.id.longValueTextView);
        mAddressUpdatedTextView = (TextView) mView.findViewById(R.id.addressUpdatedTextView);
        mAddressUpdatedOtherTextView = (TextView) mView.findViewById(R.id.addressUpdatedOtherTextView);
        mAccuracyTextView = (TextView) mView.findViewById(R.id.accuracyTextView);

        ibtnMyLocation = (ImageButton) mView.findViewById(R.id.ibtnMyLocation);
        ibtnMyLocation.setOnClickListener(getListenerOnClickIbtnMyLocation());

        TextView tvNoGPS = (TextView) mView.findViewById(R.id.tvNoGPS);
        tvNoGPS.setText(getString(R.string.LocationDisabled, getString(R.string.EmergencyPlus)));

        Button btnCall000 = (Button) mView.findViewById(R.id.btnCall000);
        btnCall000.setOnClickListener(getListenerOnClickBtnCall000());

        npeReceiver = GoogleMapsNPEHandler.register(this);

        //Getting reference to the SupportMapFragment
        SupportMapFragment mSupportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        initMap(mSupportMapFragment);

        //updateMapWithLocation(mLatestLocation);
        MainActivity.setMapFragment(this);

        mView.findViewById(R.id.rlAddressLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBig = !isBig;

                if(isBig) {
                    mGPSTitleTextView.setTextSize(15);
                    mAddressTextView.setTextSize(15);
                    mLatValueTextView.setTextSize(18);
                    mLongValueTextView.setTextSize(18);
                    mAccuracyTextView.setTextSize(15);
                    mAddressUpdatedTextView.setVisibility(View.GONE);
                    mAddressUpdatedOtherTextView.setVisibility(View.VISIBLE);
                } else {
                    mGPSTitleTextView.setTextSize(12);
                    mAddressTextView.setTextSize(12);
                    mLatValueTextView.setTextSize(14);
                    mLongValueTextView.setTextSize(14);
                    mAccuracyTextView.setTextSize(12);
                    mAddressUpdatedTextView.setVisibility(View.VISIBLE);
                    mAddressUpdatedOtherTextView.setVisibility(View.GONE);
                }

            }
        });

        mAddressUpdatedOtherTextView.setVisibility(View.GONE);

        return mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewDestroyed = true;

        //stop updating the text view
        mAddressUpdatedHandler.removeCallbacks(mUpdateAddressUpdatedTask);
        GoogleMapsNPEHandler.unregister(npeReceiver);
    }

    @Override
    protected String getScreenName() {
        return SCREEN_NAME;
    }

    private View.OnClickListener getListenerOnClickBtnCall000() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tracker.trackButtonPressAction(BUTTON_PRESS_000);
                PhoneService.callEmergency(getActivity());
            }
        };
    }

    private GoogleMap.OnCameraChangeListener getListenerOnCameraChangeGoogleMap() {
        return new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (moveToCurrentLocation)
                    moveToCurrentLocation = false;

                    // If manually moved, change the nav icon.
                else
                    ibtnMyLocation.setBackgroundResource(R.drawable.navlocate);
            }
        };
    }

    private View.OnClickListener getListenerOnClickIbtnMyLocation() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibtnMyLocation.setBackgroundResource(R.drawable.navblue);
                showLocationInMap(mLatestLocation);
            }
        };
    }

    @Override
    public void onLocationChanged(Location location) {
        mLatestLocation = location;

        updateLocationView(true);
        updateMapWithLocation(mLatestLocation);
    }

    @Override
    public void onLocationServiceStatusChanged(boolean enabled) {
        updateLocationView(enabled);
    }

    @Override
    public void onAddressChanged(String address) {
        mLatestAddress = address;
        mAddressUpdatedTimeStamp = System.currentTimeMillis();

        updateAddressInfoWithAddress(mLatestAddress);
    }

    @Override
    public void initMap(final SupportMapFragment mapFragment) {
        if (mViewDestroyed)
            return;

        mGoogleMap = mapFragment.getMap();
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

        firstFix = true;

        //Enable MyLocation Layer of Google Map
        mGoogleMap.setMyLocationEnabled(true);

        Location location = LocationHelper.getLatestLocation();
        if (location == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-25, 135)));
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(2.5f));
        }
        else
            onLocationChanged(location);

        mGoogleMap.setOnCameraChangeListener(getListenerOnCameraChangeGoogleMap());

        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMyLocationButtonEnabled(false);
    }

    private void updateMapWithLocation(Location location) {
        if (location != null && getActivity() != null) {

            if (firstFix && mGoogleMap != null && location != null) {
                firstFix = false;
                ibtnMyLocation.setBackgroundResource(R.drawable.navblue);
                showLocationInMap(location);
            }

            if (mLatValueTextView != null) {
                mLatValueTextView.setText(mDecimalFormat.format(location.getLatitude()));
            }
            if (mLongValueTextView != null) {
                mLongValueTextView.setText(mDecimalFormat.format(location.getLongitude()));
            }

            if (mAccuracyTextView != null) {
                mAccuracyTextView.setText(getActivity().getResources().getString(R.string.Accurate, Math.round(location.getAccuracy())));
            }
        }
    }

    private void showLocationInMap(Location location) {
        if (mGoogleMap != null && location != null) {
            //Creating a LatLng object for the current location
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            //Show the current location in Google Map
            moveToCurrentLocation = true;
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void updateAddressInfoWithAddress(String address) {
        if (mAddressTextView != null) {
            mAddressTextView.setText(address);
        }

        mAddressUpdatedHandler.removeCallbacks(mUpdateAddressUpdatedTask);
        mAddressUpdatedHandler.post(mUpdateAddressUpdatedTask);
    }

    private Runnable mUpdateAddressUpdatedTask = new Runnable() {
        public static final int SECOND = 1;
        public static final int MINUTE = 60 * SECOND;
        public static final int HOUR = 60 * MINUTE;
        public static final int DAY = 24 * HOUR;
        public static final int MONTH = 30 * DAY;

        public void run() {
            if (getActivity() != null) {
                if (mAddressUpdatedTextView != null) {
                    mAddressUpdatedTextView.setText(getActivity().getResources().getString(R.string.AddressUpdated, LocationHelper.fuzzyTimeIntervalSinceNow()));
                    mAddressUpdatedOtherTextView.setText(getActivity().getResources().getString(R.string.AddressUpdated, LocationHelper.fuzzyTimeIntervalSinceNow()));
                }

                mAddressUpdatedHandler.postDelayed(this, 200);
            }
        }
    };

    private void updateLocationView(boolean locationAvailable) {
        if (mView != null) {
            View rlAddressLayout = (RelativeLayout) mView.findViewById(R.id.rlAddressLayout);
            View llNoGPS = mView.findViewById(R.id.rlNoGPS);

            if (locationAvailable) {
                llNoGPS.setVisibility(View.GONE);
                rlAddressLayout.setVisibility(View.VISIBLE);
            }
            else {
                llNoGPS.setVisibility(View.VISIBLE);
                rlAddressLayout.setVisibility(View.GONE);
            }
        }
    }
}
