package com.rocks.kevinwalker.parkit.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.rocks.kevinwalker.parkit.R;
import com.rocks.kevinwalker.parkit.spot.Spot;
import com.rocks.kevinwalker.parkit.utils.FirestoreHelper;
import com.rocks.kevinwalker.parkit.utils.LocationHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MapsFragment extends android.support.v4.app.Fragment implements OnMapReadyCallback,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    private static final String TAG = MapsFragment.class.getName();
    private static final float DEFAULT_ZOOM = 9f;
    private static final String EPOCH_TIME = "fragment_timestamp";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private boolean isCameraAnimationFinished = true;

    private long epochTimeStamp;

    private String currentAddress = "";
    private Geocoder geocoder;
    private List<Address> addressList = new ArrayList<>();
    private ArrayList<Spot> userSpotsForMap = new ArrayList<>();

    private GoogleMap map;
    private Marker userMarker;
    private LocationHelper locationHelper;

    private View mView;
    @BindView(R.id.btn_park) Button btn_park;
    @BindView(R.id.btn_leave) Button btn_leave;
    @BindView(R.id.map) MapView mapView;
    @BindView(R.id.btn_find_user_current_location) FloatingActionButton btn_find_user_current_location;
    @BindView(R.id.btn_find_user_parked_location) FloatingActionButton btn_find_user_parked_location;


    private MapsFragment.MapsCallBack mapsCallBack;
    private Context mContext;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);



        if (savedInstanceState != null) {
            epochTimeStamp = savedInstanceState.getLong(EPOCH_TIME);
            Log.i(TAG, String.valueOf(epochTimeStamp));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapsFragment.MapsCallBack) {
            mapsCallBack = (MapsFragment.MapsCallBack) context;
            mContext = context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MapsFragment.MapsCallBack");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geocoder = new Geocoder(mContext);
        locationHelper = new LocationHelper(this.getContext());
        locationHelper.startLocationUpdates();
        locationHelper.getCurrentLocation();

        userSpotsForMap = FirestoreHelper.getInstance().getSpotForMap();


        getActivity().setTitle(getResources().getString(R.string.map_nav_title));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.activity_maps, container, false);
        ButterKnife.bind(this, mView);

        btn_park.setOnClickListener(this);
        btn_leave.setOnClickListener(this);
        btn_find_user_current_location.setOnClickListener(this);
        btn_find_user_parked_location.setOnClickListener(this);
        btn_find_user_current_location.setEnabled(true);

        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        if (mapView != null) {
            initMap(mapView);
        }

        if (FirestoreHelper.getInstance().getCurrentUser().isUserParked()) {
            btn_park.setEnabled(false); //Do not show
            btn_leave.setEnabled(true); // Show
        } else {
            Log.i(TAG, "from onViewCreated");
            btn_park.setEnabled(true); // Show
            btn_leave.setEnabled(false); // Do not show
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveEpochTimeToBundle(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    public void onPause() {
        super.onPause();

        locationHelper.getmFusedLocationProviderClient().removeLocationUpdates(locationHelper.getLocationCallback());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, String.valueOf(epochTimeStamp));

        isCameraAnimationFinished = true;

        locationHelper.startLocationUpdates();

        refreshUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO: Determine why this does not remove location updates to Firebase user once logged out
        locationHelper.getmFusedLocationProviderClient().removeLocationUpdates(locationHelper.getLocationCallback());
    }

    public void refreshUI() {
        if (FirestoreHelper.getInstance().getCurrentUser().isUserParked()) {
            btn_leave.setEnabled(true);
            btn_park.setEnabled(false);
            btn_find_user_current_location.setEnabled(true);
            //placeMarkerOnMap(FirestoreHelper.getInstance().getCurrentUser().getUserParkedLocation(), currentAddress, true);
        } else {
            btn_park.setEnabled(true);
            btn_leave.setEnabled(false);
        }
    }

    private void saveEpochTimeToBundle(Bundle outState) {
        long epochTime;
        Date myDate = new Date();
        epochTime = myDate.getTime();
        Log.i(TAG, String.valueOf(epochTime));
        outState.putLong(EPOCH_TIME, epochTime);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_park:
                if (locationHelper.hasLocationPermission()) {
                    setUserParkedLocation();
                    btn_park.setEnabled(false);
                    btn_leave.setEnabled(true);
                }
                break;

            case R.id.btn_leave:
                // TODO: Remove - replace with LeaveDialogFragment
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                dialog.setTitle(getResources().getString(R.string.LEAVE_DIALOG_TITLE))
                        .setMessage(getResources().getString(R.string.LEAVE_DIALOG_MESSAGE))
                        .setNegativeButton(getResources().getString(R.string.LEAVE_DIALOG_NEGATIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                dialoginterface.cancel();
                            }
                        })
                        .setPositiveButton(getResources().getString(R.string.LEAVE_DIALOG_POSITIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                // If user clicks yes, call to leaveSpace to clear data and marker
                                leaveSpace();
                            }
                        }).show();
                break;

            case R.id.btn_find_user_current_location:
                locationHelper.getCurrentLocation();
                animateCamera(FirestoreHelper.getInstance().getCurrentUser().getUserCurrentLocation(), DEFAULT_ZOOM, currentAddress);
                break;

            case R.id.btn_find_user_parked_location:
                if (FirestoreHelper.getInstance().getCurrentUser().isUserParked()) {
                    animateCamera(FirestoreHelper.getInstance().getCurrentUser().getUserParkedLocation(), DEFAULT_ZOOM, currentAddress);
                } else {
                    if (getView()!= null) {
                        Snackbar.make(getView(), getString(R.string.btn_find_user_parked_location_not_parked),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
        }
    }

    private void initMap(MapView mapFragment) {
            // Already have permission, so set up map
            mapFragment.onCreate(null);
            mapFragment.onResume();
            mapFragment.getMapAsync(this);
    }

    private void animateCamera(CustomLocation customLocation, float zoom, String title) {
        if (isCameraAnimationFinished) {
            Log.d(TAG, "Moving camera to: " + "Lat: " + customLocation.getLatitude() + " Lon: " + customLocation.getLongitude());
            GoogleMap.CancelableCallback mapsCancellableCallback = new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    cameraAnimationFinished();
                }
                @Override
                public void onCancel() {

                }
            };

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(customLocation.getLatitude(), customLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), mapsCancellableCallback);
        } else {
            isCameraAnimationFinished = false;
        }
    }

    private void animateCamera(CustomLocation customLocation, float zoom) {
        if (isCameraAnimationFinished) {
            isCameraAnimationFinished = false;
            Log.d(TAG, "Moving camera to: " + "Lat: " + customLocation.getLatitude() + " Lon: " + customLocation.getLongitude());
            GoogleMap.CancelableCallback mapsCancellableCallback = new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    cameraAnimationFinished();
                }

                @Override
                public void onCancel() {}
            };

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(customLocation.getLatitude(), customLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), mapsCancellableCallback);
        }
    }

    private void cameraAnimationFinished() {
        isCameraAnimationFinished = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            initMap(mapView);
                            // TODO: Do not continue the MapsFragment, continue to request permission or force them out of the Activity
                            return;
                        } else {
                            initMap(mapView);
                        }
                    }
                }
            }
        }
    }

    // TODO: Add or remove arguments to match our needs for the various Marker types we'll be using/ defining
    protected void placeMarkerOnMap(CustomLocation customLocation, String title, boolean markerVisible) {
        if (map != null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(customLocation.getLatitude(), customLocation.getLongitude()));
            markerOptions.title(title);
            markerOptions.icon(bitmapDescriptorFromVector(mContext, R.drawable.pin));
            markerOptions.visible(markerVisible);
            //map.clear();

            userMarker = map.addMarker(markerOptions);

            setMarkerBounce(userMarker);
        }
    }

    public void placeSpotsOnMap(ArrayList<Spot> spots) {
        if (map != null) {

            for (int i = 0; i < spots.size(); i++) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(spots.get(i).getLatitude(), spots.get(i).getLongitude()));
                markerOptions.title(spots.get(i).getName());
                markerOptions.icon(bitmapDescriptorFromVector(mContext, R.drawable.pin));
                markerOptions.visible(true);

                map.addMarker(markerOptions);
            }
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void setMarkerBounce(final Marker marker) {
        final Handler handler = new Handler();
        final long startTime = SystemClock.uptimeMillis();
        final long duration = 2000;
        final Interpolator interpolator = new BounceInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 1.2f * t);

                if (t > 0.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private String getAddressFromGeocoder(CustomLocation customLocation) {

        String address = "";
        try {
            addressList = geocoder.getFromLocation(customLocation.getLatitude(), customLocation.getLongitude(), 1);
            address = addressList.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.getStackTrace().toString();
        }

        addressList.clear();

        return address;
    }

    private void setCurrentAddress(String address) {
        currentAddress = address;
    }

    private String getCurrentAddress() {
        return this.currentAddress;
    }

    private LatLng getCurrentLatLng() {
        return new LatLng(FirestoreHelper.getInstance().getCurrentUser().getUserCurrentLocation().getLongitude(), FirestoreHelper.getInstance().getCurrentUser().getUserCurrentLocation().getLatitude());
    }

    private void userParkedLocationUpdated(CustomLocation customLocation, boolean isParked) {
        setCurrentAddress(getAddressFromGeocoder(customLocation));
        placeMarkerOnMap(customLocation, currentAddress, true);
        mapsCallBack.parkedLocationUpdate(customLocation, isParked);
    }

    private void setUserParkedLocation() {
        userParkedLocationUpdated(locationHelper.getCurrentLocation(), true);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        MapsInitializer.initialize(mContext);

        if (map == null) {
            map = googleMap;
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            setMyLocationEnabled();
            animateCamera(FirestoreHelper.getInstance().getCurrentUser().getUserCurrentLocation(), DEFAULT_ZOOM, currentAddress);
        }

        if (FirestoreHelper.getInstance().getCurrentUser().isUserParked()) {
            btn_leave.setEnabled(true);
            btn_park.setEnabled(false);
            placeMarkerOnMap(FirestoreHelper.getInstance().getCurrentUser().getUserParkedLocation(), currentAddress, true);
        } else {
            btn_park.setEnabled(true);
            btn_leave.setEnabled(false);
        }

        placeSpotsOnMap(userSpotsForMap);

        // Set the button to be enabled when the map is ready
        btn_park.setEnabled(true);
    }

    private void setMyLocationEnabled() {
        // Checks for permission
        if (locationHelper.hasLocationPermission()) {

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            // Adds blue dot to current location once map is centered on it
            map.setMyLocationEnabled(true);
        }
    }

    // TODO: Update user's Marker
    @Override
    public void onLocationChanged(Location location) {


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // TODO: Clear 'GPS unavailable' notification
    @SuppressLint("RestrictedApi")
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // TODO: Notify user that their GPS is unavailable
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // TODO: Pull information from Marker; display to user
    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    public void leaveSpace() {
        userMarker.remove();
        btn_leave.setEnabled(false);
        btn_park.setEnabled(true);
        mapsCallBack.parkedLocationUpdate(new CustomLocation(), false);
    }

    public interface MapsCallBack {

        void parkedLocationUpdate(CustomLocation userParkedLocation, boolean isParked);
    }
}