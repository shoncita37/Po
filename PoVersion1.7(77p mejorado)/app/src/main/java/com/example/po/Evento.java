package com.example.po;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Evento implements Parcelable {
    private String fecha;
    private String nombre;
    private String notas;
    private String id;
    private List<Producto> listaDeseo;  // lista de productos
    private int tipoRecordatorio; // 0: mismo día, 1: 1 día antes, 2: 3 días antes, 3: 1 semana antes, 4: 2 semanas antes
    private List<String> tags; // Lista de tags para el evento
    private List<String> encargados; // Encargados asignados al evento (nombres)
    private List<String> clientes;   // Clientes asociados al evento (nombres)

    public Evento() {
        this.listaDeseo = new ArrayList<>(); // evita null
        this.tipoRecordatorio = 0; // Por defecto, el mismo día
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }

    public Evento(String fecha, String nombre, String notas, List<Producto> listaDeseo) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = listaDeseo;
        this.id = "NN";
        this.tipoRecordatorio = 0; // Por defecto, el mismo día
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }
    
    public Evento(String fecha, String nombre, String notas, List<Producto> listaDeseo, String id) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = listaDeseo;
        this.id = id;
        this.tipoRecordatorio = 0; // Por defecto, el mismo día
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }
    
    public Evento(String fecha, String nombre, String notas) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = new ArrayList<>();
        this.id = "NN";
        this.tipoRecordatorio = 0; // Por defecto, el mismo día
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }
    
    public Evento(String fecha, String nombre, String notas, int tipoRecordatorio) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = new ArrayList<>();
        this.id = "NN";
        this.tipoRecordatorio = tipoRecordatorio;
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }
    
    public Evento(String fecha, String nombre, String notas, List<Producto> listaDeseo, int tipoRecordatorio) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = listaDeseo;
        this.id = "NN";
        this.tipoRecordatorio = tipoRecordatorio;
        this.tags = new ArrayList<>(Arrays.asList("todos")); // Por defecto, se agrega el tag "todos"
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }
    
    public Evento(String fecha, String nombre, String notas, List<Producto> listaDeseo, int tipoRecordatorio, List<String> tags) {
        this.fecha = fecha;
        this.nombre = nombre;
        this.notas = notas;
        this.listaDeseo = listaDeseo;
        this.id = "NN";
        this.tipoRecordatorio = tipoRecordatorio;
        this.tags = tags != null ? tags : new ArrayList<>(Arrays.asList("todos"));
        if (!this.tags.contains("todos")) {
            this.tags.add("todos");
        }
        this.encargados = new ArrayList<>();
        this.clientes = new ArrayList<>();
    }

    public String getFecha() {
        return fecha;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<Producto> getListaDeseo() {
        return listaDeseo;
    }

    public void setListaDeseo(List<Producto> listaDeseo) {
        this.listaDeseo = (listaDeseo != null) ? listaDeseo : new ArrayList<>();
    }
    
    public int getTipoRecordatorio() {
        return tipoRecordatorio;
    }
    
    public void setTipoRecordatorio(int tipoRecordatorio) {
        this.tipoRecordatorio = tipoRecordatorio;
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

    public List<String> getEncargados() {
        return encargados;
    }

    public void setEncargados(List<String> encargados) {
        this.encargados = (encargados != null) ? encargados : new ArrayList<>();
    }

    public List<String> getClientes() {
        return clientes;
    }

    public void setClientes(List<String> clientes) {
        this.clientes = (clientes != null) ? clientes : new ArrayList<>();
    }

    protected Evento(Parcel in) {
        fecha = in.readString();
        nombre = in.readString();
        notas = in.readString();
        listaDeseo = in.createTypedArrayList(Producto.CREATOR); // lee lista
        if (listaDeseo == null) {
            listaDeseo = new ArrayList<>();
        }
        id = in.readString();
        tipoRecordatorio = in.readInt();
        tags = in.createStringArrayList(); // lee lista de tags
        if (tags == null) {
            tags = new ArrayList<>(Arrays.asList("todos"));
        } else if (tags.isEmpty()) {
            tags.add("todos");
        }
        encargados = in.createStringArrayList();
        if (encargados == null) {
            encargados = new ArrayList<>();
        }
        clientes = in.createStringArrayList();
        if (clientes == null) {
            clientes = new ArrayList<>();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fecha);
        dest.writeString(nombre);
        dest.writeString(notas);
        dest.writeTypedList(listaDeseo); // escribe lista
        dest.writeString(id);
        dest.writeInt(tipoRecordatorio);
        dest.writeStringList(tags); // escribe lista de tags
        dest.writeStringList(encargados);
        dest.writeStringList(clientes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Evento> CREATOR = new Creator<Evento>() {
        @Override
        public Evento createFromParcel(Parcel in) {
            return new Evento(in);
        }

        @Override
        public Evento[] newArray(int size) {
            return new Evento[size];
        }
    };
}


