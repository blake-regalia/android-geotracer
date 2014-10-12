package net.blurcast.tracer.activity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class SettingsOpenHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "general";
	private static final int DATABASE_VERSION = 1;
	
	private static final String KEY_NAME = "key";
	private static final String KEY_VALUE = "value";
	
	private static final String TABLE_NAME = "settings";
	
	private static final String SQL_CREATE_TABLE =
			"CREATE TABLE "+TABLE_NAME+" ("+
					KEY_NAME + " TEXT PRIMARY KEY, "+
					KEY_VALUE + " TEXT"+
					");";
	
	private static final String[] mColumns = {
		KEY_NAME,
		KEY_VALUE
	};
	
	public SettingsOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_TABLE);
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, ServiceControlActivity.DB_KEY_LOCATION_PROVIDER);
		values.putNull(KEY_VALUE);
		
		db.insert(TABLE_NAME, null, values);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	
	public String get(String keyName) {
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_NAME, mColumns, KEY_NAME+"=?", new String[]{keyName}, null, null, null, "1");
		
		if(cursor == null || !cursor.moveToFirst()) {
			return null;
		}
		
		int valueIndex = cursor.getColumnIndex(KEY_VALUE);
		return cursor.getString(valueIndex);
	}
		
	public boolean set(String name, String value) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, name);
		values.put(KEY_VALUE, value);
		
		db.update(TABLE_NAME, values, KEY_NAME+"=?", new String[]{name});
		db.close();
		
		return true;
	}
}
