package com.threesixtyentertainment.nesn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.text.DecimalFormat;

public class PhoneService extends Service {
    private static String mNumberPolice, mNumberEmergency, mNumberSES;
    private static boolean calling;
    private static boolean started;

    private static boolean makingCall;
    private static PhoneService instance;
    private static Context context;
    private static String service;
    private static Handler handler;
    private static Toast toast;
    private static boolean displayLocation;
    private static boolean appActive;

    public static void setAppActive(boolean appActive) {
        PhoneService.appActive = appActive;

        // Assume that when the app becomes active again, the call has been stopped.
        if (appActive)
            makingCall = false;
    }

    public class CustomPhoneStateListener extends PhoneStateListener {
        private static final String TAG = "NESN-CustomPhoneStateListener";
        private boolean toasted;

        @Override
        public void onCallStateChanged(int state, String number) {
            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "CALL_STATE_RINGING");
                    break;


                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                    calling = true;
                    break;


                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "CALL_STATE_IDLE");

                    calling = false;
                    toasted = false;
                    break;

            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("service", "destroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        Resources res = getResources();
        mNumberEmergency = res.getString(R.string.number_Emergency);
        mNumberPolice = res.getString(R.string.number_Police);
        mNumberSES = res.getString(R.string.number_SES);
        handler = new Handler();

        TelephonyManager telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        CustomPhoneStateListener customPhoneListener = new CustomPhoneStateListener();
        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        return START_STICKY;
    }

    static void outgoingCallDetected(String outgoingNo) {
        if (displayLocation)
          showLocation(outgoingNo, true);
    }

    static Toast getToast() {
        if (toast != null)
            return toast;

        Activity activity = (Activity) context;

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.toast, (ViewGroup) activity.findViewById(R.id.toast_layout_root));

        toast = new Toast(activity);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setView(view);

        return toast;
    }

    private static void updateLocationDetails(String outgoingNo, View view)
    {
        SherlockFragmentActivity activity = (SherlockFragmentActivity) context;

        View llNoGPS = view.findViewById(R.id.llNoGPS);
        View rlThumbMapArea = view.findViewById(R.id.rlThumMapArea);

        TextView tvCallingInfo1 = (TextView) view.findViewById(R.id.tvCallingInfo1);
        TextView tvCallingInfo2 = (TextView) view.findViewById(R.id.tvCallingInfo2);
        TextView tvLatValue = (TextView) view.findViewById(R.id.latValueTextView);
        TextView tvLongValue = (TextView) view.findViewById(R.id.longValueTextView);
        TextView tvAddress = (TextView) view.findViewById(R.id.addressTextView);
        TextView tvAccuracy = (TextView) view.findViewById(R.id.accuracyTextView);
        TextView tvAddressUpdated = (TextView) view.findViewById(R.id.addressUpdatedTextView);

        String callingInfo = String.format("Calling %s (%s)", service, outgoingNo);

        tvCallingInfo1.setText(callingInfo);
        tvCallingInfo2.setText(callingInfo);

        Location location = LocationHelper.getLatestLocation();
        if (location != null && LocationHelper.isProviderAvailable()) {
            llNoGPS.setVisibility(View.GONE);
            rlThumbMapArea.setVisibility(View.VISIBLE);

            llNoGPS.setVisibility(View.GONE);

            DecimalFormat decimalFormat = new DecimalFormat("#.00000");

            tvLatValue.setText(decimalFormat.format(location.getLatitude()));
            tvLongValue.setText(decimalFormat.format(location.getLongitude()));

            tvAccuracy.setText(activity.getResources().getString(R.string.Accurate, Math.round(location.getAccuracy())));

            String address = LocationHelper.getLatestAddress();
            if (address == null)
                tvAddressUpdated.setText("");
            else {
                tvAddress.setText(address);
                tvAddressUpdated.setText(activity.getResources().getString(R.string.AddressUpdated, LocationHelper.fuzzyTimeIntervalSinceNow()));
            }
        }
        else {
            llNoGPS.setVisibility(View.VISIBLE);
            rlThumbMapArea.setVisibility(View.GONE);
        }
    }

    static void showLocation(final String outgoingNo, boolean forceShow) {
        if (!appActive && (calling || forceShow)) {
            final Toast toast = getToast();
            updateLocationDetails(outgoingNo, toast.getView());
            toast.show();
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                if (calling)
                    showLocation(outgoingNo, false);
            }
        }, 3300);
    }

    public static boolean isCalling() {
        return calling;
    }

    public static boolean isMakingCall() {
        return makingCall;
    }

    public static void start(Context context) {
        if (!started) {
            Intent intent = new Intent(context, PhoneService.class);
            context.startService(intent);
            started = true;
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, PhoneService.class);
        context.stopService(intent);
        started = false;
    }

    public static void callEmergency(Context context) {
        if (LocationHelper.isProviderAvailable() && LocationHelper.isInAustralia())
            call(context, R.string.number_Emergency, "Emergency", R.string.LifeThreatCritical, true, true);
        else
            call(context, R.string.number_Emergency2, "Emergency", R.string.OutsideAus, true, true);
    }

    public static void callPolice(Context context) {
        call(context, R.string.number_Police, "Police", R.string.PoliceNonUrgent, false, true);
    }

    public static void callSES(Context context) {
        call(context, R.string.number_SES, "SES", R.string.StateEmergencyHelp, false, true);
    }

    public static void callCrimeStoppers(Context context) {
        call(context, R.string.number_CrimeStoppers, "CrimeStoppers", R.string.InfoPolice, false, false);
    }

    public static void callHealthDirect(Context context) {
        call(context, R.string.number_HealthDirect, "HealthDirect", R.string.HealthConcerns, false, false);
    }

    private static void call(final Context context, int stringId, final String service,
                             int descriptionId, final boolean emergency, final boolean displayLocation) {

        final String number = context.getResources().getString(stringId);
        final String description = context.getResources().getString(descriptionId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String title = MyApp.getContext().getString(R.string.CallString, number);
        builder.setTitle(title)
                .setMessage(description)
                .setPositiveButton(R.string.Call, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        makingCall = true;

                        PhoneService.service = service;
                        PhoneService.context = context;
                        PhoneService.displayLocation = displayLocation;

                        // Emergency number isn't allowed to be called using ACTION_CALL.
                        Intent intent = new Intent(emergency? Intent.ACTION_DIAL: Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + number));
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
               .show();
    }
}