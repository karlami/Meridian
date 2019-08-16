package com.arubanetworks.meridiansamples

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.arubanetworks.meridian.Meridian
import com.arubanetworks.meridian.editor.EditorKey
import com.arubanetworks.meridian.internal.util.Strings
import com.arubanetworks.meridian.maps.MapFragment
import com.arubanetworks.meridian.maps.Transaction
import com.arubanetworks.meridian.tags.TagBeacon
import com.arubanetworks.meridian.tags.TagMarker
import com.arubanetworks.meridian.tags.TagStream

class SingleTagFragment : MapFragment(), TagStream.Listener {

    private var tagMac: String? = null
    private var tagMarker: TagMarker? = null
    private var tagStream: TagStream? = null

    override fun onMapLoadStart() {
        super.onMapLoadStart()
        if (Strings.isNullOrEmpty(Meridian.getShared().editorToken)) {
            if (activity != null) {
                AlertDialog.Builder(activity!!)
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show()
            }
        } else {
            // Close out any existing tags when loading a new mapkey
            tagStream?.stopUpdatingTags()

            // Create our TagStream and start it.
            tagStream = tagMac?.let {
                TagStream.Builder()
                        .addTagMac(Application.APP_KEY, it)
                        .setListener(this)
                        .build()
            }
            tagStream?.startUpdatingTags()
        }
    }

    override fun onTagsUpdated(tags: List<TagBeacon>) {
        val transaction = Transaction.Builder().setType(Transaction.Type.UPDATE)

        // We have new or updated tags lets iterate through and add them to the map.
        for (tag in tags) {
            if (tagMac != tag.mac) continue
            if (mapView != null && mapView.mapKey != null && tag.mapId != mapView.mapKey.id) {
                mapView.mapKey = EditorKey.forMap(tag.mapId, mapView.appKey)
                continue
            }

            if (tagMarker == null && context != null) {
                tagMarker = TagMarker(context!!, tag)

            } else {
                val point = tag.location?.point
                point?.let { tagMarker?.setPosition(it.x, it.y) }
            }

            tagMarker?.let { transaction.addMarker(it) }
            // Commit the transaction
            mapView?.commitTransaction(transaction.build())
            break // found it, no need to do more
        }
    }

    override fun onTagsRemoved(tags: List<TagBeacon>) {
        val transaction = Transaction.Builder().setType(Transaction.Type.REMOVE)

        if (tagMarker == null) return

        for (tag in tags) {
            if (tagMac != tag.mac) continue

            tagMarker?.let { transaction.addMarker(it) }
            mapView?.commitTransaction(transaction.build())
            break // found it, no need to do more
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            tagMac = arguments?.getString(TAG_MAC)
    }

    override fun onPause() {
        super.onPause()
        // If we are paused we stop listening to tag updates
        tagStream?.stopUpdatingTags()
    }

    override fun onResume() {
        super.onResume()
        // If we are resuming we start listening for tags again.
        tagStream?.startUpdatingTags()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up memory.
        tagStream?.dispose()
    }

    companion object {

        private const val TAG_MAC = "meridian.TAG_MAC"

        fun newInstance(mapKey: EditorKey, tagMac: String): SingleTagFragment {
            val f = SingleTagFragment()
            val args = MapFragment.Builder().setMapKey(mapKey).build().arguments
            if (args != null) {
                args.putString(TAG_MAC, tagMac)
                f.arguments = args
            }
            return f
        }
    }
}
