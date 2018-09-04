package nl.implode.weer;

import android.appwidget.AppWidgetManager;
import android.content.Context;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link ForecastWidgetConfigureActivity ForecastWidgetConfigureActivity}
 */
public class ForecastWidgetDark extends ForecastWidget {
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        doAppWidgetUpdate(context, appWidgetManager, appWidgetId,
                ForecastWidgetDark.class, ForecastWidgetDarkService.class);
    }


}

