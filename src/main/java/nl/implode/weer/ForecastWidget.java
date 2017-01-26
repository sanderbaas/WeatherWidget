package nl.implode.weer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        CharSequence stationId = ForecastWidgetConfigureActivity.loadPref(context, "stationId", appWidgetId);

        Calendar cal = Calendar.getInstance();
        Date updateTime = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
        String lastUpdate = sdf.format(updateTime);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.forecast_widget);
        views.setTextViewText(R.id.stationName, stationName);
        views.setTextViewText(R.id.stationCountry, stationCountry);
        views.setTextViewText(R.id.stationId, stationId);
        views.setTextViewText(R.id.updateTime, "updated: " + lastUpdate);

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

