package com.das.gaztemap;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.util.Log;

import org.json.JSONObject;

public class Firebase extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        Log.d("FCM", "Notificación recibida: " + title + " - " + body);
    }

    @Override
    public void onNewToken(String token) {
        enviarTokenAlServidor(token);
    }

    private void enviarTokenAlServidor(String token) {
        String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/.php";
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject json = new JSONObject();
        try {
            json.put("token", token);
            json.put("email", "usuario@example.com");
        } catch (Exception e) {
            Log.e("Token", "Error al crear JSON: " + e.getMessage());
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                response -> Log.d("Token", "Token enviado correctamente"),
                error -> Log.e("Token", "Error al enviar token: " + error.getMessage())
        );
        queue.add(request);
    }
}