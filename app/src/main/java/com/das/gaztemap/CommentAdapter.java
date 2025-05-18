
package com.das.gaztemap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter";
    private static final String PROFILE_IMG_BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/pfps/";

    private Context context;
    private List<Comment> commentList;
    private int currentUserId;
    private String currentLanguageCode;
    private Map<Integer, Translator> translatorMap = new HashMap<>();

    public CommentAdapter(Context context, List<Comment> commentList, int currentUserId) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = currentUserId;

        //device language
        currentLanguageCode = Locale.getDefault().getLanguage();
        if (currentLanguageCode.equals("es")) {
            currentLanguageCode = TranslateLanguage.SPANISH;
        } else if (currentLanguageCode.equals("eu")) {
            currentLanguageCode = TranslateLanguage.SPANISH; // Euskera da problemas no incorporado en MLKIT puto google
        } else {
            currentLanguageCode = TranslateLanguage.ENGLISH;
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.textViewUserName.setText(comment.getUserName());
        holder.textViewContent.setText(comment.getContent());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                comment.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.textViewTimestamp.setText(timeAgo);

        if (Integer.parseInt(comment.getUserId()) == currentUserId) {
            holder.comentario.setBackgroundColor(context.getResources().getColor(R.color.azuladito));
        } else {
            holder.comentario.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        loadProfileImage(holder.imageViewProfile, comment.getUserId());

        holder.buttonTraduccion.setOnClickListener(v -> translateComment(comment, holder));

        holder.isTranslated = false;
        holder.originalContent = comment.getContent();
    }

    private void loadProfileImage(ShapeableImageView imageView, String userId) {
        if (userId == null || userId.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder);
            return;
        }

        String profileImageUrl = PROFILE_IMG_BASE_URL + userId + ".jpg";

        Glide.with(context)
                .load(profileImageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imageView);
    }

    private void translateComment(Comment comment, CommentViewHolder holder) {
        if (holder.isTranslated) {
            holder.textViewContent.setText(holder.originalContent);
            holder.buttonTraduccion.setText(context.getString(R.string.traduccion));
            holder.isTranslated = false;
            return;
        }

        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        holder.buttonTraduccion.setText(context.getString(R.string.traduciendo));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String preferredLanguage = prefs.getString("idioma", "ES");

        if (preferredLanguage.equals("1")) {
            preferredLanguage = Locale.getDefault().getLanguage();
        }

        String targetLanguage;
        switch (preferredLanguage.toUpperCase()) {
            case "ES":
                targetLanguage = TranslateLanguage.SPANISH;
                break;
            case "EU":
                targetLanguage = TranslateLanguage.SPANISH; // Euskera no soportado
                break;
            case "EN":
                targetLanguage = TranslateLanguage.ENGLISH;
                break;
            default:
                targetLanguage = TranslateLanguage.ENGLISH;
        }

        final String textToTranslate = comment.getContent();
        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(textToTranslate)
                .addOnSuccessListener(sourceLanguageCode -> {
                    Log.d(TAG, "Idioma detectado: " + sourceLanguageCode);

                    if (sourceLanguageCode.equals("und") || !isLanguageSupported(sourceLanguageCode)) {
                        sourceLanguageCode = TranslateLanguage.SPANISH;
                Log.w(TAG, "Idioma no soportado o indetectable. Usando Español por defecto.");
                    }

                    if (sourceLanguageCode.equals(targetLanguage)) {
                        Toast.makeText(context, "El texto ya está en el idioma objetivo", Toast.LENGTH_SHORT).show();
                        holder.buttonTraduccion.setText(context.getString(R.string.traduccion));
                        return;
                    }

                    Translator translator = getTranslator(sourceLanguageCode, targetLanguage);
                    translator.downloadModelIfNeeded()
                            .addOnSuccessListener(unused -> {
                                // Modelo descargado
                                translator.translate(textToTranslate)
                                        .addOnSuccessListener(translatedText -> {
                                            Log.d(TAG, "Translation successful. Translated text: " + translatedText);
                                            holder.textViewContent.setText(translatedText);
                                            holder.buttonTraduccion.setText(context.getString(R.string.ver_original)); // Or a suitable text like "View Original"
                                            holder.isTranslated = true;
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error traducción: " + e.getMessage());
                                            Toast.makeText(context, "Error al traducir texto", Toast.LENGTH_SHORT).show();
                                            holder.buttonTraduccion.setText(context.getString(R.string.traduccion)); // Reset button text on failure
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error descarga modelo: " + e.getMessage());
                                Toast.makeText(context, "Error descargando modelo. Verifica tu conexión a Internet", Toast.LENGTH_LONG).show();
                                holder.buttonTraduccion.setText(context.getString(R.string.traduccion)); // Reset button text on failure
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error detección idioma", e);
                    Toast.makeText(context, "Error detectando idioma", Toast.LENGTH_SHORT).show();
                    holder.buttonTraduccion.setText(context.getString(R.string.traduccion)); // Reset button text on failure
                });
    }
    private boolean isLanguageSupported(String languageCode) {
        Set<String> supportedLanguages = new HashSet<>(TranslateLanguage.getAllLanguages());
        return supportedLanguages.contains(languageCode);
    }



    private Translator getTranslator(String sourceLanguage, String targetLanguage) {
        int key = (sourceLanguage + targetLanguage).hashCode();

        if (!translatorMap.containsKey(key)) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build();
            translatorMap.put(key, Translation.getClient(options));
        }

        return translatorMap.get(key);
    }

    public void closeTanslators() {
        for (Translator translator : translatorMap.values()) {
            translator.close();
        }
        translatorMap.clear();
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName, textViewContent, textViewTimestamp;
        ShapeableImageView imageViewProfile;
        MaterialButton buttonTraduccion;
        LinearLayout comentario;
        boolean isTranslated = false;
        String originalContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            buttonTraduccion = itemView.findViewById(R.id.buttonTraduccion);
            comentario = itemView.findViewById(R.id.comentario);
        }
    }
}