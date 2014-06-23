package com.radiopirate.android.beta;

import android.os.Bundle;
import android.os.Handler;

import com.actionbarsherlock.view.MenuItem;
import com.radiopirate.android.beta.frag.HomeFragment;
import com.radiopirate.android.beta.frag.MenuFragmentPopulate;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity {

	protected MenuFragmentPopulate mFrag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// create menu Fragment
		mFrag = new MenuFragmentPopulate();
		setContentView(R.layout.activity_main);

		// switching to home fragment
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.content_frame, new HomeFragment()).commit();

		// set the Behind View
		setBehindContentView(R.layout.activity_menu);

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.menu_frame, mFrag).commit();

		// customize the SlidingMenu
		SlidingMenu sm = getSlidingMenu();
		sm.setMode(SlidingMenu.LEFT);
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setSlidingActionBarEnabled(false);

	}
	
	//Opening Slidingmenu
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
	    super.onPostCreate(savedInstanceState);
	    new Handler().postDelayed(new Runnable() {
	        @Override
	        public void run() {
	            toggle();
	        }
	    }, 2000);
	}
	
	// toggle title to menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			toggle();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
