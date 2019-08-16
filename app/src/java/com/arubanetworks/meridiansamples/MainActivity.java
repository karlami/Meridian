package com.arubanetworks.meridiansamples;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.arubanetworks.meridian.campaigns.CampaignsService;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.location.LocationRequest;
import com.arubanetworks.meridian.location.MeridianLocation;
import com.arubanetworks.meridian.location.MeridianOrientation;
import com.arubanetworks.meridian.maps.HighlightedMarkers;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.MapOptions;
import com.arubanetworks.meridian.maps.MapView;
import com.arubanetworks.meridian.maps.Marker;
import com.arubanetworks.meridian.maps.Transaction;
import com.arubanetworks.meridian.requests.MeridianRequest;

import org.acra.ACRA;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private CharSequence drawerTitle;
    private CharSequence title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = drawerTitle = getTitle();
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerList = findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, getResources().getStringArray(R.array.section_titles)));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(title);
                getDelegate().invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(drawerTitle);
                getDelegate().invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);

        if (savedInstanceState == null) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            else
                selectItem(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action buttons
        switch(item.getItemId()) {
            case R.id.action_settings:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
                setTitle(getString(R.string.action_settings));
                return true;
            case R.id.action_reset_campaigns:
                resetCampaigns();
                return true;
            case R.id.action_manual_report:
                ACRA.getErrorReporter().handleException(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Resets Campaigns so you can receive Beacon-based notifications without waiting for a potentially long "cool-down" period (for testing).
    private void resetCampaigns() {

        CampaignsService.resetAllCampaigns(this, Application.APP_KEY, new MeridianRequest.Listener<Void>() {
            @Override
            public void onResponse(Void response) {
                Toast.makeText(MainActivity.this, "Reset Campaigns Succeeded.", Toast.LENGTH_SHORT).show();
                CampaignsService.startMonitoring(MainActivity.this, Application.APP_KEY);
            }
        }, new MeridianRequest.ErrorListener() {
            @Override
            public void onError(Throwable tr) {
                Toast.makeText(MainActivity.this, "Reset Campaigns Failed: " + tr.getMessage(), Toast.LENGTH_LONG).show();
                CampaignsService.startMonitoring(MainActivity.this, Application.APP_KEY);
            }
        });
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {

        // update the main content by replacing fragments
        final Fragment fragment;

        switch (position) {
            case 0:
                MapFragment.Builder builder = new MapFragment.Builder()
                        .setMapKey(Application.MAP_KEY);
                MapOptions mapOptions = MapOptions.getDefaultOptions();
                // Turn off the overview button (only shown if there is an overview map for the location)
                mapOptions.HIDE_OVERVIEW_BUTTON = true;
                builder.setMapOptions(mapOptions);
                // example: how to set placemark markers text size
                /*
                    MapOptions mapOptions = ((MapFragment) fragment).getMapOptions();
                    mapOptions.setTextSize(14);
                    builder.setMapOptions(mapOptions);
                */
                // example: how to start directions programmatically

                final MapFragment mapFragment = builder.build();
                mapFragment.setMapEventListener(new MapView.MapEventListener() {

                    @Override
                    public void onMapLoadFinish() {

                    }

                    @Override
                    public void onMapLoadStart() {

                    }

                    @Override
                    public void onPlacemarksLoadFinish() {
                        /*for (Placemark placemark : mapFragment.getMapView().getPlacemarks()) {
                            if ("APPLE".equals(placemark.getName())) {
                                mapFragment.startDirections(DirectionsDestination.forPlacemarkKey(placemark.getKey()));
                            }
                        }*/
                        // example: highlight the first four placemarks
                        /*
                        ArrayList<Marker> markerList = new ArrayList<>();
                        int index = 0;
                        MapView mapView = mapFragment.getMapView();
                        if (mapView != null && mapView.getAllMarkers() != null) {
                            for (Marker marker : mapView.getAllMarkers()) {
                                markerList.add(marker);
                                index++;
                                if (index >= 4) {
                                    HighlightedMarkers highlightedMarkers = new HighlightedMarkers.Builder(markerList).build();
                                    mapView.commitTransaction(new Transaction.Builder().setAnimationDuration(500).addMarker(highlightedMarkers).build());
                                    break;
                                }
                            }
                        }
                        */
                    }

                    @Override
                    public void onMapRenderFinish() {

                    }

                    @Override
                    public void onMapLoadFail(Throwable tr) {
                        if (mapFragment.isAdded() && mapFragment.getActivity() != null) {
                            String message = getString(com.arubanetworks.meridian.R.string.mr_error_invalid_map);
                            if (tr != null) {
                                if (tr instanceof VolleyError && ((VolleyError) tr).networkResponse != null && ((VolleyError) tr).networkResponse.statusCode == 401) {
                                    message = "HTTP 401 Error: Please verify the Editor token.";
                                } else if (!Strings.isNullOrEmpty(tr.getLocalizedMessage())) {
                                    message = tr.getLocalizedMessage();
                                }
                            }
                            new AlertDialog.Builder(mapFragment.getActivity())
                                    .setTitle(getString(com.arubanetworks.meridian.R.string.mr_error_title))
                                    .setMessage(message)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(com.arubanetworks.meridian.R.string.mr_ok, null)
                                    .show();
                        }
                    }

                    @Override
                    public void onMapTransformChange(Matrix transform) {

                    }

                    @Override
                    public void onLocationUpdated(MeridianLocation location) {

                    }

                    @Override
                    public void onOrientationUpdated(MeridianOrientation orientation) {

                    }

                    @Override
                    public boolean onLocationButtonClick() {
                        // example of how to override the behavior of the location button
                        final MapView mapView = mapFragment.getMapView();
                        MeridianLocation location = mapView.getUserLocation();
                        if (location != null) {
                            mapView.updateForLocation(location);

                        } else {
                            LocationRequest.requestCurrentLocation(getApplicationContext(), Application.APP_KEY, new LocationRequest.LocationRequestListener() {
                                @Override
                                public void onResult(MeridianLocation location) {
                                    mapView.updateForLocation(location);
                                }

                                @Override
                                public void onError(LocationRequest.ErrorType location) {
                                    // handle the error
                                }
                            });
                        }
                        return true;
                    }
                });
                fragment = mapFragment;

                break;
            case 1:
                fragment = NearbyFragment.newInstance(Application.APP_KEY);
                break;
            case 2:
                fragment = SearchFragment.newInstance(Application.APP_KEY);
                break;
            case 3:
                fragment = CustomMarkerFragment.newInstance(Application.MAP_KEY);
                break;
         //   case 4:
           //     fragment = CampaignFragment.newInstance(Application.APP_KEY);
             //   break;
            case 5:
                fragment = ScrollingFragment.newInstance(Application.MAP_KEY);
                break;
           // case 6:
             //   fragment = LocationFragment.newInstance(Application.APP_KEY);
               // break;
            //case 7:
              //  fragment = LocationSharingFragment.newInstance();
                //break;
            case 8:
                fragment = SingleMarkerIDFragment.newInstance(Application.APP_KEY, Application.PLACEMARK_UID, null);
                break;
           // case 9:
             //   fragment = TagsFragment.newInstance(Application.MAP_KEY);
               // break;
            case 10:
                fragment = SingleTagFragment.newInstance(Application.MAP_KEY, Application.TAG_MAC);
                break;
           // case 11:
             //   fragment = LocationSharingMapFragment.newInstance(Application.MAP_KEY);
               // break;
            case 12:
                fragment = OverlayMarkerFragment.newInstance(Application.MAP_KEY);
                break;
            case 13:
                fragment = new MapViewFragment();
                break;
            default:
                return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        setTitle(getResources().getStringArray(R.array.section_titles)[position]);
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Begin monitoring for Aruba Beacon-based Campaign events
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // This odd delay thing is due to a bug with 23 currently.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CampaignsService.startMonitoring(MainActivity.this, Application.APP_KEY);
                selectItem(0);
            }
        },1000);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }

}
