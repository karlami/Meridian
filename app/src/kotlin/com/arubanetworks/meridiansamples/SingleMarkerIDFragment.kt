package com.arubanetworks.meridiansamples

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.editor.Placemark
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianOrientation
import com.arubanetworks.meridian.maps.MapFragment
import com.arubanetworks.meridian.maps.MapView
import com.arubanetworks.meridian.requests.MeridianRequest
import com.arubanetworks.meridian.requests.PlacemarkRequest


class SingleMarkerIDFragment : Fragment(), MeridianRequest.Listener<Placemark>, MeridianRequest.ErrorListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            val builder = PlacemarkRequest.Builder()
                    .setAppKey(arguments!!.getSerializable(KEY_APPKEY) as EditorKey)
                    .setListener(this)
                    .setErrorListener(this)

            if (!Strings.isNullOrEmpty(arguments!!.getString(KEY_PID))) {
                builder.setPlacemarkID(arguments!!.getString(KEY_PID))
            } else if (!Strings.isNullOrEmpty(arguments!!.getString(KEY_UID))) {
                builder.setUID(arguments!!.getString(KEY_UID))
            }

            builder.build().sendRequest()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        v.isIndeterminate = true
        return v
    }

    override fun onResponse(placemark: Placemark?) {
        if (placemark?.key?.parent == null) {
            if (activity != null) {
                AlertDialog.Builder(activity!!)
                        .setMessage("You need to provide a valid placemark")
                        .setPositiveButton("OK", null)
                        .show()
            }
            view?.visibility = View.INVISIBLE
            return
        }
        if (view != null) {
            fragmentManager?.beginTransaction()?.replace((view?.parent as ViewGroup).id, SingleMarkerInteriorFragment.newInstance(placemark))?.commitAllowingStateLoss()
        }
    }

    override fun onError(tr: Throwable) {
        Toast.makeText(activity, "Failed with error " + tr.localizedMessage, Toast.LENGTH_SHORT).show()
        view?.visibility = View.INVISIBLE
    }

    class SingleMarkerInteriorFragment : MapFragment() {
        private var placemarkKey: EditorKey? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            placemarkKey = arguments?.getSerializable(KEY_PKEY) as EditorKey
            super.onCreate(savedInstanceState)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val v = super.onCreateView(inflater, container, savedInstanceState)
            mapView.setMapEventListener(object : MapView.MapEventListener {
                override fun onMapLoadStart() {}
                override fun onMapLoadFinish() {}
                override fun onPlacemarksLoadFinish() {}
                override fun onMapLoadFail(tr: Throwable) {}
                override fun onMapTransformChange(transform: Matrix) {}
                override fun onLocationUpdated(location: MeridianLocation) {}
                override fun onOrientationUpdated(orientation: MeridianOrientation) {}
                override fun onLocationButtonClick(): Boolean {
                    return false
                }

                override fun onMapRenderFinish() {
                    // Set our callout to the correct Marker
                    val target = mapView.allMarkers[0]
                    mapView.onMarkerClick(target)

                    // Get the View rect and center it on 0,0
                    val r = RectF(0f, 0f, mapView.width.toFloat(), mapView.height.toFloat())
                    r.offset(-r.width() / 2, -r.height() / 2)

                    // map that to the map coordinates and center on the placemark
                    val t = Matrix()
                    mapView.currentScreenToMapTransform.invert(t)
                    t.mapRect(r)
                    r.offset(target.position[0] - 100, target.position[1])

                    // Scroll on over
                    mapView.scrollToRect(r, true)
                }

            })
            return v
        }

        companion object {

            private const val KEY_PKEY = "meridian.SingleMarkerInteriorFragment.PKEY"

            fun newInstance(placemark: Placemark): SingleMarkerInteriorFragment {
                val f = MapFragment.Builder()
                        .setMapKey(placemark.key.parent)
                        .setPlacemarkId(placemark.key.id)
                        .build()
                val args = f.arguments
                args?.putSerializable(KEY_PKEY, placemark.key)
                val tf = SingleMarkerInteriorFragment()
                tf.arguments = args
                return tf
            }
        }
    }

    companion object {

        private const val KEY_UID = "meridian.SingleMarkerIDFragment.UID"
        private const val KEY_PID = "meridian.SingleMarkerIDFragment.PID"
        private const val KEY_APPKEY = "meridian.SingleMarkerIDFragment.APPKEY"

        fun newInstance(appKey: EditorKey?, uid: String, pid: String?): SingleMarkerIDFragment {
            val args = Bundle()

            if (!Strings.isNullOrEmpty(uid))
                args.putString(KEY_UID, uid)
            else if (!Strings.isNullOrEmpty(pid))
                args.putString(KEY_PID, pid)
            else
                throw RuntimeException("UID or PID must not be null when initialising SingleMarkerIDFragment!")

            if (appKey == null || appKey.parent != null)
                throw RuntimeException("AppKey must be a valid top level EditorKey when initialising SingleMarkerIDFragment!")

            args.putSerializable(KEY_APPKEY, appKey)
            val f = SingleMarkerIDFragment()
            f.arguments = args
            return f
        }
    }
}
