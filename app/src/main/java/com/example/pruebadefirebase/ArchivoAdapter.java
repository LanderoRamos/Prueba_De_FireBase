package com.example.pruebadefirebase;

// ArchivoAdapter.java
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.List;

public class ArchivoAdapter extends RecyclerView.Adapter<ArchivoAdapter.ViewHolder> {

    private Context context;
    private List<Archivo> archivos;
    private FirebaseStorage storage;
    private OnItemClickListener listener;

    public ArchivoAdapter(Context context, List<Archivo> archivos) {
        this.context = context;
        this.archivos = archivos;
        this.storage = FirebaseStorage.getInstance();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_archivo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Archivo archivo = archivos.get(position);
        holder.tvNombreArchivo.setText(archivo.getNombre());
        holder.link.setText(archivo.getUrlDescarga());
        holder.link.setMovementMethod(LinkMovementMethod.getInstance());
        holder.link.setText(Html.fromHtml("<a href='"+archivo.getUrlDescarga()+"'>"+archivo.getNombre()+"</a>"));


        holder.btnDescargar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return archivos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombreArchivo, link;
        Button btnDescargar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreArchivo = itemView.findViewById(R.id.tvNombreArchivo);
            btnDescargar = itemView.findViewById(R.id.btnDescargar);
            link = itemView.findViewById(R.id.textViewLink);
        }
    }

    public void descargarArchivo(String nombreArchivo, String urlDescarga) {
        StorageReference storageRef = storage.getReferenceFromUrl(urlDescarga);

        // Crear una referencia local en la carpeta de descargas
        File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), nombreArchivo);

        // Descargar archivo a través de la URL
        storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            // Archivo descargado exitosamente
            Toast.makeText(context, "Archivo descargado en la carpeta Download", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(exception -> {
            // Error al descargar archivo
            Toast.makeText(context, "Error al descargar archivo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
