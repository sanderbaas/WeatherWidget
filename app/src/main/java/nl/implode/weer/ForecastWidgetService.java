package nl.implode.weer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import com.skedgo.converter.TimezoneMapper;

public class ForecastWidgetService extends JobIntentService {

    public static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ForecastWidgetService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        handleStart(intent);
    }

    private Context gContext;
    private AppWidgetManager appWidgetManager;

    protected boolean dark;

    private int getLayout(String id) {
        switch(id){
            case "forecast_widget":
                return dark ? R.layout.forecast_widget_dark : R.layout.forecast_widget;
            case "day":
                return dark ? R.layout.day_dark : R.layout.day;
            case "forecast":
                return dark ? R.layout.forecast_dark : R.layout.forecast;
            case "times":
                return dark ? R.layout.times_dark : R.layout.times;
            case "color_temp":
                return dark ? 0xFFe91e63 : Color.RED;
            case "color_temp_freezing":
                return dark ? 0xFF7dc6bf : 0xFF3F51B5;
        }
        return 0;
    }

    private String getIconImage(String iconCode) {
        switch(iconCode) {
            case "01d": return "icon_32";
            case "02d": return "icon_30";
            case "03d": return "icon_28";
            case "04d": return "icon_26";
            case "09d": return "icon_12";
            case "10d": return "icon_39";
            case "11d": return "icon_37";
            case "13d": return "icon_41";
            case "50d": return "icon_20";

            case "01n": return "icon_31";
            case "02n": return "icon_33";
            case "03n": return "icon_27";
            case "04n": return "icon_26";
            case "09n": return "icon_12";
            case "10n": return "icon_45";
            case "11n": return "icon_47";
            case "13n": return "icon_46";
            case "50n": return "icon_20";

            case "r":  return "icon_12";
            case "sn50": return "icon_14";
            case "t50":  return "icon_35";
            case "w50":  return "icon_23";
        }
        return "";
    }

    private void processForecasts(JSONObject forecast, Integer widgetId) {
        int maxDays = 4;
        CharSequence widgetStyle = ForecastWidgetConfigureActivity.loadPref(gContext, "widgetStyle", widgetId);
        dark = widgetStyle.equals("dark");
        RemoteViews views = new RemoteViews(gContext.getPackageName(), getLayout("forecast_widget"));
        CharSequence stationName = ForecastWidgetConfigureActivity.loadPref(gContext, "stationName", widgetId);
        CharSequence stationCountry = ForecastWidgetConfigureActivity.loadPref(gContext, "stationCountry", widgetId);

        Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            // portrait: minWidth x maxHeight
            maxDays = (int) Math.floor((maxHeight-55)/85);
        }

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape: maxWidth x minHeight
            maxDays = (int) Math.floor((minHeight-55)/85);
        }

        views.setTextViewText(R.id.stationName, stationName);
        views.setTextViewText(R.id.stationCountry, stationCountry);

        JSONObject days = new JSONObject();
        // rainScale (0 mm, 1 inch)
        // tempScale (0 Celsius, 1 Fahrenheit, 2 Kelvin)
        // time (0 24h, 1 am/pm)
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        boolean useLocalTime = sharedPrefs.getBoolean("use_local_time", false);
        String prefTime = sharedPrefs.getString("time", "0");
        String prefTempScale = sharedPrefs.getString("temp_scale", "0");
        String prefRainScale = sharedPrefs.getString("rain_scale", "0");
        String timeZone = "";

        try {
            if (forecast.has("latitude")) {
                double lat = forecast.getDouble("latitude");
                double lon = forecast.getDouble("longitude");
                timeZone = TimezoneMapper.latLngToTimezoneString(lat, lon);
            }

            //only update view when we have new forecast data, preventing empty results
            if (forecast.has("hourly") && forecast.getJSONObject("hourly").getJSONArray("time").length() > 0) {
                Calendar cal = Calendar.getInstance();
                Date updateTime = cal.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                if (prefTime.equals("1")) {
                    sdf = new SimpleDateFormat("KK:mm a", Locale.getDefault());
                }
                String lastUpdate = sdf.format(updateTime);

                views.setTextViewText(R.id.updateTime, lastUpdate);

                JSONArray listIsDay = forecast.getJSONObject("hourly").getJSONArray("is_day");
                JSONArray listTimes = forecast.getJSONObject("hourly").getJSONArray("time");
                JSONArray listTemperatures = forecast.getJSONObject("hourly").getJSONArray("temperature_2m");
                JSONArray listPrecipitations = forecast.getJSONObject("hourly").getJSONArray("precipitation");
                JSONArray listRain = forecast.getJSONObject("hourly").getJSONArray("rain");
                JSONArray listSnow = forecast.getJSONObject("hourly").getJSONArray("snowfall");
                JSONArray listWeatherCodes = forecast.getJSONObject("hourly").getJSONArray("weather_code");
                JSONArray listWindSpeeds = forecast.getJSONObject("hourly").getJSONArray("wind_speed_10m");
                JSONArray listWindDirections = forecast.getJSONObject("hourly").getJSONArray("wind_direction_10m");
                String[] times = {
                        "0:00", "3:00","6:00","9:00","12:00","15:00","18:00","21:00",
                };
                int dayNum = 0;
                int timeNum = 0;

                for (int i = 1; i < listTimes.length(); i = i + 3) {
                    SimpleDateFormat sdfListTime = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm",
                            Locale.getDefault()
                    );
                    cal.setTime(
                            sdfListTime.parse(listTimes.getString(i))
                    );

                    SimpleDateFormat sdfDate = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    );

                    if (useLocalTime) {
                        sdfDate.setTimeZone(TimeZone.getTimeZone(timeZone));
                    }

                    String day = sdfDate.format(cal.getTime());
                    if (!days.has(day)) {
                        dayNum++;
                        days.put(day, new JSONArray());
                    }
                    if (dayNum == 2) {
                        // gather times of second day
                        SimpleDateFormat sdfTime = new SimpleDateFormat("H:mm", Locale.getDefault());
                        if (prefTime.equals("1")) {
                            sdfTime = new SimpleDateFormat("K:mma", Locale.getDefault());
                        }
                        if (useLocalTime) {
                            sdfTime.setTimeZone(TimeZone.getTimeZone(timeZone));
                        }
                        String time = sdfTime.format(cal.getTime());
                        time = time.replace("a.m.","").replace("p.m.","p");
                        time = time.replace("AM","").replace("PM","p");
                        time = time.replace("A.M.","").replace("P.M.","p");
                        times[timeNum] = time;
                        timeNum++;
                    }

                    JSONObject forecastObject = new JSONObject();

                    forecastObject.put("is_day", listIsDay.get(i));

                    double[] temperatures = {
                            listTemperatures.getDouble(i-1),
                            listTemperatures.getDouble(i),
                            listTemperatures.getDouble(i+1)
                    };

                    forecastObject.put("temp", Arrays.stream(temperatures).average().getAsDouble());

                    double[] windSpeeds = {
                            listWindSpeeds.getDouble(i-1),
                            listWindSpeeds.getDouble(i),
                            listWindSpeeds.getDouble(i+1)
                    };

                    forecastObject.put("wind_speed", Arrays.stream(windSpeeds).max().getAsDouble());

                    forecastObject.put("wind_direction", listWindDirections.get(i));

                    double[] precipitations = {
                            listPrecipitations.getDouble(i-1),
                            listPrecipitations.getDouble(i),
                            listPrecipitations.getDouble(i+1)
                    };

                    forecastObject.put("precipitation", Arrays.stream(precipitations).sum());

//                    forecastObject.put("rain", listRain.get(i));

                    double[] snows = {
                            listSnow.getDouble(i-1),
                            listSnow.getDouble(i),
                            listSnow.getDouble(i+1)
                    };

                    forecastObject.put("snow", Arrays.stream(snows).sum());

                    double[] weatherCodes = {
                            listWeatherCodes.getDouble(i-1),
                            listWeatherCodes.getDouble(i),
                            listWeatherCodes.getDouble(i+1)
                    };

                    forecastObject.put("weather_code", Arrays.stream(weatherCodes).max().getAsDouble());

                    days.getJSONArray(day).put(forecastObject);
                }

                // remove old forecasts
                views.removeAllViews(R.id.widgetForecasts);

                // add times
                for (String time : times) {
                    RemoteViews timeView = new RemoteViews(gContext.getPackageName(), getLayout("times"));
                    timeView.setTextViewText(R.id.time, time);
                    views.addView(R.id.widgetForecasts, timeView);
                }

                Iterator<String> keys = days.keys();
                int numDays = 0;
                while (keys.hasNext() && numDays < maxDays) {
                    numDays++;
                    String dayName = (String) keys.next();
                    SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
                    SimpleDateFormat keyDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    String dayLabel = sdfDate.format(keyDate.parse(dayName));
                    JSONArray dayForecasts = days.getJSONArray(dayName);
                    // add tablerow with just day name
                    RemoteViews dayLineView = new RemoteViews(gContext.getPackageName(), getLayout("day"));
                    dayLineView.setTextViewText(R.id.day, dayLabel);
                    views.addView(R.id.widgetForecasts, dayLineView);

                    if (numDays < 2) {
                        for (int n = 0; n < (8 - dayForecasts.length()); n++) {
                            // add empty forecast
                            RemoteViews forecastView = new RemoteViews(gContext.getPackageName(), getLayout("forecast"));
                            views.addView(R.id.widgetForecasts, forecastView);
                        }
                    }

                    for (int j = 0; j < dayForecasts.length(); j++) {
                        JSONObject dayForecast = dayForecasts.getJSONObject(j);
                        double temp = Double.valueOf(dayForecast.getDouble("temp"));
                        boolean isFreezing = temp < 0;

                        String tempSuffix = "" + (char) 0x00B0;

                        if (prefTempScale.equals("1")) {
                            // fahrenheit
                            double factor = (double)9/(double)5;
                            temp = factor * (temp) + 32;
                        }
                        if (prefTempScale.equals("2")) {
                            // kelvin
                            temp = temp + 273.15;
                            tempSuffix = "" + (char) 0x004B;
                        }

                        int tempColor = getLayout(isFreezing ? "color_temp_freezing" : "color_temp");

                        RemoteViews forecastView = new RemoteViews(gContext.getPackageName(), getLayout("forecast"));
                        forecastView.setTextViewText(R.id.forecast_temp, String.valueOf(Math.round(temp)) + tempSuffix);

                        forecastView.setTextColor(R.id.forecast_temp, tempColor);
                        String rain = "";

                        if (dayForecast.has("precipitation") && dayForecast.getDouble("precipitation") > 0) {
                            double dRain = dayForecast.getDouble("precipitation");
                            String postFix = " ㎜";
                            if (prefRainScale.equals("1")) {
                                // inches
                                postFix = "\"";
                                dRain = dRain / 2.54;
                            }
                            String lessThan = "";
                            if (dRain < 0.1) {
                                lessThan = "<";
                                dRain = 0.1;
                            }
                            rain = lessThan + String.format(Locale.getDefault(), "%.1f", dRain) + postFix;
                        }

                        if (dayForecast.has("snow") && dayForecast.getDouble("snow") > 0) {
                            double dSnow = dayForecast.getDouble("snow");
                            String postFix = " cm";
                            if (prefRainScale.equals("1")) {
                                // inches
                                postFix = "\"";
                                dSnow = dSnow / 2.54;
                            }
                            String lessThan = "";
                            if (dSnow < 1) {
                                lessThan = "<";
                                dSnow = 1;
                            }
                            rain = lessThan + String.format(Locale.getDefault(), "%.1f", dSnow) + postFix;
                        }
                        forecastView.setTextViewText(R.id.forecast_rain, rain);

                        String wind = "";
                        if (dayForecast.has("wind_speed") && dayForecast.has("wind_direction")) {
                            double speed = dayForecast.getDouble("wind_speed");
                            double deg = dayForecast.getDouble("wind_direction");
                            String bft = "0";
                            if (speed >= 0.3 && speed < 1.6) { bft = "1"; }
                            if (speed >= 1.6 && speed < 3.4) { bft = "2"; }
                            if (speed >= 3.4 && speed < 5.5) { bft = "3"; }
                            if (speed >= 5.5 && speed < 8.0) { bft = "4"; }
                            if (speed >= 8.0 && speed < 10.8) { bft = "5"; }
                            if (speed >= 10.8 && speed < 13.9) { bft = "6"; }
                            if (speed >= 13.9 && speed < 17.2) { bft = "7"; }
                            if (speed >= 17.2 && speed < 20.8) { bft = "8"; }
                            if (speed >= 20.8 && speed < 24.5) { bft = "9"; }
                            if (speed >= 24.5 && speed < 28.5) { bft = "10"; }
                            if (speed >= 28.5 && speed < 32.7) { bft = "11"; }
                            if (speed >= 32.7) { bft = "12"; }

                            String dir = "wind_n";
                            if (deg >= 22.5 && deg < 67.5) { dir = "wind_ne"; }
                            if (deg >= 67.5 && deg < 112.5) { dir = "wind_e"; }
                            if (deg >= 112.5 && deg < 157.5) { dir = "wind_se"; }
                            if (deg >= 157.5 && deg < 202.5) { dir = "wind_s"; }
                            if (deg >= 202.5 && deg < 247.5) { dir = "wind_sw"; }
                            if (deg >= 247.5 && deg < 292.5) { dir = "wind_w"; }
                            if (deg >= 292.5 && deg < 337.5) { dir = "wind_nw"; }

                            wind = (char) 0x2332 + " " + gContext.getResources().getString(gContext.getResources().getIdentifier(dir, "string", gContext.getPackageName())) + " " + bft;

                        }
                        forecastView.setTextViewText(R.id.forecast_wind, wind);

                        if (dayForecast.has("weather_code") && dayForecast.has("is_day")) {
                            double weatherCode = dayForecast.getDouble("weather_code");
                            boolean isDay = dayForecast.getDouble("is_day") > 0;
                            String iconCode = "";

                            if (weatherCode == 0) { iconCode = "01"; }
                            if (weatherCode == 1) { iconCode = "01"; }
                            if (weatherCode == 2) { iconCode = "02"; }
                            if (weatherCode == 3) { iconCode = "03"; }
                            if (weatherCode == 45) { iconCode = "50"; }
                            if (weatherCode == 48) { iconCode = "50"; }
                            if (weatherCode == 51) { iconCode = "09"; }
                            if (weatherCode == 53) { iconCode = "09"; }
                            if (weatherCode == 55) { iconCode = "09"; }
                            if (weatherCode == 56) { iconCode = "09"; }
                            if (weatherCode == 57) { iconCode = "09"; }
                            if (weatherCode == 61) { iconCode = "10"; }
                            if (weatherCode == 63) { iconCode = "10"; }
                            if (weatherCode == 65) { iconCode = "10"; }
                            if (weatherCode == 66) { iconCode = "10"; }
                            if (weatherCode == 67) { iconCode = "10"; }
                            if (weatherCode == 71) { iconCode = "13"; }
                            if (weatherCode == 73) { iconCode = "13"; }
                            if (weatherCode == 75) { iconCode = "13"; }
                            if (weatherCode == 77) { iconCode = "13"; }
                            if (weatherCode == 80) { iconCode = "09"; }
                            if (weatherCode == 81) { iconCode = "09"; }
                            if (weatherCode == 82) { iconCode = "09"; }
                            if (weatherCode == 85) { iconCode = "13"; }
                            if (weatherCode == 86) { iconCode = "13"; }
                            if (weatherCode == 95) { iconCode = "11"; }
                            if (weatherCode == 96) { iconCode = "11"; }
                            if (weatherCode == 99) { iconCode = "11"; }

                            iconCode = iconCode + (isDay ? "d" : "n");

                            String icon = getIconImage(iconCode);
                            forecastView.setImageViewResource(R.id.forecast_icon, gContext.getResources().getIdentifier(icon, "drawable", gContext.getPackageName()));
                        }

                        views.addView(R.id.widgetForecasts, forecastView);
                    }

                    if (numDays > 1) {
                        for (int n = 0; n < (8 - dayForecasts.length()); n++) {
                            // add empty forecast
                            RemoteViews forecastView = new RemoteViews(gContext.getPackageName(), getLayout("forecast"));
                            views.addView(R.id.widgetForecasts, forecastView);
                        }
                    }
                }
            }

            Intent clickIntent = new Intent(gContext, ForecastWidget.class);
            clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            int[] appWidgetIds = new int[] {widgetId};
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(gContext,
                    widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetForecasts, pendingIntent);

            Class configurationClass = ForecastWidgetConfigureActivity.class;
            if (dark) {
                configurationClass = ForecastWidgetDarkConfigureActivity.class;
            }
            Intent configurationIntent = new Intent(gContext, configurationClass);

            // Create a extra giving the App Widget Id
            configurationIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

            // Create a pending intent giving configurationIntent as parameter
            PendingIntent configurationPendingIntent = PendingIntent.getActivity(gContext,
                    widgetId, configurationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // Here we fecth the layout item and give it a action

            // Setting onClick event that will lauch ConfigurationActivity
            views.setOnClickPendingIntent(R.id.settings, configurationPendingIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(widgetId, views);
        }catch(Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void handleStart(Intent intent) {
        appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());

        int[] allWidgetIds = intent
                .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        gContext = this.getApplicationContext();

        for (final int widgetId : allWidgetIds) {
            final String stationId = ForecastWidgetConfigureActivity.loadPref(gContext, "stationId", widgetId);
            new Thread() {
                public void run() {
                    Looper.prepare();
                    JSONObject forecast = new JSONObject();
                    WeatherStationsDatabase weatherStationsDatabase = new WeatherStationsDatabase(gContext);
                    WeatherStation weatherStation = null;
                    if (!stationId.isEmpty()) {
                        weatherStation = weatherStationsDatabase.findWeatherStation(Integer.valueOf(stationId));
                        forecast = weatherStation.get5DayForecast();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("forecast", forecast.toString());
                    bundle.putInt("widgetId", widgetId);
                    Message msg = new Message();
                    msg.setData(bundle);

                    Handler mHandler = new Handler(new Handler.Callback() {

                        @Override
                        public boolean handleMessage(Message msg) {
                            Bundle bundle = msg.getData();
                            String jsonForecast = bundle.getString("forecast");
                            Integer mWidgetId = bundle.getInt("widgetId");
                            JSONObject forecast;
                            try {
                                forecast = new JSONObject(jsonForecast);
                                processForecasts(forecast, mWidgetId);
                            }catch(Exception e) {
                                Log.d("weer", e.getMessage());
                            }
                            return false;
                        }
                    });
                    mHandler.sendMessage(msg);
                    Looper.loop();
                }
            }.start();
        }
        stopSelf();
    }
}
