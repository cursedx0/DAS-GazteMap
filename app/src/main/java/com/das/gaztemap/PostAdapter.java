package com.das.gaztemap;

import static com.das.gaztemap.ForumActivity.BASE_URL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private int currentUserId;

    public PostAdapter(Context context, List<Post> postList, int currentUserId) {
        this.context = context;
        this.postList = postList;
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
        holder.textViewLikes.setText(String.valueOf(post.getLikeCount()) +" "+ context.getString(R.string.likes));
        holder.textViewComments.setText(String.valueOf(post.getCommentCount()) +" "+ context.getString(R.string.comentarios));

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
                showOptionsMenu(post, position, holder.buttonMore);
            });
        }
        String userAvatarUrl = post.getProfileImageUrl();
        if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
            holder.imageViewUserAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getProfileImageUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imageViewUserAvatar);
            Log.d("fafafageg","afegqgq");
        } else {
            holder.imageViewUserAvatar.setVisibility(View.GONE);
            Log.d("faffafgafageg", "fqegqetg");
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
        public ImageView imageViewUserAvatar;
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
            imageViewUserAvatar = itemView.findViewById(R.id.imageViewUserAvatar);
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

    private void editPost(Post post, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar publicación");

        EditText input = new EditText(context);
        input.setText(post.getContent());
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newContent = input.getText().toString().trim();
            if(!newContent.isEmpty()) {
                updatePostContent(post, newContent, position);
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performDeletePost(Post post, int position) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "eliminar_hilo");
            jsonBody.put("post_id", post.getPostId());
            jsonBody.put("user_id", currentUserId);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al crear la solicitud", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                jsonBody,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            // Eliminar de la lista local
                            postList.remove(position);
                            // Notificar al adaptador
                            notifyItemRemoved(position);
                            // Actualizar posiciones de los elementos siguientes
                            notifyItemRangeChanged(position, postList.size());
                            Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = response.getString("message");
                            Toast.makeText(context, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                    post.setLiked(!post.isLiked());
                    notifyItemChanged(position);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 segundos timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(context).add(request);
    }

    private void deletePost(Post post, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Eliminar publicación")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Eliminar", (dialog, which) -> performDeletePost(post, position))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showOptionsMenu(Post post, int position, View anchorView) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.inflate(R.menu.post_options);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_edit) {
                editPost(post, position);
                return true;
            } else if (item.getItemId() == R.id.menu_delete) {
                deletePost(post, position);
                return true;
            }
            return false;
        });
        popup.show();
    }
    private void updatePostContent(Post post, String newContent, int position) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("accion", "editar_hilo");
            jsonBody.put("post_id", post.getPostId());
            jsonBody.put("user_id", currentUserId);
            jsonBody.put("contenido", newContent);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al crear la solicitud", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                jsonBody,
                response -> {
                    try {
                        if (response.getString("status").equals("success")) {
                            post.setContent(newContent);
                            notifyItemChanged(position);
                            Toast.makeText(context, "Publicación actualizada", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = response.getString("message");
                            Toast.makeText(context, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                    notifyItemChanged(position);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(context).add(request);
    }
}