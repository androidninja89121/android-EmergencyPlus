package com.threesixtyentertainment.nesn;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import android.Manifest;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//flow of main activity -> display disclaimer if not agreed to already -> check if google play services are available -> load tabs

public class MainActivity extends TabSwipeActivity implements LocationListener {
	
	private static final int HOME_TAB_POSITION = 0;
	private static final int MAP_TAB_POSITION = 1;
	private static final int INFO_TAB_POSITION = 2;
	
	public static final int DISCLAIMER_REQUEST = 99;
    private static final int MAX_AGE = 15;       // Minutes
    private static String TAG = "NESN-MainActivity";
	private static NESNMapFragment mMapFragment;
	private static HomeFragment mHomeFragment;
	public static Location mLatestLocation;
    public static String mLatestAddress;
    private static boolean mLocationProviderEnabled;

    private NESNLocationListener mCurListener;
    private BroadcastReceiver brAirplaneMode, brOutgoingCall;
    private boolean mLocationListenerSetup;
    private int googlePlayStatus;

    @Override
	public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


        LocationHelper.setActivity(this);

//		setContentView(R.layout.activity_main);

		//display the weight activity
        if (!DisclaimerActivity.eulaHasBeenAccepted(this)) {
        	Intent intent = new Intent(this, DisclaimerActivity.class);
            //startActivity(intent);
            startActivityForResult(intent, DISCLAIMER_REQUEST);
        } else {
        	checkGooglePlayServices();
        }

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setLogo(new ColorDrawable(Color.TRANSPARENT));
        //actionBar.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.header));

        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setLogo(null);
        actionBar.setCustomView(R.layout.header);

        initReceivers();
        PhoneService.start(this);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(brOutgoingCall, intentFilter);

        changeTitle();
	}

    private void changeTitle() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              setTitle(getResources().getString(R.string.app_name));
            }
        }, 20000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    Log.d(TAG, "onRequestPermissionsResult: ");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    Log.d(TAG, "onRequestPermissionsResult: ");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (homeFragment() != null)
          homeFragment().adjustButtonSize();
    }

    @Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		
		if (fragment instanceof NESNLocationListener) {
			if (mLatestLocation != null) { 
				((NESNLocationListener) fragment).onLocationChanged(mLatestLocation);	
			}
			if (mLatestAddress != null) {
				((NESNLocationListener) fragment).onAddressChanged(mLatestAddress);
			}
		}
		
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PhoneService.stop(this);
        unregisterReceiver(brOutgoingCall);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationHelper.setActivity(this);
        PhoneService.setAppActive(true);
        if (googlePlayStatus == ConnectionResult.SUCCESS && !mLocationListenerSetup)
            setupLocationListener();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(brAirplaneMode, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        PhoneService.setAppActive(false);
        unregisterReceiver(brAirplaneMode);

        if (!PhoneService.isMakingCall())
            removeLocationListener();
    }

    private void checkGooglePlayServices() {
		//Getting Google Play availability status
//		googlePlayStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        googlePlayStatus = googleApiAvailability.isGooglePlayServicesAvailable(getBaseContext());

		//show status
		if (googlePlayStatus != ConnectionResult.SUCCESS) {
			//Google Play Services are not available
			int requestCode = 10; //??? Why 10??
//			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayStatus, this, requestCode);
//			dialog.show();
            if (googleApiAvailability.isUserResolvableError(googlePlayStatus)) {
                googleApiAvailability.getErrorDialog(this, googlePlayStatus, requestCode).show();
            }
		} else {
			//Google Play Services are available... on with the show
			tabSetup();
		}
	}

    private void initReceivers() {
        brOutgoingCall = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String outgoingNo = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                PhoneService.outgoingCallDetected(outgoingNo);
            }
        };

        brAirplaneMode = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean airplanModeEnabled = intent.getBooleanExtra("state", false);
                if (airplanModeEnabled)
                  showAirplaneModeAlert();
            }
        };

        if (isAirplaneModeOn())
            showAirplaneModeAlert();
    }

    public boolean isAirplaneModeOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            return Settings.System.getInt(getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        else
            return Settings.Global.getInt(getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static void setHomeFragment(HomeFragment homeFragment) {
        if (mHomeFragment != homeFragment) {
            mHomeFragment = homeFragment;

            updateLocationAndAddress(mHomeFragment);
        }
    }

    public static void setMapFragment(NESNMapFragment mapFragment) {
        if (mMapFragment != mapFragment) {
            mMapFragment = mapFragment;

            updateLocationAndAddress(mMapFragment);
        }
    }

    private static void updateLocationAndAddress(NESNLocationListener listener) {
        listener.onLocationServiceStatusChanged(mLocationProviderEnabled);

        if (mLatestAddress != null)
            listener.onAddressChanged(mLatestAddress);

        if (mLatestLocation != null)
            listener.onLocationChanged(mLatestLocation);
    }

    private void showAirplaneModeAlert() {
        UIHelper.showAlert(this, R.string.TurnOffAirplane, android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
    }

    private void showNoGPSAlert() {
        UIHelper.showAlert(this, R.string.noGPSDialogMessage, android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

	private void tabSetup() {
		//create the tabs we want to display
		addTab(getString(R.string.Home), R.drawable.icon_home, HomeFragment.class, NESNBaseFragment.createBundle("Home"));
		addTab(getString(R.string.Map), R.drawable.icon_map, NESNMapFragment.class, NESNBaseFragment.createBundle("Map"));
		addTab(getString(R.string.iosInfo), R.drawable.icon_info, InfoFragment.class, NESNBaseFragment.createBundle("Info"));
        // addTab("Debug", R.drawable.icon_info, DebugFragment.class, NESNBaseFragment.createBundle("Debug"));
    }
	
	private void setupLocationListener() {		
        mLocationListenerSetup = true;

        Location location = LocationHelper.getLastKnownLocation(MAX_AGE);
        if (location != null) {
            onLocationChanged(location);
        }

        //Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, this);
	}

    private void removeLocationListener() {
        mLocationListenerSetup = false;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
			case DISCLAIMER_REQUEST:
				if (resultCode == RESULT_CANCELED) {
					finish();
				} else if (resultCode == RESULT_OK) {
					checkGooglePlayServices();
				}
				break;
		}
	}

	@Override
	public synchronized void onLocationChanged(Location location) {

        if (!LocationHelper.isBetterLocation(location, mLatestLocation))
            return;

        mLatestLocation = location;

        LocationHelper.setLatestLocation(location);

		//Setting latitude and longitude in the TextView
		Log.d(TAG, "onLocationChanged - Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
		
		//pass the change of location to the map
		NESNMapFragment mapFragment = mapFragment();
		if (mapFragment != null) {
			mapFragment.onLocationChanged(location);
		}
		
		//pass the change of location to the home thumb map
		HomeFragment homeFragment = homeFragment();
		if (homeFragment != null) {
			homeFragment.onLocationChanged(location);
		}
		
		getAddressFromLocation(mLatestLocation, this, new GeocoderHandler(this));
	}
	
	public void onUpdatedAddress(String address, String countryCode, long timestamp) {
		Log.d(TAG, "onUpdatedAddress: " + address);

        LocationHelper.setLatestAddress(address, countryCode, timestamp);

		//pass the change of location to the map
		NESNMapFragment mapFragment = mapFragment();
		if (mapFragment != null) {
			mapFragment.onAddressChanged(address);
		}
		
		//pass the change of location to the home thumb map
		HomeFragment homeFragment = homeFragment();
		if (homeFragment != null) {
			homeFragment.onAddressChanged(address);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
        mLocationProviderEnabled = LocationHelper.isProviderAvailable();

        if (!mLocationProviderEnabled) {
            if (mCurListener != null)
                mCurListener.onLocationServiceStatusChanged(false);
            showNoGPSAlert();
        }
	}

	@Override
	public void onProviderEnabled(String provider) {
        mLocationProviderEnabled = LocationHelper.isProviderAvailable();
        if (mCurListener != null)
            mCurListener.onLocationServiceStatusChanged(true);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	//helpful getter to make sure that we have a calid fragment
	public HomeFragment homeFragment() {

        if (mHomeFragment != getFragmentForPosition(HOME_TAB_POSITION)) {
            Log.d(TAG, "mHomeFragment: " + mHomeFragment);
            Log.d(TAG, "getFragment: " + getFragmentForPosition(HOME_TAB_POSITION));
        }

        if (mHomeFragment == null) {
			mHomeFragment = (HomeFragment) getFragmentForPosition(HOME_TAB_POSITION);
		}

		return mHomeFragment;
	}
	
	//helpful getter to make sure that we have a valid fragment
	public NESNMapFragment mapFragment() {

        if (mMapFragment != getFragmentForPosition(MAP_TAB_POSITION)) {
            Log.d(TAG, "mMapFragment: " + mMapFragment);
            Log.d(TAG, "getFragment: " + getFragmentForPosition(MAP_TAB_POSITION));
        }

		if (mMapFragment == null) {
			mMapFragment = (NESNMapFragment) getFragmentForPosition(MAP_TAB_POSITION);
		}
		
		return mMapFragment;
	}

	public InfoFragment infoFragment() {
		return (InfoFragment) getFragmentForPosition(INFO_TAB_POSITION);
	}

	@Override
	public void onTabSelected(int tabPosition) {
        NESNBaseFragment fragment = null;
		switch (tabPosition) {
            case MAP_TAB_POSITION:
                NESNMapFragment mapFragment = mapFragment();
                fragment = mapFragment;
                mCurListener = mapFragment;
                if (mapFragment != null)
                  mapFragment.onLocationServiceStatusChanged(LocationHelper.isProviderAvailable());
                break;

            case HOME_TAB_POSITION:
                HomeFragment homeFragment = homeFragment();
                fragment = homeFragment;
                mCurListener = homeFragment;
                if (homeFragment != null)
                    homeFragment.onLocationServiceStatusChanged(LocationHelper.isProviderAvailable());
                break;

            case INFO_TAB_POSITION:
                fragment = infoFragment();

            default:
                mCurListener = null;
                break;
		}

        if (fragment != null)
            com.threesixtyentertainment.nesn.Tracker.trackPage(fragment.getScreenName());

        //checkLocationService(mCurListener);
	}
	
	public static void getAddressFromLocation(
	        final Location location, final Context context, final Handler handler) {
	    Thread thread = new Thread() {
	        @Override public void run() {
	            Geocoder geocoder = new Geocoder(context);
                List<Address> list = null;

	            try {
                    // Check to make sure that the underlying platform supports it.
                    if (geocoder.isPresent()) {
                        list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        Log.d(TAG, "getAddress: " + list);
                    }
                    else
                        list = LocationHelper.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

	            } catch (IOException e) {
	                Log.e(TAG, "Impossible to connect to Geocoder", e);
                    list = LocationHelper.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

	            } finally {
                    Address address = null;

                    if (list != null && list.size() > 0) {
                        address = list.get(0);
                    }
                    else
                        return;

                    String countryCode = address.getCountryCode();

                    // sending back first address line and locality
                    //result = address.getAddressLine(0) + ", " + address.getLocality();
                    String result = "";
                    for (int i = 0; i < address.getMaxAddressLineIndex(); ++i) {
                        String addressLine = address.getAddressLine(i);
                        if (addressLine != address.getCountryName()) {
                            if (result.length() > 0) {
                                result += "\n";
                            }
                            result += addressLine;
                        }
                    }

	                Message msg = Message.obtain();
	                msg.setTarget(handler);
	                if (result != null) {
	                    msg.what = 1;
	                    Bundle bundle = new Bundle();
	                    bundle.putString("address", result);
                        bundle.putString("countryCode", countryCode);
                        bundle.putLong("timestamp", location.getTime());
	                    msg.setData(bundle);
	                } else 
	                    msg.what = 0;
	                msg.sendToTarget();
	            }
	        }
	    };
	    thread.start();
	}

	private static class GeocoderHandler extends Handler {
		private final WeakReference<MainActivity> mMainActivity;
		
		GeocoderHandler(MainActivity activity) {
	        mMainActivity = new WeakReference<MainActivity>(activity);
	    }
		
	    @Override
	    public void handleMessage(Message message) {
	        String result, countryCode;
            long timestamp;
	        switch (message.what) {
	        case 1:
	            Bundle bundle = message.getData();
	            result = bundle.getString("address");
                countryCode = bundle.getString("countryCode");
                timestamp = bundle.getLong("timestamp");
	            break;
	        default:
	            result = null;
                countryCode = null;
                timestamp = 0;
	        }
	        // replace by what you need to do
	        if (result != null) {
	        	MainActivity mainActivity = mMainActivity.get();
                if (mainActivity != null)
	        	  mainActivity.onUpdatedAddress(result, countryCode, timestamp);
	        }
	    }   
	}
	
	
}
