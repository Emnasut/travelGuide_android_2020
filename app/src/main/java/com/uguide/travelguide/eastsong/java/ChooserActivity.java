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
package com.uguide.travelguide.eastsong.java;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.uguide.travelguide.eastsong.BuildConfig;
import com.uguide.travelguide.eastsong.R;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

public final class ChooserActivity extends AppCompatActivity
    implements OnRequestPermissionsResultCallback {
  private static final String TAG = "ChooserActivity";
  private static final int PERMISSION_REQUESTS = 1;

  private static final int SCAN_QR_CODE = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_chooser);



    Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(myToolbar);

      ((TextView)findViewById(R.id.versionInfo)).setText("v" + BuildConfig.VERSION_CODE);

    //setTitle("EastSong Travel Guide");
    //myToolbar.setTitle("EastSong Travel Guide");

    if (!allPermissionsGranted()) {
      getRuntimePermissions();
    }
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info =
          this.getPackageManager()
              .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
          this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  private static boolean isPermissionGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission granted: " + permission);
      return true;
    }
    Log.i(TAG, "Permission NOT granted: " + permission);
    return false;
  }

    public void openQRScanner(View view) {
        startActivityForResult(new Intent(this, LivePreviewActivity.class), SCAN_QR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG, "result " + requestCode + ", " + resultCode + ", " + data);

        // Check which request we're responding to
        if (requestCode == SCAN_QR_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            } else if (resultCode == LivePreviewActivity.CODE_NOT_FOUND) {
                Toast.makeText(this, "This QR Code was not recognized.", Toast.LENGTH_LONG).show();
            }
        }
    }

  public void showVersionInfo(View view) {
    Toast.makeText(this, "Version " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")", Toast.LENGTH_SHORT).show();
  }
}
