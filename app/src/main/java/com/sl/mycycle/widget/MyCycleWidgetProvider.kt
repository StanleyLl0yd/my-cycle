package com.sl.mycycle.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.sl.mycycle.R
import com.sl.mycycle.ui.MainActivity

class MyCycleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_log)
            views.setOnClickPendingIntent(R.id.widget_root, openTodayIntent(context))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun openTodayIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1301,
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("mycycle://log/today")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
