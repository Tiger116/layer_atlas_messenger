package com.layer.atlas.messenger;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AtlasMapScreen extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    private static final boolean debug = false;
    private static final String TAG = AtlasMessagesScreen.class.getSimpleName();
    private static final int LOCATION_EXPIRATION_TIME = 60 * 1000; // 1 minute
    private Bundle bundle;
    private List<LatLng> marks;
    private MapFragment mapView = null;
    private boolean isSend = false;
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private Handler uiHandler;
    LocationListener locationTracker = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastKnownLocation = location;
                    if (debug) Log.d(TAG, "onLocationChanged() location: " + location);
                }
            });
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_map);

        this.uiHandler = new Handler();

        marks = new ArrayList<>();

        Intent intent = getIntent();
        bundle = intent.getExtras();

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()) == ConnectionResult.SUCCESS) {
            mapView = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
            mapView.getMapAsync(this);
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(locationTracker);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // restore location tracking
        int requestLocationTimeout = 1 * 1000; // every second
        int distance = 100;
        Location loc = null;
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from gps: " + loc);
        }
        if (loc == null && locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from network: " + loc);
        }
        if (loc != null && loc.getTime() < System.currentTimeMillis() + LOCATION_EXPIRATION_TIME) {
            locationTracker.onLocationChanged(loc);
        }
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }
        if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.button_ok:
                if (!marks.isEmpty()) {
                    double[] longitude = new double[marks.size()];
                    double[] latitude = new double[marks.size()];
                    for (int i = 0; i < marks.size(); i++) {
                        latitude[i] = marks.get(i).latitude;
                        longitude[i] = marks.get(i).longitude;
                    }
                    Intent intent = new Intent();
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(isSend);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.setMyLocationEnabled(true);
        if (bundle != null) {
            if (bundle.containsKey(getString(R.string.locations_json_array_key))) {
                String json = bundle.getString(getString(R.string.locations_json_array_key));
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        LatLng latLng = new LatLng(object.getDouble("lat"), object.getDouble("lon"));
                        builder.include(latLng);
                        map.addMarker(new MarkerOptions().position(latLng));
                        marks.add(latLng);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LatLngBounds bounds = builder.build();
                int padding = 32; // offset from edges of the map in pixels
                CameraUpdate cameraUpdate;
                if (marks.size() > 1)
                    cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                else
                    cameraUpdate = CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 14);
                map.moveCamera(cameraUpdate);

            }
        } else {
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);
            CameraUpdate cameraUpdate;
            LatLng latLng;
            if (location != null) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
            } else {
                latLng = new LatLng(53.0, 21.0);
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 3);
            }
            mapView.getMap().animateCamera(cameraUpdate);
            map.setOnMapClickListener(this);
            map.setOnMarkerClickListener(this);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        isSend = true;
        invalidateOptionsMenu();
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mapView.getMap().addMarker(markerOptions);
        marks.add(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (isSend) {
            marker.remove();
            marks.remove(marker.getPosition());
            if (marks.isEmpty()) {
                isSend = false;
                invalidateOptionsMenu();
            }
        }
        return true;
    }
}
