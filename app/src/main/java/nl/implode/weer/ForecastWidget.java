package nl.implode.weer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ForecastWidgetConfigureActivity ForecastWidgetConfigureActivity}
 */
public class ForecastWidget extends AppWidgetProvider {
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        doAppWidgetUpdate(context, appWidgetManager, appWidgetId,
                ForecastWidget.class, ForecastWidgetService.class);
        doAppWidgetUpdate(context, appWidgetManager, appWidgetId,
                ForecastWidgetDark.class, ForecastWidgetDarkService.class);
    }

    static void doAppWidgetUpdate(Context context, AppWidgetManager appWidgetManager,
                                  int appWidgetId, Class widgetClass, Class widgetServiceClass) {

        ComponentName thisWidget = new ComponentName(context, widgetClass);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(), widgetServiceClass);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        // Update the widgets via the service
        context.startService(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            Bundle options=appWidgetManager.getAppWidgetOptions(appWidgetId);
            onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, options);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
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

