package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewComments;
    private TextInputEditText editTextComment;
    private FloatingActionButton fabSendComment;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    private int currentUserId;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comentarios);

        postId = getIntent().getStringExtra("POST_ID");
        String postContent = getIntent().getStringExtra("POST_CONTENT");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentUserId = prefs.getInt("id", 0);

        inicializarVistas();
        cargarComentarios();
    }

    private void inicializarVistas() {
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editTextComment = findViewById(R.id.editTextComment);
        fabSendComment = findViewById(R.id.fabSendComment);

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);

        fabSendComment.setOnClickListener(v -> enviarComentario());
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