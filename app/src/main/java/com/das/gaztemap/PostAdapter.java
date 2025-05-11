package com.das.gaztemap;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        // Verificar si el post pertenece al usuario actual para habilitar opciones adicionales
        boolean isMyPost = String.valueOf(currentUserId).equals(post.getUserId());
        holder.buttonMore.setVisibility(isMyPost ? View.VISIBLE : View.INVISIBLE);

        holder.buttonLike.setOnClickListener(view -> likePost(post));
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

    private void likePost(Post post) {
        String userId = String.valueOf(currentUserId);
        DatabaseReference likesRef = mDatabase.child("post-likes")
                .child(post.getPostId()).child(userId);

        likesRef.setValue(true)
                .addOnSuccessListener(aVoid -> {
                    int newLikeCount = post.getLikeCount() + 1;
                    mDatabase.child("posts").child(post.getPostId())
                            .child("likeCount").setValue(newLikeCount);

                    Toast.makeText(context, "Te gusta esta publicación", Toast.LENGTH_SHORT).show();
                });
    }

    private void openComments(Post post) {
        Toast.makeText(context, "BOMBARDIRO CROCODILO", Toast.LENGTH_SHORT).show();
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