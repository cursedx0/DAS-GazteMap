package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SubscriptionsPrefs";
    private static final String SUBSCRIPTION_KEY_PREFIX = "subscribed_to_";

    private RecyclerView recyclerViewComments;
    private TextInputEditText editTextComment;
    private FloatingActionButton fabSendComment;
    private FloatingActionButton fabSubscribe;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    private int currentUserId;
    private String postId;
    private FirebaseMessaging firebaseMessaging;
    private boolean isSubscribed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comentarios);

        postId = getIntent().getStringExtra("POST_ID");
        firebaseMessaging = FirebaseMessaging.getInstance();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentUserId = prefs.getInt("id", 0);

        inicializarVistas();
        cargarComentarios();
        configurarBotonNotificaciones();
    }

    private void inicializarVistas() {
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editTextComment = findViewById(R.id.editTextComment);
        fabSendComment = findViewById(R.id.fabSendComment);
        fabSubscribe = findViewById(R.id.fabSubscribe);

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);

        fabSendComment.setOnClickListener(v -> enviarComentario());
    }

    private void configurarBotonNotificaciones() {
        Log.d("FCM", "Intentando configurar botón para: post_" + postId);

        isSubscribed = estaSubscrito();

        actualizarIconoSuscripcion();

        fabSubscribe.setOnClickListener(v -> {
            if(isSubscribed) {
                desuscribirDeTopic();
            } else {
                suscribirATopic();
            }
        });
    }

    private void actualizarIconoSuscripcion() {
        if(isSubscribed) {
            fabSubscribe.setImageResource(R.drawable.notifications_active);
        } else {
            fabSubscribe.setImageResource(R.drawable.notifications);
        }
    }

    private boolean estaSubscrito() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(SUBSCRIPTION_KEY_PREFIX + postId, false);
    }

    private void guardarEstadoSubscripcion(boolean subscribed) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(SUBSCRIPTION_KEY_PREFIX + postId, subscribed);
        editor.apply();

        isSubscribed = subscribed;

        actualizarIconoSuscripcion();
    }

    private void suscribirATopic() {
        Log.d("FCM", "Intentando suscribir a: post_" + postId);
        String topic = "post_" + postId;
        firebaseMessaging.subscribeToTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Suscripción exitosa");
                        guardarEstadoSubscripcion(true);
                        Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("FCM", "Error en suscripción", task.getException());
                        Toast.makeText(this, "Error al activar notificaciones", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(this::suscribirATopic, 2000);
                    }
                });
    }

    private void desuscribirDeTopic() {
        String topic = "post_" + postId;
        firebaseMessaging.unsubscribeFromTopic(topic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        guardarEstadoSubscripcion(false);
                        Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error al desactivar notificaciones", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(this::desuscribirDeTopic, 2000);
                    }
                });
    }

    private void cargarComentarios() {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "obtener_comentarios");
            jsonBody.put("post_id", postId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ForumActivity.BASE_URL,
                jsonBody,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            commentList.clear();
                            JSONArray comentarios = response.getJSONArray("comentarios");
                            for (int i = 0; i < comentarios.length(); i++) {
                                JSONObject comentario = comentarios.getJSONObject(i);
                                Comment comment = new Comment(
                                        comentario.getString("commentId"),
                                        comentario.getString("userId"),
                                        comentario.getString("userName"),
                                        comentario.getString("content"),
                                        comentario.getLong("timestamp") * 1000
                                );
                                commentList.add(comment);
                            }
                            commentAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void enviarComentario() {
        String contenido = editTextComment.getText().toString().trim();
        if (contenido.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "crear_comentario");
            jsonBody.put("post_id", postId);
            jsonBody.put("user_id", currentUserId);
            jsonBody.put("content", contenido);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ForumActivity.BASE_URL,
                jsonBody,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            editTextComment.setText("");
                            cargarComentarios();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error al enviar comentario", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}