package widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.das.gaztemap.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardAppWidget extends AppWidgetProvider {

    private static final String API_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/leaderboard.php";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_leaderboard);
        Log.d("LeaderboardWidget", "updateWidget called for appWidgetId: " + appWidgetId);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("LeaderboardWidget", "API Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getString("status").equals("success")) {
                            JSONArray usersArray = jsonResponse.getJSONArray("users");
                            StringBuilder leaderboardText = new StringBuilder();

                            for (int i = 0; i < Math.min(5, usersArray.length()); i++) {
                                JSONObject user = usersArray.getJSONObject(i);
                                String name = user.getString("nombre");
                                int points = user.getInt("puntos");
                                leaderboardText.append(i + 1).append(". ").append(name).append(" - ").append(points).append(" pts\n");
                            }

                            Log.d("LeaderboardWidget", "Leaderboard text: " + leaderboardText.toString());
                            views.setTextViewText(R.id.widget_title, context.getString(R.string.leaderboard_top_5));
                            views.setTextViewText(R.id.leaderboard_container, leaderboardText.toString());
                        } else {
                            Log.e("LeaderboardWidget", "API returned error status");
                            views.setTextViewText(R.id.leaderboard_container, context.getString(R.string.error_loading_leaderboard));
                        }
                    } catch (JSONException e) {
                        Log.e("LeaderboardWidget", "Error parsing JSON: " + e.getMessage());
                        views.setTextViewText(R.id.leaderboard_container, context.getString(R.string.error_loading_leaderboard));
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                },
                error -> {
                    Log.e("LeaderboardWidget", "Network error: " + error.toString());
                    views.setTextViewText(R.id.leaderboard_container, context.getString(R.string.error_loading_leaderboard));
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "getLeaderboard");
                params.put("type", "global"); // Cambiar a "friends" si es necesario
                return params;
            }
        };

        requestQueue.add(request);
    }
}