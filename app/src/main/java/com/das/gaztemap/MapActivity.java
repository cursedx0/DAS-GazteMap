package com.das.gaztemap;

import android.app.AlertDialog;
import android.app.ComponentCaller;
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
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import okhttp3.RequestBody;
import widget.MapAppWidget;

//import com.google.transit.realtime.GtfsRealtime; //mirar builld.gradle para detalles
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends BaseActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private NavigationView navigationView;
    private String nombre;
    private String email;
    private ShapeableImageView imgPerfil;
    private String lastPfp;
    private TextView txtNombre, txtEmail;

    private FloatingActionButton transportButton;
    private FloatingActionButton layersButton;
    private FloatingActionButton nearMeButton;

    private LinearLayout distanceTimeDialog;
    private GeoJsonLayer bicycleLayer; // Variable para almacenar el layer
    private List<GeoJsonLayer> busRoutesLayers = new ArrayList<>();

    private LinearLayout transportOptions;
    private LinearLayout optionWalking, optionBus, optionBicycle;
    private String selectedTransportMode = "walking"; // valor por defecto
    private String currentLanguage;

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);
        currentLanguage = prefs.getString("idioma","ES");

        Button botonEdit = headerView.findViewById(R.id.btnEditUser);
        botonEdit.setOnClickListener(v -> {
            EditarUsuarioDF edf = EditarUsuarioDF.newIntance(nombre);
            edf.show(getSupportFragmentManager(), "edit_dialog");
        });

        Button botonCerrar = headerView.findViewById(R.id.btnLogout);
        botonCerrar.setOnClickListener(a -> {
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

        transportButton = findViewById(R.id.transport_button);
        transportOptions = findViewById(R.id.transport_options);
        optionWalking = findViewById(R.id.option_walking);
        optionBus = findViewById(R.id.option_bus);
        optionBicycle = findViewById(R.id.option_bicycle);

        // botón de transporte para mostrar/ocultar opciones
        transportButton.setOnClickListener(view -> {
            if (transportOptions.getVisibility() == View.VISIBLE) {
                transportOptions.setVisibility(View.GONE);
            } else {
                transportOptions.setVisibility(View.VISIBLE);
            }
        });

        //  listeners de cada opción de transporte
        optionWalking.setOnClickListener(view -> {
            selectedTransportMode = "walking";
            transportOptions.setVisibility(View.GONE);
            updateTransportIcon();
            recalculateRoute();
        });

        optionBus.setOnClickListener(view -> {
            selectedTransportMode = "bus";
            transportOptions.setVisibility(View.GONE);
            updateTransportIcon();
            recalculateRoute();
        });

        optionBicycle.setOnClickListener(view -> {
            selectedTransportMode = "bicycle";
            transportOptions.setVisibility(View.GONE);
            updateTransportIcon();
            recalculateRoute();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Controlar la visibilidad del layer con el botón de capas
        layersButton = findViewById(R.id.layers_button);
        layersButton.setOnClickListener(view -> {
            if (bicycleLayer != null) {
                try {
                    if (bicycleLayer.getFeatures().iterator().hasNext()) { // Verifica si tiene datos
                        if (bicycleLayer.getMap() == null) {
                            bicycleLayer.setMap(mMap); // Mostrar el layer
                        } else {
                            bicycleLayer.setMap(null); // Ocultar el layer
                        }
                    } else {
                        Log.e("MapActivity", "El layer no contiene datos.");
                        Toast.makeText(this, "El layer no contiene datos.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("MapActivity", "Error al alternar la visibilidad del layer", e);
                    Toast.makeText(this, "Error al alternar la visibilidad del layer.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("MapActivity", "El layer no está inicializado.");
                Toast.makeText(this, "El layer no está disponible.", Toast.LENGTH_SHORT).show();
            }
        });
        nearMeButton = findViewById(R.id.near_me_button);
        nearMeButton.setOnClickListener(view -> showStartGameDialog());
    }

    private void updateTransportIcon() {
        FloatingActionButton nearMeButton = findViewById(R.id.near_me_button);

        switch (selectedTransportMode) {
            case "walking":
                transportButton.setImageResource(R.drawable.steps_40px);
                nearMeButton.setVisibility(View.VISIBLE); // Mostrar el botón
                layersButton.setVisibility(View.GONE); // Ocultar el botón de capas
                break;
            case "bus":
                transportButton.setImageResource(R.drawable.directions_bus_40px);
                nearMeButton.setVisibility(View.GONE); // Ocultar el botón
                layersButton.setVisibility(View.GONE); // Mostrar el botón de capas
                break;
            case "bicycle":
                transportButton.setImageResource(R.drawable.pedal_bike_40px);
                nearMeButton.setVisibility(View.VISIBLE); // Mostrar el botón
                layersButton.setVisibility(View.VISIBLE); // Mostrar el botón de capas
                break;
        }
    }

    private void recalculateRoute() {
        if (mMap == null) return;

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                LatLng destination = new LatLng(42.8394805013312, -2.670361262280153); // Ubicación de la UPV

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Tu ubicación"));
                mMap.addMarker(new MarkerOptions().position(destination).title("Destino"));

                // Actualizar la ubicación del usuario en la base de datos
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int userId = prefs.getInt("id", -1);
                if (userId != -1) {
                    updateUserLocation(userId, location.getLatitude(), location.getLongitude());
                } else {
                    Log.e("MapActivity", "Error: ID de usuario no encontrado en SharedPreferences.");
                }
                // Cargar ubicaciones de amigos
                if (userId != -1) {
                    loadFriendsLocations(userId);
                } else {
                    Log.e("MapActivity", "Error: No tienes amigos con la ubicación activada.");
                }

                // Dependiendo del modo de transporte, cargar rutas diferentes
                switch (selectedTransportMode) {
                    case "bicycle":
                        loadBicycleRoute(userLocation, destination);
                        break;
                    case "walking":
                        loadWalkingRoute(userLocation, destination);
                        break;
                    case "bus":
                        loadBusRoute(userLocation, destination);
                        break;
                }
            }
        });
    }

    private void loadBicycleRoute(LatLng userLocation, LatLng destination) {
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

                // Configurar estilo predeterminado para las líneas
                GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
                lineStyle.setColor(Color.RED); // Color predeterminado
                lineStyle.setWidth(10); // Ancho de línea
                for (GeoJsonFeature feature : layer.getFeatures()) {
                    if (feature.getGeometry() instanceof GeoJsonLineString) {
                        feature.setLineStringStyle(lineStyle);
                    }
                }

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

                LatLng nearestNode = findNearestNode(userLocation, graph);
                LatLng nearestToDestination = findNearestNode(destination, graph);
                List<LatLng> shortestPath = graph.getShortestPath(nearestNode, nearestToDestination);

                // Calcular distancia total
                double totalDistance = 0.0;
                for (int i = 0; i < shortestPath.size() - 1; i++) {
                    totalDistance += calculateDistance(shortestPath.get(i), shortestPath.get(i + 1));
                }

                // Estimar tiempo de viaje (velocidad promedio: 15 km/h)
                double averageSpeed = 15.0; // km/h
                double totalTimeHours = totalDistance / averageSpeed;
                int totalMinutes = (int) (totalTimeHours * 60);

                String distanceText = String.format("%.2f km", totalDistance);
                String durationText = totalMinutes + " min";

                runOnUiThread(() -> {
                    bicycleLayer = layer; // Guardar el layer para controlarlo
                    for (GeoJsonFeature feature : layer.getFeatures()) {
                        if (feature.getGeometry() != null && feature.getGeometry().getGeometryType().equals("LineString")) {
                            List<LatLng> points = ((GeoJsonLineString) feature.getGeometry()).getCoordinates();

                            for (int i = 0; i < points.size() - 1; i++) {
                                LatLng start = points.get(i);
                                LatLng end = points.get(i + 1);

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

                                PolylineOptions segmentPolyline = new PolylineOptions()
                                        .add(start, end)
                                        .width(10)
                                        .color(isPartOfShortestPath ? Color.GREEN : Color.RED);

                                if (isPartOfShortestPath) {
                                    mMap.addPolyline(segmentPolyline); // Dibujar solo las líneas verdes
                                }
                            }
                        }
                    }

                    // Ocultar el layer inicialmente
                    bicycleLayer.setMap(null);

                    // Mostrar diálogo con distancia y tiempo
                    showDistanceTimeDialog(distanceText, durationText);
                });

            } catch (Exception e) {
                Log.e("MapActivity", "Error al procesar el GeoJsonLayer", e);
            }
        }).start();
    }


    private void loadWalkingRoute(LatLng userLocation, LatLng destination) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyBBeagiyy4wY0h1RnbKgoK7kpadmYAhU1o";
                String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                        + userLocation.latitude + "," + userLocation.longitude
                        + "&destination=" + destination.latitude + "," + destination.longitude
                        + "&mode=walking&key=" + apiKey;

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);

                    if ("OK".equals(jsonResponse.getString("status"))) {
                        JSONArray routes = jsonResponse.getJSONArray("routes");
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPolyline = overviewPolyline.getString("points");

                        // Obtener distancia y duración
                        JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                        String distance = leg.getJSONObject("distance").getString("text");
                        String duration = leg.getJSONObject("duration").getString("text");

                        List<LatLng> path = decodePolyline(encodedPolyline);

                        runOnUiThread(() -> {
                            // Dibujar la ruta
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(path)
                                    .width(10)
                                    .color(Color.GREEN);
                            mMap.addPolyline(polylineOptions);

                            // Mostrar diálogo con distancia y tiempo
                            showDistanceTimeDialog(distance, duration);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(MapActivity.this, "No hay rutas disponibles.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al obtener la ruta.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error al calcular la ruta a pie", e);
                runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al calcular la ruta a pie.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Metodo para decodificar una polilínea codificada
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }

        return poly;
    }


    private void showDistanceTimeDialog(String distance, String duration) {
        if (distanceTimeDialog == null) {
            // Crear el diálogo solo si no existe
            distanceTimeDialog = new LinearLayout(this);
            distanceTimeDialog.setOrientation(LinearLayout.VERTICAL);
            distanceTimeDialog.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_background)); // Fondo redondeado
            distanceTimeDialog.setPadding(32, 32, 32, 32);

            TextView distanceText = new TextView(this);
            distanceText.setId(View.generateViewId()); // Generar ID único
            distanceText.setTextSize(16);
            distanceText.setTextColor(Color.WHITE); // Texto blanco
            distanceTimeDialog.addView(distanceText);

            TextView durationText = new TextView(this);
            durationText.setId(View.generateViewId()); // Generar ID único
            durationText.setTextSize(16);
            durationText.setTextColor(Color.WHITE); // Texto blanco
            distanceTimeDialog.addView(durationText);

            // Configurar las propiedades de diseño
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.setMargins(32, 32, 32, 32);

            // Añadir el diálogo al contenedor raíz
            FrameLayout rootLayout = findViewById(android.R.id.content);
            rootLayout.addView(distanceTimeDialog, params);
        }

        // Actualizar el contenido del diálogo
        TextView distanceText = (TextView) distanceTimeDialog.getChildAt(0);
        TextView durationText = (TextView) distanceTimeDialog.getChildAt(1);
        distanceText.setText("Distancia: " + distance);
        durationText.setText("Duración: " + duration);

    }

    private void loadBusRoute(LatLng userLocation, LatLng destination) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                String urlParadas = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/api_bus.php?accion=paradasCampus";
                Request requestParadas = new Request.Builder().url(urlParadas).build();
                Response responseParadas = client.newCall(requestParadas).execute();

                if (responseParadas.isSuccessful()) {
                    String responseDataParadas = responseParadas.body().string();
                    JSONArray paradasArray = new JSONArray(responseDataParadas);

                    String nearestStopId = null;
                    double minDistance = Double.MAX_VALUE;

                    for (int i = 0; i < paradasArray.length(); i++) {
                        JSONObject parada = paradasArray.getJSONObject(i);
                        double stopLat = parada.getDouble("stop_lat");
                        double stopLon = parada.getDouble("stop_lon");
                        LatLng stopLocation = new LatLng(stopLat, stopLon);

                        double distance = calculateDistance(userLocation, stopLocation);
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestStopId = parada.getString("stop_id");
                        }
                    }

                    if (nearestStopId == null) {
                        runOnUiThread(() -> Toast.makeText(MapActivity.this, "No se encontró una parada cercana.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String urlRecorrido = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/api_bus.php?accion=recorrido&origen=" + nearestStopId;
                    Request requestRecorrido = new Request.Builder().url(urlRecorrido).build();
                    Response responseRecorrido = client.newCall(requestRecorrido).execute();

                    if (responseRecorrido.isSuccessful()) {
                        String responseDataRecorrido = responseRecorrido.body().string();
                        JSONObject recorridoData = new JSONObject(responseDataRecorrido);
                        JSONObject paradaOrigen = recorridoData.getJSONObject("parada_origen");
                        JSONObject paradaCampus = recorridoData.getJSONObject("parada_campus");
                        JSONArray recorridoShape = recorridoData.getJSONArray("recorrido_shape");
                        String proximaSalida = recorridoData.getString("proxima_salida");

                        LatLng campusLocation = new LatLng(paradaCampus.getDouble("lat"), paradaCampus.getDouble("lon"));
                        List<LatLng> path = new ArrayList<>();

                        for (int i = 0; i < recorridoShape.length(); i++) {
                            JSONObject point = recorridoShape.getJSONObject(i);
                            double lat = point.getDouble("lat");
                            double lon = point.getDouble("lon");
                            LatLng node = new LatLng(lat, lon);

                            path.add(node);

                            // Detenerse si el nodo está cerca de la parada del campus
                            if (calculateDistance(node, campusLocation) < 0.005) { // 50 metros
                                break;
                            }
                        }

                        runOnUiThread(() -> {
                            // Dibujar la polilínea del recorrido
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(path)
                                    .width(10)
                                    .color(Color.BLUE);
                            mMap.addPolyline(polylineOptions);

                            LatLng origenLocation = null; // Declarar fuera del bloque try-catch

                            // Añadir marcador en la parada de origen
                            try {
                                origenLocation = new LatLng(paradaOrigen.getDouble("lat"), paradaOrigen.getDouble("lon"));
                                mMap.addMarker(new MarkerOptions()
                                        .position(origenLocation)
                                        .title(paradaOrigen.getString("nombre"))
                                        .snippet("Próxima salida: " + proximaSalida));
                            } catch (JSONException e) {
                                Log.e("MapActivity", "Error al procesar la parada de origen", e);
                            }

                            // Añadir marcador en la parada del campus
                            try {
                                mMap.addMarker(new MarkerOptions()
                                        .position(campusLocation)
                                        .title(paradaCampus.getString("nombre")));
                            } catch (JSONException e) {
                                Log.e("MapActivity", "Error al obtener el nombre de la parada del campus", e);
                            }

                            // Dibujar rutas a pie reutilizando loadWalkingRoute
                            loadWalkingRoute(userLocation, origenLocation); // Usuario -> Parada origen
                            loadWalkingRoute(campusLocation, destination); // Parada campus -> Universidad
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al obtener el recorrido.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al obtener las paradas.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error al cargar la ruta en bus", e);
                runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al cargar la ruta en bus.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

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

        // Iniciar (bicicleta)
        updateTransportIcon();
    }

    // Metodo para calcular la distancia entre dos puntos
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

    private void updateUserLocation(int userId, double lat, double lon) {
        new Thread(() -> {
            try {
                // URL del endpoint
                String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/api.php";

                // Crear el JSON con los datos
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("accion", "actualizar_ubicacion");
                jsonBody.put("user_id", userId);
                jsonBody.put("lat", lat);
                jsonBody.put("lon", lon);

                // Configurar la solicitud POST
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                // Ejecutar la solicitud
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d("MapActivity", "Ubicación actualizada correctamente.");
                } else {
                    Log.e("MapActivity", "Error al actualizar la ubicación: " + response.message());
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error al enviar la ubicación al servidor", e);
            }
        }).start();
    }

    private void loadFriendsLocations(int userId) {
        new Thread(() -> {
            try {
                // URL del endpoint
                String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/api.php";

                // Crear el JSON con los datos
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("accion", "ubicacion_amigos");
                jsonBody.put("user_id", userId);

                // Configurar la solicitud POST
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                // Ejecutar la solicitud
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);

                    if ("success".equals(jsonResponse.getString("status"))) {
                        JSONArray friendsArray = jsonResponse.getJSONArray("amigos");

                        // Añadir marcadores en el mapa para cada amigo
                        runOnUiThread(() -> {
                            for (int i = 0; i < friendsArray.length(); i++) {
                                try {
                                    JSONObject friend = friendsArray.getJSONObject(i);

                                    // Verificar si lat y lon no son null
                                    if (!friend.isNull("lat") && !friend.isNull("lon")) {
                                        String name = friend.getString("nombre");
                                        double lat = friend.getDouble("lat");
                                        double lon = friend.getDouble("lon");

                                        LatLng friendLocation = new LatLng(lat, lon);
                                        Log.d("MapActivity", "Añadiendo marcador para amigo: " + name + " en lat: " + lat + ", lon: " + lon);
                                        mMap.addMarker(new MarkerOptions()
                                                .position(friendLocation)
                                                .title(name)
                                                .snippet("Amigo")
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                    } else {
                                        Log.e("MapActivity", "Amigo con ubicación inválida: " + friend.toString());
                                    }
                                } catch (Exception e) {
                                    Log.e("MapActivity", "Error al procesar la ubicación de un amigo", e);
                                }
                            }
                        });
                    } else {
                        Log.e("MapActivity", "Error en la respuesta: " + jsonResponse.getString("status"));
                    }
                } else {
                    Log.e("MapActivity", "Error al obtener ubicaciones de amigos: " + response.message());
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error al cargar ubicaciones de amigos", e);
            }
        }).start();
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

                    // Calcular ruta inicial con el modo de transporte predeterminado
                    recalculateRoute();
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

    private void showStartGameDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar inicio")
                .setMessage("¿Estás seguro de que quieres iniciar el modo juego?")
                .setPositiveButton("Sí", (dialog, which) -> startGame())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    //esto subirlo arriba con lo demas
    private boolean isGameActive = false;
    private long startTime;
    private final LatLng UPV_LOCATION = new LatLng(42.8394805013312, -2.670361262280153); // Coordenadas de la UPV/EHU

    private android.location.Location startLocation; // Variable de instancia para almacenar la ubicación inicial

    private void startGame() {
        if (isGameActive) {
            Toast.makeText(this, "El juego ya está en curso.", Toast.LENGTH_SHORT).show();
            return;
        }

        isGameActive = true;
        startTime = System.currentTimeMillis();
        Toast.makeText(this, "¡El juego ha comenzado! Llega a la UPV/EHU lo más rápido posible.", Toast.LENGTH_SHORT).show();

        // Bloquear interacciones
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        transportButton.setEnabled(false);
        layersButton.setEnabled(false);
        nearMeButton.setEnabled(false);

        // Desactivar el botón del menú
        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setEnabled(false);

        // Inicia la verificación periódica de la ubicación
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                startLocation = location; // Guardar la ubicación inicial
                checkUserProximity(location);
            } else {
            Toast.makeText(this, "No se pudo obtener la ubicación inicial.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserProximity(android.location.Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        double distanceToUPV = calculateDistance(userLocation, UPV_LOCATION);

        if (distanceToUPV <= 0.05) { // Si está a menos de 50 metros
            endGame();
        } else {
            // Continúa verificando la ubicación
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this::checkUserProximity);
        }
    }

    private void endGame() {
        if (startLocation == null) {
            Log.e("MapActivity", "Error: La ubicación inicial no está disponible.");
            Toast.makeText(this, "Error: No se pudo calcular la distancia.", Toast.LENGTH_SHORT).show();
            return;
        }

        isGameActive = false;
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime; // Tiempo en milisegundos

        // Calcular la distancia desde la ubicación inicial hasta la UPV/EHU
        double distanceToUPV = calculateDistance(
                new LatLng(startLocation.getLatitude(), startLocation.getLongitude()),
                UPV_LOCATION
        );

        // Puntuación basada en tiempo y distancia
        int basePoints = 1000; // Puntos base
        int timePenalty = (int) (elapsedTime / 1000); // Penalización por tiempo (1 punto por segundo)
        int distancePenalty = (int) ((100 - distanceToUPV) * 10); // Penalización por distancia (10 puntos por metro)

        int points = Math.max(basePoints - timePenalty - distancePenalty, 0); // Asegurar que no sea negativo
        Toast.makeText(this, "¡Has llegado a la UPV/EHU! Puntos obtenidos: " + points, Toast.LENGTH_LONG).show();

        // Rehabilitar interacciones
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        transportButton.setEnabled(true);
        layersButton.setEnabled(true);
        nearMeButton.setEnabled(true);

        // Reactivar el botón del menú
        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setEnabled(true);

        // Enviar puntos a la base de datos
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("id", -1); // Cambiado a "id" y valor por defecto -1
        if (userId != -1) {
            sendPointsToDatabase(String.valueOf(userId), points);
        } else {
            Log.e("MapActivity", "Error: ID de usuario no encontrado en SharedPreferences.");
        }
    }

    private void sendPointsToDatabase(String userId, int points) {
        new Thread(() -> {
            try {
                String url = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/puntos.php?id="
                        + userId + "&puntos=" + points;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d("MapActivity", "Puntos enviados correctamente: " + points);
                } else {
                    Log.e("MapActivity", "Error al enviar los puntos: " + response.message());
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error al enviar los puntos a la base de datos", e);
            }
        }).start();
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
        if (transportOptions.getVisibility() == View.VISIBLE) {
            transportOptions.setVisibility(View.GONE);
            return;
        }

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
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //| Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //| Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.Foro){
            Intent intent = new Intent(MapActivity.this, ForumActivity.class);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Foro);
            actExtras();
            intent.putExtra("nombre",nombre);
            intent.putExtra("email",email);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //| Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        else if (id == R.id.Opciones) {
            Intent intent = new Intent(MapActivity.this, AllActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //| Intent.FLAG_ACTIVITY_SINGLE_TOP);
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