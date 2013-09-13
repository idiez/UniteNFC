package com.quantum.unitenfc;

import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Fragment;
import android.app.FragmentTransaction;

public class CustomTabListener implements TabListener{

	private Fragment fragment;
	private boolean isActive;
	
	public CustomTabListener(Fragment fragment){
		this.fragment = fragment;
	}
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
        isActive = true;
		ft.add(R.id.fragment_holder,fragment, null);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		isActive = false;
		ft.remove(fragment);
	}
	
	public boolean isActive() {
		return isActive;
	}
}
