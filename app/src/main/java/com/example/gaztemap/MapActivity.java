package com.example.gaztemap;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.Viajar);
        // Configurar el perfil en el menú lateral
        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            ShapeableImageView imgPerfil = headerView.findViewById(R.id.imgPerfil);
            TextView txtNombre = headerView.findViewById(R.id.campoNombreNav);
            TextView txtEmail = headerView.findViewById(R.id.campoEmail);

            // poner aqui datos sacados al hacer login
            String nombre = getIntent().getStringExtra("nombre");
            String email = getIntent().getStringExtra("email");
            txtNombre.setText(nombre);
            txtEmail.setText(email);
            // imagen: imgPerfil.setImageResource(fotolukenserver);
        }
        Button botonEdit = headerView.findViewById(R.id.btnEditUser);
        botonEdit.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        });
        // botón para abrir el menú lateral
        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Marcar Moyua porque si
        LatLng exampleLocation = new LatLng(43.2630, -2.9350); // Coordenadas de Bilbao
        mMap.addMarker(new MarkerOptions().position(exampleLocation).title("Marcador en Bilbao"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(exampleLocation, 15));
    }

    @Override
    public void onBackPressed() {
        // Cerrar el drawer si está abierto cuando se presiona atrás
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d("faga", item.toString());
        if (id == R.id.map) {
            // No se, ahora actualiza
            if (mMap != null) {
                LatLng bilbao = new LatLng(13.2630, -2.9350);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 1));
            }
        } else if (id == R.id.amigos) {
        } else if (id == R.id.top500) {
        } else if (id == R.id.opciones) {

        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
