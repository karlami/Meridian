package com.arubanetworks.meridiansamples

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.editor.Placemark
import com.arubanetworks.meridian.log.MeridianLogger
import com.arubanetworks.meridian.maps.*
import java.util.*


class CustomMarkerFragment : MapFragment() {

    /**
     * This method is an opportunity to supply your own [Marker] instance for a given Placemark.
     * You can use this to completely customize the look of placemarks on the map. The default implementation
     * returns null.
     *
     * @param placemark The placemark that is requesting a [Marker] for displaying on the map
     * @return A new instance of a [Marker] subclass, or null to request that MapView create a default [Marker].
     */
    override fun markerForPlacemark(placemark: Placemark): Marker? {
        val c = activity ?: return null
        if (placemark.type.contains("shop")) {
            val pm = PlacemarkMarker.Builder(c, placemark)
                    //.setIcon(R.drawable.my_icon) // use your custom icon
                    //.setIconColor(ContextCompat.getColor(c, R.color.my_color)) // sets the background color
                    //.setIconPadding(R.dimen.my_padding) // sets the padding for the icon
                    .build()
            //pm.weight = 2f // Make shops show up on top of other placemarks.
            //pm.collision = false // Exclude shops from collision checks.
            //pm.scaleFixedToMap = true// Make shops with fixed scale on the map surface.
            return pm
        }
        return super.markerForPlacemark(placemark)
    }
    /**
     * This method is an opportunity to supply your own selected [Marker] instance for a given [Marker].
     * You can use this to completely customize the look of selected placemarks on the map. The default implementation
     * returns null.
     *
     * @param markerToSelect The Marker that is being selected.
     * @return A new instance of a [Marker] subclass, or null to request that MapView create a default [Marker].
     */
    override fun markerForSelectedMarker(markerToSelect: Marker): Marker? {
        return if (markerToSelect is RandMarker && context != null) RandSelectedMarker(context!!, markerToSelect) else super.markerForSelectedMarker(markerToSelect)
    }

    /**
     * Called after the map and placemarks have been loaded and rendered. Changing any markers
     * should be avoided.
     */
    override fun onMapRenderFinish() {
        super.onMapRenderFinish()
        // Remove an existing marker named lobby.
        for (m in mapView.allMarkers) {
            if (m is PlacemarkMarker && m.placemark.name.equals("lobby", ignoreCase = true)) {
                mapView.commitTransaction(Transaction.Builder().setType(Transaction.Type.REMOVE).addMarker(m).setListener(object : Transaction.Listener {
                    override fun onTransactionAnimationComplete() {
                        LOG.d("Animation complete removal.")
                    }

                    override fun onTransactionComplete() {
                        LOG.d("Completed removal.")
                    }

                    override fun onTransactionCanceled() {
                        LOG.d("Canceled removal.")
                    }

                    override fun onTransactionFailed() {
                        LOG.d("Failed removal.")
                    }
                }).build())
                break
            }
        }

        // Create 20 random Markers
        val random = Random(18923501986340L)
        val markerList = ArrayList<Marker>()
        for (i in 0..19) {
            val c = activity ?: break
            if (mapView != null) {
                val marker = RandMarker(c, mapView.mapInfo, random)
                marker.name = "Custom marker $i"
                markerList.add(marker)
            }
        }

        // Add them to the map the markers.
        mapView.commitTransaction(Transaction.Builder().setAnimationDuration(500).addMarkers(markerList).build())
    }

    /**
     * A class the extends Marker and displays a copy of the apps launcher icon to use as an
     * example.
     */
    private class RandMarker(context: Context, mapInfo: MapInfo, random: Random) : Marker(random.nextInt(mapInfo.width.toInt()).toFloat(), random.nextInt(mapInfo.height.toInt()).toFloat()) {

        private val appContext: Context = context.applicationContext

        init {
            weight = 2.1f
        }

        override fun getBitmap(): Bitmap {
            return BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_launcher)
        }
    }

    /**
     * A class the extends Marker and displays an enlarged copy of the apps launcher icon to use as an
     * example.
     */
    private class RandSelectedMarker(context: Context, baseMarker: Marker) : Marker(baseMarker.position[0], baseMarker.position[1]) {

        private val appContext: Context = context.applicationContext

        init {
            weight = 3.1f
            xScale = 1.5f
            yScale = 1.5f
        }

        override fun getBitmap(): Bitmap {
            return BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_launcher)
        }

        override fun canBeSelected(): Boolean {
            return false
        }
    }

    companion object {

        private val LOG = MeridianLogger.forTag("TEST").andFeature(MeridianLogger.Feature.MAPS)

        fun newInstance(mapKey: EditorKey): CustomMarkerFragment {
            val f = CustomMarkerFragment()
            val mapFragment = MapFragment.Builder()
                    .setMapKey(mapKey)
                    .build()
            f.arguments = mapFragment.arguments
            return f
        }
    }
}
