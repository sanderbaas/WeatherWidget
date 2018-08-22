package nl.implode.weer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ForecastWidgetConfigureActivity ForecastWidgetConfigureActivity}
 */
public class ForecastWidgetDark extends ForecastWidget {
    public ForecastWidgetDark(){
        Log.d("weather","dark widget contrstur");
        widgetClass = ForecastWidgetDark.class;
        widgetServiceClass = ForecastWidgetDarkService.class;
    }
}

