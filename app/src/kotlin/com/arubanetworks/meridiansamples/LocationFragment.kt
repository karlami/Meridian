package com.arubanetworks.meridiansamples

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.location.LocationRequest
import com.arubanetworks.meridian.location.MeridianLocation
import com.arubanetworks.meridian.location.MeridianLocationManager

class LocationFragment : Fragment() {

    private var locationHelper: MeridianLocationManager? = null
    private var locationRequest: LocationRequest? = null
    private var appKey: EditorKey? = null
    private var isListening = false

    private val listener = object : MeridianLocationManager.LocationUpdateListener {
        override fun onLocationUpdate(location: MeridianLocation?) {
            addLogMessage(location)
        }

        override fun onLocationError(tr: Throwable) {
            addLogMessage("Error retrieving location: " + tr.localizedMessage)
        }

        override fun onEnableBluetoothRequest() {
            toastMessage("Requested Bluetooth")
        }

        override fun onEnableWiFiRequest() {
            toastMessage("Requested Wifi")
        }

        override fun onEnableGPSRequest() {
            toastMessage("Requested GPS")
        }
    }


    private val requestListener = View.OnClickListener { v ->
        if (locationRequest?.isRunning == true) locationRequest?.cancel()
        addLogMessage("Requesting Location")
        locationRequest = LocationRequest.requestCurrentLocation(v.context, appKey, locationRequestListener)
    }

    private val locationRequestListener = object : LocationRequest.LocationRequestListener {
        override fun onResult(location: MeridianLocation) {
            addLogMessage("Requested Location")
            addLogMessage(location)
        }

        override fun onError(type: LocationRequest.ErrorType) {
            addLogMessage("Error requesting location: " + type.name)
        }
    }

    private val toggleListener = View.OnClickListener {
        isListening = !isListening
        if (isListening)
            locationHelper?.startListeningForLocation()
        else
            locationHelper?.stopListeningForLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args : Bundle? = arguments
        appKey = args?.getSerializable(APP_KEY) as EditorKey
        if (appKey == null) {
            Toast.makeText(activity, "No EditorKey passed to Fragment!", Toast.LENGTH_LONG).show()
            return
        }

        locationHelper = MeridianLocationManager(activity, appKey, listener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.location_example, container, false)
        val locationRequestButton = rootView.findViewById<Button>(R.id.location_request_button)
        locationRequestButton.setOnClickListener(requestListener)
        val toggle = rootView.findViewById<ToggleButton>(R.id.location_toggle)
        toggle.setOnClickListener(toggleListener)
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (isListening) locationHelper?.startListeningForLocation()
    }

    override fun onPause() {
        super.onPause()
        locationHelper?.stopListeningForLocation()
        if (locationRequest?.isRunning == true) locationRequest?.cancel()
    }

    private fun addLogMessage(location: MeridianLocation?) {
        addLogMessage(location?.toString() ?: "Null Location")
    }

    private fun addLogMessage(message: String) {
        val v = view ?: return
        val tv = v.findViewById<TextView>(R.id.location_log)
        val sv = v.findViewById<ScrollView>(R.id.location_scroll_log)
        if (tv == null || sv == null) return
        tv.append("\n" + message)
        if (tv.bottom - (sv.height + sv.scrollY) <= 0)
            sv.post { sv.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun toastMessage(message: String) {
        val c = activity ?: return
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show()
    }

    companion object {

        private const val APP_KEY = "LocationFragment.APP_KEY"

        fun newInstance(appKey: EditorKey): LocationFragment {
            val fragment = LocationFragment()
            var args = fragment.arguments
            if (args == null) args = Bundle()
            args.putSerializable(APP_KEY, appKey)
            fragment.arguments = args
            return fragment
        }
    }
}
