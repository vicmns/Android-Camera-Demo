package org.vicmns.camerageolocation;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

public class CameraSelectorActivity extends Activity {
	
	private Button nativeCameraBttn;
	private Button customCameraBttn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_selector_activity);
		
		initializeViewObjects();
		setClickListeners();
	}
	
	private void initializeViewObjects() {
		nativeCameraBttn = (Button) findViewById(R.id.native_camera_selector);
		customCameraBttn = (Button) findViewById(R.id.custom_camera_selector);
	}
	
	private void setClickListeners() {
		nativeCameraBttn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent nativeCameraIntent = new Intent(getApplicationContext(), NativeCameraActivity.class);
				startActivity(nativeCameraIntent);
			}
		});
		
		customCameraBttn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent customCameraIntent = new Intent(getApplicationContext(), CustomCameraActivity.class);
				startActivity(customCameraIntent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera_selector, menu);
		return true;
	}

}
