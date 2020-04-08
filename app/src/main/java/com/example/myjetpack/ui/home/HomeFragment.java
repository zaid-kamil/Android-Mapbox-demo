package com.example.myjetpack.ui.home;

import android.animation.ValueAnimator;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myjetpack.MainActivity;
import com.example.myjetpack.R;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;

import java.util.List;

public class HomeFragment extends Fragment implements PermissionsListener {

    private static final int REQUEST_CHECK_SETTINGS = 23;
    private MapView mapView;
    public static final String API_KEY = "pk.eyJ1IjoiemFpZGthbWlsIiwiYSI6ImNrNXh2Z2xiYjBnazkzbHBkNG03enQ4NTYifQ.8sAJfK4lDkZ8hysdCxF-Ag";
    private HomeViewModel homeViewModel;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mlocationRequest;
    private LocationSettingsRequest.Builder settingBuilder;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private MapboxMap mapbox;
    private MarkerViewManager markerViewManager;
    private MarkerView markerView;
    private View mView;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Mapbox.getInstance(getActivity(), API_KEY);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        createLocationRequest();
        settingBuilder = new LocationSettingsRequest.Builder().addLocationRequest(mlocationRequest);
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        return root;
    }

    private void createCustomMarker(MapView mapView) {
        mView = LayoutInflater.from(getActivity()).inflate(R.layout.marker_view, null,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TextView textStatus = view.findViewById(R.id.textStatus);
        getLocationSettingStatus();

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textStatus.setText(s);
            }
        });


        mapView = view.findViewById(R.id.mapView);

        createCustomMarker(mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                HomeFragment.this.mapboxMap = mapboxMap;
                LatLng latlng = new LatLng(26, 80);
                CameraPosition camPos = new CameraPosition.Builder().target(latlng).zoom(10).build();
                mapboxMap.setCameraPosition(camPos);
                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        Toast.makeText(getActivity(), "loaded map", Toast.LENGTH_SHORT).show();
                        enableLocationComponent(style);
                        markerViewManager = new MarkerViewManager(mapView, mapboxMap);

                    }
                });
                mapbox = mapboxMap;
            }
        });
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    if (mapbox != null) {
                        if (markerView!=null){
                            markerViewManager.removeMarker(markerView);
                        }
                        markerView = new MarkerView(new LatLng(location.getLatitude(),location.getLongitude()), mView);
                        markerViewManager.addMarker(markerView);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mlocationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void getLocationSettingStatus() {
        SettingsClient client = LocationServices.getSettingsClient(getActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(settingBuilder.build());
        task.addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(getActivity(), new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Toast.makeText(getActivity(), "some error occurred", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        try {
            startLocationUpdates();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        markerViewManager.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(getActivity())) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(getActivity(), loadedMapStyle).build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        Toast.makeText(getActivity(), "status" + granted, Toast.LENGTH_SHORT).show();
    }

    protected void createLocationRequest() {
        mlocationRequest = LocationRequest.create();
        mlocationRequest.setInterval(1000 * 30);
        mlocationRequest.setFastestInterval(5000);
        mlocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
