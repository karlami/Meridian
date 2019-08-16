package com.arubanetworks.meridiansamples

import android.graphics.Color
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.editor.Placemark
import com.arubanetworks.meridian.maprender.TextureProvider
import com.arubanetworks.meridian.maps.*
import java.util.*


/**
 * Fragment to demonstrate how to use the different flavors of OverlayMarker
 */


class OverlayMarkerFragment : MapFragment(), MapView.MarkerEventListener {

    // Cache the 'unselected' overlayMarker's colors
    //   so they can be toggled when the OverlayMarker is selected
    //
    private var markerEdgeColors: MutableMap<Long, Int>? = null
    private var markerFillColors: MutableMap<Long, Int>? = null

    // Cache a group of overlay markers whose colors can toggled as if they are one selectable item
    //   implemented as circular linked list within a map
    private var markerGroups: MutableMap<Long, Marker>? = null

    // ------------------------------------------------------------------------

    private fun toggleColor(marker: Marker) {
        val options = (marker as OverlayMarker).overlayMarkerOptions

        run {
            // when selected, toggle Fill Color between original color, and #FFFF3333
            val currentFillColor = options.overlayFillColor
            var updatedFillColor: Int? = Color.parseColor("#FFFF3333")
            if (markerFillColors == null) {
                markerFillColors = HashMap()
                markerFillColors?.put(marker.getId(), currentFillColor)
            } else if (markerFillColors?.containsKey(marker.getId()) != true) {
                markerFillColors?.put(marker.getId(), currentFillColor)
            } else if (currentFillColor == updatedFillColor) {
                updatedFillColor = markerFillColors?.get(marker.getId())
            }
            updatedFillColor?.let { options.overlayFillColor = it }
        }

        run {
            // when selected, toggle Edge Color between original color, and #22AA22AA
            val currentEdgeColor = options.overlayColor
            var updatedEdgeColor: Int? = Color.parseColor("#22AA22AA")
            if (markerEdgeColors == null) {
                markerEdgeColors = HashMap()
                markerEdgeColors?.put(marker.getId(), currentEdgeColor)
            } else if (markerEdgeColors?.containsKey(marker.getId()) != true) {
                markerEdgeColors?.put(marker.getId(), currentEdgeColor)
            } else if (currentEdgeColor == updatedEdgeColor) {
                updatedEdgeColor = markerEdgeColors?.get(marker.getId())
            }
            updatedEdgeColor?.let { options.overlayColor = it }
        }
    }

    // MapView.MarkerEventListener methods
    //
    override fun onMarkerSelect(marker: Marker): Boolean {

        // When an OverlayMarker is selected, toggle the colors
        if (marker is OverlayMarker) {

            // trigger a redraw
            toggleColor(marker)
            marker.invalidate(true)

            // if this marker is part of a group... toggle the rest of the group as well
            if (markerGroups != null) {
                var nextMarkerInGroup: Marker? = markerGroups?.get(marker.getId())
                while (nextMarkerInGroup != null && nextMarkerInGroup !== marker) {
                    toggleColor(nextMarkerInGroup)
                    nextMarkerInGroup.invalidate(true)
                    nextMarkerInGroup = markerGroups?.get(nextMarkerInGroup.id)
                }
            }

            // let the calling function know it's been handled by returning true.
            return true
        }
        return false
    }

    /**
     * Substitute a OverlayMarker for the default FlatPlacemarkMarker for select placemarks (labeled "EventSpace")
     *
     * @param placemark The placemark that is requesting a [Marker] for displaying on the map
     * @return A new instance of a [Marker] subclass, or null to request that MapView create a default [Marker].
     */
    override fun markerForPlacemark(placemark: Placemark): Marker? {
        val c = activity ?: return null

        // Substitute a OverlayMarker for the default FlatPlacemarkMarker
        if (placemark.name.contains("EventSpace")) {
            val gm = OverlayMarker.Builder(c,
                    OverlayMarkerOptions.fromPlacemark(placemark)).build()

            // Override the default colors
            gm.overlayMarkerOptions.overlayColor = Color.parseColor("#22AA2255")
            gm.overlayMarkerOptions.overlayFillColor = Color.parseColor("#22AA2233")

            return gm

        }
        return null
    }

    /**
     * Add an additional OverlayMarker for a FlatPlacemarkMarker for select placemarks (labeled "Storage")
     *
     */

    override fun onPlacemarksLoadFinish() {
        super.onPlacemarksLoadFinish()

        for (placemark in mapView.placemarks) {
            if (placemark.name.contains("Storage")) {
                val c = activity ?: return

                // Create a secondary OverlayMarker in addition to a FlatPlacemarkMarker for this placemark

                val om = OverlayMarker.Builder(c, OverlayMarkerOptions.fromPlacemark(placemark)).build()
                om.overlayMarkerOptions.overlayColor = Color.parseColor("#2222AA55")
                om.overlayMarkerOptions.overlayFillColor = Color.parseColor("#2222AA33")

                // Add the marker to the map
                val markerList = ArrayList<Marker>()
                markerList.add(om)
                mapView.commitTransaction(Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build())

            }
        }
    }

    /**
     * Called after the map and placemarks have been loaded and rendered.
     * programmatically add several OverlayMarkers
     */

    override fun onMapRenderFinish() {
        super.onMapRenderFinish()

        val markerList = ArrayList<Marker>()
        val c = activity
        if (c != null) {

            val openPathMarker: OverlayMarker
            val arrowHeadMarker: OverlayMarker
            val circleMarker: OverlayMarker
            val floatsPerPoint = 2  // points are 2D

            // Create an interesting path to demonstrate the OverlayMarker with OPEN_PATH

            val numPathPts = 4
            val openPts = FloatArray(numPathPts * floatsPerPoint + 1)
            openPts[0] = numPathPts.toFloat()
            openPts[1] = 0.0f
            openPts[2] = 0.0f
            openPts[3] = 0.0f
            openPts[4] = 40.0f
            openPts[5] = 140.0f
            openPts[6] = 280.0f
            openPts[7] = 60.0f
            openPts[8] = 360.0f

            val optionOpenPath = OverlayMarkerOptions(
                    TextureProvider.OverlayType.OPEN_PATH.toInt(),
                    240.0f, 300.0f,
                    OverlayMarkerOptions.OverlayMarkerCoordinateType.RELATIVE,
                    openPts)

            optionOpenPath.overlayWidth = 7f
            optionOpenPath.overlayColor = Color.parseColor("#AA2222BB")
            optionOpenPath.overlayFillColor = Color.parseColor("#33337733")
            openPathMarker = OverlayMarker.Builder(c, optionOpenPath).build()
            openPathMarker.name = "Path Marker"
            markerList.add(openPathMarker)


            // Create an interesting arrowhead on the path to demonstrate the OverlayMarker with FILL

            val numArrowPts = 4
            val points = FloatArray(numArrowPts * floatsPerPoint + 1)
            points[0] = numArrowPts.toFloat()
            points[1] = 0.0f
            points[2] = 0.0f
            points[3] = -20.0f
            points[4] = 20.0f
            points[5] = 0.0f
            points[6] = -50.0f
            points[7] = 20.0f
            points[8] = 20.0f

            val optionsArrow = OverlayMarkerOptions(
                    TextureProvider.OverlayType.OUTLINE_WITH_FILL.toInt(),
                    240.0f, 300.0f,
                    OverlayMarkerOptions.OverlayMarkerCoordinateType.RELATIVE,
                    points)

            optionsArrow.overlayWidth = 3f
            optionsArrow.overlayColor = Color.parseColor("#AA2222BB")
            optionsArrow.overlayFillColor = Color.parseColor("#AA222233")

            arrowHeadMarker = OverlayMarker.Builder(c, optionsArrow).build()
            arrowHeadMarker.name = "Arrow Marker"
            markerList.add(arrowHeadMarker)


            // Create a circle to demonstrate the OverlayMarker fromCircle

            val radius = 10.0f
            circleMarker = OverlayMarker.Builder(c, OverlayMarkerOptions.fromCircle(240.0f, 230.0f - radius, radius)).build()
            circleMarker.overlayMarkerOptions.overlayWidth = 3f
            circleMarker.overlayMarkerOptions.overlayColor = Color.parseColor("#555555FF")
            circleMarker.overlayMarkerOptions.overlayFillColor = Color.parseColor("#AA222233")
            circleMarker.name = "Circle Marker"
            markerList.add(circleMarker)

            // This demonstrates how the overlay marker can be grouped to act as one
            if (markerGroups == null) {
                markerGroups = HashMap()
            }
            markerGroups?.put(openPathMarker.id, arrowHeadMarker)
            markerGroups?.put(arrowHeadMarker.id, circleMarker)
            markerGroups?.put(circleMarker.id, openPathMarker)

        }

        // Add the markers to the map
        mapView.commitTransaction(Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build())
    }

    companion object {

        fun newInstance(mapKey: EditorKey): OverlayMarkerFragment {
            val f = OverlayMarkerFragment()
            val mapFragment = MapFragment.Builder()
                    .setMapKey(mapKey)
                    .build()
            f.arguments = mapFragment.arguments
            return f
        }
    }
}
