package org.vicmns.camerageolocation;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class NativeCameraActivity extends Activity {
	
	private Button takePicture;
	private Button takeVideo;
	private OrientationEventListener myOrientationEventListener;
	private Context context;
	
	private final static int PHOTOCODE = 0x0;
	private final static int VIDEOCODE = 0x1;
	private static final String TAG = "NativeCameraActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_native_camera);
		
		context = this;
		initializeViews();
		setClickListeners();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.native_camera, menu);
		return true;
	}
	
	
	private void initializeViews() {
		takePicture = (Button) findViewById(R.id.native_picture_button);
		takeVideo = (Button) findViewById(R.id.native_video_button);
	}
	
	private void setClickListeners() {
		takePicture.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			    startActivityForResult(takePictureIntent, PHOTOCODE);
			}
		});
		
		takeVideo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			    startActivityForResult(takePictureIntent, VIDEOCODE);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    String[] latLong = null;
	    if(resultCode == RESULT_OK){
	    	Uri mediaUri = data.getData();
	    	switch(requestCode) {
		    	case PHOTOCODE:
		    		Log.d(TAG, "Pic saved");
		    		latLong = MetaDataTools.ReadGeotagData(getApplicationContext(), mediaUri);
		    		break;
		    	case VIDEOCODE:
		    		Log.d(TAG, "Video saved");
		    		latLong = MetaDataTools.ReadGeotagData(getApplicationContext(), mediaUri);
		    		break;
	    	}
	    	if(latLong != null) {
    			showToastMessage("Gps data, latitude: " + latLong[0] + ", longitude: " + latLong[1]);		    			
    		}
	    }
	}
	
	private void showToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}

}
