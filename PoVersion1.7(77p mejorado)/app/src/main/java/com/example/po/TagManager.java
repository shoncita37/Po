package com.example.po;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagManager {
    private static final String TAG = "TagManager";
    private static final List<String> DEFAULT_TAGS = Arrays.asList("todos", "urgente", "familiar", "trabajo");
    
    private String userId;
    private List<String> availableTags;
    private DatabaseReference tagsRef;
    private Context context;
    
    public interface TagsLoadedListener {
        void onTagsLoaded(List<String> tags);
    }
    
    public TagManager(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.availableTags = new ArrayList<>();
        
        // Verificar que userId no sea nulo antes de usarlo
        if (userId != null) {
            this.tagsRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Tags");
        } else {
            // Usar una referencia predeterminada si userId es nulo
            this.tagsRef = FirebaseDatabase.getInstance().getReference("DefaultTags");
        }
        
        initializeDefaultTags();
    }
    
    private void initializeDefaultTags() {
        loadTags(tags -> {
            if (tags.isEmpty()) {
                // Primera vez que se ejecuta la app, inicializar con tags predeterminados
                availableTags = new ArrayList<>(DEFAULT_TAGS);
                saveTags();
            }
        });
    }
    
    public void loadTags(TagsLoadedListener listener) {
        tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableTags.clear();
                for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                    String tag = tagSnapshot.getValue(String.class);
                    if (tag != null) {
                        availableTags.add(tag);
                    }
                }
                
                // Asegurarse de que "todos" siempre esté en la lista
                if (!availableTags.contains("todos")) {
                    availableTags.add("todos");
                    saveTags();
                }
                
                if (listener != null) {
                    listener.onTagsLoaded(new ArrayList<>(availableTags));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar tags: " + error.getMessage());
                if (listener != null) {
                    listener.onTagsLoaded(new ArrayList<>(DEFAULT_TAGS));
                }
                Toast.makeText(context, "Error al cargar tags: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public List<String> getAllTags() {
        return new ArrayList<>(availableTags);
    }
    
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        if (!availableTags.contains(tag)) {
            availableTags.add(tag);
            saveTags();
        }
    }
    
    public void removeTag(String tag) {
        if (tag == null || tag.trim().isEmpty() || "todos".equals(tag)) {
            return; // No permitir eliminar el tag "todos"
        }
        
        if (availableTags.contains(tag)) {
            availableTags.remove(tag);
            saveTags();
        }
    }
    
    public void updateTag(String oldTag, String newTag) {
        if (oldTag == null || newTag == null || oldTag.trim().isEmpty() || newTag.trim().isEmpty()) {
            return;
        }
        
        if ("todos".equals(oldTag)) {
            return; // No permitir editar el tag "todos"
        }
        
        if (availableTags.contains(oldTag)) {
            availableTags.remove(oldTag);
            if (!availableTags.contains(newTag)) {
                availableTags.add(newTag);
            }
            saveTags();
        }
    }
    
    private void saveTags() {
        tagsRef.removeValue();
        for (int i = 0; i < availableTags.size(); i++) {
            tagsRef.child(String.valueOf(i)).setValue(availableTags.get(i));
        }
    }
    

    
    public void removeTagFromAllItems(String tag, TagDeletedListener listener) {
        if ("todos".equals(tag)) {
            if (listener != null) {
                listener.onTagDeleted(false);
            }
            return; // No permitir eliminar el tag "todos" de ningún elemento
        }
        
        removeTagFromEvents(tag, success -> {
            if (success) {
                removeTagFromProducts(tag, productSuccess -> {
                    removeTag(tag);
                    if (listener != null) {
                        listener.onTagDeleted(productSuccess);
                    }
                });
            } else if (listener != null) {
                listener.onTagDeleted(false);
            }
        });
    }
    
    private void removeTagFromEvents(String tag, TagDeletedListener listener) {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Events");
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean success = true;
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    try {
                        Evento evento = eventSnapshot.getValue(Evento.class);
                        if (evento != null && evento.getTags() != null && evento.getTags().contains(tag)) {
                            List<String> updatedTags = new ArrayList<>(evento.getTags());
                            updatedTags.remove(tag);
                            eventSnapshot.getRef().child("tags").setValue(updatedTags);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al actualizar tags del evento: " + e.getMessage());
                        success = false;
                    }
                }
                if (listener != null) {
                    listener.onTagDeleted(success);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al acceder a eventos: " + error.getMessage());
                if (listener != null) {
                    listener.onTagDeleted(false);
                }
            }
        });
    }
    
    private void removeTagFromProducts(String tag, TagDeletedListener listener) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Products");
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean success = true;
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    try {
                        Producto producto = productSnapshot.getValue(Producto.class);
                        if (producto != null && producto.getTags() != null && producto.getTags().contains(tag)) {
                            List<String> updatedTags = new ArrayList<>(producto.getTags());
                            updatedTags.remove(tag);
                            productSnapshot.getRef().child("tags").setValue(updatedTags);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al actualizar tags del producto: " + e.getMessage());
                        success = false;
                    }
                }
                if (listener != null) {
                    listener.onTagDeleted(success);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al acceder a productos: " + error.getMessage());
                if (listener != null) {
                    listener.onTagDeleted(false);
                }
            }
        });
    }
    
    public interface TagDeletedListener {
        void onTagDeleted(boolean success);
    }
    

    
    public void removeTagFromAllItems(Context context, String tag, String userId, TagDeletedListener listener) {
        if (tag == null || tag.equals("todos")) {
            if (listener != null) {
                listener.onTagDeleted(false);
            }
            return; // No se puede eliminar el tag "todos"
        }
        
        // Eliminar el tag de todos los eventos
        removeTagFromEvents(userId, tag, success -> {
            if (success) {
                // Luego eliminar el tag de todos los productos
                removeTagFromProducts(userId, tag, success2 -> {
                    if (success2) {
                        // Finalmente eliminar el tag de la lista de tags disponibles
                        removeTag(tag);
                        if (listener != null) {
                            listener.onTagDeleted(true);
                        }
                    } else {
                        if (listener != null) {
                            listener.onTagDeleted(false);
                        }
                    }
                });
            } else {
                if (listener != null) {
                    listener.onTagDeleted(false);
                }
            }
        });
    }
    
    private void removeTagFromEvents(String userId, String tag, TagDeletedListener listener) {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(userId).child("ListEvents");
        
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Evento evento = eventSnapshot.getValue(Evento.class);
                    if (evento != null && evento.getTags() != null && evento.getTags().contains(tag)) {
                        List<String> updatedTags = new ArrayList<>(evento.getTags());
                        updatedTags.remove(tag);
                        evento.setTags(updatedTags);
                        eventSnapshot.getRef().setValue(evento);
                    }
                }
                if (listener != null) {
                    listener.onTagDeleted(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TagManager", "Error al eliminar tag de eventos: " + error.getMessage());
                if (listener != null) {
                    listener.onTagDeleted(false);
                }
            }
        });
    }

    private void removeTagFromProducts(String userId, String tag, TagDeletedListener listener) {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(userId).child("ListEvents");
        
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pendingEvents = 0;
                
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    pendingEvents++;
                    DatabaseReference wishlistRef = eventSnapshot.getRef().child("listaDeseo");
                    
                    final int[] finalPendingEvents = {pendingEvents};
                    
                    wishlistRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot productsSnapshot) {
                            for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                                Producto producto = productSnapshot.getValue(Producto.class);
                                if (producto != null && producto.getTags() != null && producto.getTags().contains(tag)) {
                                    producto.removeTag(tag);
                                    productSnapshot.getRef().setValue(producto);
                                }
                            }
                            
                            finalPendingEvents[0]--;
                            if (finalPendingEvents[0] == 0 && listener != null) {
                                listener.onTagDeleted(true);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("TagManager", "Error al eliminar tag de productos: " + error.getMessage());
                            if (listener != null) {
                                listener.onTagDeleted(false);
                            }
                        }
                    });
                }
                
                // Si no hay eventos, llamar al listener inmediatamente
                if (pendingEvents == 0 && listener != null) {
                    listener.onTagDeleted(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TagManager", "Error al acceder a eventos para eliminar tag de productos: " + error.getMessage());
                if (listener != null) {
                    listener.onTagDeleted(false);
                }
            }
        });
    }
    

    

}