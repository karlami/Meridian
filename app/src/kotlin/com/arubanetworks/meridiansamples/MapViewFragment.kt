package com.arubanetworks.meridiansamples

import android.graphics.Matrix
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import com.arubanetworks.meridian.editor.Placemark
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianOrientation
import com.arubanetworks.meridian.maps.*
import java.util.ArrayList

/**
 * Demonstrates the use of the MapView.  It is recommended to use the SDK map fragment instead of the mapview
 */

class MapViewFragment : androidx.fragment.app.Fragment(), MapView.DirectionsEventListener, MapView.MapEventListener, MapView.MarkerEventListener {
    private var mapView: MapView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_mapview, container, false)
        mapView = layout.findViewById(R.id.demo_mapview)
        mapView?.appKey = Application.APP_KEY

        // If you want to handle MapView events
        mapView?.setMapEventListener(this)

        // If you want to handle directions events
        mapView?.setDirectionsEventListener(this)

        // If you want to handle marker events
        mapView?.setMarkerEventListener(this)

        // Set map options if desired
        val mapOptions = mapView?.options
        mapOptions?.HIDE_MAP_LABEL = true
        mapOptions?.HIDE_DIRECTIONS_CONTROLS = true
        mapView?.options = mapOptions

        // If you want to load a map other than the default one
        // It is recommended to do this after setting the map options
        mapView?.mapKey = Application.MAP_KEY

        // Demonstration of how to customize the mapView's locationMarker:
        //    change default color for Bluetooth to orange
        //    modify the name
        //    modify the details
        //    alternatively... hide the call-out entirely
        //
        // val lm = mapView?.getLocationMarker()
        // lm?.setCustomColor(LocationMarker.State.BLUETOOTH, 0xFFFF7700.toInt())
        // lm?.name = "Current Location Label"
        // lm?.details = "Details"
        // // lm?.setShowsCallout(false)

        return layout
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }


    //
    // MapViewListener methods
    //
    override fun onMapLoadStart() {}
    override fun onMapLoadFinish() {}
    override fun onPlacemarksLoadFinish() {
        // example: highlight the first four placemarks
        /*
        val markerList = ArrayList<Marker>()
        var index = 0
        if (mapView?.getAllMarkers() != null) {
            for (marker in mapView!!.getAllMarkers()) {
                markerList.add(marker)
                index++
                if (index >= 4) {
                    val highlightedMarkers = HighlightedMarkers.Builder(markerList).build()
                    mapView!!.commitTransaction(Transaction.Builder().setAnimationDuration(500).addMarker(highlightedMarkers).build())
                    break
                }
            }
        }
        */
    }
    override fun onMapRenderFinish() {}
    override fun onMapLoadFail(tr: Throwable) {}
    override fun onMapTransformChange(transform: Matrix) {}
    override fun onLocationUpdated(meridianLocation: MeridianLocation) {}
    override fun onOrientationUpdated(orientation: MeridianOrientation) {}
    override fun onLocationButtonClick(): Boolean {return false}
    //
    // DirectionsEventListener methods
    //
    override fun onDirectionsReroute() {}
    override fun onDirectionsClick(marker: Marker): Boolean {
        if (activity != null) {
            AlertDialog.Builder(activity!!)
                    .setMessage("Directions not implemented here.")
                    .setPositiveButton("OK", null)
                    .show()
        }
        return false
    }
    override fun onDirectionsStart(): Boolean {return false}
    override fun onRouteStepIndexChange(index: Int): Boolean {return false}
    override fun onDirectionsClosed(): Boolean {return false}
    override fun onDirectionsError(tr: Throwable): Boolean {return false}
    override fun onUseAccessiblePathsChange() {}
    //
    // MarkerEventListener methods
    //
    override fun onMarkerSelect(marker: Marker): Boolean {
        // prevent clustered markers from being selected
        return marker is ClusteredMarker
    }
    override fun onMarkerDeselect(marker: Marker): Boolean {return false}
    override fun markerForPlacemark(placemark: Placemark): Marker? {return null}
    override fun markerForSelectedMarker(markerToSelect: Marker): Marker? {return null}
    override fun onCalloutClick(marker: Marker): Boolean {return false}
}
