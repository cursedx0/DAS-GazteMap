package com.example.gaztemap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BDConnector extends Worker {

    public BDConnector(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //--PARÁMETROS--\\
        String direccion = "";
        if(getInputData().getString("url")!=null){
            if(getInputData().getString("url").equals("0")){
                Log.d("WORKER", "Usando url por defecto.");
                direccion = "http://51.44.167.78:80/lbilbao040/WEB/GazteMap/api.php"; //POR DEFECTO
            }else if(getInputData().getString("url").equals("1")){
                direccion = "http://51.44.167.78:80/lbilbao040/WEB/GazteMap/amigos.php"; //monedas
            }else if(getInputData().getString("url").equals("2")){
                direccion = "http://51.44.167.78:80/lbilbao040/WEB/GazteMap/pfp.php"; //monedas
            }else {
                direccion = getInputData().getString("url");
            }
        }else{
            Log.d("WORKER", "Usando url por defecto.");
            direccion = "http://51.44.167.78:80/lbilbao040/WEB/GazteMap/api.php"; //POR DEFECTO
        }

        if(getInputData().getString("accion")!=null){ //código función
            String accion = getInputData().getString("accion");
            boolean paramsValidos = false;
            switch (accion){
                case "insertar": //crear usuario
                    //POST, parametros etc
                    HttpURLConnection urlConnection = null;
                    URL destino = null;
                    try {
                        destino = new URL(direccion);
                        urlConnection = (HttpURLConnection) destino.openConnection();
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.setReadTimeout(5000);
                        urlConnection.setRequestMethod("POST");
                        urlConnection.setDoOutput(true);
                        urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("usuario")!=null && getInputData().getString("pw")!=null && getInputData().getString("email")!=null){
                            jsonParam.put("usuario", getInputData().getString("usuario"));
                            jsonParam.put("pw", getInputData().getString("pw"));
                            jsonParam.put("email", getInputData().getString("email"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnection.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnection.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "login": //verificar usuario, para inicio de sesión
                    //POST
                    HttpURLConnection urlConnectionLogin = null;
                    URL destinoLogin = null;
                    try {
                        destinoLogin = new URL(direccion);
                        urlConnectionLogin = (HttpURLConnection) destinoLogin.openConnection();
                        urlConnectionLogin.setConnectTimeout(5000);
                        urlConnectionLogin.setReadTimeout(5000);
                        urlConnectionLogin.setRequestMethod("POST");
                        urlConnectionLogin.setDoOutput(true);
                        urlConnectionLogin.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("email")!=null && getInputData().getString("pw")!=null){
                            jsonParam.put("email", getInputData().getString("email"));
                            jsonParam.put("pw", getInputData().getString("pw"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionLogin.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionLogin.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionLogin.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                int id = respuestaJson.optInt("id", -1);
                                String nombre = respuestaJson.optString("nombre", "error");
                                int puntos = respuestaJson.optInt("puntos", 0);

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putInt("id", id)
                                        .putString("nombre", nombre)
                                        .putInt("puntos", puntos)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "eliminar": //borrar usuario
                    //POST
                    break;
                case "info": //verificar usuario, para inicio de sesión
                    //POST
                    HttpURLConnection urlConnectionInfo = null;
                    URL destinoInfo = null;
                    try {
                        destinoInfo = new URL(direccion);
                        urlConnectionLogin = (HttpURLConnection) destinoInfo.openConnection();
                        urlConnectionLogin.setConnectTimeout(5000);
                        urlConnectionLogin.setReadTimeout(5000);
                        urlConnectionLogin.setRequestMethod("POST");
                        urlConnectionLogin.setDoOutput(true);
                        urlConnectionLogin.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("usuario")!=null){
                            jsonParam.put("usuario", getInputData().getString("usuario"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionLogin.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionLogin.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionLogin.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                String nombre = respuestaJson.optString("nombre", "error");
                                int monedas = respuestaJson.optInt("monedas", 0);
                                int victs = respuestaJson.optInt("victs", 0);
                                int derrs = respuestaJson.optInt("derrs", 0);
                                int empts = respuestaJson.optInt("empts", 0);

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putString("nombre", nombre)
                                        .putInt("monedas", monedas)
                                        .putInt("victs", victs)
                                        .putInt("derrs", derrs)
                                        .putInt("empts", empts)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break

                case "amigos": //verificar usuario, para inicio de sesión
                    //POST
                    HttpURLConnection urlConnectionAmigos = null;
                    URL destinoAmigos = null;
                    try {
                        destinoAmigos = new URL(direccion);
                        urlConnectionLogin = (HttpURLConnection) destinoAmigos.openConnection();
                        urlConnectionLogin.setConnectTimeout(5000);
                        urlConnectionLogin.setReadTimeout(5000);
                        urlConnectionLogin.setRequestMethod("POST");
                        urlConnectionLogin.setDoOutput(true);
                        urlConnectionLogin.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("usuario")!=null){
                            jsonParam.put("usuario", getInputData().getString("usuario"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionLogin.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionLogin.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionLogin.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                String nombre = respuestaJson.optString("nombre", "error");
                                JSONArray amigosArray = respuestaJson.optJSONArray("amigos");

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putString("nombre", nombre)
                                        .putString("amigos_json",amigosArray != null ? amigosArray.toString() : "[]")
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break

                case "monedas":
                    HttpURLConnection urlConnectionMonedas = null;
                    URL destinoMonedas = null;
                    try {
                        destinoMonedas = new URL(direccion);
                        urlConnectionMonedas = (HttpURLConnection) destinoMonedas.openConnection();
                        urlConnectionMonedas.setConnectTimeout(5000);
                        urlConnectionMonedas.setReadTimeout(5000);
                        urlConnectionMonedas.setRequestMethod("POST");
                        urlConnectionMonedas.setDoOutput(true);
                        urlConnectionMonedas.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        // Crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("usuario")!=null){
                            jsonParam.put("usuario", getInputData().getString("usuario"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionMonedas.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionMonedas.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionMonedas.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                String nombre = respuestaJson.optString("nombre", "error");
                                int monedas = respuestaJson.optInt("monedas", 0);

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putString("nombre", nombre)
                                        .putInt("monedas", monedas)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "sumar":
                    HttpURLConnection urlConnectionSumar = null;
                    URL destinoSumar = null;
                    try {
                        destinoSumar = new URL(direccion);
                        urlConnectionSumar = (HttpURLConnection) destinoSumar.openConnection();
                        urlConnectionSumar.setConnectTimeout(5000);
                        urlConnectionSumar.setReadTimeout(5000);
                        urlConnectionSumar.setRequestMethod("POST");
                        urlConnectionSumar.setDoOutput(true);
                        urlConnectionSumar.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getInt("id",0)!=0 && getInputData().getInt("monedas",0)!=0){
                            jsonParam.put("id", getInputData().getInt("id",0));
                            jsonParam.put("monedas", getInputData().getInt("monedas",0));
                            if(getInputData().getString("empate")!=null){
                                jsonParam.put("empate", getInputData().getString("empate"));
                            }
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionSumar.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionSumar.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionSumar.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                int id = respuestaJson.optInt("id", 0);
                                int monedas = respuestaJson.optInt("monedas", 0);

                                Log.d("RESPUESTA", response.toString()); // Imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putInt("id", id)
                                        .putInt("monedas", monedas)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "restar":
                    HttpURLConnection urlConnectionRestar = null;
                    URL destinoRestar = null;
                    try {
                        destinoRestar = new URL(direccion);
                        urlConnectionRestar = (HttpURLConnection) destinoRestar.openConnection();
                        urlConnectionRestar.setConnectTimeout(5000);
                        urlConnectionRestar.setReadTimeout(5000);
                        urlConnectionRestar.setRequestMethod("POST");
                        urlConnectionRestar.setDoOutput(true);
                        urlConnectionRestar.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getInt("id",0)!=0 && getInputData().getInt("monedas",0)!=0){
                            jsonParam.put("id", getInputData().getInt("id",0));
                            jsonParam.put("monedas", getInputData().getInt("monedas",0));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionRestar.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionRestar.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionRestar.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                int id = respuestaJson.optInt("id", 0);
                                int monedas = respuestaJson.optInt("monedas", 0);

                                Log.d("RESPUESTA", response.toString()); //imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putInt("id", id)
                                        .putInt("monedas", monedas)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "setpfp":
                    HttpURLConnection urlConnectionSetPfp = null;
                    URL destinoSetPfp = null;
                    try {
                        destinoSetPfp = new URL(direccion);
                        urlConnectionSetPfp = (HttpURLConnection) destinoSetPfp.openConnection();
                        urlConnectionSetPfp.setConnectTimeout(5000);
                        urlConnectionSetPfp.setReadTimeout(5000);
                        urlConnectionSetPfp.setRequestMethod("POST");
                        urlConnectionSetPfp.setDoOutput(true);
                        urlConnectionSetPfp.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        //crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getInt("id",0)!=0 && getInputData().getString("pic")!=null){
                            String imagePath = getInputData().getString("pic");
                            File imageFile = new File(imagePath);
                            Log.d("WORKER","11111111111111");
                            if (imageFile.exists()) {
                                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] imageBytes = stream.toByteArray();
                                Log.d("WORKER","22222222222222");
                                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                                Log.d("WORKER","3333333333333");
                                jsonParam.put("id", getInputData().getInt("id", 0));
                                jsonParam.put("pic", base64Image);
                                paramsValidos = true;
                                Log.d("WORKER","4444444444444");
                            }
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionSetPfp.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionSetPfp.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionSetPfp.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                String url = respuestaJson.optString("url", null);

                                Log.d("RESPUESTA", response.toString()); //imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        .putString("url", url)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
                case "getpfp":
                    HttpURLConnection urlConnectionGetPfp = null;
                    URL destinoGetPfp = null;
                    try {
                        destinoGetPfp = new URL(direccion);
                        urlConnectionGetPfp = (HttpURLConnection) destinoGetPfp.openConnection();
                        urlConnectionGetPfp.setConnectTimeout(5000);
                        urlConnectionGetPfp.setReadTimeout(5000);
                        urlConnectionGetPfp.setRequestMethod("POST");
                        urlConnectionGetPfp.setDoOutput(true);
                        urlConnectionGetPfp.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        // Crear JSON con los parámetros
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("accion", accion);

                        if(getInputData().getString("nombre")!=null){
                            jsonParam.put("nombre", getInputData().getString("nombre"));
                            paramsValidos = true;
                        }

                        Log.d("WORKER", "JSON definido");
                        Log.d("WORKER", "JSON a enviar: " + jsonParam.toString());
                        if (paramsValidos) {
                            //escribir el JSON en el cuerpo de la solicitud
                            OutputStream os = urlConnectionGetPfp.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(jsonParam.toString());
                            writer.flush();
                            writer.close();
                            os.close();

                            //enviar la solicitud y recibir la respuesta
                            int responseCode = urlConnectionGetPfp.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnectionGetPfp.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                //parsear respuesta JSON
                                JSONObject respuestaJson = new JSONObject(response.toString());
                                String mensaje = respuestaJson.optString("message", "Sin mensaje");
                                String codigo = respuestaJson.optString("code", "-1");
                                String imagen = respuestaJson.optString("imagen",null);
                                String url = respuestaJson.optString("url","Sin url.");
                                int id = respuestaJson.optInt("id", 0);

                                Log.d("RESPUESTA", response.toString()); //imprimir respuesta del servidor
                                return Result.success(new Data.Builder()
                                        .putString("message", mensaje)
                                        .putString("code", codigo)
                                        //.putString("imagen",imagen) //evitado por problemas de overflow
                                        .putString("url", url)
                                        .build());
                            } else {
                                Log.e("ERROR", "Error en la solicitud: " + responseCode);
                                return Result.failure();
                            }
                        }else{
                            return Result.failure();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e("WORKER", "Excepción en doWork: " + e.getMessage(), e);
                        return Result.failure(new Data.Builder()
                                .putString("message", "Excepción: " + e.getMessage())
                                .putString("code", "-1")
                                .build());
                    } //break
            }
        }
        return null;
    }
}

