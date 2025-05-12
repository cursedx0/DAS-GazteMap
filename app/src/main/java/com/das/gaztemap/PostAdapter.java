package com.das.gaztemap;

import static com.das.gaztemap.ForumActivity.BASE_URL;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private DatabaseReference mDatabase;
    private int currentUserId;

    public PostAdapter(Context context, List<Post> postList, int currentUserId) {
        this.context = context;
        this.postList = postList;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewUsername.setText(post.getUserName());
        holder.textViewContent.setText(post.getContent());

        //tiempo relativo (ej: "hace 5 minutos")
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                post.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.textViewTimestamp.setText(timeAgo);

        holder.textViewLikes.setText(String.valueOf(post.getLikeCount()) + " likes");
        holder.textViewComments.setText(String.valueOf(post.getCommentCount()) + " comentarios");

        boolean isMyPost = String.valueOf(currentUserId).equals(post.getUserId());
        holder.buttonMore.setVisibility(isMyPost ? View.VISIBLE : View.INVISIBLE);

        updateLikeButton(holder.buttonLike, post.isLiked());

        holder.buttonLike.setOnClickListener(v -> {
            likePost(post, position);
            post.setLiked(!post.isLiked());
            updateLikeButton(holder.buttonLike, post.isLiked());
        });
        holder.buttonComment.setOnClickListener(view -> openComments(post));
        holder.buttonShare.setOnClickListener(view -> sharePost(post));

        //  el post del usuario actual, agregar opción para eliminar
        if (isMyPost) {
            holder.buttonMore.setOnClickListener(view -> {
                // mostrar un PopupMenu con opciones como eliminar o editar
                deletePost(post);
            });
        }
    }
    private void updateLikeButton(MaterialButton button, boolean isLiked) {
        button.setIconResource(isLiked ? R.drawable.likefull : R.drawable.like);
        button.setIconTint(ColorStateList.valueOf(
                isLiked ? ContextCompat.getColor(context, R.color.azul_600)
                        : ContextCompat.getColor(context, R.color.azul_800)
        ));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUsername, textViewContent, textViewTimestamp;
        TextView textViewLikes, textViewComments;
        com.google.android.material.button.MaterialButton buttonLike, buttonComment, buttonShare, buttonMore;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            textViewLikes = itemView.findViewById(R.id.textViewLikes);
            textViewComments = itemView.findViewById(R.id.textViewComments);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }

    private void likePost(Post post, int position) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "dar_like");
            jsonBody.put("post_id", post.getPostId());
            jsonBody.put("user_id", currentUserId);
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
                            int newLikeCount = response.getInt("newCount");
                            boolean isLiked = response.getBoolean("isLiked");

                            post.setLikeCount(newLikeCount);
                            post.setLiked(isLiked);

                            notifyItemChanged(position);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error en la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                    // Opcional: Revertir el cambio visual si falla
                    post.setLiked(!post.isLiked());
                    notifyItemChanged(position);
                }
        );

        Volley.newRequestQueue(context).add(request);
    }

    private void openComments(Post post) {
        Intent intent = new Intent(context, CommentsActivity.class);
        intent.putExtra("POST_ID", post.getPostId());
        intent.putExtra("POST_CONTENT", post.getContent());
        context.startActivity(intent);
    }

    private void sharePost(Post post) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                post.getUserName() + " publicó en el foro:\n\n" +
                        post.getContent());
        shareIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(shareIntent, "Compartir publicación"));
    }

    // solo si el usuario es el autor
    private void deletePost(Post post) {
        if (String.valueOf(currentUserId).equals(post.getUserId())) {
            mDatabase.child("posts").child(post.getPostId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(context, "Solo puedes eliminar tus propias publicaciones", Toast.LENGTH_SHORT).show();
        }
    }
}