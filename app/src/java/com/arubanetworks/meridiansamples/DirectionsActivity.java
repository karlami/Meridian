package com.arubanetworks.meridiansamples;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.arubanetworks.meridian.editor.EditorKey;
import com.arubanetworks.meridian.maps.MapFragment;
import com.arubanetworks.meridian.maps.directions.DirectionsDestination;
import com.arubanetworks.meridian.maps.directions.DirectionsSource;

/**
 * Demonstrates the use of the directions API to request a route and display the steps as text.
 */

public class DirectionsActivity extends AppCompatActivity {

    private static final String APP_KEY = "meridian.AppKey";
    private static final String MAP_KEY = "meridian.MapKey";
    private static final String DIRECTIONS_SOURCE = "meridian.DirectionsSource";
    private static final String DIRECTIONS_DESTINATION = "meridian.DirectionsDestination";

    public static Intent createIntent(Context context, EditorKey appKey, DirectionsSource source, DirectionsDestination destination) {
        return new Intent(context, DirectionsActivity.class)
                .putExtra(APP_KEY, appKey)
                .putExtra(DIRECTIONS_SOURCE, source)
                .putExtra(DIRECTIONS_DESTINATION, destination);
    }
    public static Intent createIntent(Context context, EditorKey appKey, EditorKey mapKey, DirectionsDestination destination) {
        return new Intent(context, DirectionsActivity.class)
                .putExtra(APP_KEY, appKey)
                .putExtra(MAP_KEY, mapKey)
                .putExtra(DIRECTIONS_DESTINATION, destination);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditorKey appKey = null;
        EditorKey mapKey = null;
        DirectionsSource source = null;
        DirectionsDestination destination = null;

        if (getIntent() != null) {
            appKey = (EditorKey) getIntent().getSerializableExtra(APP_KEY);
            mapKey = (EditorKey) getIntent().getSerializableExtra(MAP_KEY);
            source = (DirectionsSource) getIntent().getSerializableExtra(DIRECTIONS_SOURCE);
            destination = (DirectionsDestination) getIntent().getSerializableExtra(DIRECTIONS_DESTINATION);
        }

        if (savedInstanceState == null) {
            MapFragment mapFragment = new MapFragment.Builder()
                    .setAppKey(appKey)
                    .setMapKey(mapKey)
                    .setSource(source)
                    .setDestination(destination)
                    .build();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, mapFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this will pass through to our map fragment
        super.onActivityResult(requestCode, resultCode, data);
    }
}
