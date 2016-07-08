package com.threesixtyentertainment.nesn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class InfoFragment extends NESNBaseFragment {
    private static final String SCREEN_NAME = "Info Screen";
    private static final String BUTTON_PRESS_POLICE_ASSIST = "info police assistance button";
    private static final String BUTTON_PRESS_000 = "info 000 button";
    private static final String BUTTON_PRESS_SES = "info SES button";
    private static final String BUTTON_PRESS_HEALTH_DIRECT = "health direct button";
    private static final String BUTTON_PRESS_CRIME_STOPPERS = "crime stoppers button";

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.info_fragment, container, false);

        //ImageButton ibtnViewDemo = (ImageButton) view.findViewById(R.id.ibtnViewDemo);

        ImageButton ibtnCall000 = (ImageButton) view.findViewById(R.id.ibtnCall000);
        ImageButton ibtnCallSES = (ImageButton) view.findViewById(R.id.ibtnCallSES);
        ImageButton ibtnCallPolice = (ImageButton) view.findViewById(R.id.ibtnCallPolice);
        ImageButton ibtnCallCrimeStoppers = (ImageButton) view.findViewById(R.id.ibtnCallCrimeStoppers);
        ImageButton ibtnCallHealthDirect = (ImageButton) view.findViewById(R.id.ibtnCallHealthDirect);
        ImageButton ibtnRelayService = (ImageButton) view.findViewById(R.id.ibtnRelayService);
        Button btnViewDisclaimer = (Button) view.findViewById(R.id.btnViewDisclaimer);
        TextView tvCrimeStoppersInfo = (TextView) view.findViewById(R.id.tvCrimeStoppersInfo);
        TextView tvHealthDirectInfo = (TextView) view.findViewById(R.id.tvHealthDirectInfo);
        TextView tvRelayServiceInfo = (TextView) view.findViewById(R.id.tvRelayServiceInfo);

        tvCrimeStoppersInfo.setText(String.format("%s %s %s",
            getString(R.string.CrimeStoppersInfo), getString(R.string.Anonymous),
            getString(R.string.CrimeStoppersVisit)));

        tvHealthDirectInfo.setText(String.format("%s %s",
            getString(R.string.HealthDirectInfo), getString(R.string.HealthDirectAdvice)));

        tvRelayServiceInfo.setText(String.format("%s %s %s %s %s %s",
            getString(R.string.DeafHearing), getString(R.string.Speech),
            getString(R.string.NRSInfo), getString(R.string.RelayOfficerInfo),
            getString(R.string.NRSMore), getString(R.string.TapNRSIcon)));

        ibtnCall000.setOnClickListener(getListenerOnClickCall(R.string.number_Emergency));
        ibtnCallSES.setOnClickListener(getListenerOnClickCall(R.string.number_SES));
        ibtnCallPolice.setOnClickListener(getListenerOnClickCall(R.string.number_Police));
        ibtnCallCrimeStoppers.setOnClickListener(getListenerOnClickCall(R.string.number_CrimeStoppers));
        ibtnCallHealthDirect.setOnClickListener(getListenerOnClickCall(R.string.number_HealthDirect));
        ibtnRelayService.setOnClickListener(getListenerOnClickIbtnRelayService());
        btnViewDisclaimer.setOnClickListener(getListenerOnClickBtnViewDisclaimer());

		return view;
	}

    @Override
    protected String getScreenName() {
        return SCREEN_NAME;
    }

    private View.OnClickListener getListenerOnClickIbtnRelayService() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getActivity().getString(R.string.relay_service_url)));
                startActivity(intent);
            }
        };
    }

    private View.OnClickListener getListenerOnClickCall(final int numberResId) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (numberResId) {
                    case R.string.number_Emergency:
                        Tracker.trackButtonPressAction(BUTTON_PRESS_000);
                        goHome();
                        PhoneService.callEmergency(getActivity());
                        break;
                    case R.string.number_Police:
                        Tracker.trackButtonPressAction(BUTTON_PRESS_POLICE_ASSIST);
                        goHome();
                        PhoneService.callPolice(getActivity());
                        break;
                    case R.string.number_SES:
                        Tracker.trackButtonPressAction(BUTTON_PRESS_SES);
                        goHome();
                        PhoneService.callSES(getActivity());
                        break;
                    case R.string.number_CrimeStoppers:
                        Tracker.trackButtonPressAction(BUTTON_PRESS_CRIME_STOPPERS);
                        PhoneService.callCrimeStoppers(getActivity());
                        break;
                    case R.string.number_HealthDirect:
                        Tracker.trackButtonPressAction(BUTTON_PRESS_HEALTH_DIRECT);
                        PhoneService.callHealthDirect(getActivity());
                        break;
                }
            }
        };
    }

    private void goHome() {
        ((SherlockFragmentActivity) getActivity()).getSupportActionBar().setSelectedNavigationItem(0);
    }

    private View.OnClickListener getListenerOnClickBtnViewDisclaimer() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(DisclaimerActivity.PARAM_VIEW_ONLY, true);

                Intent intent = new Intent(getActivity(), DisclaimerActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        };
    }
}
