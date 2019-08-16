package com.arubanetworks.meridiansamples

import android.app.AlertDialog
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Dev
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianOrientation
import com.arubanetworks.meridian.maps.MapView
import java.util.*

/**
 * A Fragment to demonstrate the use of the raw MapView to do custom scroll and zoom animations.
 */
class ScrollingFragment : Fragment(), MapView.MapEventListener {
    private var mapView: MapView? = null
    private val random = Random(18923501986340L)
    private var timer: Timer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The MapView requires OpenGL 2.0 support
        if (Dev.getOpenGLMajorVersion(activity) < MapView.REQUIRED_OPENGL_LEVEL) {
            Toast.makeText(activity, String.format(Locale.US, "OpenGL %d.0 is not supported on this device.", MapView.REQUIRED_OPENGL_LEVEL), Toast.LENGTH_LONG).show()
            return FrameLayout(activity!!)
        }

        val arg : Bundle? = arguments
        val mapKey: EditorKey? = arg?.getSerializable(MAP_KEY) as EditorKey
        if (mapKey == null) {
            Toast.makeText(activity, "No EditorKey passed to Fragment!", Toast.LENGTH_LONG).show()
            return FrameLayout(activity!!)
        }

        val view = inflater.inflate(com.arubanetworks.meridian.R.layout.mr_map_fragment, container, false)
        mapView = view.findViewById(com.arubanetworks.meridian.R.id.mr_mapview)

        // Set us up as the listener
        mapView?.setMapEventListener(this)

        // AppKey must be set before a map is loaded.
        mapView?.appKey = mapKey.parent
        mapView?.mapKey = mapKey

        // Set options.
        val mapOptions = mapView?.options
        mapView?.options = mapOptions

        return view
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()

        // Restart our timer if able.
        scheduleScrolling()
    }

    override fun onPause() {
        mapView?.onPause()

        // End our timer.
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
        super.onPause()
    }


    //
    // MapViewListener methods
    //

    override fun onMapLoadStart() {}

    override fun onMapLoadFinish() {}

    override fun onPlacemarksLoadFinish() {}

    override fun onMapRenderFinish() {
        // Now that the map has finished loading lets schedule a timer to zoom to one of them.
        scheduleScrolling()
    }

    private fun scheduleScrolling() {
        if (mapView == null || mapView!!.placemarks.size <= 0) return
        if (timer == null) timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // First lets select a random placemark
                val placemarks = mapView?.placemarks
                val placemark = placemarks?.get(random.nextInt(placemarks.size - 1))

                // Now we need a rect to point the map to, we make it a single point to force the
                // map to zoom in all the way. If we wanted some other behavior we could specify
                // a larger rect to target a different zoom level.
                val target = placemark?.let { RectF(it.x, it.y, it.x, it.y) }

                // We can now pass the rect to the MapView along with a flag to indicate we want it
                // to animate.
                mapView?.scrollToRect(target, true)
            }
        }, 0, 3000)
    }

    override fun onMapLoadFail(tr: Throwable) {
        // there was an error while loading the map!
        AlertDialog.Builder(activity)
                .setTitle("Loading Error")
                .setMessage(tr.message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(com.arubanetworks.meridian.R.string.mr_ok, null)
                .show()
    }

    override fun onMapTransformChange(transform: Matrix) {}

    override fun onLocationUpdated(location: MeridianLocation) {}

    override fun onOrientationUpdated(orientation: MeridianOrientation) {}

    override fun onLocationButtonClick(): Boolean {
        return false
    }

    companion object {

        // The map to load
        private const val MAP_KEY = "ScrollingFragment.MapKey"

        fun newInstance(mapKey: EditorKey): ScrollingFragment {
            val markerFragment = ScrollingFragment()
            if (mapKey.parent == null)
                throw IllegalArgumentException("MapKey must have parent.")
            var b = markerFragment.arguments
            if (b == null) b = Bundle()
            b.putSerializable(MAP_KEY, mapKey)
            markerFragment.arguments = b
            return markerFragment
        }
    }
}
