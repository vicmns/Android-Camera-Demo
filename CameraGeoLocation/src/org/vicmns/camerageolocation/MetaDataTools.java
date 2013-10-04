package org.vicmns.camerageolocation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;

public class MetaDataTools {
	
	public static boolean SetGeotagData(Context context, Location location, Uri contentUri) {
		ContentValues values = new ContentValues(1);
		
		values.put(MediaStore.Video.Media.LATITUDE, location.getLatitude());
		values.put(MediaStore.Video.Media.LONGITUDE, location.getLongitude());
		try {
			if (context.getContentResolver().update(contentUri, values, null, null) == 1) {
				return true;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String[] ReadGeotagData(Context context, Uri contentUri) {
		String[] latLong = new String[2];
		try {
			Cursor cursor = context.getContentResolver().query(contentUri, 
					new String[] { MediaStore.Video.Media.LATITUDE, 
					MediaStore.Video.Media.LONGITUDE}, null, null, null);
			cursor.moveToFirst();
			latLong[0] = cursor.getString(0);
			latLong[1] = cursor.getString(1);
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return latLong;
	}

}
