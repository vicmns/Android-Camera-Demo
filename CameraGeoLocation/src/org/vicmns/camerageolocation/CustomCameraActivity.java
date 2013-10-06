package org.vicmns.camerageolocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.MediaRecorder.OnInfoListener;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView.ScaleType;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class CustomCameraActivity extends Activity implements Camera.ShutterCallback, Camera.PictureCallback, OnCheckedChangeListener ,
			OnClickListener, OnInfoListener, SensorEventListener  {
	
	private static final String TAG = "CustomCameraActivity";
	
	private ToggleButton startStopRecording;
	private ToggleButton photoVideo;
	private Button shutterButton;
	private ImageView recordIndicator;
	private ImageView albumPreview;
	private ImageView takePictureBkg;
	private ImageView gpsIndicator;
	private RelativeLayout mainCameraLayout;
	private FrameLayout previewCamera;
	private FrameLayout albumPreviewLayout;
	private FrameLayout gpsIndicatorLayout;
	private Camera mCamera;
	private CameraPreview cameraPreview;
	
	private Context context;
	private OrientationEventListener myOrientationEventListener;
	
	private static boolean stopAnimation = false;
	private boolean isRecording = false;
	
	
	private boolean isLandscapeInverted = false;
	private boolean isPortrait = false;
	private boolean isLandscape = true;
	
	private int lastRotation;
	
	private File lastMediaUri;
	private boolean isPictureSelected;

	private MediaRecorder mediaRecorder;

	private CameraVideoPreview cameraVideoPreview;
	
	private Location gpsLocation;
	private LocationManager locationManager;
	private LocationListener locListener;
	private static final long TIMEUPDATE = 300000;
	private static final long DISTANCEUPDATE = 300;
	private boolean stopGpsAnimation = false;
	
	private String videoFilePath;
	
	private DrawingView drawingView;

	private SensorManager sensorManager;
	
	private float mLastX;
	private float mLastY;
	private float mLastZ;
	private boolean mInitialized = false;
	private boolean mAutoFocus = true;
	private boolean isFocused = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.custom_camera_view);
		
		context = this;
		
		mainCameraLayout = (RelativeLayout) findViewById(R.id.mainCameraLayout);
		previewCamera = (FrameLayout) findViewById(R.id.cameraPreview);
		albumPreviewLayout = (FrameLayout) findViewById(R.id.album_preview_layout);
		gpsIndicatorLayout = (FrameLayout) findViewById(R.id.gps_indicator_layout);
		
		startStopRecording = (ToggleButton) findViewById(R.id.record_toggle);
		photoVideo = (ToggleButton) findViewById(R.id.change_media_toggle);
		shutterButton = (Button) findViewById(R.id.shutter_button);
		recordIndicator = (ImageView) findViewById(R.id.record_indicator_iv);
		albumPreview = (ImageView) findViewById(R.id.album_preview_iv);
		takePictureBkg = (ImageView) findViewById(R.id.take_picture_bkg_iv);
		gpsIndicator = (ImageView) findViewById(R.id.gps_indicator_iv);
		
		startStopRecording.setOnCheckedChangeListener(this);
		photoVideo.setOnCheckedChangeListener(this);
		shutterButton.setOnClickListener(this);
		albumPreview.setOnClickListener(this);
		

        drawingView = new DrawingView(this);
        LayoutParams layoutParamsDrawing 
        	= new LayoutParams(LayoutParams.FILL_PARENT, 
        			LayoutParams.FILL_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);
		
		setRotationListener();
		isPictureSelected = true;
		getLocation();
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}
	
	@Override
	protected void onDestroy() {
	 super.onDestroy();
	 myOrientationEventListener.disable();
	}
	
	@Override
	protected void onStart() {
		prepareCamera();
		if(isPictureSelected)
			new GetCameraRollThumbImage().execute();
		else
			new GetCameraRollThumbImageVideo().execute();
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
		        SensorManager.SENSOR_DELAY_NORMAL);
		super.onResume();
	}

	@Override
	protected void onPause() {
		sensorManager.unregisterListener(this);
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		locationManager.removeUpdates(locListener);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putBoolean("CameraVideoState", true);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  photoVideo.setChecked(savedInstanceState.getBoolean("CameraVideoState"));
	}
	
	private void prepareCamera() {
		previewCamera.removeAllViews();
		mCamera = getCameraInstance();
		if(mCamera != null) {
			cameraPreview = new CameraPreview(this, mCamera,
					((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
			previewCamera.addView(cameraPreview);
			
			//mCamera.setAutoFocusMoveCallback(moveFocus);
		}
	}
	
	private void setVideoView() {
		previewCamera.removeAllViews();
		mCamera = getCameraInstance();
		if(mCamera != null) {
			cameraVideoPreview= new CameraVideoPreview(this, mCamera,
					((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
			previewCamera.addView(cameraVideoPreview);
		}
	}
	
	private void prepareRecorder() {
		mCamera.stopPreview();
		mCamera.unlock();
		mediaRecorder = new MediaRecorder();
		setupVideoCamera();			
	}
	
	private void releaseRecorder() {
		mediaRecorder.reset();   // clear recorder configuration
        mediaRecorder.release(); // release the recorder object
        mediaRecorder = null;
        mCamera.lock();
        mCamera.startPreview();
        new GetCameraRollThumbImageVideo().execute();
	}
	
	private void setupVideoCamera() {
		CamcorderProfile camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
		if(camProfile == null) {
			camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
		}
		mediaRecorder.setCamera(mCamera);
		
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setProfile(camProfile);
		mediaRecorder.setOrientationHint(lastRotation);
    	
    	mediaRecorder.setPreviewDisplay(cameraVideoPreview.getSufaceHolderHolder());
    	mediaRecorder.setOnInfoListener(this);
    	this.videoFilePath = getVideoFileName();
		mediaRecorder.setOutputFile(this.videoFilePath);
	}
	
	public String getVideoFileName() {
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM ), "Camera" );
			if (! mediaStorageDir.exists()){
				if (! mediaStorageDir.mkdirs()){
					Log. d(TAG, "failed to create directory" );
					return null ;
				}
			}

		/** Create media file name */
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss" ).format( new Date());
		File mediaFile;
		return new File(mediaStorageDir.getPath() + File. separator +timeStamp + ".mp4" ).toString();
	}
	
	//Create a instance of Camera
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}
	
	private void setRotationListener() {
		myOrientationEventListener = new OrientationEventListener(this, 
				SensorManager.SENSOR_DELAY_UI) {			
				@Override
				public void onOrientationChanged(int grades) {
					if(grades < 280 && grades > 210) {
						//This is rotation 0
						if(!isLandscape) {
							Log.i(TAG, "Landscape 1");
							rotate0Deg();
							lastRotation = 0;
						}
						isLandscape = true;
						isLandscapeInverted = false;
						isPortrait = false;
					} else if(grades > 70 && grades < 130) {
						//this is rotation 180
						if(!isLandscapeInverted) {
							Log.i(TAG, "Landscape 2");
							rotateM180Deg();
							lastRotation = 180;
						}
						isLandscapeInverted = true;
						isLandscape = false;
						isPortrait = false;
					} else if(grades > 330 || grades < 70) {
						//this is rotation 90
						if(!isPortrait) {
							Log.i(TAG, "Portrait");
							rotateM90Deg();
							lastRotation = 90;
						}
						isPortrait = true;
						isLandscape = false;
						isLandscapeInverted = false;
					}
					//Log.i(TAG, "Orientation: " + String.valueOf(grades));
			}};
			
		if (myOrientationEventListener.canDetectOrientation()){
			Log.i(TAG, "Detecting Orientation");
			myOrientationEventListener.enable();
		}
		else{
			Log.i(TAG, "Something went wrong");
			finish();
		}
	}
	
	private void rotateM180Deg() {
		ObjectAnimator startStopRecordingAnimation = ObjectAnimator.ofFloat(startStopRecording,
				"rotation", -180);
		ObjectAnimator photoVideoAnimation = ObjectAnimator.ofFloat(photoVideo,
				"rotation", -180);
		ObjectAnimator shutterButtonAnimation = ObjectAnimator.ofFloat(shutterButton,
				"rotation", -180);
		ObjectAnimator albumPreviewAnimation = ObjectAnimator.ofFloat(albumPreviewLayout,
				"rotation", -180);
		
		startStopRecordingAnimation.setDuration(500);
		photoVideoAnimation.setDuration(500);
		shutterButtonAnimation.setDuration(500);
		albumPreviewAnimation.setDuration(500);
		
		startStopRecordingAnimation.start();
		photoVideoAnimation.start();
		shutterButtonAnimation.start();
		albumPreviewAnimation.start();
	}
	
	private void rotate0Deg() {
		ObjectAnimator startStopRecordingAnimation = ObjectAnimator.ofFloat(startStopRecording,
				"rotation", 0);
		ObjectAnimator photoVideoAnimation = ObjectAnimator.ofFloat(photoVideo,
				"rotation", 0);
		ObjectAnimator shutterButtonAnimation = ObjectAnimator.ofFloat(shutterButton,
				"rotation", 0);
		ObjectAnimator albumPreviewAnimation = ObjectAnimator.ofFloat(albumPreviewLayout,
				"rotation", 0);
		
		startStopRecordingAnimation.setDuration(500);
		photoVideoAnimation.setDuration(500);
		shutterButtonAnimation.setDuration(500);
		albumPreviewAnimation.setDuration(500);
		
		startStopRecordingAnimation.start();
		photoVideoAnimation.start();
		shutterButtonAnimation.start();
		albumPreviewAnimation.start();
	}
	
	private void rotateM90Deg() {
		ObjectAnimator startStopRecordingAnimation = ObjectAnimator.ofFloat(startStopRecording,
				"rotation", -90);
		ObjectAnimator photoVideoAnimation = ObjectAnimator.ofFloat(photoVideo,
				"rotation", -90);
		ObjectAnimator shutterButtonAnimation = ObjectAnimator.ofFloat(shutterButton,
				"rotation", -90);
		ObjectAnimator albumPreviewAnimation = ObjectAnimator.ofFloat(albumPreviewLayout,
				"rotation", -90);
		
		startStopRecordingAnimation.setDuration(500);
		photoVideoAnimation.setDuration(500);
		shutterButtonAnimation.setDuration(500);
		albumPreviewAnimation.setDuration(500);
		
		startStopRecordingAnimation.start();
		photoVideoAnimation.start();
		shutterButtonAnimation.start();
		albumPreviewAnimation.start();
	}
	
	private void startRecordAnimation() {
		recordIndicator.setVisibility(View.VISIBLE);
		final Animation in = new AlphaAnimation(0.0f, 1.0f);
		in.setInterpolator(new DecelerateInterpolator());
		in.setDuration(1000);
		
		in.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(!stopAnimation)
					startRecordAnimation();
			}
		});

		final Animation out = new AlphaAnimation(1.0f, 0.0f);
		out.setInterpolator(new AccelerateInterpolator());
		out.setDuration(1000);
		
		out.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(!stopAnimation)
					recordIndicator.startAnimation(in);				
			}
		});
		
		recordIndicator.startAnimation(out);
	}
	
	
	private void startGpsAnimation() {
		final Animation in = new AlphaAnimation(0.0f, 1.0f);
		in.setInterpolator(new DecelerateInterpolator());
		in.setDuration(1000);
		
		in.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(!stopGpsAnimation)
					startGpsAnimation();
			}
		});

		final Animation out = new AlphaAnimation(1.0f, 0.0f);
		out.setInterpolator(new AccelerateInterpolator());
		out.setDuration(1000);
		
		out.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(!stopGpsAnimation)
					gpsIndicatorLayout.startAnimation(in);				
			}
		});
		
		gpsIndicatorLayout.startAnimation(out);
	}
	
	private void fadeOutAnimation(final View view, int time){
		final Animation out = new AlphaAnimation(1.0f, 0.0f);
		out.setInterpolator(new AccelerateInterpolator());
		out.setDuration(time);
		out.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.INVISIBLE);				
			}
		});
		view.startAnimation(out);
	}
	
	private void fadeInAnimation(View view, int time) {
		view.setVisibility(View.VISIBLE);
		final Animation in = new AlphaAnimation(0.0f, 1.0f);
		in.setInterpolator(new DecelerateInterpolator());
		in.setDuration(time);
		view.startAnimation(in);
	}
	
	private void stopRecordAnimation() {
		stopAnimation = true;
		recordIndicator.clearAnimation();
		recordIndicator.setVisibility(View.GONE);
	}
	
	private void highlightScreen() {
		takePictureBkg.setVisibility(View.VISIBLE);
		final Animation in = new AlphaAnimation(0.0f, 1.0f);
		in.setInterpolator(new DecelerateInterpolator());
		in.setDuration(200);

		final Animation out = new AlphaAnimation(1.0f, 0.0f);
		out.setInterpolator(new AccelerateInterpolator());
		out.setDuration(200);
		
		in.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				takePictureBkg.setAnimation(out);			}
		});
		
		out.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				takePictureBkg.setVisibility(View.GONE);
			}
		});
		
		takePictureBkg.setAnimation(in);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		File pictureFileDir = new File(Environment.
				getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) 
				+ File.separator + "Camera");
		
	    if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

	      Log.d(TAG, "Can't create directory to save image.");
	      Toast.makeText(context, "Can't create directory to save image.",
	          Toast.LENGTH_LONG).show();
	      return;

	    }

	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
	    String date = dateFormat.format(new Date());
	    String photoFile = "Picture_" + date + ".jpg";

	    String filename = pictureFileDir.getPath() + File.separator + photoFile;
	    Log.i(TAG, "To save: " + filename);
	    File pictureFile = new File(filename);

	    try {
	      FileOutputStream fos = new FileOutputStream(pictureFile);
	      fos.write(data);
	      fos.close();
	      
	      sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
	    		  Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
	      
	      setImageExifData(pictureFile.toString());
	      
	    } catch (Exception error) {
	      Log.d(TAG, "File" + filename + "not saved: "
	          + error.getMessage());
	      Toast.makeText(context, "Image could not be saved.",
	          Toast.LENGTH_LONG).show();
	    }
	    new GetCameraRollThumbImage().execute();
	    
		camera.startPreview();	
	}

	@Override
	public void onShutter() {
		highlightScreen();
	}
		
	AutoFocusCallback touchAutofocus = new AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			mAutoFocus = true;
			isFocused = true;
			drawingView.setVisibility(View.INVISIBLE);
		}
	};
	
	AutoFocusCallback moveAutofocus = new AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			mAutoFocus = true;
			isFocused = true;
		}
	};
		
	//Compatible only for 4.1 devices
	@SuppressLint("NewApi")
	AutoFocusMoveCallback moveFocus = new AutoFocusMoveCallback() {
		
		@Override
		public void onAutoFocusMoving(boolean start, Camera camera) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private void takePicture() {
		mCamera.takePicture(this, null, this);
	}
	
	private void setImageExifData(String pictureFile) {
		ExifInterface exif;
		try {
			exif = new ExifInterface(pictureFile);
			if(isLandscape) {
				Log.i(TAG, "Exif Rotation normal");
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ExifInterface.ORIENTATION_NORMAL);
			}
			else if(isPortrait) {
				Log.i(TAG, "Exif Rotation 90");
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ExifInterface.ORIENTATION_ROTATE_90);
			}
			else if(isLandscapeInverted) {
				Log.i(TAG, "Exif Rotation 180");
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, ""+ExifInterface.ORIENTATION_ROTATE_180);
			}
			exif.saveAttributes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		setPictureGpsExifData(pictureFile);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(buttonView.equals(photoVideo)) {
			if(isChecked) {
				//Video
				shutterButton.setVisibility(View.GONE);
				startStopRecording.setVisibility(View.VISIBLE);
				new GetCameraRollThumbImageVideo().execute();
				setVideoView();
			} else {
				//Photo
				prepareCamera();
				shutterButton.setVisibility(View.VISIBLE);
				startStopRecording.setVisibility(View.GONE);
				new GetCameraRollThumbImage().execute();
			}
		} else {
			if(isChecked && !isRecording) {
				isRecording = true;
				stopAnimation = false;
				photoVideo.setVisibility(View.GONE);
				albumPreviewLayout.setVisibility(View.GONE);
				startRecordAnimation();
				prepareRecorder();
				try {
					mediaRecorder.prepare();
					mediaRecorder.start();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if(isRecording) {
				isRecording = false;
				photoVideo.setVisibility(View.VISIBLE);
				albumPreviewLayout.setVisibility(View.VISIBLE);
				stopRecordAnimation();
				try{
					mediaRecorder.stop();
					setVideoGpsExifData(this.videoFilePath);
					sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
							Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
				} catch(IllegalStateException e) {
					e.printStackTrace();
				}
				releaseRecorder();
			}			
		}
		
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.shutter_button:
				if(isFocused) {
					takePicture();
				}
				break;
			case R.id.album_preview_iv:
				if(lastMediaUri != null) {
					if(isPictureSelected)
						openImageIntent();
					else
						openVideoIntent();
				}
				break;
		}
		
	}
	
	private void openImageIntent() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(lastMediaUri), "image/*");
		startActivity(intent);	
	}
	
	private void openVideoIntent() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(lastMediaUri), "video/*");
		startActivity(intent);
	}
	
	private class GetCameraRollThumbImage extends AsyncTask<Void, Void, Void> {
		File[] imagesFiles;
		
		@Override
		protected void onPreExecute() {
			fadeOutAnimation(albumPreview, 500);
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			File currentDir = new File(Environment.
					getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) 
					+ File.separator + "Camera");
			if(currentDir.canRead()) {
				//Make an array type File  with the list of all files of each folder
				imagesFiles = currentDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return FileExtensionFilter.isFileImage(name);
					}
				});
				if (imagesFiles.length > 0) {
					Arrays.sort(imagesFiles, new Comparator<Object>()
							{
						public int compare(Object o1, Object o2) {
							if (((File)o1).lastModified() > ((File)o2).lastModified()) {
								return -1;
							} else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
								return +1;
							} else {
								return 0;
							}
						}
							});
				}
			}// read card 
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Set Image
			if(imagesFiles != null && imagesFiles.length > 0) {
				lastMediaUri = imagesFiles[0];
				Bitmap bitmap = PictureTools.decodeAndCropBitmapFromUri(context, 
						imagesFiles[0].getAbsolutePath(), 
						albumPreview.getWidth(), 
						albumPreview.getHeight());
				albumPreview.setScaleType(ScaleType.CENTER_CROP);
				albumPreview.setImageBitmap(bitmap);
				fadeInAnimation(albumPreview,500);
			}
			isPictureSelected = true;
			super.onPostExecute(result);
		}
	}
	
	private class GetCameraRollThumbImageVideo extends AsyncTask<Void, Void, Void> {
		File[] imagesFiles;
		
		@Override
		protected void onPreExecute() {
			fadeOutAnimation(albumPreview, 500);
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			File currentDir = new File(Environment.
					getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) 
					+ File.separator + "Camera");
	 	    if(currentDir.canRead()) {
	 	    	//Make an array type File  with the list of all files of each folder
	 	    	imagesFiles = currentDir.listFiles(new FilenameFilter() {
	 	    		@Override
	 	    		public boolean accept(File dir, String name) {
	 	    			return FileExtensionFilter.isFileVideo(name);
	 	    		}
	 	    	});
	 	    	if (imagesFiles.length > 0) {
	 	    		Arrays.sort(imagesFiles, new Comparator<Object>()
	 	    				{
	 	    			public int compare(Object o1, Object o2) {
	 	    				if (((File)o1).lastModified() > ((File)o2).lastModified()) {
	 	    					return -1;
	 	    				} else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
	 	    					return +1;
	 	    				} else {
	 	    					return 0;
	 	    				}
	 	    			}
	 	    				});
	 	    	}
	 	    }// read card 
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			//Set Image
			if(imagesFiles != null && imagesFiles.length > 0) {
				Bitmap bmThumbnail;
				lastMediaUri = imagesFiles[0];
				// MINI_KIND: 512 x 384 thumbnail 
				bmThumbnail = PictureTools.cropBitmap(ThumbnailUtils.
						createVideoThumbnail(imagesFiles[0].
								getAbsolutePath(), Thumbnails.MINI_KIND), 
						albumPreview.getWidth(), 
						albumPreview.getHeight());
				albumPreview.setScaleType(ScaleType.CENTER_CROP);
				albumPreview.setImageBitmap(bmThumbnail);
				fadeInAnimation(albumPreview,500);
			}
			isPictureSelected = false;
			super.onPostExecute(result);
		}	
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		// TODO Auto-generated method stub
		
	}
	
	private void setVideoGpsExifData(String filePath) {
		if(gpsLocation != null) {
			final File videoFile = new File(filePath);
			if(videoFile.exists()) {
				MediaScannerConnection.scanFile(this, new String[]{videoFile.toString()}, 
						null, new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri contentUri) {
						StringBuffer location = new StringBuffer();
						location.append(gpsLocation.getLatitude());
						location.append(gpsLocation.getLongitude());
						ContentValues values = new ContentValues(1);
						
						values.put(MediaStore.Video.Media.LATITUDE, gpsLocation.getLatitude());
						values.put(MediaStore.Video.Media.LONGITUDE, gpsLocation.getLongitude());
						getContentResolver().update(contentUri, values, null, null);
					}
				});
			}
		}
	}
	
	private void setPictureGpsExifData(String filePath) {
		ExifInterface exif;
		if(gpsLocation != null) {
			double latitude = gpsLocation.getLatitude();
			double longitude = gpsLocation.getLongitude();
			try {
				exif = new ExifInterface(filePath);
				int num1Lat = (int)Math.floor(latitude);
				int num2Lat = (int)Math.floor((latitude - num1Lat) * 60);
				double num3Lat = (latitude - ((double)num1Lat+((double)num2Lat/60))) * 3600000;
				
				int num1Lon = (int)Math.floor(longitude);
				int num2Lon = (int)Math.floor((longitude - num1Lon) * 60);
				double num3Lon = (longitude - ((double)num1Lon+((double)num2Lon/60))) * 3600000;
				
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, num1Lat+"/1,"+num2Lat+"/1,"+num3Lat+"/1000");
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, num1Lon+"/1,"+num2Lon+"/1,"+num3Lon+"/1000");
				
				if (latitude > 0) {
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); 
				} else {
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
				}
				
				if (longitude > 0) {
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");    
				} else {
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
				}
				
				exif.saveAttributes();
				
			} catch (IOException e) {
				Log.e("PictureActivity", e.getLocalizedMessage());
			}   	
		}
	}
	
	private void getLocation()
	{
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		// Best provider
		String provider = locationManager.getBestProvider(criteria, true);
		
		
		// Getting Current Location
		Location location = locationManager.getLastKnownLocation(provider);
		
		locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				stopGpsAnimation = true;
				gpsIndicator.clearAnimation();
				gpsIndicatorLayout.setVisibility(View.GONE);
				gpsLocation = location;
				Log.i(TAG, "Location adquired");
			}
			public void onProviderDisabled(String provider){
				Log.i(TAG, "Provider OFF: " );
			}
			public void onProviderEnabled(String provider){
				Log.i(TAG, "Provider ON: " );
			}
			public void onStatusChanged(String provider, int status, Bundle extras){
				Log.i(TAG, "Provider Status: " + status);
			}
		};
		
		if(location!=null){
			gpsLocation = location;
		} else {
			try {
				//locationManager.requestLocationUpdates(provider, 120000, 200, this);
				// Constantly update the location
				gpsIndicatorLayout.setVisibility(View.VISIBLE);
				startGpsAnimation();
				locationManager.requestLocationUpdates(provider, TIMEUPDATE , DISTANCEUPDATE, locListener);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void touchFocus(final Rect tfocusRect) {		
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(tfocusRect, 1);
		focusList.add(focusArea);
		
		mAutoFocus = false;
		isFocused = false;
		
		Parameters para = mCamera.getParameters();
		para.setFocusAreas(focusList);
		para.setMeteringAreas(focusList);
		mCamera.setParameters(para);
		
		mCamera.autoFocus(touchAutofocus);
		
		drawingView.setVisibility(View.VISIBLE);
		drawingView.setHaveTouch(true, tfocusRect);
  		drawingView.invalidate();
    }
	
private class DrawingView extends View{
		
		boolean haveFace;
		Paint drawingPaint;
		
		boolean haveTouch;
		Rect touchArea;
		
		private int left, right, top, bottom;

		public DrawingView(Context context) {
			super(context);
			haveFace = false;
			drawingPaint = new Paint();
			drawingPaint.setColor(Color.GREEN);
			drawingPaint.setStyle(Paint.Style.STROKE); 
			drawingPaint.setStrokeWidth(2);
			
			haveTouch = false;
		}
		
		public void setHaveFace(boolean h){
			haveFace = h;
		}
		
		public void setHaveTouch(boolean t, Rect tArea){
			haveTouch = t;
			touchArea = tArea;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			
//			if(haveFace) {
//
//				// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
//				 // UI coordinates range from (0, 0) to (width, height).
//				 
//				 int vWidth = getWidth();
//				 int vHeight = getHeight();
//				
//				for(int i=0; i<detectedFaces.length; i++){
//					
//					if(i == 0){
//						drawingPaint.setColor(Color.GREEN);
//					}else{
//						drawingPaint.setColor(Color.RED);
//					}
//					
//					int l = detectedFaces[i].rect.left;
//					int t = detectedFaces[i].rect.top;
//					int r = detectedFaces[i].rect.right;
//					int b = detectedFaces[i].rect.bottom;
//					int left	= (l+1000) * vWidth/2000;
//					int top		= (t+1000) * vHeight/2000;
//					int right	= (r+1000) * vWidth/2000;
//					int bottom	= (b+1000) * vHeight/2000;
//					canvas.drawRect(
//							left, top, right, bottom,  
//							drawingPaint);
//				}
//			}else {
//				canvas.drawColor(Color.TRANSPARENT);
//			}
			if(haveTouch) {
				drawingPaint.setColor(Color.BLUE);
				canvas.drawRect(
						touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,  
						drawingPaint);
			}
		}
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized){
		    mLastX = x;
		    mLastY = y;
		    mLastZ = z;
		    mInitialized = true;
		}
		float deltaX  = Math.abs(mLastX - x);
		float deltaY = Math.abs(mLastY - y);
		float deltaZ = Math.abs(mLastZ - z);

		if (deltaX > .5 && mAutoFocus){ //AUTOFOCUS (while it is not autofocusing)
		    mAutoFocus = false;
		    isFocused = false;
		    mCamera.autoFocus(moveAutofocus);
		}
		if (deltaY > .5 && mAutoFocus){ //AUTOFOCUS (while it is not autofocusing)
		    mAutoFocus = false;
		    isFocused = false;
		    mCamera.autoFocus(moveAutofocus);
		}
		if (deltaZ > .5 && mAutoFocus){ //AUTOFOCUS (while it is not autofocusing)
		    mAutoFocus = false;
		    isFocused = false;
		    mCamera.autoFocus(moveAutofocus);
		}

		mLastX = x;
		mLastY = y;
		mLastZ = z;
		
	}
}
