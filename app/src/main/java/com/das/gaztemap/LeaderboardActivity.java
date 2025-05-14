package com.das.gaztemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private TabLayout tabLayout;
    private String nombre;
    private String email;
    private ShapeableImageView imgPerfil;
    private TextView tvUserRank;
    private TextView tvUserPoints;
    private ShapeableImageView userProfileImage;
    private View userRankCard;
    private List<LeaderboardUser> userList = new ArrayList<>();
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        nombre = getIntent().getStringExtra("nombre");
        email = getIntent().getStringExtra("email");

        setupNavigation();

        userRankCard = findViewById(R.id.current_user_rank_card);
        userProfileImage = findViewById(R.id.user_profile_image);
        tvUserRank = findViewById(R.id.tv_user_rank);
        tvUserPoints = findViewById(R.id.tv_user_points);

        recyclerView = findViewById(R.id.recycler_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.historico));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.mensual));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.semanal));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.diario));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadLeaderboardData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadLeaderboardData(tab.getPosition());
            }
        });

        loadLeaderboardData();
        loadCurrentUserRank();
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.Top500);

        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            imgPerfil = headerView.findViewById(R.id.imgPerfil);
            TextView txtNombre = headerView.findViewById(R.id.campoNombreNav);
            TextView txtEmail = headerView.findViewById(R.id.campoEmail);

            txtNombre.setText(nombre);
            txtEmail.setText(email);
            obtenerPfpNav();
        }

        Button botonEdit = headerView.findViewById(R.id.btnEditUser);
        botonEdit.setOnClickListener(v -> {
            EditarUsuarioDF edf = EditarUsuarioDF.newIntance(nombre);
            edf.show(getSupportFragmentManager(), "edit_dialog");
        });

        Button botonCerrar = headerView.findViewById(R.id.btnLogout);
        botonCerrar.setOnClickListener(a -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeaderboardActivity.this);
            SharedPreferences.Editor editor = prefs.edit();
            Intent logout = new Intent(LeaderboardActivity.this, MainActivity.class);
            editor.putBoolean("rember", false);
            editor.apply();
            startActivity(logout);
        });

        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadLeaderboardData() {
        loadLeaderboardData(0);
    }

    private void loadLeaderboardData(int periodType) {
        String period;
        switch (periodType) {
            case 1:
                period = "monthly";
                break;
            case 2:
                period = "weekly";
                break;
            case 3:
                period = "daily";
                break;
            default:
                period = "alltime";
                break;
        }

        Data data = new Data.Builder()
                .putString("url", "3")
                .putString("accion", "getLeaderboard")
                .putString("period", period)
                .putString("limit", "100")
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String response = workInfo.getOutputData().getString("message");
                            String code = workInfo.getOutputData().getString("code");

                            if (code != null && code.equals("0")) {
                                parseLeaderboardData(response);
                            } else {
                                Log.e("LEADERBOARD", "Error loading data: " + response);
                            }
                        } else {
                            Log.e("LEADERBOARD", "Connection error");
                        }
                    }
                });
    }

    private void loadCurrentUserRank() {
        if (nombre == null) {
            userRankCard.setVisibility(View.GONE);
            return;
        }

        Data data = new Data.Builder()
                .putString("url", "3")
                .putString("accion", "getUserRank")
                .putString("nombre", nombre)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String response = workInfo.getOutputData().getString("message");
                            String code = workInfo.getOutputData().getString("code");

                            if (code != null && code.equals("0")) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    int rank = jsonObject.getInt("rank");
                                    int points = jsonObject.getInt("points");
                                    String imageUrl = jsonObject.getString("profile_image");

                                    tvUserRank.setText(String.format(getString(R.string.rank), rank));
                                    tvUserPoints.setText(String.format(getString(R.string.points_cantidad), points));

                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        String urlWithCache = imageUrl + "?nocache=" + System.currentTimeMillis();
                                        Glide.with(LeaderboardActivity.this)
                                                .load(urlWithCache)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(userProfileImage);
                                    } else {
                                        userProfileImage.setImageResource(R.drawable.placeholder);
                                    }
                                } catch (JSONException e) {
                                    Log.e("LEADERBOARD", "Error parsing user rank JSON", e);
                                    userRankCard.setVisibility(View.GONE);
                                }
                            } else {
                                userRankCard.setVisibility(View.GONE);
                            }
                        } else {
                            userRankCard.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void parseLeaderboardData(String response) {
        userList.clear();

        try {
            JSONArray jsonArray = new JSONArray(response);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObject = jsonArray.getJSONObject(i);

                LeaderboardUser user = new LeaderboardUser(
                        userObject.getInt("rank"),
                        userObject.getString("nombre"),
                        userObject.getInt("points"),
                        userObject.optString("profile_image", "")
                );

                userList.add(user);
            }

            adapter.notifyDataSetChanged();

            if (userList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

        } catch (JSONException e) {
            Log.e("LEADERBOARD", "Error parsing JSON", e);
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    public void obtenerPfpNav() {
        if (nombre != null) {
            Data datos = new Data.Builder()
                    .putString("url", "2")
                    .putString("accion", "getpfp")
                    .putString("nombre", nombre)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(LeaderboardActivity.this).enqueue(request);

            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(LeaderboardActivity.this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String mensaje = workInfo.getOutputData().getString("message");
                                String code = workInfo.getOutputData().getString("code");
                                if (code.equals("0")) {
                                    String url = workInfo.getOutputData().getString("url");
                                    String urlConId = url + "?nocache=" + System.currentTimeMillis();
                                    if (url != null) {
                                        Glide.with(LeaderboardActivity.this)
                                                .load(urlConId)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(imgPerfil);
                                    } else {
                                        imgPerfil.setImageResource(R.drawable.placeholder);
                                    }
                                }
                            }
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.Viajar) {
            Intent intent = new Intent(LeaderboardActivity.this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            navigationView.setCheckedItem(R.id.Viajar);
            startActivity(intent);
        } else if (id == R.id.Amigos) {
            Intent intent = new Intent(LeaderboardActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("frag", "amigos");
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            navigationView.setCheckedItem(R.id.Amigos);
            startActivity(intent);
        } else if (id == R.id.Top500) {
            navigationView.setCheckedItem(R.id.Top500);
        } else if (id == R.id.Foro) {
            Intent intent = new Intent(LeaderboardActivity.this, ForumActivity.class);
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            navigationView.setCheckedItem(R.id.Foro);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.Top500);
    }
}