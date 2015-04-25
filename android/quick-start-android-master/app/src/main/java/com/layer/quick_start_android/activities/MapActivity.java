package com.layer.quick_start_android.activities;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import com.layer.quick_start_android.R;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends ActionBarActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    private Bundle bundle;
    private List<LatLng> marks;
    private MapFragment mapView = null;
    private boolean isSend = false;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(59.931994, 30.420635), 15));
        map.setMyLocationEnabled(true);
        if (bundle != null) {
            if (bundle.getDoubleArray("latitude") != null && bundle.getDoubleArray("longitude") != null) {
                double[] latitude = bundle.getDoubleArray("latitude");
                double[] longitude = bundle.getDoubleArray("longitude");
                if (latitude.length > 0 && longitude.length > 0) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (int i = 0; i < latitude.length || i < longitude.length; i++) {
                        LatLng latLng = new LatLng(latitude[i], longitude[i]);
                        builder.include(latLng);
                        map.addMarker(new MarkerOptions().position(latLng));
                        marks.add(latLng);
                    }
                    LatLngBounds bounds = builder.build();
                    int padding = 5; // offset from edges of the map in pixels
                    CameraUpdate cameraUpdate;
                    if (marks.size() > 1)
                        cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    else
                        cameraUpdate = CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 12);
                    map.moveCamera(cameraUpdate);
                }
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
                latLng = new LatLng(59.932005, 30.420464);
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 7);
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
