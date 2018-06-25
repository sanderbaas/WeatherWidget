package nl.implode.weer;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.BoolRes;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * The configuration screen for the {@link ForecastWidget ForecastWidget} AppWidget.
 */
public class ForecastWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "nl.implode.weer.ForecastWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    DelayAutoCompleteTextView mAppWidgetLocation;
    TextView mStationId;
    String stationId;
    String stationName;
    String stationCountry;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = ForecastWidgetConfigureActivity.this;
            if (stationName != null && stationId != null) {

                // When the button is clicked, store the settings locally
                savePref(context, "stationName", mAppWidgetId, stationName);
                savePref(context, "stationCountry", mAppWidgetId, stationCountry);
                savePref(context, "stationId", mAppWidgetId, stationId);

                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ForecastWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
                return;
            }
            new AlertDialog.Builder(context)
                    .setTitle("No station selected")
                    .setMessage("Please select a station from the list before adding widget")
                    .setPositiveButton("OK", null)
                    .show();
        }
    };

    public ForecastWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void savePref(Context context, String tag, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + tag + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadPref(Context context, String tag, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String value = prefs.getString(PREF_PREFIX_KEY + tag + appWidgetId, null);
        if (value != null) {
            return value;
        } else {
            return "";
        }
    }

    static void deletePref(Context context, String tag, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + tag + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.forecast_widget_configure);
        final DelayAutoCompleteTextView mAppWidgetLocation = (DelayAutoCompleteTextView) findViewById(R.id.appwidget_location);
        mAppWidgetLocation.setThreshold(3);
        mAppWidgetLocation.setAdapter(new WeatherStationAutoCompleteAdapter(this)); // 'this' is Activity instance
        mAppWidgetLocation.setLoadingIndicator(
                (android.widget.ProgressBar) findViewById(R.id.pb_loading_indicator));
        mAppWidgetLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                WeatherStation weatherStation = (WeatherStation) adapterView.getItemAtPosition(position);
                mAppWidgetLocation.getText().clear();
                mAppWidgetLocation.append(weatherStation.name + ", " + weatherStation.country);
                stationName = weatherStation.name;
                stationCountry = weatherStation.country;
                stationId = String.valueOf(weatherStation._id);
            }
        });

        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        String defaultName = loadPref(ForecastWidgetConfigureActivity.this, "stationName", mAppWidgetId);
        String defaultCountry =loadPref(ForecastWidgetConfigureActivity.this, "stationCountry", mAppWidgetId);
        if (defaultName != "" && defaultCountry != "") {
            mAppWidgetLocation.setText(defaultName + ", " + defaultCountry);
        }
    }
}

