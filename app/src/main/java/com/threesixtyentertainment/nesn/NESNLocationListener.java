package com.threesixtyentertainment.nesn;

import android.location.Location;

public interface NESNLocationListener {

	void onLocationChanged(Location location);
	void onAddressChanged(String address);
    void onLocationServiceStatusChanged(boolean enabled);
}
