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

import java.math.BigDecimal;
import java.math.MathContext;
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
        Boolean useCelsius = true;
        Boolean useFahrenheit = false;

        JSONObject forecast = new JSONObject();
        WeatherStationsDatabase weatherStationsDatabase = new WeatherStationsDatabase(context);
        WeatherStation weatherStation = null;
        if (!stationId.isEmpty()) {
            weatherStation = weatherStationsDatabase.findWeatherStation(Integer.valueOf(stationId));
            forecast = weatherStation.get5DayForecast();
            Log.d("nl.implode.weer", forecast.toString());
        }

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.forecast_widget);
        views.setTextViewText(R.id.stationName, stationName);
        views.setTextViewText(R.id.stationCountry, stationCountry);
        //views.setTextViewText(R.id.stationId, stationId);

        JSONObject days = new JSONObject();
        //only update view when we have new forecast data, preventing empty results
        try {
            if (forecast.has("list") && forecast.getJSONArray("list").length() > 0) {
                Calendar cal = Calendar.getInstance();
                Date updateTime = cal.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMANY);
                String lastUpdate = sdf.format(updateTime);

                views.setTextViewText(R.id.updateTime, lastUpdate);


                JSONArray list = forecast.getJSONArray("list");
                String[] times = {
                        "0:00", "3:00","6:00","9:00","12:00","15:00","18:00","21:00"
                };
                Integer dayNum = 0;
                Integer timeNum = 0;
                for (int i = 0; i < list.length(); i++) {
                    cal.setTimeInMillis(list.getJSONObject(i).getInt("dt") * 1000L);
                    SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE d MMMM");
                    String day = sdfDate.format(cal.getTime());
                    if (!days.has(day)) {
                        dayNum++;
                        days.put(day, new JSONArray());
                    }
                    if (dayNum==2) {
                        // gather times of second day
                        SimpleDateFormat sdfTime = new SimpleDateFormat("H:mm");
                        String time = sdfTime.format(cal.getTime());
                        times[timeNum] = time;
                        timeNum++;
                    }
                    days.getJSONArray(day).put(list.getJSONObject(i));
                }

                // remove old forecasts
                views.removeAllViews(R.id.widgetForecasts);

                // add times
                String[] times = {
                        "0:00", "3:00","6:00","9:00","12:00","15:00","18:00","21:00"
                };

                for (int l=0; l<times.length; l++) {
                    RemoteViews timeView = new RemoteViews(context.getPackageName(), R.layout.times);
                    timeView.setTextViewText(R.id.time, times[l]);
                    views.addView(R.id.widgetForecasts, timeView);
                }

                Iterator<String> keys = days.keys();
                Integer maxDays = 5;
                Integer numDays = 0;
                while (keys.hasNext() && numDays < maxDays) {
                    numDays++;
                    String dayName = (String) keys.next();
                    JSONArray dayForecasts = days.getJSONArray(dayName);
                    // add tablerow with just day name
                    RemoteViews dayLineView = new RemoteViews(context.getPackageName(), R.layout.day);
                    dayLineView.setTextViewText(R.id.day, dayName);
                    views.addView(R.id.widgetForecasts, dayLineView);

                    //RemoteViews forecastLineView = new RemoteViews(context.getPackageName(), R.layout.forecastline);
                    for (int j = 0; j < dayForecasts.length(); j++) {
                        JSONObject dayForecast = dayForecasts.getJSONObject(j);
                        Double temp = Double.valueOf(dayForecast.getJSONObject("main").getString("temp"));
                        if (useCelsius) {
                            temp = temp - 273.15;
                        }
                        if (useFahrenheit) {
                            temp = 9/5*(temp - 273.15) + 32;
                        }

                        RemoteViews forecastView = new RemoteViews(context.getPackageName(), R.layout.forecast);
                        forecastView.setTextViewText(R.id.forecast_temp, String.valueOf(Math.round(temp)) + (char) 0x00B0);
                        if (temp < 1) {
                            forecastView.setTextColor(R.id.forecast_temp, Color.BLUE);
                        } else {
                            forecastView.setTextColor(R.id.forecast_temp, Color.RED);
                        }
                        String rain = "";
                        if (dayForecast.has("rain") && dayForecast.getJSONObject("rain").has("3h")) {
                            String sRain = dayForecast.getJSONObject("rain").getString("3h");
                            Float fRain = Float.valueOf(sRain);
                            String lessThan = "";
                            if (fRain < 0.1) {
                                lessThan = "<";
                                fRain = new Float(0.1);
                            }
                            rain = lessThan + String.format("%.1f", fRain) + " mm";
                        }
                        forecastView.setTextViewText(R.id.forecast_rain, rain);

                        if (dayForecast.has("weather") && dayForecast.getJSONArray("weather").length()>0) {
                            JSONArray weather = dayForecast.getJSONArray("weather");
                            if (weather.getJSONObject(0).has("icon")) {
                                String icon = "icon" + weather.getJSONObject(0).getString("icon");
                                forecastView.setImageViewResource(R.id.forecast_icon, context.getResources().getIdentifier(icon, "drawable", context.getPackageName()));
                            }
                        }

                        views.addView(R.id.widgetForecasts, forecastView);
                    }
                }
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

