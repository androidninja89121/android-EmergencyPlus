package com.threesixtyentertainment.nesn;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private final static int SECOND = 1;
    private final static int MINUTE = 60 * SECOND;
    private final static int HOUR = 60 * MINUTE;
    private final static int DAY = 24 * HOUR;
    private final static int MONTH = 30 * DAY;

    private final static double AUS_NORTH_LAT = -9.0;
    private final static double AUS_SOUTH_LAT = -44.0;
    private final static double AUS_WEST_LONG = 112.0;
    private final static double AUS_EAST_LONG = 154.0;

    private static String TAG = "LocationHelper";

    private static boolean initialised;
    private static Location latestLocation;
    private static String latestAddress;
    private static long lastAddressUpdated;
    private static Activity activity;
    private static String countryCode;

    public static void setActivity(Activity activity) {
        LocationHelper.activity = activity;
    }

    public static Location getLastKnownLocation(int minutes) {
        Criteria criteria = new Criteria();

        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(criteria, true);

        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        Location location;
        if (networkLocation != null && isBetterLocation(networkLocation, gpsLocation))
            location = networkLocation;
        else
            location = gpsLocation;

        if (location != null) {
            long elapse = System.currentTimeMillis() - location.getTime();
            if (elapse <= minutes * MINUTE * 1000)
                return location;
        }

        return null;
    }


    public static Location getLatestLocation() {
        return latestLocation;
    }

    public static void setLatestLocation(Location latestLocation) {
        LocationHelper.latestLocation = latestLocation;
    }

    public static String getLatestAddress() {
        return latestAddress;
    }

    public static boolean isInAustralia() {

        if (latestLocation == null)
            return false;

        if (countryCode == null || "".equals(countryCode)) {
            return latestLocation.getLatitude() >= AUS_SOUTH_LAT && latestLocation.getLatitude() <= AUS_NORTH_LAT &&
                   latestLocation.getLongitude() >= AUS_WEST_LONG && latestLocation.getLongitude() <= AUS_EAST_LONG;
        }
        else
          return "AU".equals(countryCode);
    }

    public static void setLatestAddress(String latestAddress, String countryCode, long timestamp) {
        LocationHelper.lastAddressUpdated = timestamp;
        LocationHelper.latestAddress = latestAddress;
        LocationHelper.countryCode = countryCode;
    }

    public static boolean isProviderAvailable() {
        LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean available = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!available)
            available = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return available;
    }

    public static String fuzzyTimeIntervalSinceNow() {
        final long start = lastAddressUpdated;
        long millis = System.currentTimeMillis() - start;
        int deltaInt = Math.round(millis / 1000);

        //Calculate the delta in seconds between the two dates
        if (deltaInt < 1 * MINUTE) {
            return deltaInt <= 1 ? getString(R.string.OneSecondAgo) : getString(R.string.SecondsAgo, deltaInt);
        }
        if (deltaInt < 2 * MINUTE){
            return getString(R.string.OneMinAgo);
        }
        if (deltaInt < 45 * MINUTE) {
            int minutes = (int)Math.floor((double)deltaInt/MINUTE);
            return getString(R.string.MinutesAgo, minutes);
        }
        if (deltaInt < 90 * MINUTE) {
            return getString(R.string.OneHourAgo);
        }
        if (deltaInt < 24 * HOUR) {
            int hours = (int)Math.floor((double)deltaInt/HOUR);
            return getString(R.string.HoursAgo, hours);
        }
//			    if (delta < 48 * HOUR) {
//			        return "yesterday";
//			    }
        if (deltaInt < 30 * DAY) {
            int days = (int)Math.floor((double)deltaInt/DAY);
            return getString(R.string.DaysAgo, days);
        }
        if (deltaInt < 12 * MONTH) {
            int months = (int)Math.floor((double)deltaInt/MONTH);
            return months <= 1 ? getString(R.string.OneMonthAgo): getString(R.string.MonthsAgo, months);
        } else {
            int years = (int)Math.floor((double)deltaInt/MONTH/12.0);
            return years <= 1 ? getString(R.string.OneYearAgo) : getString(R.string.YearsAgo, years);
        }
    }

    // Adapted from https://code.google.com/p/android/issues/detail?id=38009#c49
    public static List<Address> getFromLocation(double lat, double lng, int maxResult){

        String address = String.format(Locale.ENGLISH, "http://maps.googleapis.com/maps/api/geocode/json?latlng=%1$f,%2$f&sensor=true&language="+Locale.getDefault().getCountry(), lat, lng);
        HttpGet httpGet = new HttpGet(address);
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();

        List<Address> retList = null;

        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject = new JSONObject(stringBuilder.toString());


            retList = new ArrayList<Address>();


            if("OK".equalsIgnoreCase(jsonObject.getString("status"))) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i=0;i<results.length();i++ ) {
                    JSONObject result = results.getJSONObject(i);

                    Address addr = new Address(Locale.ENGLISH);
                    retList.add(addr);

                    // Get each address line
                    String[] components = result.getString("formatted_address").split(", ");
                    for (int j=0; j<components.length; j++)
                        addr.setAddressLine(j, components[j]);

                    // Get country code & name
                    JSONArray addrComponents = result.getJSONArray("address_components");
                    for (int k=0; k<addrComponents.length(); k++) {
                        JSONObject comp = addrComponents.getJSONObject(k);
                        JSONArray types = comp.getJSONArray("types");
                        for (int l=0; l<types.length(); l++) {
                            String type = types.getString(l);
                            if ("country".equals(type)) {
                                String countryCode = comp.getString("short_name");
                                String countryName = comp.getString("long_name");

                                addr.setCountryCode(countryCode);
                                addr.setCountryName(countryName);
                            }
                        }
                    }
                }
            }


        } catch (ClientProtocolException e) {
            Log.e(TAG, "Error calling Google geocode webservice.", e);
        } catch (IOException e) {
            Log.e(TAG, "Error calling Google geocode webservice.", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Google geocode webservice response.", e);
        }

        return retList;
    }

    // Adapted from http://developer.android.com/guide/topics/location/strategies.html
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isNewer = timeDelta > 0;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private static String getString(int resId, Object... formatArgs) {
        return MyApp.getContext().getString(resId, formatArgs);
    }
}
