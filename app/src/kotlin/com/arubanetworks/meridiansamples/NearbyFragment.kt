package com.arubanetworks.meridiansamples

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianLocationManager
import com.arubanetworks.meridian.maps.directions.DirectionsDestination
import com.arubanetworks.meridian.search.LocalSearch
import com.arubanetworks.meridian.search.LocalSearchResponse
import com.arubanetworks.meridian.search.LocalSearchResult
import java.util.*

/**
 * Demonstrates the use of the search API to list nearby placemarks.
 */

class NearbyFragment : androidx.fragment.app.ListFragment(), SearchView.OnQueryTextListener, LocalSearch.LocalSearchListener, MeridianLocationManager.LocationUpdateListener {
    private var appKey: EditorKey? = null
    private val results = ArrayList<LocalSearchResult>()
    private var localSearch: LocalSearch? = null
    private var searchView: SearchView? = null
    private var spinner: ProgressBar? = null
    private var locationHelper: MeridianLocationManager? = null
    private var adapter: ArrayAdapter<LocalSearchResult>? = null

    private var currentSearchTerm: String? = null

    private var lastLocation: MeridianLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null || activity == null) {
            return
        }
        appKey = arguments!!.getSerializable(APP_KEY_KEY) as EditorKey

        // init meridian location
        locationHelper = MeridianLocationManager(context, appKey, this)

        if (activity != null) {
            adapter = object : ArrayAdapter<LocalSearchResult>(activity!!, android.R.layout.simple_list_item_2, android.R.id.text1, results) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text1 = view.findViewById<TextView>(android.R.id.text1)
                    val text2 = view.findViewById<TextView>(android.R.id.text2)

                    val result = results[position]

                    var timeString = ""
                    val seconds = result.time.toDouble() / 1000.0
                    if (seconds > 0) {
                        timeString = if (seconds >= 60)
                            String.format(Locale.US, "%.0f min", seconds / 60)
                        else
                            String.format(Locale.US, "%.0f sec", seconds)
                    }

                    val placemark = results[position].placemark
                    if (!Strings.isNullOrEmpty(placemark.name))
                        text1.text = placemark.name
                    else
                        text1.text = placemark.typeName
                    text2.text = timeString
                    return view
                }
            }

            listAdapter = adapter
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_nearby, container, false)
        spinner = layout.findViewById(R.id.progress_bar)

        return layout
    }

    override fun onStart() {
        super.onStart()
        locationHelper?.startListeningForLocation()
    }

    override fun onStop() {
        super.onStop()
        locationHelper?.stopListeningForLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchView?.setOnQueryTextListener(null)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.nearby, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        searchItem?.icon?.setTintList(ColorStateList.valueOf(Color.BLACK))
        searchView = searchItem?.actionView as SearchView
        searchView?.queryHint = getString(R.string.query_hint)
        searchView?.setOnQueryTextListener(this)
    }

    override fun onQueryTextChange(s: String): Boolean {

        currentSearchTerm = s

        // kick off a new search query
        updateNearby(currentSearchTerm, locationHelper?.lastLocation)

        return true
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        return false
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val result = results[position]
        val destination = DirectionsDestination.forPlacemarkKey(result.placemark.key)
        startActivity(DirectionsActivity.createIntent(activity, appKey, Application.MAP_KEY, destination))
    }

    private fun updateNearby(term: String?, location: MeridianLocation?) {

        if (location == null || location.mapKey == null)
            return

        localSearch?.cancel()

        spinner?.visibility = View.VISIBLE

        localSearch = LocalSearch.Builder()
                .setQuery(term)
                .setLimit(10)
                .setApp(appKey)
                .setLocation(location)
                .setListener(this)
                .build()

        localSearch?.start()

        lastLocation = location
    }

    override fun onLocationUpdate(location: MeridianLocation?) {
        if (location == null)
            return

        if (lastLocation != null) {
            val distanceToNewLocation = lastLocation?.distanceTo(location)
            if (distanceToNewLocation != null && lastLocation != null && distanceToNewLocation >= 0 && distanceToNewLocation < 10)
                return
        }
        updateNearby(currentSearchTerm, location)
    }

    override fun onLocationError(tr: Throwable) {

    }

    override fun onEnableBluetoothRequest() {

    }

    override fun onEnableGPSRequest() {

    }

    override fun onEnableWiFiRequest() {

    }

    override fun onSearchComplete(response: LocalSearchResponse) {

        spinner?.visibility = View.INVISIBLE

        adapter?.let {
            it.clear()
            it.addAll(response.results)
            it.notifyDataSetChanged()
        }
    }

    override fun onSearchError(tr: Throwable) {
        spinner?.visibility = View.INVISIBLE

        Log.e(TAG, "search error: $tr")
    }

    companion object {

        private const val APP_KEY_KEY = "meridian.AppKey"

        private const val TAG = "NearbyFragment"

        /**
         * Constructs a NearbyFragment for the given Meridian AppKey.
         */
        fun newInstance(appKey: EditorKey): NearbyFragment {
            val nf = NearbyFragment()
            var b = nf.arguments
            if (b == null) b = Bundle()
            b.putSerializable(APP_KEY_KEY, appKey)
            nf.arguments = b
            return nf
        }
    }
}
