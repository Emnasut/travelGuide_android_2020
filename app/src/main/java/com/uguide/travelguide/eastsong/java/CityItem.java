package com.uguide.travelguide.eastsong.java;

import android.os.Bundle;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class CityItem implements Comparable<CityItem> {

    private static final String GUID = "guid";
    private static final String HEADLINE = "headline";
    private static final String DISTANCE = "distance";
    private static final String LOCATION_TYPE = "location_type";
    private static final String NEAREST_LATITUDE = "nearest_latitude";
    private static final String NEAREST_LONGITUDE = "nearest_longitude";
    private static final String LOCATIONS = "locations";

    private String guid;
    private String headline;
    private float distance;
    private String type;
    private List<GeoPoint> locations;
    private GeoPoint nearestLocation;


    @Exclude
    public static CityItem fromBundle(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            final CityItem cityItem = new CityItem();
            cityItem.guid = savedInstanceState.getString(GUID);
            cityItem.headline = savedInstanceState.getString(HEADLINE);
            cityItem.distance = savedInstanceState.getFloat(DISTANCE);
            cityItem.type = savedInstanceState.getString(LOCATION_TYPE);

            final GeoPoint nearestLocation;
            if(savedInstanceState.containsKey(NEAREST_LATITUDE)) {
                nearestLocation = new GeoPoint(savedInstanceState.getDouble(NEAREST_LATITUDE), savedInstanceState.getDouble(NEAREST_LONGITUDE));
            } else {
                nearestLocation = null;
            }

            cityItem.nearestLocation = nearestLocation;



            ArrayList<GeoPoint> locationsList = new ArrayList<>();
            final double[] locations = savedInstanceState.getDoubleArray(LOCATIONS);

            if(locations != null) {
                for(int i = 0; i < locations.length - 1; i+=2) {
                    locationsList.add(new GeoPoint(locations[0], locations[1]));
                }
            }


            cityItem.locations = locationsList;

            return cityItem;
        } else {
            return null;
        }
    }

    @Exclude
    public Bundle toBundle() {
        final Bundle bundle = new Bundle();

        bundle.putString(GUID, guid);
        bundle.putString(HEADLINE, headline);
        bundle.putFloat(DISTANCE, distance);
        bundle.putString(LOCATION_TYPE, type);
        if(nearestLocation != null) {
            bundle.putDouble(NEAREST_LATITUDE, nearestLocation.getLatitude());
            bundle.putDouble(NEAREST_LONGITUDE, nearestLocation.getLongitude());
        }

        if(locations != null) {
            final double[] locationsArray = new double[locations.size() * 2];
            for(int i = 0; i < locationsArray.length - 1; i +=2) {
                final GeoPoint loc = locations.get(i/2);
                locationsArray[i] = loc.getLatitude();
                locationsArray[i+1] = loc.getLongitude();
            }

            bundle.putDoubleArray(LOCATIONS, locationsArray);
        }


        return bundle;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public String getType() {
        return type;
    }

    public void setType(String date) {
        this.type = date;
    }

    public List<GeoPoint> getLocations() {
        return locations;
    }

    public void setLocations(List<GeoPoint> locations) {
        this.locations = locations;
    }

    public GeoPoint getNearestLocation() {
        return nearestLocation;
    }

    public void setNearestLocation(GeoPoint nearestLocation) {
        this.nearestLocation = nearestLocation;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public int compareTo(CityItem otherItem) {
        return this.getDistance() < otherItem.getDistance() ? -1 : (this.getDistance() > otherItem.getDistance() ? 1 : 0);
    }
}