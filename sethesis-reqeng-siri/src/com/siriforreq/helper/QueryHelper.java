package com.siriforreq.helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class QueryHelper {
	public static String queryForColumn(Context c, Uri uri, String type){
		 String scheme = uri.getScheme();
		 if (scheme.equals("file")) {
		     return uri.getLastPathSegment();
		 }
		 else if (scheme.equals("content")) {
			 String[] queryProjectionTitle = {type}; 
		     Cursor cursorTitle = c.getContentResolver().query(uri, queryProjectionTitle, null, null, null);
		     if (cursorTitle != null && cursorTitle.getCount() != 0) {
		         int columnIndex = cursorTitle.getColumnIndexOrThrow(type);
		         cursorTitle.moveToFirst();
		         return cursorTitle.getString(columnIndex);
		     }	
		 }
		return "";
	}
}
