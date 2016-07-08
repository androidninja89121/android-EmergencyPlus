package com.threesixtyentertainment.nesn;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;

 
public abstract class TabSwipeActivity extends SherlockFragmentActivity implements TabSwipeListener {
 
    private NonSwipeableViewPager mViewPager;
    private TabsAdapter adapter;
    private static String TAG = "TabSwipeActivity";
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
         * Create the ViewPager and our custom adapter
         */
        mViewPager = new NonSwipeableViewPager(this);
        adapter = new TabsAdapter( this, mViewPager );
        mViewPager.setAdapter( adapter );
        mViewPager.setOnPageChangeListener( adapter );

        /*
         * We need to provide an ID for the ViewPager, otherwise we will get an exception like:
         *
         * java.lang.IllegalArgumentException: No view found for id 0xffffffff for fragment TestFragment{40de5b90 #0 id=0xffffffff android:switcher:-1:0}
         * at android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:864)
         *
         * The ID 0x7F04FFF0 is large enough to probably never be used for anything else
         */
        mViewPager.setId( 0x7F04FFF0 );
 
        super.onCreate(savedInstanceState);
 
        /*
         * Set the ViewPager as the content view
         */
        setContentView(mViewPager);
    }
 
    /**
     * Add a tab with a backing Fragment to the action bar
     * @param titleRes A string resource pointing to the title for the tab
     * @param fragmentClass The class of the Fragment to instantiate for this tab
     * @param args An optional Bundle to pass along to the Fragment (may be null)
     */
    protected void addTab(int titleRes, int iconResId, Class<?> fragmentClass, Bundle args ) {
        adapter.addTab( getString( titleRes ), iconResId, fragmentClass, args );
    }

    public void selectTab(int index)
    {
        adapter.selectTab(index);
    }
    /**
     * Add a tab with a backing Fragment to the action bar
     * @param title A string to be used as the title for the tab
     * @param fragmentClass The class of the Fragment to instantiate for this tab
     * @param args An optional Bundle to pass along to the Fragment (may be null)
     */
    protected void addTab(CharSequence title, int iconResid, Class<?> fragmentClass, Bundle args ) {
        if (adapter.getCount() > 2)
            return;

        adapter.addTab( title, iconResid, fragmentClass, args );
    }
    
    protected Fragment getFragmentForPosition(int position) {
    	return adapter.getFragmentForPosition(position);
    }
 
    private static class TabsAdapter extends FragmentPagerAdapter implements TabListener, NonSwipeableViewPager.OnPageChangeListener {
 
        private final TabSwipeActivity mActivity;
        private final ActionBar mActionBar;
        private final NonSwipeableViewPager mPager;
 
        public TabsAdapter(TabSwipeActivity activity, NonSwipeableViewPager pager) {
            super(activity.getSupportFragmentManager());
            this.mActivity = activity;
            this.mActionBar = activity.getSupportActionBar();
            this.mPager = pager;
 
            mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
            //mActionBar.setLogo(new ColorDrawable(Color.TRANSPARENT));
            //mActionBar.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.header));
        }
 
        private static class TabInfo {
            public final Class<?> fragmentClass;
            public final Bundle args;
            private int tabId;
            public TabInfo(Class<?> fragmentClass,
                    Bundle args) {
                this.fragmentClass = fragmentClass;
                this.args = args;
            }
            
            public int getTabId() {
            	return tabId;
            }
            
        }
 
        private List<TabInfo> mTabs = new ArrayList<TabInfo>();
        private SparseArray<Fragment> mFragmentReferenceMap = new SparseArray<Fragment>();
 
        public void addTab( CharSequence title, int iconResId, Class<?> fragmentClass, Bundle args ) {
            final TabInfo tabInfo = new TabInfo( fragmentClass, args );

            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v =  inflater.inflate(R.layout.tab_view, null);

            ((ImageView) v.findViewById(R.id.tab_icon)).setImageResource(iconResId);
            ((TextView) v.findViewById(R.id.tab_title)).setText(title);

            Tab tab = mActionBar.newTab();
            tab.setCustomView(v);

            tab.setTabListener( this );
            tab.setTag( tabInfo );
 
            mTabs.add( tabInfo );

            Fragment fragment = (Fragment) Fragment.instantiate( mActivity, tabInfo.fragmentClass.getName(), tabInfo.args );
            mFragmentReferenceMap.put(mTabs.size()-1, fragment);

            mActionBar.addTab( tab );

            notifyDataSetChanged();
        }

        public void selectTab(int index)
        {
            Tab tab = mActionBar.getTabAt(index);
            mActionBar.selectTab(tab);
        }
 /*
        @Override
		public Object instantiateItem(ViewGroup container, int position) {
			final Fragment fragment = (Fragment) super.instantiateItem(container, position);
		    final TabInfo info = mTabs.get(position);
		    info.tag = fragment.getTag(); // set it here
		    return fragment;
		}
*/
		@Override
        public Fragment getItem(int position) {
            //final TabInfo tabInfo = mTabs.get( position );
            //Fragment fragment = (Fragment) Fragment.instantiate( mActivity, tabInfo.fragmentClass.getName(), tabInfo.args );
            //mFragmentReferenceMap.put(Integer.valueOf(position), fragment);

            Fragment fragment = mFragmentReferenceMap.get(position);
            Log.d("nesn", "getItem: " + position + " - " + fragment);

            //Log.d(TAG, "PUTTING TAB INTO REFERENCE MAP");
            return fragment;
        }
 
        @Override
        public int getCount() {
            return mTabs.size();
        }
        
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			super.destroyItem(container, position, object);
			mFragmentReferenceMap.remove(Integer.valueOf(position));
		}
		
		public Fragment getFragmentForPosition(int position) {
			return mFragmentReferenceMap.get(Integer.valueOf(position));
		}
		
		public void onPageScrollStateChanged(int arg0) {
        }
 
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }
 
        public void onPageSelected(int position) {
        	Log.d(TAG, "!!! onPageSelected");
            mActionBar.setSelectedNavigationItem( position );
        }
 
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
        	Log.d(TAG, "!!! onTabSelected");
            TabInfo tabInfo = (TabInfo) tab.getTag();
            int position = -1;
            for ( int i = 0; i < mTabs.size(); i++ ) {
                if ( mTabs.get( i ) == tabInfo ) {
                    mPager.setCurrentItem( i );
                    position = i;
                    break;
                }
            }
            
           	mActivity.onTabSelected(position);
        }
 
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        	Log.d(TAG, "!!! onTabUnselected");
        }
 
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        	Log.d(TAG, "!!! onTabReselected");
        }
    }
}