package com.threesixtyentertainment.nesn;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class NESNBaseFragment extends SherlockFragment {
	
	public static final String EXTRA_TITLE = "title";

	public static Bundle createBundle( String title ) {
        Bundle bundle = new Bundle();
        bundle.putString( EXTRA_TITLE, title );
        return bundle;
    }

    protected abstract String getScreenName();
}
