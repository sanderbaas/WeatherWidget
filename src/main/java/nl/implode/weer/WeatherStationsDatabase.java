package nl.implode.weer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by sander on 23-1-17.
 */
public class WeatherStationsDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "cities.db";
    private static final int DATABASE_VERSION = 1;

    public WeatherStationsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public WeatherStation findWeatherStation(int id) {
        SQLiteDatabase db = super.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM cities WHERE _id="+id+";", null);
        WeatherStation weatherStation = null;
        try {
            while (cursor.moveToNext()) {
                weatherStation = new WeatherStation(
                    cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("country")),
                    cursor.getString(cursor.getColumnIndex("lat")),
                    cursor.getString(cursor.getColumnIndex("lon")
                ));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return weatherStation;
    }
}
