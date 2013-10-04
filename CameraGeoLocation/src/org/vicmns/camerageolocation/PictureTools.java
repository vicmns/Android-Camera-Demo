package org.vicmns.camerageolocation;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

public class PictureTools {
	
	private static final String TAG = "PictureTools";

	private static int getCameraPhotoOrientation(Context context, Uri imageUri, String imagePath){
		int rotate = 0;
		try {
			context.getContentResolver().notifyChange(imageUri, null);
			File imageFile = new File(imagePath);
			ExifInterface exif = new ExifInterface(
					imageFile.getAbsolutePath());
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			}
			
			Log.v(TAG, "Exif orientation: " + orientation);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rotate;
	}
	
	public static Bitmap decodeSampledBitmapFromUri(Context context, String dir, int Width, int Height) 
	{   
		Bitmap rotatedBitmap = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(dir, options);
			
			options.inSampleSize = calculateInSampleSize(options, Width, Height);
			options.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeFile(dir, options);
			Uri pictureUri = Uri.parse(dir);
			Matrix matrix = new Matrix();
			matrix.postRotate(PictureTools.getCameraPhotoOrientation(context, pictureUri, dir));
			rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		} catch(Exception e) {
			return null;
		}    
		return rotatedBitmap;   
	}
	
	private static int calculateInSampleSize( BitmapFactory.Options options, int Width, int Height) 
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int size_inicialize = 1;
		
		if (height > Height || width > Width) 
		{
			if (width > height) 
			{		
				size_inicialize = Math.round((float)height / (float)Height);    
			} 
			else 
			{
				size_inicialize = Math.round((float)width / (float)Width);    
			}   
		}
		return size_inicialize;    
	}
	
	public static Bitmap decodeAndCropBitmapFromUri(Context context, String dir, int Width, int Height) {
		Bitmap srcBmp = decodeSampledBitmapFromUri(context, dir, Width, Height);
		Bitmap dstBmp = null;
		if (srcBmp.getWidth() >= srcBmp.getHeight()) {
			dstBmp = Bitmap.createBitmap(
					srcBmp, 
					srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
					0,
					srcBmp.getHeight(), 
					srcBmp.getHeight()
					);
			
		} else {
			dstBmp = Bitmap.createBitmap(
					srcBmp,
					0, 
					srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
					srcBmp.getWidth(),
					srcBmp.getWidth() 
					);
		}
		return dstBmp;
	}
	
	public static Bitmap cropBitmap(Bitmap srcBmp, int Width, int Height) {
		Bitmap dstBmp = null;
		if (srcBmp.getWidth() >= srcBmp.getHeight()) {
			dstBmp = Bitmap.createBitmap(
					srcBmp, 
					srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
					0,
					srcBmp.getHeight(), 
					srcBmp.getHeight()
					);
			
		} else {
			dstBmp = Bitmap.createBitmap(
					srcBmp,
					0, 
					srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
					srcBmp.getWidth(),
					srcBmp.getWidth() 
					);
		}
		return dstBmp;
	}
}
