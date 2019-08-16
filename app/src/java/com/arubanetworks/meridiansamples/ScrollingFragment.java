package com.arubanetworks.meridiansamples;

import android.app.AlertDialog;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.editor.Placemark;
import com.arubanetworks.meridian.internal.util.Dev;
import com.arubanetworks.meridian.location.MeridianLocation;
import com.arubanetworks.meridian.location.MeridianOrientation;
import com.arubanetworks.meridian.maps.MapOptions;
import com.arubanetworks.meridian.maps.MapView;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Fragment to demonstrate the use of the raw MapView to do custom scroll and zoom animations.
 */
public class ScrollingFragment extends Fragment implements MapView.MapEventListener {

    // The map to load
    private static final String MAP_KEY = "ScrollingFragment.MapKey";
    private MapView mapView;
    private final Random random = new Random(18923501986340L);
    private Timer timer = null;

    public static ScrollingFragment newInstance(EditorKey mapKey) {
        ScrollingFragment markerFragment = new ScrollingFragment();
        if (mapKey.getParent() == null)
            throw new IllegalArgumentException("MapKey must have parent.");
        Bundle b = markerFragment.getArguments();
        if (b == null) b = new Bundle();
        b.putSerializable(MAP_KEY, mapKey);
        markerFragment.setArguments(b);
        return markerFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // The MapView requires OpenGL 2.0 support
        if (Dev.getOpenGLMajorVersion(getActivity()) < MapView.REQUIRED_OPENGL_LEVEL) {
            Toast.makeText(getActivity(), String.format(Locale.US,"OpenGL %d.0 is not supported on this device.", MapView.REQUIRED_OPENGL_LEVEL), Toast.LENGTH_LONG).show();
            return new FrameLayout(getActivity());
        }

        EditorKey mapKey;
        Bundle arg = getArguments();
        if (arg == null || ((mapKey = (EditorKey) arg.getSerializable(MAP_KEY)) == null)) {
            Toast.makeText(getActivity(), "No EditorKey passed to Fragment!", Toast.LENGTH_LONG).show();
            return new FrameLayout(getActivity());
        }

        View view = inflater.inflate(com.arubanetworks.meridian.R.layout.mr_map_fragment, container, false);
        mapView = view.findViewById(com.arubanetworks.meridian.R.id.mr_mapview);

        // Set us up as the listener
        mapView.setMapEventListener(this);

        // AppKey must be set before a map is loaded.
        mapView.setAppKey(mapKey.getParent());
        mapView.setMapKey(mapKey);

        // Set options.
        MapOptions mapOptions = mapView.getOptions();
        mapView.setOptions(mapOptions);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // Restart our timer if able.
        scheduleScrolling();
    }

    @Override
    public void onPause() {
        mapView.onPause();

        // End our timer.
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onPause();
    }


    //
    // MapViewListener methods
    //

    @Override
    public void onMapLoadStart() {}

    @Override
    public void onMapLoadFinish() {}

    @Override
    public void onPlacemarksLoadFinish() {}

    @Override
    public void onMapRenderFinish() {
        // Now that the map has finished loading lets schedule a timer to zoom to one of them.
        scheduleScrolling();
    }

    private void scheduleScrolling() {
        if (mapView == null || mapView.getPlacemarks().size() <= 0) return;
        if (timer == null) timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // First lets select a random placemark
                List<Placemark> placemarks = mapView.getPlacemarks();
                Placemark placemark = placemarks.get(random.nextInt(placemarks.size() - 1));

                // Now we need a rect to point the map to, we make it a single point to force the
                // map to zoom in all the way. If we wanted some other behavior we could specify
                // a larger rect to target a different zoom level.
                RectF target = new RectF(placemark.getX(), placemark.getY(), placemark.getX(), placemark.getY());

                // We can now pass the rect to the MapView along with a flag to indicate we want it
                // to animate.
                mapView.scrollToRect(target, true);
            }
        }, 0, 3000);
    }

    @Override
    public void onMapLoadFail(Throwable tr) {
        // there was an error while loading the map!
        new AlertDialog.Builder(getActivity())
                .setTitle("Loading Error")
                .setMessage(tr.getMessage())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(com.arubanetworks.meridian.R.string.mr_ok, null)
                .show();
    }

    @Override
    public void onMapTransformChange(Matrix transform) { }

    @Override
    public void onLocationUpdated(MeridianLocation location) {}

    @Override
    public void onOrientationUpdated(MeridianOrientation orientation) {}

    @Override
    public boolean onLocationButtonClick() {
        return false;
    }
}
