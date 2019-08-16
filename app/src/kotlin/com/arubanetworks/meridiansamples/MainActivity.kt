package com.arubanetworks.meridiansamples

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.android.volley.VolleyError
import com.arubanetworks.meridian.campaigns.CampaignsService
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.location.LocationRequest
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianOrientation
import com.arubanetworks.meridian.maps.MapFragment
import com.arubanetworks.meridian.maps.MapOptions
import com.arubanetworks.meridian.maps.MapView
import org.acra.ACRA

class MainActivity : AppCompatActivity() {

    private var drawerLayout: DrawerLayout? = null
    private var drawerList: ListView? = null
    private var drawerToggle: ActionBarDrawerToggle? = null

    private var drawerTitle: CharSequence? = null
    private var actionBarTitle: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerTitle = title
        actionBarTitle = drawerTitle
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerList = findViewById(R.id.left_drawer)

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout?.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        // set up the drawer's list view with items and click listener
        drawerList?.adapter = ArrayAdapter(this, R.layout.drawer_list_item, resources.getStringArray(R.array.section_titles))
        drawerList?.onItemClickListener = DrawerItemClickListener()

        // enable ActionBar app icon to behave as action to toggle nav drawer
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = object : ActionBarDrawerToggle(
                this, /* host Activity */
                drawerLayout, /* DrawerLayout object */
                R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            override fun onDrawerClosed(view: View) {
                supportActionBar?.title = actionBarTitle
                delegate.invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View) {
                supportActionBar?.title = drawerTitle
                delegate.invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
        drawerLayout?.addDrawerListener(drawerToggle as ActionBarDrawerToggle)

        if (savedInstanceState == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            else
                selectItem(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* Called whenever we call invalidateOptionsMenu() */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // If the nav drawer is open, hide action items related to the content view
        val drawerOpen = drawerList?.let { drawerLayout?.isDrawerOpen(it) }
        menu.findItem(R.id.action_settings).isVisible = drawerOpen != true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        }

        // Handle action buttons
        when (item.itemId) {
            R.id.action_settings -> {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                title = getString(R.string.action_settings)
                return true
            }
            R.id.action_reset_campaigns -> {
                resetCampaigns()
                return true
            }
            R.id.action_manual_report -> {
                ACRA.getErrorReporter().handleException(null)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Resets Campaigns so you can receive Beacon-based notifications without waiting for a potentially long "cool-down" period (for testing).
    private fun resetCampaigns() {

        CampaignsService.resetAllCampaigns(this, Application.APP_KEY, {
            Toast.makeText(this@MainActivity, "Reset Campaigns Succeeded.", Toast.LENGTH_SHORT).show()
            CampaignsService.startMonitoring(this@MainActivity, Application.APP_KEY)
        }, { tr ->
            Toast.makeText(this@MainActivity, "Reset Campaigns Failed: " + tr.message, Toast.LENGTH_LONG).show()
            CampaignsService.startMonitoring(this@MainActivity, Application.APP_KEY)
        })
    }

    /* The click listener for ListView in the navigation drawer */
    private inner class DrawerItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            selectItem(position)
        }
    }

    private fun selectItem(position: Int) {

        // update the main content by replacing fragments
        val fragment: Fragment

        when (position) {
            0 -> {
                val builder = MapFragment.Builder()
                        .setMapKey(Application.MAP_KEY)
                val mapOptions = MapOptions.getDefaultOptions()
                mapOptions.HIDE_OVERVIEW_BUTTON = true
                builder.setMapOptions(mapOptions)
                // example: how to set placemark markers text size
                /*
                    val mapOptions = (fragment as MapFragment).mapOptions
                    mapOptions.setTextSize(14)
                    builder.setMapOptions(mapOptions)
                */
                // example: how to start directions programmatically

                val mapFragment = builder.build()
                mapFragment.setMapEventListener(object : MapView.MapEventListener {

                    override fun onMapLoadFinish() {

                    }

                    override fun onMapLoadStart() {

                    }

                    override fun onPlacemarksLoadFinish() {
                      /*    for (placemark in mapFragment.mapView.placemarks) {
                            if ("APPLE" == placemark.name) {
                                mapFragment.startDirections(DirectionsDestination.forPlacemarkKey(placemark.key))
                            }
                        }*/
                    }

                    override fun onMapRenderFinish() {

                    }

                    override fun onMapLoadFail(tr: Throwable?) {
                        if (mapFragment.isAdded && mapFragment.activity != null) {
                            var message = getString(com.arubanetworks.meridian.R.string.mr_error_invalid_map)
                            if (tr != null) {
                                if (tr is VolleyError && tr.networkResponse != null && tr.networkResponse.statusCode == 401) {
                                    message = "HTTP 401 Error: Please verify the Editor token."
                                } else if (!Strings.isNullOrEmpty(tr.localizedMessage)) {
                                    message = tr.localizedMessage
                                }
                            }
                            AlertDialog.Builder(mapFragment.activity!!)
                                    .setTitle(getString(com.arubanetworks.meridian.R.string.mr_error_title))
                                    .setMessage(message)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(com.arubanetworks.meridian.R.string.mr_ok, null)
                                    .show()
                        }
                    }

                    override fun onMapTransformChange(transform: Matrix) {

                    }

                    override fun onLocationUpdated(location: MeridianLocation) {

                    }

                    override fun onOrientationUpdated(orientation: MeridianOrientation) {

                    }

                    override fun onLocationButtonClick(): Boolean {
                        // example of how to override the behavior of the location button
                        val mapView = mapFragment.mapView
                        val location = mapView.userLocation
                        if (location != null) {
                            mapView.updateForLocation(location)

                        } else {
                            LocationRequest.requestCurrentLocation(applicationContext, Application.APP_KEY, object : LocationRequest.LocationRequestListener {
                                override fun onResult(location: MeridianLocation) {
                                    mapView.updateForLocation(location)
                                }

                                override fun onError(location: LocationRequest.ErrorType) {
                                    // handle the error
                                }
                            })
                        }
                        return true
                    }
                })
                fragment = mapFragment
            }
            1 -> fragment = NearbyFragment.newInstance(Application.APP_KEY)
            2 -> fragment = SearchFragment.newInstance(Application.APP_KEY)
            3 -> fragment = CustomMarkerFragment.newInstance(Application.MAP_KEY)
            4 -> fragment = CampaignFragment.newInstance(Application.APP_KEY)
            5 -> fragment = ScrollingFragment.newInstance(Application.MAP_KEY)
            6 -> fragment = LocationFragment.newInstance(Application.APP_KEY)
            7 -> fragment = LocationSharingFragment.newInstance()
            8 -> fragment = SingleMarkerIDFragment.newInstance(Application.APP_KEY, Application.PLACEMARK_UID, null)
            9 -> fragment = TagsFragment.newInstance(Application.MAP_KEY)
            10 -> fragment = SingleTagFragment.newInstance(Application.MAP_KEY, Application.TAG_MAC)
            11 -> fragment = LocationSharingMapFragment.newInstance(Application.MAP_KEY)
            12 -> fragment = OverlayMarkerFragment.newInstance(Application.MAP_KEY)
            13 -> fragment = MapViewFragment()
            else -> return
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()

        title = resources.getStringArray(R.array.section_titles)[position]
        drawerList?.let { drawerLayout?.closeDrawer(it) }
    }

    override fun setTitle(title: CharSequence) {
        val actionBar = supportActionBar
        actionBarTitle = title
        actionBar?.title = actionBarTitle
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Begin monitoring for Aruba Beacon-based Campaign events
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // This odd delay thing is due to a bug with 23 currently.
        Handler().postDelayed({
            CampaignsService.startMonitoring(this@MainActivity, Application.APP_KEY)
            selectItem(0)
        }, 1000)
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the drawer toggls
        drawerToggle?.onConfigurationChanged(newConfig)
    }
}
