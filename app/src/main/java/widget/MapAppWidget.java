package widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;

import com.das.gaztemap.R;

import java.io.File;

public class MapAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_map);
            File cacheDir = context.getCacheDir();
            File file = new File(cacheDir, "map_snapshot.png");
            Log.d("MapAppWidget", "Buscando archivo en: " + file.getAbsolutePath());
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    Log.d("MapAppWidget", "Bitmap cargado correctamente");
                    views.setImageViewBitmap(R.id.map_view, bitmap);
                } else {
                    Log.e("MapAppWidget", "Error al cargar el Bitmap desde el archivo");
                }
            } else {
                Log.e("MapAppWidget", "Archivo no encontrado en caché");
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}