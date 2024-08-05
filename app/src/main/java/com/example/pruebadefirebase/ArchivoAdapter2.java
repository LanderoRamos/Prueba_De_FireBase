package com.example.pruebadefirebase;


import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class ArchivoAdapter2 extends RecyclerView.Adapter<ArchivoAdapter2.ViewHolder> {

    private Context context;
    private List<Archivo> listaArchivos;
    private OnArchivoClickListener listener;

    public ArchivoAdapter2(Context context, List<Archivo> listaArchivos) {
        this.context = context;
        this.listaArchivos = listaArchivos;
    }

    public interface OnArchivoClickListener {
        void onArchivoClick(Archivo archivo);
        void onArchivoLongClick(Archivo archivo);
    }

    public void setOnArchivoClickListener(OnArchivoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archivo_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Archivo archivo = listaArchivos.get(position);
        holder.nombreArchivo.setText(archivo.getNombre());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArchivoClick(archivo);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onArchivoLongClick(archivo);
            }
            return true; // Indica que el evento de clic largo está consumido
        });
    }

    @Override
    public int getItemCount() {
        return listaArchivos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombreArchivo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreArchivo = itemView.findViewById(R.id.nombreArchivo);
        }
    }

    public void ver(String urlDescarga){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga));
        context.startActivity(intent);
    }

    public void descargar(String urlDescarga) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlDescarga));
        request.setTitle("Descargando archivo");
        request.setDescription("Por favor, espere...");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(urlDescarga).getLastPathSegment());

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Descarga iniciada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
        }
    }



}
