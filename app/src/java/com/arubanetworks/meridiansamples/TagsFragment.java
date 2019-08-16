package com.arubanetworks.meridiansamples;

import android.graphics.PointF;
import androidx.appcompat.app.AlertDialog;

import com.arubanetworks.meridian.Meridian;
import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.internal.util.Strings;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.Transaction;
import com.arubanetworks.meridian.tags.TagBeacon;
import com.arubanetworks.meridian.tags.TagMarker;
import com.arubanetworks.meridian.tags.TagStream;

import java.util.HashMap;
import java.util.List;


/**
 * A Fragment to demonstrate the use of the TagStream for use in displaying Tags
 */
public class TagsFragment extends MapFragment implements TagStream.Listener {

    // A collection to maintain our TagMarkers
    private final HashMap<TagBeacon, TagMarker> tagMarkers = new HashMap<>();

    // The TagStream itself
    private TagStream tagStream;

    public static TagsFragment newInstance(EditorKey mapKey) {
        TagsFragment f = new TagsFragment();
        f.setArguments(new MapFragment.Builder().setMapKey(mapKey).build().getArguments());
        return f;
    }

    @Override
    public void onMapLoadStart() {
        super.onMapLoadStart();
        if (Strings.isNullOrEmpty(Meridian.getShared().getEditorToken())) {
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                        .setMessage("You need to provide a valid editor token")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            // Close out any existing tags when loading a new map key
            if (tagStream != null) tagStream.stopUpdatingTags();

            // Clear any references to old markers so we don't re-add them to the new map on accident
            tagMarkers.clear();

            // Create our TagStream and start it.
            tagStream = new TagStream.Builder()
                    .addMapKey(getMapView().getMapKey())
                    .setListener(this)
                    .build();
            tagStream.startUpdatingTags();
        }
    }

    @Override
    public void onTagsUpdated(List<TagBeacon> tags) {

        Transaction.Builder transaction = new Transaction.Builder().setType(Transaction.Type.UPDATE);

        // We have new or updated tags lets iterate through and add them to the map.
        for (TagBeacon tag : tags) {
            // ignore tags that are not on the current map
            if (getMapView() != null && getMapView().getMapKey() != null && !tag.getMapId().equals(getMapView().getMapKey().getId())) continue;
            TagMarker currentMarker = tagMarkers.get(tag);

            // check for a new Tag and add
            if (currentMarker == null) {
                if (getContext() == null) {return;}
                currentMarker = new TagMarker(getContext(), tag);
                tagMarkers.put(tag, currentMarker);
            } else {
                // Otherwise we need to update our current marker to reflect the new tag state
                PointF point = tag.getLocation().getPoint();
                currentMarker.setPosition(point.x, point.y);

                // The title or image could also have changed but the TagMarker provided with the
                // SDK doesn't show those fields by default so we can ignore those changes here.
            }

            transaction.addMarker(currentMarker);
        }

        // Commit the transaction
        if (transaction.getSize() > 0)
            getMapView().commitTransaction(transaction.build());
    }

    @Override
    public void onTagsRemoved(List<TagBeacon> tags) {
        Transaction.Builder transaction = new Transaction.Builder().setType(Transaction.Type.REMOVE);

        for (TagBeacon tag : tags) {
            TagMarker currentMarker = tagMarkers.get(tag);

            // We can ignore removing the tags marker if we never added it.
            if (currentMarker == null) continue;

            transaction.addMarker(currentMarker);
        }

        getMapView().commitTransaction(transaction.build());
    }

    @Override
    public void onPause() {
        super.onPause();
        // If we are paused we stop listening to tag updates
        if (tagStream != null) tagStream.stopUpdatingTags();
    }

    @Override
    public void onResume() {
        super.onResume();
        // If we are resuming we start listening for tags again.
        if (tagStream != null) tagStream.startUpdatingTags();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up memory.
        if (tagStream != null) tagStream.dispose();
    }
}
