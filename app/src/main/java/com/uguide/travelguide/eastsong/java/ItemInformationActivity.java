package com.uguide.travelguide.eastsong.java;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.uguide.travelguide.eastsong.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class ItemInformationActivity extends AppCompatActivity {

    private final static String TAG = "ItemInformationActivity";

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 111;
    private boolean mLocationPermissionGranted = false;

    private CityItem cityItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        setContentView(R.layout.activity_item_information);

        final Toolbar mToolbar= (Toolbar) findViewById(R.id.itemInformationToolbar);
        setSupportActionBar(mToolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if(getIntent() != null) {
            cityItem = CityItem.fromBundle(getIntent().getExtras());

            if(cityItem != null) {
                ((TextView)findViewById(R.id.itemViewHeadline)).setText(cityItem.getHeadline());

                final FirebaseFirestore db = FirebaseFirestore.getInstance();
                final DocumentReference docRef = db.collection("places").document(cityItem.getGuid());

                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {


                                final String headline = document.getString("name");
                                final String bannerUrl = document.getString("banner_url");
                                final String textBody = document.getString("text_body");
                                final String homepage = document.getString("homepage");

                                if(headline != null) {
                                    //actionBar.setTitle(headline);
                                    ((TextView)findViewById(R.id.itemViewHeadline)).setText(headline);
                                }

                                final String usedBannerUrl;
                                if(bannerUrl != null && !bannerUrl.isEmpty()) {
                                    usedBannerUrl = bannerUrl;
                                } else {
                                    usedBannerUrl = "https://storage.googleapis.com/u-guide-me-imgs/" + document.getId() + "/banner.jpg";
                                }

                                final Drawable fallbackDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.loading_placeholder, null);

                                Glide.with(ItemInformationActivity.this)
                                        //.asBitmap()
                                        .load(usedBannerUrl)
                                        .apply(new RequestOptions()
                                                        .placeholder(R.drawable.loading_placeholder_white)
                                                        //.centerInside()

//                                                    .centerCrop()
//                                                    .dontAnimate()
//                                                    .dontTransform()
                                                        .encodeQuality(100)
                                                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                        )
                                        .into((ImageView) findViewById(R.id.itemViewBanner))/*.onLoadFailed(fallbackDrawable)*/;

                                if(textBody != null) {
                                    ((TextView)findViewById(R.id.itemViewTextBody)).setText(textBody);
                                }

                                if(homepage != null && !homepage.isEmpty()) {
                                    findViewById(R.id.itemViewButtonOpenHomepage).setVisibility(View.VISIBLE);
                                    ((Button)findViewById(R.id.itemViewButtonOpenHomepage)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(homepage));
                                            startActivity(intent);
                                        }
                                    });
                                } else {
                                    findViewById(R.id.itemViewButtonOpenHomepage).setVisibility(View.GONE);
                                }

                                final List<GeoPoint> locations = (List<GeoPoint>) document.get("locations");


                                if(locations != null && !locations.isEmpty()) {

                                    cityItem.setLocations(locations);

                                    findViewById(R.id.itemViewButtonStartNavigation).setVisibility(View.VISIBLE);
                                    ((Button)findViewById(R.id.itemViewButtonStartNavigation)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            navigateToClosestLocation();


                                        }
                                    });
                                } else {
                                    findViewById(R.id.itemViewButtonStartNavigation).setVisibility(View.GONE);
                                }


                                //runOnUiThread(() -> notifyDataSetChanged());


                                //Toast.makeText(activity, "Data: " + document.getData(), Toast.LENGTH_LONG).show();
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Toast.makeText(ItemInformationActivity.this, "Ort nicht gefunden", Toast.LENGTH_SHORT).show(); //TODO: remove
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });

            } else {
                Log.i(TAG, "Null cityItem");
            }
        }

    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Log.i(TAG, "Location permission is true");
            navigateToClosestLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void navigateToClosestLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();


                            GeoPoint nearestLocation = null;

                            float minDistance = Float.MAX_VALUE;

                            for (GeoPoint p : cityItem.getLocations()) {
                                final float[] distanceResultInMeters = new float[1];
                                Location.distanceBetween(p.getLatitude(), p.getLongitude(), mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), distanceResultInMeters);

                                if (distanceResultInMeters[0] < minDistance) {
                                    minDistance = distanceResultInMeters[0];
                                    nearestLocation = p;
                                }
                            }

                            cityItem.setNearestLocation(nearestLocation);
                            cityItem.setDistance(minDistance);

                            final String mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=" + nearestLocation.getLatitude() + "," + nearestLocation.getLongitude();
                            final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                            startActivity(intent);

                            //cityListAdapter.refreshDistances(mLastKnownLocation);

                            Log.i(TAG, "Current location: " + mLastKnownLocation.getLongitude() + "," + mLastKnownLocation.getLatitude());
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                        }
                    }
                });
            } else {
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    navigateToClosestLocation();
                }
            }
        }
        //updateLocationUI();
    }
}
