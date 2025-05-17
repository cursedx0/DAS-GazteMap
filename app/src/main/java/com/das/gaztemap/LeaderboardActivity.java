package com.das.gaztemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, Parcelable {

    private static final String TAG = "LeaderboardActivity";
    private static final String API_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/leaderboard.php";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private TabLayout tabLayout;
    private String nombre;
    private String email;
    private ShapeableImageView imgPerfilNavHeader;
    private TextView tvUserRank;
    private TextView tvUserPoints;
    private ShapeableImageView userProfileImageCard;
    private View userRankCard;
    private List<LeaderboardUser> userList = new ArrayList<>();
    private TextView emptyView;
    private ProgressBar loadingView;
    private RequestQueue requestQueue;

    // State variables for user rank information
    private int userRank = -1;
    private int userPoints = 0;
    private String userImageUrl = "";

    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String KEY_USER_RANK = "user_rank";
    private static final String KEY_USER_POINTS = "user_points";
    private static final String KEY_USER_IMAGE_URL = "user_image_url";
    private static final String KEY_USER_RANK_LOADED = "user_rank_loaded";

    public LeaderboardActivity(){

    }
    protected LeaderboardActivity(Parcel in) {
        nombre = in.readString();
        email = in.readString();
        userRank = in.readInt();
        userPoints = in.readInt();
        userImageUrl = in.readString();
    }

    public static final Creator<LeaderboardActivity> CREATOR = new Creator<LeaderboardActivity>() {
        @Override
        public LeaderboardActivity createFromParcel(Parcel in) {
            return new LeaderboardActivity(in);
        }

        @Override
        public LeaderboardActivity[] newArray(int size) {
            return new LeaderboardActivity[size];
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        requestQueue = Volley.newRequestQueue(this);
        nombre = getIntent().getStringExtra("nombre");
        email = getIntent().getStringExtra("email");

        userRankCard = findViewById(R.id.current_user_rank_card);
        userProfileImageCard = findViewById(R.id.imgPerfil);
        tvUserRank = findViewById(R.id.tv_user_rank);
        tvUserPoints = findViewById(R.id.tv_user_points);
        loadingView = findViewById(R.id.loading_view);
        emptyView = findViewById(R.id.empty_view);

        setupNavigation();

        recyclerView = findViewById(R.id.recycler_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.removeAllTabs(); // Clear existing tabs
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.lGlobal)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.lamigos)));

        if (savedInstanceState != null) {
            //ArrayList<LeaderboardUser> savedList = (ArrayList<LeaderboardUser>) savedInstanceState.getSerializable(KEY_USER_LIST);
            ArrayList<LeaderboardUser> savedList = savedInstanceState.getParcelableArrayList(KEY_USER_LIST);

            if (savedList != null && !savedList.isEmpty()) {
                userList.clear();
                userList.addAll(savedList);
                adapter.notifyDataSetChanged();
            }

            int selectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            if (tabLayout != null && selectedTab < tabLayout.getTabCount()) {
                tabLayout.getTabAt(selectedTab).select();
            }

            userRank = savedInstanceState.getInt(KEY_USER_RANK, -1);
            userPoints = savedInstanceState.getInt(KEY_USER_POINTS, 0);
            userImageUrl = savedInstanceState.getString(KEY_USER_IMAGE_URL, "");
            boolean userRankLoaded = savedInstanceState.getBoolean(KEY_USER_RANK_LOADED, false);

            if (userRankLoaded) {
                updateUserRankUI();
            }

            showLoading(false);
            showEmptyState(userList.isEmpty());

            if (userRank == -1) {
                loadCurrentUserRank();
            }
        } else {
            loadLeaderboardData(tabLayout.getSelectedTabPosition());
            loadCurrentUserRank();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadLeaderboardData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadLeaderboardData(tab.getPosition());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<LeaderboardUser> listToSave = new ArrayList<>(userList);
        //outState.putSerializable(KEY_USER_LIST, listToSave);
        outState.putParcelableArrayList(KEY_USER_LIST, (ArrayList<? extends Parcelable>) userList);
        outState.putInt(KEY_SELECTED_TAB, tabLayout.getSelectedTabPosition());
        outState.putInt(KEY_USER_RANK, userRank);
        outState.putInt(KEY_USER_POINTS, userPoints);
        outState.putString(KEY_USER_IMAGE_URL, userImageUrl);
        outState.putBoolean(KEY_USER_RANK_LOADED, userRank != -1);

        Log.d(TAG, "onSaveInstanceState: Saving state with " + listToSave.size() + " leaderboard items");
    }

    private void updateUserRankUI() {
        if (userRank > 0) {
            tvUserRank.setText(String.format(getString(R.string.rank), userRank));
            tvUserPoints.setText(String.format(getString(R.string.points_cantidad), userPoints));

            if (!userImageUrl.isEmpty() && !userImageUrl.equals("null")) {
                Glide.with(LeaderboardActivity.this)
                        .load(userImageUrl)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(userProfileImageCard);
            } else {
                userProfileImageCard.setImageResource(R.drawable.placeholder);
            }
            userRankCard.setVisibility(View.VISIBLE);
        } else {
            userRankCard.setVisibility(View.GONE);
        }
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem profileMenuItem = navigationView.getMenu().findItem(R.id.n_perfil);
        View headerView = null;
        if (profileMenuItem != null) {
            headerView = profileMenuItem.getActionView();
            if (headerView == null && navigationView.getHeaderCount() > 0) {
                headerView = navigationView.getHeaderView(0);
            }
        } else if (navigationView.getHeaderCount() > 0) {
            headerView = navigationView.getHeaderView(0);
        }

        if (headerView != null) {
            imgPerfilNavHeader = headerView.findViewById(R.id.imgPerfil);
            TextView txtNombre = headerView.findViewById(R.id.campoNombreNav);
            TextView txtEmail = headerView.findViewById(R.id.campoEmail);

            if (txtNombre != null) txtNombre.setText(nombre);
            if (txtEmail != null) txtEmail.setText(email);

            if (imgPerfilNavHeader != null) {
                obtenerPfpNav();
            } else {
                Log.w(TAG, "imgPerfilNavHeader in NavHeader not found.");
            }

            Button botonEdit = headerView.findViewById(R.id.btnEditUser);
            if (botonEdit != null) {
                botonEdit.setOnClickListener(v -> {
                    EditarUsuarioDF edf = EditarUsuarioDF.newIntance(nombre);
                    edf.show(getSupportFragmentManager(), "edit_dialog");
                });
            }

            Button botonCerrar = headerView.findViewById(R.id.btnLogout);
            if (botonCerrar != null) {
                botonCerrar.setOnClickListener(a -> {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeaderboardActivity.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    Intent logout = new Intent(LeaderboardActivity.this, MainActivity.class);
                    editor.putBoolean("rember", false);
                    editor.apply();
                    startActivity(logout);
                    finishAffinity();
                });
            }
        } else {
            Log.e(TAG, "Navigation header view not found.");
        }

        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void loadLeaderboardData(int tabPosition) {
        String type = (tabPosition == 0) ? "global" : "friends";
        showLoading(true);
        userList.clear();
        adapter.notifyDataSetChanged();
        showEmptyState(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeaderboardActivity.this);
        int userId = prefs.getInt("id", 0);

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    showLoading(false);
                    parseLeaderboardData(response);
                },
                error -> {
                    showLoading(false);
                    Log.e(TAG, "Error in Volley request for leaderboard (" + type + "): " + error.toString());
                    showEmptyState(true);

                    String errorMessage = "Error loading leaderboard";
                    if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                        errorMessage += ": " + error.getMessage();
                    } else {
                        if (error instanceof NoConnectionError) {
                            errorMessage += ": No internet connection.";
                        } else if (error instanceof TimeoutError) {
                            errorMessage += ": Request timed out.";
                        } else if (error instanceof NetworkError) {
                            errorMessage += ": Network error.";
                        } else if (error instanceof ServerError) {
                            errorMessage += ": Server error";
                            if (error.networkResponse != null) {
                                errorMessage += " (Code: " + error.networkResponse.statusCode + ")";
                            }
                        } else {
                            errorMessage += ". Please try again.";
                        }
                    }
                    Toast.makeText(LeaderboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "getLeaderboard");
                params.put("type", type);

                if (type.equals("friends")) {
                    if (userId == 0) {
                        runOnUiThread(() -> Toast.makeText(
                                LeaderboardActivity.this,
                                "User ID not found. Please login again.",
                                Toast.LENGTH_LONG
                        ).show());
                    } else {
                        params.put("userId", String.valueOf(userId));
                    }
                }
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void loadCurrentUserRank() {
        if (nombre == null || nombre.isEmpty()) {
            userRankCard.setVisibility(View.GONE);
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");

                        if (status.equals("success")) {
                            userRank = jsonResponse.getInt("rank");
                            userPoints = jsonResponse.getInt("points");
                            userImageUrl = jsonResponse.optString("profile_image", "");
                            updateUserRankUI();
                        } else {
                            Log.e(TAG, "Error getting user rank (API): " + jsonResponse.optString("message", "Unknown API error"));
                            userRankCard.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing user rank JSON", e);
                        userRankCard.setVisibility(View.GONE);
                        Toast.makeText(LeaderboardActivity.this, "Error parsing your rank data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error in Volley request for user rank: " + error.toString());
                    userRankCard.setVisibility(View.GONE);
                    String errorMessage = "Error loading your rank";
                    Toast.makeText(LeaderboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("action", "getUserRank");
                params.put("nombre", nombre);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void parseLeaderboardData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");

            if (status.equals("success")) {
                userList.clear();
                JSONArray usersArray = jsonResponse.getJSONArray("users");
                for (int i = 0; i < usersArray.length(); i++) {
                    JSONObject userObject = usersArray.getJSONObject(i);
                    boolean isCurrentUser = userObject.getString("nombre").equals(nombre);
                    LeaderboardUser user = new LeaderboardUser(
                            i + 1,
                            userObject.getString("nombre"),
                            userObject.getInt("puntos"),
                            userObject.optString("profile_image", ""),
                            userObject.optInt("conectado", 0) == 1
                    );
                    user.setCurrentUser(isCurrentUser);
                    userList.add(user);
                }
                adapter.notifyDataSetChanged();
                showEmptyState(userList.isEmpty());
                Log.d(TAG, "parseLeaderboardData: Loaded " + userList.size() + " users");
            } else {
                showEmptyState(true);
                Log.e(TAG, "Leaderboard data status not success: " + jsonResponse.optString("message"));
                Toast.makeText(this, "Failed to load leaderboard: " + jsonResponse.optString("message", "API error"), Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing leaderboard JSON", e);
            showEmptyState(true);
            Toast.makeText(this, "Error parsing leaderboard data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean isLoading) {
        if (loadingView != null) {
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            if (!isEmpty && (loadingView != null && loadingView.getVisibility() == View.GONE)) {
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
            }
        }
        if (isEmpty && loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
    }

    public void obtenerPfpNav() {
        if (nombre == null || nombre.isEmpty() || imgPerfilNavHeader == null) {
            if (imgPerfilNavHeader != null) imgPerfilNavHeader.setImageResource(R.drawable.placeholder);
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");

                        if (status.equals("success")) {
                            String imageUrl = jsonResponse.optString("url", "");
                            if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {
                                Glide.with(LeaderboardActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.placeholder)
                                        .error(R.drawable.placeholder)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .skipMemoryCache(true)
                                        .into(imgPerfilNavHeader);
                            } else {
                                imgPerfilNavHeader.setImageResource(R.drawable.placeholder);
                            }
                        } else {
                            imgPerfilNavHeader.setImageResource(R.drawable.placeholder);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing profile image JSON for nav", e);
                        imgPerfilNavHeader.setImageResource(R.drawable.placeholder);
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading profile image for nav: " + error.toString());
                    if (imgPerfilNavHeader != null) imgPerfilNavHeader.setImageResource(R.drawable.placeholder);
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("action", "getPfp");
                params.put("nombre", nombre);
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.Top500 && !(this instanceof LeaderboardActivity)) {
        } else if (id == R.id.Viajar) {
            intent = new Intent(LeaderboardActivity.this, MapActivity.class);
        } else if (id == R.id.Amigos) {
            intent = new Intent(LeaderboardActivity.this, AllActivity.class);
            intent.putExtra("frag", "amigos");
        } else if (id == R.id.Foro) {
            intent = new Intent(LeaderboardActivity.this, ForumActivity.class);
        } else if (id == R.id.Opciones){
            intent = new Intent(LeaderboardActivity.this, AllActivity.class);
            intent.putExtra("frag","opciones");
        }

        if (intent != null) {
            actExtras();
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        if (id != R.id.n_perfil) {
            navigationView.setCheckedItem(id);
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

    private void actExtras(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeaderboardActivity.this);
        nombre = prefs.getString("nombre","error");
        email = prefs.getString("email","errormail");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(nombre);
        dest.writeString(email);
        dest.writeInt(userRank);
        dest.writeInt(userPoints);
        dest.writeString(userImageUrl);
    }
}