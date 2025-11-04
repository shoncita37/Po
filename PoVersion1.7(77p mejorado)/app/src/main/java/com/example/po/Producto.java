package com.example.po;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Producto implements Parcelable {
    private String titulo;
    private String url;
    private List<String> tags;

    public Producto() {
        this.tags = new ArrayList<>(Arrays.asList("todos"));
    }


    public Producto(String titulo, String url) {
        this.titulo = titulo;
        this.url = url;
        this.tags = new ArrayList<>(Arrays.asList("todos"));
    }
    
    public Producto(String titulo, String url, List<String> tags) {
        this.titulo = titulo;
        this.url = url;
        this.tags = tags != null ? tags : new ArrayList<>(Arrays.asList("todos"));
        if (!this.tags.contains("todos")) {
            this.tags.add("todos");
        }
    }

    public String getTitulo() {
        return titulo;
    }
    public String getUrl() {
        return url;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>(Arrays.asList("todos"));
        if (!this.tags.contains("todos")) {
            this.tags.add("todos");
        }
    }
    
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>(Arrays.asList("todos"));
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }
    
    public void removeTag(String tag) {
        if (this.tags != null && !tag.equals("todos")) {
            this.tags.remove(tag);
        }
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }


    protected Producto(Parcel in) {
        titulo = in.readString();
        url = in.readString();
        tags = new ArrayList<>();
        in.readStringList(tags);
        if (tags.isEmpty() || !tags.contains("todos")) {
            tags.add("todos");
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(titulo);
        dest.writeString(url);
        dest.writeStringList(tags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Producto> CREATOR = new Creator<Producto>() {
        @Override
        public Producto createFromParcel(Parcel in) {
            return new Producto(in);
        }

        @Override
        public Producto[] newArray(int size) {
            return new Producto[size];
        }
    };
}