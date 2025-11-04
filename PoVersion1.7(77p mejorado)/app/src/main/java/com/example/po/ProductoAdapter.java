package com.example.po;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    public interface OnProductoActionListener {
        void onEditProducto(Producto producto, int position);
        void onDeleteProducto(Producto producto, int position);
    }

    private List<Producto> listaProductos;
    private OnProductoActionListener actionListener;

    public ProductoAdapter(List<Producto> listaProductos, OnProductoActionListener actionListener) {
        this.listaProductos = listaProductos;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);
        holder.productName.setText(producto.getTitulo());

        holder.productUrl.setText(producto.getUrl());

        holder.buttonEditProduct.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditProducto(producto, holder.getAdapterPosition());
            }
        });

        holder.buttonDeleteProduct.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteProducto(producto, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productUrl;
        ImageButton buttonEditProduct;
        ImageButton buttonDeleteProduct;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.textViewProductName);
            productUrl = itemView.findViewById(R.id.textViewProductUrl);
            buttonEditProduct = itemView.findViewById(R.id.buttonEditProduct);
            buttonDeleteProduct = itemView.findViewById(R.id.buttonDeleteProduct);
        }
    }
}