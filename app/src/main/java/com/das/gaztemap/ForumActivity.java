package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForumActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPosts;
    private EditText editTextPost;
    private Button buttonPost;
    private FloatingActionButton fabCreatePost;
    private View newPostLayout;

    private PostAdapter postAdapter;
    private List<Post> postList;

    // Datos del usuario
    private int userId;
    private String userName;
    private String userEmail;

    // Volley y URL
    public static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/api.php";
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);
        requestQueue = Volley.newRequestQueue(this);

        obtenerDatosUsuario();

        inicializarVistas();

        configurarRecyclerView();

        configurarListeners();

        loadPosts();
    }

    private void inicializarVistas() {
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        editTextPost = findViewById(R.id.editTextPost);
        buttonPost = findViewById(R.id.buttonPost);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        newPostLayout = findViewById(R.id.newPostLayout);
        newPostLayout.setVisibility(View.GONE);
    }

    private void configurarRecyclerView() {
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(this, postList, userId);
        recyclerViewPosts.setAdapter(postAdapter);
    }

    private void configurarListeners() {
        fabCreatePost.setOnClickListener(v -> toggleNewPostLayout());
        buttonPost.setOnClickListener(v -> publicarPost());
    }

    private void toggleNewPostLayout() { //TRANSITIONMANAGER PARA QUE SEA SMOTH
        if (newPostLayout.getVisibility() == View.VISIBLE) {
            TransitionManager.beginDelayedTransition((ViewGroup) newPostLayout.getParent());
            newPostLayout.setVisibility(View.GONE);
            fabCreatePost.show();
        } else {
            TransitionManager.beginDelayedTransition((ViewGroup) newPostLayout.getParent());
            newPostLayout.setVisibility(View.VISIBLE);
            fabCreatePost.hide();
        }
    }

    private void publicarPost() {
        String contenido = editTextPost.getText().toString().trim();
        if (!TextUtils.isEmpty(contenido)) {
            createPost(contenido);
        } else {
            Toast.makeText(this, "Por favor, escribe algo", Toast.LENGTH_SHORT).show();
        }
    }

    private void obtenerDatosUsuario() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("id", 0);
        userName = prefs.getString("nombre", "Usuario");
        userEmail = prefs.getString("email", "");

        if (userId == 0) {
            Toast.makeText(this, "No se detectó una sesión de usuario", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createPost(String content) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "crear_hilo");
            jsonBody.put("user_id", userId);
            jsonBody.put("user_name", userName);
            jsonBody.put("contenido", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                jsonBody,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            editTextPost.setText("");
                            newPostLayout.setVisibility(View.GONE);
                            fabCreatePost.show(); // Mostrar el FAB nuevamente
                            loadPosts();

                            // Opcional: desplazar al primer elemento
                            recyclerViewPosts.smoothScrollToPosition(0);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> {
                    // Mantener visible si hay error
                    newPostLayout.setVisibility(View.VISIBLE);
                    fabCreatePost.hide();
                }
        );

        requestQueue.add(request);
    }


    private void manejarRespuestaCrearPost(JSONObject response) {
        try {
            if (response.getString("status").equals("success")) {
                editTextPost.setText("");
                newPostLayout.setVisibility(View.GONE);
                loadPosts();
                Toast.makeText(this, "Hilo creado", Toast.LENGTH_SHORT).show();
            } else {
                mostrarError(response.getString("message"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadPosts() {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "obtener_hilos");
            jsonBody.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                jsonBody,
                response -> manejarRespuestaCargarPosts(response),
                error -> {
                    Log.e("JSON_ERROR", "Error en la solicitud: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("JSON_ERROR", "Código de respuesta HTTP: " + error.networkResponse.statusCode);
                        Log.e("JSON_ERROR", "Datos de error: " + new String(error.networkResponse.data));
                    }
}       );
        Log.d("fafga", String.valueOf(request));

        requestQueue.add(request);
    }

    private void manejarRespuestaCargarPosts(JSONObject response) {
        postList.clear();
        try {
            if (response.getString("status").equals("success")) {
                JSONArray hilos = response.getJSONArray("hilos");
                Log.d("API_DEBUG", "Número de hilos: " + hilos.length()); // ✅
                for (int i = 0; i < hilos.length(); i++) {
                    JSONObject hilo = hilos.getJSONObject(i);
                    Post post = new Post(
                            String.valueOf(hilo.getInt("postId")),
                            String.valueOf(hilo.getInt("userId")),
                            hilo.getString("userName"),
                            hilo.getString("content"),
                            hilo.getLong("timestamp") * 1000
                    );
                    post.setLikeCount(hilo.getInt("likeCount"));
                    post.setCommentCount(hilo.getInt("commentCount"));
                    post.setLiked(hilo.getBoolean("isLiked"));
                    postList.add(post);
                }
                postAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            Log.e("API_ERROR", "Error al parsear JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}