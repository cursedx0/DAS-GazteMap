package com.das.gaztemap;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import widget.MapAppWidget;

//import com.google.transit.realtime.GtfsRealtime; //mirar builld.gradle para detalles
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private NavigationView navigationView;
    private String nombre;
    private String email;
    private ShapeableImageView imgPerfil;
    private String lastPfp;
    private TextView txtNombre, txtEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.Viajar);
        // Configurar el perfil en el menú lateral
        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            imgPerfil = headerView.findViewById(R.id.imgPerfil);
            txtNombre = headerView.findViewById(R.id.campoNombreNav);
            txtEmail = headerView.findViewById(R.id.campoEmail);

            // poner aqui datos sacados al hacer login
            nombre = getIntent().getStringExtra("nombre");
            email = getIntent().getStringExtra("email");
            obtenerPfpNav();
            // imagen: imgPerfil.setImageResource(fotolukenserver);
        }
        Button botonEdit = headerView.findViewById(R.id.btnEditUser);
        botonEdit.setOnClickListener(v -> {
            EditarUsuarioDF edf = EditarUsuarioDF.newIntance(nombre);
            edf.show(getSupportFragmentManager(), "edit_dialog");
        });

        Button botonCerrar = headerView.findViewById(R.id.btnLogout);
        botonCerrar.setOnClickListener(a -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);
            SharedPreferences.Editor editor = prefs.edit();
            Intent logout = new Intent(MapActivity.this, MainActivity.class);
            editor.putBoolean("rember", false);
            editor.apply();
            startActivity(logout);

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

    // Encuentra el nodo más cercano al usuario
    private LatLng findNearestNode(LatLng userLocation, Graph graph) {
        LatLng nearestNode = null;
        double minDistance = Double.MAX_VALUE;

        for (LatLng node : graph.getNodes()) {
            double distance = calculateDistance(userLocation, node);
            if (distance < minDistance) {
                minDistance = distance;
                nearestNode = node;
            }
        }
        return nearestNode;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verificar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        LatLng vitoria = new LatLng(42.8460, -2.6716);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vitoria, 15));

        new Thread(() -> {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/viasciclistas23Maps.geojson");
                InputStream inputStream = url.openStream();
                StringBuilder jsonBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                }
                String geoJsonString = jsonBuilder.toString();
                JSONObject geoJsonData = new JSONObject(geoJsonString);
                GeoJsonLayer layer = new GeoJsonLayer(mMap, geoJsonData);

                Graph graph = new Graph();
                for (GeoJsonFeature feature : layer.getFeatures()) {
                    if (feature.getGeometry() != null && feature.getGeometry().getGeometryType().equals("LineString")) {
                        List<LatLng> points = ((GeoJsonLineString) feature.getGeometry()).getCoordinates();
                        for (int i = 0; i < points.size() - 1; i++) {
                            LatLng start = points.get(i);
                            LatLng end = points.get(i + 1);
                            double distance = calculateDistance(start, end);
                            graph.addEdge(start, end, distance);
                        }
                    }
                }

                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        LatLng upvLocation = new LatLng(42.8394805013312, -2.670361262280153);

                        // Encuentra el nodo más cercano al usuario
                        LatLng nearestNode = findNearestNode(userLocation, graph);
                        Log.d("NearestNodeDebug", "Nearest node to user: " + nearestNode);

                        // Encuentra el nodo más cercano a la UPV
                        LatLng nearestToDestination = findNearestNode(upvLocation, graph);
                        Log.d("NearestNodeDebug", "Nearest node to destination: " + nearestToDestination);

                        // Calcula la ruta más corta desde el nodo más cercano
                        List<LatLng> shortestPath = graph.getShortestPath(nearestNode, nearestToDestination);
                        Log.d("ShortestPathDebug", "Shortest path: " + shortestPath);

                        runOnUiThread(() -> {
                            PolylineOptions shortestPathPolyline = new PolylineOptions()
                                    .width(10)
                                    .color(Color.GREEN);

                            for (GeoJsonFeature feature : layer.getFeatures()) {
                                if (feature.getGeometry() != null && feature.getGeometry().getGeometryType().equals("LineString")) {
                                    List<LatLng> points = ((GeoJsonLineString) feature.getGeometry()).getCoordinates();

                                    for (int i = 0; i < points.size() - 1; i++) {
                                        LatLng start = points.get(i);
                                        LatLng end = points.get(i + 1);

                                        // Verificar si el segmento (start -> end) está en el shortestPath
                                        boolean isPartOfShortestPath = false;
                                        for (int j = 0; j < shortestPath.size() - 1; j++) {
                                            LatLng pathStart = shortestPath.get(j);
                                            LatLng pathEnd = shortestPath.get(j + 1);

                                            if ((start.equals(pathStart) && end.equals(pathEnd)) ||
                                                    (start.equals(pathEnd) && end.equals(pathStart))) {
                                                isPartOfShortestPath = true;
                                                break;
                                            }
                                        }

                                        // Dibujar solo el segmento necesario
                                        PolylineOptions segmentPolyline = new PolylineOptions()
                                                .add(start, end)
                                                .width(10)
                                                .color(isPartOfShortestPath ? Color.GREEN : Color.RED);

                                        mMap.addPolyline(segmentPolyline);
                                    }
                                }
                            }
                        });
                    }
                });

            } catch (Exception e) {
                Log.e("MapActivity", "Error al procesar el GeoJsonLayer", e);
            }
        }).start();
    }

    // Método para calcular la distancia entre dos puntos
    private double calculateDistance(LatLng start, LatLng end) {
        double earthRadius = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(end.latitude - start.latitude);
        double dLng = Math.toRadians(end.longitude - start.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void captureMapSnapshot() {
        if (mMap != null) {
            mMap.snapshot(bitmap -> {
                if (bitmap != null) {
                    saveBitmapToCache(bitmap);
                }
            });
        }
    }

    private void saveBitmapToCache(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File file = new File(cacheDir, "map_snapshot.png");
            Log.d("MapActivity", "Intentando guardar el archivo en: " + file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.d("MapActivity", "Mapa guardado en caché exitosamente: " + file.getAbsolutePath());
            updateWidget(); // Actualizar el widget
        } catch (Exception e) {
            Log.e("MapActivity", "Error al guardar el mapa en caché", e);
        }
    }

    private void updateWidget() {
        Intent intent = new Intent(this, MapAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        sendBroadcast(intent);
        Log.d("MapActivity", "Widget actualizado");
    }
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Obtener la última ubicación conocida
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(userLocation).title("Tu ubicación"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e("MapActivity", "Error al obtener la ubicación", e);
                Toast.makeText(this, "Error al obtener la ubicación.", Toast.LENGTH_SHORT).show();
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Llamada a la clase base
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            }
        }
    }

    /* //mirar build.gradle para detalles
    public GtfsRealtime.FeedMessage downloadFeed(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        return GtfsRealtime.FeedMessage.parseFrom(response.body().byteStream());
    }*/

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
        if (id == R.id.Viajar) {
            // No se, ahora actualiza
            if (mMap != null) {
                LatLng bilbao = new LatLng(13.2630, -2.9350);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 1));
            }
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Viajar);
        } else if (id == R.id.Amigos) {
            Intent intent = new Intent(MapActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("frag","amigos");
            actExtras();
            intent.putExtra("nombre",nombre);
            intent.putExtra("email",email);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Amigos);
            startActivity(intent);
        } else if (id == R.id.Top500) {
            Intent intent = new Intent(MapActivity.this, LeaderboardActivity.class);
            actExtras();
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            startActivity(intent);
        } else if (id == R.id.Foro){
            Intent intent = new Intent(MapActivity.this, ForumActivity.class);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Foro);
            actExtras();
            intent.putExtra("nombre",nombre);
            intent.putExtra("email",email);
            startActivity(intent);
        }
        else if (id == R.id.Opciones) {
            Intent intent = new Intent(MapActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("frag","opciones");
            actExtras();
            intent.putExtra("nombre",nombre);
            intent.putExtra("email",email);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Amigos);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void actExtras(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);
        nombre = prefs.getString("nombre","error");
        email = prefs.getString("email","errormail");
    }

    @Override
    public void onResume(){
        super.onResume();
        navigationView.setCheckedItem(R.id.Viajar);
        if(lastPfp!=null && nombre!=null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);
            String lp = prefs.getString("lastPfp", "error");
            if (!lastPfp.equals(lp)) {
                obtenerPfpNav();
            }
            nombre = prefs.getString("nombre","error");
            email = prefs.getString("email","errormail");
        }
    }

    public void obtenerPfpNav(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);
        nombre = prefs.getString("nombre","error");
        email = prefs.getString("email","errormail");
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

            WorkManager.getInstance(MapActivity.this).enqueue(request);

            //escuchar resultado
            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(MapActivity.this, workInfo -> {
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
                                        lastPfp = urlConId;
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

        }else{
            Toast.makeText(getApplicationContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
        }
    }
}
