// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.uguide.travelguide.eastsong.java.barcodescanning;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.uguide.travelguide.eastsong.common.CameraImageGraphic;
import com.uguide.travelguide.eastsong.common.FrameMetadata;
import com.uguide.travelguide.eastsong.common.GraphicOverlay;
import com.uguide.travelguide.eastsong.java.CityItem;
import com.uguide.travelguide.eastsong.java.CityListActivity;
import com.uguide.travelguide.eastsong.java.ItemInformationActivity;
import com.uguide.travelguide.eastsong.java.LivePreviewActivity;
import com.uguide.travelguide.eastsong.java.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.app.Activity.RESULT_OK;
import static com.uguide.travelguide.eastsong.java.LivePreviewActivity.CODE_NOT_FOUND;

/**
 * Barcode Detector Demo.
 */
public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {

    private static final String TAG = "BarcodeScanProc";

    private final FirebaseVisionBarcodeDetector detector;

    private final LivePreviewActivity activity;

    private boolean scanning = true;

    public BarcodeScanningProcessor(LivePreviewActivity activity) {
        this.activity = activity;
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        final FirebaseVisionBarcodeDetectorOptions options =  new FirebaseVisionBarcodeDetectorOptions.Builder()
             .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
             .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Barcode Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionBarcode> barcodes,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {

        if(scanning) {
            scanning = false;
            graphicOverlay.clear();

            if (originalCameraImage != null) {
                CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
                graphicOverlay.add(imageGraphic);
            }

            if(!barcodes.isEmpty()) {
                FirebaseVisionBarcode barcode = barcodes.get(0);
                Log.i(TAG, "found QR code: " + barcode.getRawValue());

                //for (int i = 0; i < barcodes.size(); ++i) {
                BarcodeGraphic barcodeGraphic = new BarcodeGraphic(graphicOverlay, barcode);
                graphicOverlay.add(barcodeGraphic);

                evaluateQRCodeAndOpenMaps(barcode);
                //}
                graphicOverlay.postInvalidate();
            } else {
                scanning = true;
            }

        }

    }

    private void openUrl(String url) {
        final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);

        Intent returnIntent = new Intent();
        //returnIntent.putExtra("result", result);
        activity.setResult(RESULT_OK, returnIntent);

        activity.finish();
    }

    private void evaluateQRCodeAndOpenMaps(FirebaseVisionBarcode barcode) {


        final String barcodeValue = barcode.getRawValue();

        final String code;

        //TODO: simplify somehow
        if(barcodeValue.toLowerCase().trim().startsWith("http")) {
            if(barcodeValue.toLowerCase().contains("u-guide.me") || barcodeValue.toLowerCase().contains("gide.me")) {
                if(barcodeValue.contains("?c=")) {
                    code = barcodeValue.split("\\?c=")[1];
                } else {
                    openUrl(barcodeValue);
                    return;
                }
            } else {
                openUrl(barcodeValue);
                return;
            }
        } else {
            code = barcodeValue;
        }

        Log.i(TAG, "used QR code: " + code);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        final DocumentReference docRef = db.collection("places").document(code);




        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {


                        final String type = document.getString("type");

                        if("citymap".equalsIgnoreCase(type)) {
                            final List<String> cityCodes = (List<String>) document.get("qrcodes");

                            final Intent intent = new Intent(activity, CityListActivity.class);



                            final String[] cityCodesArray = new String[cityCodes.size()];
                            cityCodes.toArray(cityCodesArray);

                            intent.putExtra("cityCodes", cityCodesArray);

                            intent.putExtra("cityName", (String)document.get("name"));

                            activity.startActivity(intent);
                            activity.finish();


                        } else {
//                            final List<GeoPoint> locations = (List<GeoPoint>) document.get("locations");

//                            if(locations != null && !locations.isEmpty()) {
                                //final GeoPoint latLong = locations.get(0);

                                //final String mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=" + latLong.getLatitude() + "," + latLong.getLongitude();

                                //Log.i(TAG, "Target: " + mapsUrl);

                                //Toast.makeText(activity, "Target: " + mapsUrl, Toast.LENGTH_LONG).show(); //never vanishes... ^^ bound to camera somehow? at least vanishes when camera is closed by user...

                                //preview.stop();
//                                final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(mapsUrl));
//                                activity.startActivity(intent);
//                                activity.finish();

                                final CityItem cityItem = new CityItem();
                                cityItem.setGuid((String)document.getId());
                                cityItem.setHeadline((String)document.get("name"));

                                final Intent intent = new Intent(activity, ItemInformationActivity.class);
                                intent.putExtras(cityItem.toBundle());
                                activity.startActivity(intent);
                                activity.finish();

//                            } else {
//                                Toast.makeText(activity, "Keine Koordinaten hinterlegt", Toast.LENGTH_LONG).show();
//                                Log.i(TAG, "No coordinates for location");
//                            }
                        }


                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //Toast.makeText(activity, "Data: " + document.getData(), Toast.LENGTH_LONG).show();
                    } else {
                        //If code doesn't exist in db
                        Log.i(TAG, "No such document: " + code);

                        //Toast.makeText(activity, "Code not recognized", Toast.LENGTH_SHORT).show();
                        //scanning = true;

                        Intent returnIntent = new Intent();
                        //returnIntent.putExtra("result", result);
                        activity.setResult(CODE_NOT_FOUND, returnIntent);

                        activity.finish();
                        return;

                    }
                } else {
                    Toast.makeText(activity, "Place not found", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "Fetching doucment failed", e);
            }
        });
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }
}