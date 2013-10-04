package org.vicmns.camerageolocation;

import java.io.IOException;
import java.util.List;


import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	
	private static final String TAG = "CameraPreview";
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Display display;
	private List<Size> sizes;
	private Parameters parameters;
	private Size optimalSize;
	private static final double WIDE_ASPECT_RATIO = 16.0 / 9.0;
	
	public CameraPreview(Context context, Camera camera, Display display) {
	    super(context);
	    this.mCamera = camera;
	    this.display = display;
	
	    // Install a SurfaceHolder.Callback so we get notified when the
	    // underlying surface is created and destroyed.
	    mHolder = getHolder();
	    mHolder.addCallback(this);
	    // deprecated setting, but required on Android versions prior to 3.0
	    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	public Surface getSufaceHolderHolder() {
		return this.mHolder.getSurface();
	}
	
	private void setupCamera() {
		parameters = mCamera.getParameters();
		sizes = parameters.getSupportedPreviewSizes();
		optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, 
				getResources().getDisplayMetrics().heightPixels);
		parameters.setPreviewSize(optimalSize.width, optimalSize.height);
		Size pictureSize = getOptimalPictureSize(parameters.getSupportedPictureSizes());
		parameters.setPictureSize(pictureSize.width, pictureSize.height);
		switch (display.getRotation()) {
		case Surface.ROTATION_0:
			mCamera.setDisplayOrientation(90);
			break;
		case Surface.ROTATION_90:
			mCamera.setDisplayOrientation(0);
			break;
		case Surface.ROTATION_180:
			mCamera.setDisplayOrientation(270);
			break;
		case Surface.ROTATION_270:
			 mCamera.setDisplayOrientation(180);
			break;
		default:
			break;
		}
		mCamera.setParameters(parameters);
	}
	
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
	    final double ASPECT_TOLERANCE = 0.05;
	    double targetRatio = (double) w/h;

	    if (sizes==null) return null;

	    Size optimalSize = null;

	    double minDiff = Double.MAX_VALUE;

	    // Find size
	    for (Size size : sizes) {
	        double ratio = (double) size.width / size.height;
	        if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
	        if (Math.abs(size.width - w) < minDiff) {
	            optimalSize = size;
	            minDiff = Math.abs(size.width - w);
	        }
	    }
	    
	    Log.i(TAG, "Optimizal size, Width: " + optimalSize.width + " , Height: " + optimalSize.height);
	    
	    if (optimalSize == null) {
	        minDiff = Double.MAX_VALUE;
	        for (Size size : sizes) {
	            if (Math.abs(size.width - w) < minDiff) {
	                optimalSize = size;
	                minDiff = Math.abs(size.width - w);
	            }
	        }
	    }
	    return optimalSize;
	}
	
	private Size getOptimalPictureSize(List<Size> sizes) {
		 final double ASPECT_TOLERANCE = 0.05;
		 
		 
		 if (sizes==null) return null;
		 
		 Size optimalSize = null;
		 
		 for (Size size : sizes) {
			 double ratio = (double) size.width / size.height;
			 if (Math.abs(ratio - WIDE_ASPECT_RATIO) > ASPECT_TOLERANCE) continue;
			 else {
				 optimalSize = size;
				 break;
			 }
		 }
		 if (optimalSize == null) {
			 return sizes.get(0);
		 }
		 
		 return optimalSize;
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
	    // The Surface has been created, now tell the camera where to draw the preview.
	    try {
	        mCamera.setPreviewDisplay(holder);
	        mCamera.startPreview();
	    } catch (IOException e) {
	        Log.d(TAG, "Error setting camera preview: " + e.getMessage());
	    }
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(mCamera != null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;			
		}
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	    // If your preview can change or rotate, take care of those events here.
	    // Make sure to stop the preview before resizing or reformatting it.
	
	    if (holder.getSurface() == null){
	      // preview surface does not exist
	      return;
	    }
	
	    // stop preview before making changes
	    try {
	        mCamera.stopPreview();
	    } catch (Exception e){
	      // ignore: tried to stop a non-existent preview
	    }
	
	    // set preview size and make any resize, rotate or
	    // reformatting changes here
	
	    // start preview with new settings
	    try {
	    	setupCamera();
	        mCamera.setPreviewDisplay(holder);
	        mCamera.startPreview();
	        //surfaceViewListeners.surfcaePainted(optimalSize);
	    } catch (Exception e){
	        Log.d(TAG, "Error starting camera preview: " + e.getMessage());
	    }
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        
        if (width > height) {
        	height = (int) Math.ceil((double)width / WIDE_ASPECT_RATIO);
    	} else if(width <= height) {
    		height = (int) Math.ceil((double)width * WIDE_ASPECT_RATIO);
    	} 
        setMeasuredDimension(width, height);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), 
        		MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
    }
}