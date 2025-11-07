package com.example.po;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para exportar los datos de Empresa a CSV.
 */
public class CsvExporter {

    public interface ExportCallback {
        void onSuccess(String filePath, Uri fileUri);
        void onError(String message);
    }

    public interface ImportCallback {
        void onSuccess(int entidadesCount, int eventosCount, int productosCount);
        void onError(String message);
    }

    public static void exportEmpresaToCsv(Context context, String idUser, ExportCallback callback) {
        if (idUser == null || idUser.isEmpty()) {
            callback.onError("idUser inválido");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("user");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean business = snapshot.child("business").getValue(Boolean.class);
                String userName = snapshot.child("name").getValue(String.class);

                if (!Boolean.TRUE.equals(business)) {
                    callback.onError("El usuario no es empresa (business=false)");
                    return;
                }

                // Leer el nodo Empresa completo
                DatabaseReference empresaRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(idUser)
                        .child("Empresa");

                empresaRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot empresaSnap) {
                        // Preparar lectura de eventos también
                        DatabaseReference eventosRef = FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(idUser)
                                .child("ListEvents");

                        eventosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot eventosSnap) {
                                List<String[]> rows = new ArrayList<>();
                                // Encabezado unificado con separador ';'
                                rows.add(new String[]{
                                        "Kind", // empresa|evento|producto
                                        "TipoEntidad",
                                        "NombreEntidad",
                                        "EmailEntidad",
                                        "TagsEntidad",
                                        "EventoId",
                                        "EventoFecha",
                                        "EventoNombre",
                                        "EventoNotas",
                                        "EventoTags",
                                        "TipoRecordatorio",
                                        "ProductoTitulo",
                                        "ProductoUrl",
                                        "ProductoTags",
                                        "ParentEventoId"
                                });

                                // Empresa: clientes, encargados, trabajadores
                                agregarFilasEmpresa(empresaSnap.child("clientes"), "cliente", rows);
                                agregarFilasEmpresa(empresaSnap.child("encargados"), "encargado", rows);
                                agregarFilasEmpresa(empresaSnap.child("trabajadores"), "trabajador", rows);

                                // Eventos y productos
                                for (DataSnapshot eSnap : eventosSnap.getChildren()) {
                                    Evento evento = eSnap.getValue(Evento.class);
                                    String eid = eSnap.getKey();
                                    if (evento != null) {
                                        // Fila evento
                                        rows.add(new String[]{
                                                "evento",
                                                "",
                                                "",
                                                "",
                                                "",
                                                safe(eid),
                                                safe(evento.getFecha()),
                                                safe(evento.getNombre()),
                                                safe(evento.getNotas()),
                                                joinTags(evento.getTags()),
                                                String.valueOf(evento.getTipoRecordatorio()),
                                                "",
                                                "",
                                                "",
                                                ""
                                        });

                                        // Productos
                                        List<Producto> productos = evento.getListaDeseo();
                                        if (productos != null) {
                                            for (Producto p : productos) {
                                                rows.add(new String[]{
                                                        "producto",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        "",
                                                        safe(p.getTitulo()),
                                                        safe(p.getUrl()),
                                                        joinTags(p.getTags()),
                                                        safe(eid)
                                                });
                                            }
                                        }
                                    }
                                }

                                // Generar CSV con ';'
                                StringBuilder sb = new StringBuilder();
                                sb.append("# UserId:").append(idUser).append("; UserName:").append(safe(userName)).append("\n");
                                for (String[] r : rows) {
                                    for (int i = 0; i < r.length; i++) {
                                        sb.append(csvEscape(r[i]));
                                        if (i < r.length - 1) sb.append(";");
                                    }
                                    sb.append("\n");
                                }

                                // Guardar archivo
                                try {
                                    File dir = context.getExternalFilesDir(null);
                                    if (dir == null) throw new IllegalStateException("Directorio externo no disponible");
                                    File out = new File(dir, "empresa_y_eventos_" + idUser + ".csv");
                                    try (FileOutputStream fos = new FileOutputStream(out);
                                         OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                                        osw.write(sb.toString());
                                        osw.flush();
                                    }
                                    callback.onSuccess(out.getAbsolutePath(), Uri.fromFile(out));
                                } catch (Exception e) {
                                    Log.e("CsvExporter", "Error guardando CSV", e);
                                    callback.onError("Error guardando CSV: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                callback.onError("Error leyendo eventos: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error leyendo Empresa: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error leyendo usuario: " + error.getMessage());
            }
        });
    }

    private static void agregarFilasEmpresa(DataSnapshot listaSnap, String tipoPorDefecto, List<String[]> rows) {
        if (listaSnap == null || !listaSnap.exists()) return;
        for (DataSnapshot child : listaSnap.getChildren()) {
            Entidad entidad = child.getValue(Entidad.class);
            if (entidad == null) continue;
            String tipo = entidad.getTipo() != null ? entidad.getTipo() : tipoPorDefecto;
            String nombre = safe(entidad.getNombre());
            String email = safe(entidad.getEmail());
            String tags = entidad.getTags() != null && !entidad.getTags().isEmpty()
                    ? String.join(";", entidad.getTags())
                    : "";
            rows.add(new String[]{
                    "empresa",
                    tipo,
                    nombre,
                    email,
                    tags,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            });
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        boolean needQuotes = value.contains(";") || value.contains("\n") || value.contains("\r") || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        return needQuotes ? "\"" + escaped + "\"" : escaped;
    }

    public static void importFromCsv(Context context, String idUser, Uri csvUri, ImportCallback callback) {
        if (idUser == null || idUser.isEmpty()) {
            callback.onError("idUser inválido");
            return;
        }
        try {
            java.io.InputStream is = context.getContentResolver().openInputStream(csvUri);
            if (is == null) throw new IllegalStateException("No se pudo abrir el archivo CSV");
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
            br.close();

            if (lines.isEmpty()) {
                callback.onError("CSV vacío");
                return;
            }

            // Header
            String[] header = splitCsvLine(lines.get(0));
            int idxKind = indexOf(header, "Kind");
            int idxTipoEntidad = indexOf(header, "TipoEntidad");
            int idxNombreEntidad = indexOf(header, "NombreEntidad");
            int idxEmailEntidad = indexOf(header, "EmailEntidad");
            int idxTagsEntidad = indexOf(header, "TagsEntidad");
            int idxEventoId = indexOf(header, "EventoId");
            int idxEventoFecha = indexOf(header, "EventoFecha");
            int idxEventoNombre = indexOf(header, "EventoNombre");
            int idxEventoNotas = indexOf(header, "EventoNotas");
            int idxEventoTags = indexOf(header, "EventoTags");
            int idxTipoRecordatorio = indexOf(header, "TipoRecordatorio");
            int idxProductoTitulo = indexOf(header, "ProductoTitulo");
            int idxProductoUrl = indexOf(header, "ProductoUrl");
            int idxProductoTags = indexOf(header, "ProductoTags");
            int idxParentEventoId = indexOf(header, "ParentEventoId");

            List<Entidad> clientes = new ArrayList<>();
            List<Entidad> encargados = new ArrayList<>();
            List<Entidad> trabajadores = new ArrayList<>();
            java.util.Map<String, Evento> eventos = new java.util.HashMap<>();
            java.util.Map<String, List<Producto>> productosPorEvento = new java.util.HashMap<>();

            int entidadesCount = 0;
            int eventosCount = 0;
            int productosCount = 0;

            for (int i = 1; i < lines.size(); i++) {
                String[] cols = splitCsvLine(lines.get(i));
                String kind = getCol(cols, idxKind);
                if ("empresa".equalsIgnoreCase(kind)) {
                    String tipo = getCol(cols, idxTipoEntidad);
                    String nombre = getCol(cols, idxNombreEntidad);
                    String email = getCol(cols, idxEmailEntidad);
                    String tagsStr = getCol(cols, idxTagsEntidad);
                    Entidad e = new Entidad();
                    e.setNombre(nombre);
                    e.setEmail(email);
                    e.setTipo(tipo);
                    e.setTags(splitTags(tagsStr));
                    switch (tipo) {
                        case "cliente": clientes.add(e); break;
                        case "encargado": encargados.add(e); break;
                        case "trabajador": trabajadores.add(e); break;
                        default: trabajadores.add(e); break;
                    }
                    entidadesCount++;
                } else if ("evento".equalsIgnoreCase(kind)) {
                    String eid = getCol(cols, idxEventoId);
                    Evento ev = new Evento();
                    ev.setId(eid);
                    ev.setFecha(getCol(cols, idxEventoFecha));
                    ev.setNombre(getCol(cols, idxEventoNombre));
                    ev.setNotas(getCol(cols, idxEventoNotas));
                    // tipoRecordatorio
                    try {
                        int tr = Integer.parseInt(getCol(cols, idxTipoRecordatorio));
                        ev.setTipoRecordatorio(tr);
                    } catch (Exception ignore) {}
                    ev.setTags(splitTags(getCol(cols, idxEventoTags)));
                    ev.setListaDeseo(new ArrayList<>());
                    eventos.put(eid, ev);
                    eventosCount++;
                } else if ("producto".equalsIgnoreCase(kind)) {
                    String titulo = getCol(cols, idxProductoTitulo);
                    String url = getCol(cols, idxProductoUrl);
                    String tagsStr = getCol(cols, idxProductoTags);
                    String parentId = getCol(cols, idxParentEventoId);
                    Producto p = new Producto(titulo, url, splitTags(tagsStr));
                    productosPorEvento.computeIfAbsent(parentId, k -> new ArrayList<>()).add(p);
                    productosCount++;
                }
            }

            // Asociar productos a cada evento
            for (java.util.Map.Entry<String, List<Producto>> entry : productosPorEvento.entrySet()) {
                Evento ev = eventos.get(entry.getKey());
                if (ev != null) {
                    ev.setListaDeseo(entry.getValue());
                }
            }

            // Guardar en Firebase
            DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference("Users").child(idUser);
            // Empresa
            DatabaseReference empresaRef = baseRef.child("Empresa");
            empresaRef.child("clientes").setValue(clientes);
            empresaRef.child("encargados").setValue(encargados);
            empresaRef.child("trabajadores").setValue(trabajadores);

            // Eventos: preservar id como clave si existe
            DatabaseReference listEventsRef = baseRef.child("ListEvents");
            for (Evento ev : eventos.values()) {
                String key = ev.getId();
                if (key == null || key.isEmpty()) {
                    DatabaseReference pushRef = listEventsRef.push();
                    ev.setId(pushRef.getKey());
                    pushRef.setValue(ev);
                } else {
                    listEventsRef.child(key).setValue(ev);
                }
            }

            // Tags: unir los tags usados en entidades, eventos y productos, garantizando "todos"
            java.util.Set<String> tagsSet = new java.util.HashSet<>();
            for (Entidad ent : clientes) { if (ent.getTags() != null) tagsSet.addAll(ent.getTags()); }
            for (Entidad ent : encargados) { if (ent.getTags() != null) tagsSet.addAll(ent.getTags()); }
            for (Entidad ent : trabajadores) { if (ent.getTags() != null) tagsSet.addAll(ent.getTags()); }
            for (Evento ev : eventos.values()) { if (ev.getTags() != null) tagsSet.addAll(ev.getTags()); }
            for (List<Producto> productos : productosPorEvento.values()) {
                for (Producto p : productos) { if (p.getTags() != null) tagsSet.addAll(p.getTags()); }
            }
            tagsSet.add("todos");
            baseRef.child("Tags").setValue(new ArrayList<>(tagsSet));

            // Verificación: leer de vuelta conteos
            listEventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    final int[] evCountDb = {(int) snapshot.getChildrenCount()};
                    empresaRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapEmpresa) {
                            final int[] entCountDb = {0};
                            for (String nodo : new String[]{"clientes", "encargados", "trabajadores"}) {
                                DataSnapshot s = snapEmpresa.child(nodo);
                                entCountDb[0] += (int) s.getChildrenCount();
                            }
                            // Leer de nuevo para verificación
                            listEventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapEvents) {
                                    final int[] productosCount = {0}; // contador de productos
                                    for (DataSnapshot evSnap : snapEvents.getChildren()) {
                                        DataSnapshot prodSnap = evSnap.child("listaDeseo");
                                        if (prodSnap.exists()) {
                                            productosCount[0] += (int) prodSnap.getChildrenCount();
                                        }
                                    }
                                    // callback con conteos
                                    callback.onSuccess(entCountDb[0], evCountDb[0], productosCount[0]);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    callback.onError("Error verificando importación: " + error.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError("Error verificando Empresa: " + error.getMessage());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError("Error verificando eventos: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            callback.onError("Error importando CSV: " + e.getMessage());
        }
    }

    private static List<String> splitTags(String tagsStr) {
        if (tagsStr == null || tagsStr.trim().isEmpty()) return new ArrayList<>();
        String[] parts = tagsStr.split(";");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        if (!list.contains("todos")) list.add("todos");
        return list;
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(";", tags);
    }

    private static int indexOf(String[] arr, String name) {
        for (int i = 0; i < arr.length; i++) {
            if (name.equalsIgnoreCase(arr[i])) return i;
        }
        return -1;
    }

    private static String[] splitCsvLine(String line) {
        // Simple split por ';' respetando comillas
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escapar comillas dobles
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ';' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }

    private static String getCol(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return "";
        return cols[idx];
    }
}