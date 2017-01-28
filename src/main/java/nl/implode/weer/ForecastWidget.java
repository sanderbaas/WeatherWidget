package nl.implode.weer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ForecastWidgetConfigureActivity ForecastWidgetConfigureActivity}
 */
public class ForecastWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        ComponentName widgetComponentName = new ComponentName(context, ForecastWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName);
        CharSequence stationName = ForecastWidgetConfigureActivity.loadPref(context, "stationName", appWidgetId);
        CharSequence stationCountry = ForecastWidgetConfigureActivity.loadPref(context, "stationCountry", appWidgetId);
        String stationId = ForecastWidgetConfigureActivity.loadPref(context, "stationId", appWidgetId);
        Boolean useCelcius = true;

        WeatherStationsDatabase weatherStationsDatabase = new WeatherStationsDatabase(context);
        WeatherStation weatherStation = null;
        weatherStation = weatherStationsDatabase.findWeatherStation(Integer.valueOf(stationId));
        JSONObject forecast = weatherStation.get5DayForecast();
        Log.d("nl.implode.weer", forecast.toString());

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.forecast_widget);
        views.setTextViewText(R.id.stationName, stationName);
        views.setTextViewText(R.id.stationCountry, stationCountry);
        views.setTextViewText(R.id.stationId, stationId);

        JSONObject days = new JSONObject();
        //only update view when we have new forecast data, preventing empty results
        try {
            Log.d("nl.implode.weer",String.valueOf(forecast.getJSONArray("list").length()));
            //if (forecast.getString("cod") == "200" && forecast.getJSONArray("list").length() > 0) {
                Calendar cal = Calendar.getInstance();
                Log.d("nl.implode.weer",cal.getTimeZone().toString());
                Date updateTime = cal.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
                String lastUpdate = sdf.format(updateTime);

                views.setTextViewText(R.id.updateTime, "updated: " + lastUpdate);


                JSONArray list = forecast.getJSONArray("list");
                for (int i=0; i<list.length();i++) {
                    cal.setTimeInMillis(list.getJSONObject(i).getInt("dt")*1000L);
                    //String day = String.valueOf(cal.get(Calendar.DAY_OF_YEAR));
                    SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE d MMMM");
                    String day = sdfDate.format(cal.getTime());
                    if (!days.has(day)) {
                        days.put(day,new JSONArray());
                    }
                    days.getJSONArray(day).put(list.getJSONObject(i));
                }

                // remove old forecasts
                views.removeAllViews(R.id.widgetForecasts);
                Iterator<String> keys= days.keys();
                while (keys.hasNext()) {
                    String dayName = (String) keys.next();
                    JSONArray dayForecasts = days.getJSONArray(dayName);
                    // add tablerow with just day name
                    RemoteViews dayLineView = new RemoteViews(context.getPackageName(), R.layout.day);
                    dayLineView.setTextViewText(R.id.day, dayName);
                    views.addView(R.id.widgetForecasts, dayLineView);

                    //RemoteViews forecastLineView = new RemoteViews(context.getPackageName(), R.layout.forecastline);
                    for (int j=0; j<dayForecasts.length(); j++) {
                        Double temp = Double.valueOf(list.getJSONObject(j).getJSONObject("main").getString("temp"));
                        if (useCelcius) { temp = temp - 273.15; }
                        RemoteViews forecastView = new RemoteViews(context.getPackageName(), R.layout.forecast);
                        forecastView.setTextViewText(R.id.forecast_temp, String.valueOf(Math.round(temp)) +(char) 0x00B0);
                        if (temp < 1){
                            forecastView.setTextColor(R.id.forecast_temp, Color.BLUE);
                        }else{
                            forecastView.setTextColor(R.id.forecast_temp, Color.RED);
                        }
                        String rain = "0mm";
                        if (list.getJSONObject(j).has("rain") && list.getJSONObject(j).getJSONObject("rain").has("3h")) {
                            rain = list.getJSONObject(j).getJSONObject("rain").getString("3h") + "mm";
                        }
                        forecastView.setTextViewText(R.id.forecast_rain, rain);
                        views.addView(R.id.widgetForecasts, forecastView);
                    }
                    //views.addView(R.id.widgetForecasts, forecastLineView);
                }
        }catch(Exception e) {
            e.printStackTrace();
        } finally {

        }

        Intent intent = new Intent(context, ForecastWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.forecast_widget, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            ForecastWidgetConfigureActivity.deletePref(context, "stationName", appWidgetId);
            ForecastWidgetConfigureActivity.deletePref(context, "stationCountry", appWidgetId);
            ForecastWidgetConfigureActivity.deletePref(context, "stationId", appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

