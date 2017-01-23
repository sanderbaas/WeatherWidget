package nl.implode.weer;

import android.content.Context;

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
}
