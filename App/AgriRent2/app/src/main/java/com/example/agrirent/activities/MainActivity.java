package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;

import com.example.agrirent.fragments.AddListingBottomSheet;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.agrirent.R;
import com.example.agrirent.fragments.AddFragment;
import com.example.agrirent.fragments.ExploreFragment;
import com.example.agrirent.fragments.HomeFragment;
import com.example.agrirent.fragments.OrdersFragment;
import com.example.agrirent.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {

    public static final String EXTRA_TARGET_TAB = "target_tab";

    private BottomNavigationView bottomNavigation;
    private final FragmentManager fragmentManager = getSupportFragmentManager();

    private Fragment homeFragment;
    private Fragment exploreFragment;
    private Fragment addFragment;
    private Fragment ordersFragment;
    private Fragment profileFragment;

    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        initFragments(savedInstanceState);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(homeFragment);
                return true;
            } else if (id == R.id.nav_explore) {
                switchFragment(exploreFragment);
                return true;
            } else if (id == R.id.nav_add) {
                if (!fragmentManager.isStateSaved()) {
                    AddListingBottomSheet.newInstance().show(getSupportFragmentManager(), "addListing");
                }
                return false; // Return false to not select the tab, just show sheet
            } else if (id == R.id.nav_orders) {
                switchFragment(ordersFragment);
                return true;
            } else if (id == R.id.nav_profile) {
                switchFragment(profileFragment);
                return true;
            }
            return false;
        });

        handleLaunchTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchTabIntent(intent);
    }

    private void initFragments(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            homeFragment = fragmentManager.findFragmentByTag("1");
            exploreFragment = fragmentManager.findFragmentByTag("2");
            addFragment = fragmentManager.findFragmentByTag("3");
            ordersFragment = fragmentManager.findFragmentByTag("4");
            profileFragment = fragmentManager.findFragmentByTag("5");

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (exploreFragment == null) exploreFragment = new ExploreFragment();
            if (addFragment == null) addFragment = new AddFragment();
            if (ordersFragment == null) ordersFragment = new OrdersFragment();
            if (profileFragment == null) profileFragment = new ProfileFragment();

            if (!homeFragment.isAdded() || !exploreFragment.isAdded() || !addFragment.isAdded()
                    || !ordersFragment.isAdded() || !profileFragment.isAdded()) {
                androidx.fragment.app.FragmentTransaction tx = fragmentManager.beginTransaction();
                if (!profileFragment.isAdded()) tx.add(R.id.fragment_container, profileFragment, "5").hide(profileFragment);
                if (!ordersFragment.isAdded()) tx.add(R.id.fragment_container, ordersFragment, "4").hide(ordersFragment);
                if (!addFragment.isAdded()) tx.add(R.id.fragment_container, addFragment, "3").hide(addFragment);
                if (!exploreFragment.isAdded()) tx.add(R.id.fragment_container, exploreFragment, "2").hide(exploreFragment);
                if (!homeFragment.isAdded()) tx.add(R.id.fragment_container, homeFragment, "1");
                tx.commit();
            }

            String activeTag = savedInstanceState.getString("active_fragment_tag", "1");
            activeFragment = fragmentManager.findFragmentByTag(activeTag);
            if (activeFragment == null) {
                activeFragment = homeFragment;
            }
        } else {
            homeFragment = new HomeFragment();
            exploreFragment = new ExploreFragment();
            addFragment = new AddFragment();
            ordersFragment = new OrdersFragment();
            profileFragment = new ProfileFragment();

            activeFragment = homeFragment;

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, profileFragment, "5").hide(profileFragment)
                    .add(R.id.fragment_container, ordersFragment, "4").hide(ordersFragment)
                    .add(R.id.fragment_container, addFragment, "3").hide(addFragment)
                    .add(R.id.fragment_container, exploreFragment, "2").hide(exploreFragment)
                    .add(R.id.fragment_container, homeFragment, "1").commit();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String activeTag = "1";
        if (activeFragment == exploreFragment) activeTag = "2";
        else if (activeFragment == addFragment) activeTag = "3";
        else if (activeFragment == ordersFragment) activeTag = "4";
        else if (activeFragment == profileFragment) activeTag = "5";
        
        outState.putString("active_fragment_tag", activeTag);
    }

    private void switchFragment(Fragment targetFragment) {
        if (targetFragment == null || activeFragment == targetFragment) return;
        if (activeFragment == null || !activeFragment.isAdded() || !targetFragment.isAdded()) return;
        if (fragmentManager.isStateSaved()) return;
        
        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit();
        activeFragment = targetFragment;
    }

    private void handleLaunchTabIntent(Intent intent) {
        int targetTab = R.id.nav_home;
        if (intent != null) {
            targetTab = intent.getIntExtra(EXTRA_TARGET_TAB, R.id.nav_home);
        }
        if (bottomNavigation.getSelectedItemId() != targetTab) {
            bottomNavigation.setSelectedItemId(targetTab);
        } else if (targetTab == R.id.nav_home && activeFragment == null) {
            switchFragment(homeFragment);
        }
    }
    
    public void navigateToTab(int itemId) {
        bottomNavigation.setSelectedItemId(itemId);
    }

    @Override
    public void onBackPressed() {
        if (bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            // If we are not on the Home tab, navigate back to Home
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            // If we are already on the Home tab, perform the default back action (close app)
            super.onBackPressed();
        }
    }
}
