package com.das.gaztemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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

    // Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton menuButton;
    private ShapeableImageView imgPerfil;
    private TextView txtNombre, txtEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        requestQueue = Volley.newRequestQueue(this);
        obtenerDatosUsuario();
        inicializarVistas();
        configurarDrawer();
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

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuButton = findViewById(R.id.menu_button);
    }

    private void configurarDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.Foro);

        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            imgPerfil = headerView.findViewById(R.id.imgPerfil);
            txtNombre = headerView.findViewById(R.id.campoNombreNav);
            txtEmail = headerView.findViewById(R.id.campoEmail);

            // Cargar datos inmediatamente
            if (txtNombre != null) txtNombre.setText(userName);
            if (txtEmail != null) txtEmail.setText(userEmail);
            obtenerPfpNav();

            Button botonEdit = headerView.findViewById(R.id.btnEditUser);
            Button botonCerrar = headerView.findViewById(R.id.btnLogout);

            if (botonEdit != null) {
                botonEdit.setOnClickListener(v -> {
                    EditarUsuarioDF edf = EditarUsuarioDF.newIntance(userName);
                    edf.show(getSupportFragmentManager(), "edit_dialog");
                });
            }

            if (botonCerrar != null) {
                botonCerrar.setOnClickListener(a -> cerrarSesion());
            }
        }

        menuButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
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


    public void obtenerPfpNav(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ForumActivity.this);
        String nombre = prefs.getString("nombre","error");
        String email = prefs.getString("email","errormail");
        txtNombre.setText(nombre);
        txtEmail.setText(email);
        if(nombre!=null) {
            Data datos = new Data.Builder()
                    .putString("url","2") //url a php gestor de monedas
                    .putString("accion", "getpfp") //obtiene monedas de usuario
                    .putString("nombre", nombre)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(ForumActivity.this).enqueue(request);

            //escuchar resultado
            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(ForumActivity.this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String mensaje = workInfo.getOutputData().getString("message");
                                Log.d("WORKER", "¡200! " + mensaje);
                                String code = workInfo.getOutputData().getString("code");
                                if(code.equals("0")) {
                                    //coger foto
                                    //String fotoraw = workInfo.getOutputData().getString("imagen");
                                    String url = workInfo.getOutputData().getString("url");
                                    String urlConId = url + "?nocache=" + System.currentTimeMillis();
                                    if (url!=null) {
                                        //Log.d("URL_DEBUG", fotoraw);
                                        /*byte[] decodedString = Base64.decode(fotoraw, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        pfp.setImageBitmap(decodedByte);*/
                                        Glide.with(this)
                                                .load(urlConId)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(imgPerfil); // Tu ImageView
                                        String lastPfp = urlConId;
                                    }else{
                                        imgPerfil.setImageResource(R.drawable.placeholder);
                                    }
                                }else{
                                    //error total
                                    Log.d("OBETENER IMAGEN", "FALLÓ");
                                }
                            } else {
                                Log.e("WORKER", "Algo falló.");
                            }
                        }
                    });        
        }else {
            Toast.makeText(getApplicationContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();

        }}
            private void cerrarSesion() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("rember", false);
        editor.apply();

        startActivity(new Intent(ForumActivity.this, MainActivity.class));
        finishAffinity();
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
                }
        );
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
                            hilo.getLong("timestamp") * 1000,
                            hilo.getString("profile_image")
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.Viajar) {
            Intent intent = new Intent(ForumActivity.this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("nombre", userName);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        } else if (id == R.id.Amigos) {
            Intent intent = new Intent(ForumActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("frag", "amigos");
            intent.putExtra("nombre", userName);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        } else if (id == R.id.Top500) {
            Intent intent = new Intent(ForumActivity.this, LeaderboardActivity.class);
            intent.putExtra("nombre", userName);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        } else if (id == R.id.Foro) {
            // Ya estamos en el foro, no hacer nada
        } else if (id == R.id.Opciones) {
            Intent intent = new Intent(ForumActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("frag", "opciones");
            intent.putExtra("nombre", userName);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.Foro);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String newName = prefs.getString("nombre", "Usuario");
        String newEmail = prefs.getString("email", "");

        if (!userName.equals(newName) || !userEmail.equals(newEmail)) {
            userName = newName;
            userEmail = newEmail;

            if (txtNombre != null) txtNombre.setText(userName);
            if (txtEmail != null) txtEmail.setText(userEmail);
            obtenerPfpNav();
        }
    }


    @Override
    public void onBackPressed() {
        if (newPostLayout.getVisibility() == View.VISIBLE) {
            toggleNewPostLayout();
            return;
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}