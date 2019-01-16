package com.uguide.travelguide.eastsong.java;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.uguide.travelguide.eastsong.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CityListActivity extends AppCompatActivity {

    private final static String TAG = "CityListActivity";

    public final static DecimalFormat KM_FORMAT = new DecimalFormat("0.00");

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 111;
    private boolean mLocationPermissionGranted = false;
    private CityItemListAdapter cityListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        setContentView(R.layout.activity_citylist);

        final Toolbar mToolbar= (Toolbar) findViewById(R.id.cityListToolbar);
        setSupportActionBar(mToolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        getDeviceLocation();

        //ArrayList image_details = getListData();

        ArrayList<CityItem> image_details = new ArrayList<>();

        final String cityName = getIntent().getStringExtra("cityName");
        setTitle(cityName);
        actionBar.setTitle(cityName);
        final String[] cityCodes = getIntent().getStringArrayExtra("cityCodes");

//        for(String s : cityCodes) {
//            CityItem cityItem = new CityItem();
//
//            cityItem.setHeadline(s);
//            cityItem.setDistance("5 km");
//            cityItem.setType("Zeugs");
//
//            image_details.add(cityItem);
//        }




        final ListView lv1 = (ListView) findViewById(R.id.custom_list);
        cityListAdapter = new CityItemListAdapter(this, cityCodes);
        lv1.setAdapter(cityListAdapter);

        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = lv1.getItemAtPosition(position);
                CityItem cityItem = (CityItem) o;
                //Toast.makeText(CityListActivity.this, "Selected :" + " " + cityItem, Toast.LENGTH_LONG).show();



                final GeoPoint latLong = cityItem.getLocations().get(0);

                final String mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=" + latLong.getLatitude() + "," + latLong.getLongitude();

                Log.i(TAG, "Target: " + mapsUrl);

                //final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                final Intent intent = new Intent(CityListActivity.this, ItemInformationActivity.class);
                intent.putExtras(cityItem.toBundle());
                startActivity(intent);

            }
        });
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Log.i(TAG, "Location permission is true");
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            cityListAdapter.refreshDistances(mLastKnownLocation);

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
                    getDeviceLocation();
                }
            }
        }
        //updateLocationUI();
    }

    public class CityItemListAdapter extends BaseAdapter {
        private ArrayList<CityItem> listData;
        private LayoutInflater layoutInflater;

        public CityItemListAdapter(Context aContext, String[] cityCodes) {
            //this.listData = listData;
            listData = new ArrayList<>();

            layoutInflater = LayoutInflater.from(aContext);

            final FirebaseFirestore db = FirebaseFirestore.getInstance();

            for(String code : cityCodes) {
                final DocumentReference docRef = db.collection("places").document(code);

                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {


                                final String name = document.getString("name");
                                final String type = document.getString("type");

                                final List<GeoPoint> locations = (List<GeoPoint>) document.get("locations");

                                final CityItem cityItem = new CityItem();

                                cityItem.setGuid(code);

                                cityItem.setHeadline(name);

                                GeoPoint nearestLocation = null;

                                float minDistance = Float.MAX_VALUE;

                                if(mLastKnownLocation != null) {
                                    for(GeoPoint p : locations) {

                                        final float[] distanceResultInMeters = new float[1];
                                        Location.distanceBetween(p.getLatitude(), p.getLongitude(), mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), distanceResultInMeters);

                                        if(distanceResultInMeters[0] < minDistance) {
                                            minDistance = distanceResultInMeters[0];
                                            nearestLocation = p;
                                        }

                                    }
                                    cityItem.setDistance(minDistance);
                                }



                                cityItem.setNearestLocation(nearestLocation);
                                cityItem.setType(type);
                                cityItem.setLocations(locations);

                                listData.add(cityItem);


                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                                Collections.sort(listData);

                                runOnUiThread(() -> notifyDataSetChanged());


                                //Toast.makeText(activity, "Data: " + document.getData(), Toast.LENGTH_LONG).show();
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Toast.makeText(CityListActivity.this, "Ort nicht gefunden", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
            }
        }

        public void refreshDistances(Location currentLocation) {
            if(currentLocation != null) {
                for (CityItem ci : listData) {
                    GeoPoint nearestLocation = null;

                    float minDistance = Float.MAX_VALUE;


                    for (GeoPoint p : ci.getLocations()) {

                        final float[] distanceResultInMeters = new float[1];
                        Location.distanceBetween(p.getLatitude(), p.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), distanceResultInMeters);

                        if (distanceResultInMeters[0] < minDistance) {
                            minDistance = distanceResultInMeters[0];
                            nearestLocation = p;
                        }

                    }

                    ci.setDistance(minDistance);
                    ci.setNearestLocation(nearestLocation);

                }
                Collections.sort(listData);
                runOnUiThread(() -> notifyDataSetChanged());
            }
        }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_row_layout, null);
                holder = new ViewHolder();
                holder.headlineView = (TextView) convertView.findViewById(R.id.title);
                holder.reporterNameView = (TextView) convertView.findViewById(R.id.reporter);
                holder.reportedDateView = (TextView) convertView.findViewById(R.id.date);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.headlineView.setText(listData.get(position).getHeadline());

            final String distance = KM_FORMAT.format((listData.get(position).getDistance()) / 1000.0) + " km";
            holder.reporterNameView.setText(distance);
            holder.reportedDateView.setText(listData.get(position).getType());
            return convertView;
        }

        class ViewHolder {
            TextView headlineView;
            TextView reporterNameView;
            TextView reportedDateView;
        }


    }

}
