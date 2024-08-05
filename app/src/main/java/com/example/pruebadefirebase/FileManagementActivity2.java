package com.example.pruebadefirebase;

import android.content.pm.PackageManager;
import android.os.Bundle;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FileManagementActivity2 extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 101;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    private Button btnSelectFile, btnUploadFile;
    private RecyclerView recyclerViewFiles;
    private DatabaseReference mDatabase;

    private Uri fileUri;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private ArchivoAdapter2 archivoAdapter;
    private List<Archivo> listaArchivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_file_management2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios").child(userId).child("archivos");

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);

        listaArchivos = new ArrayList<>();
        archivoAdapter = new ArchivoAdapter2(this, listaArchivos);
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(archivoAdapter);

        archivoAdapter.setOnArchivoClickListener(new ArchivoAdapter2.OnArchivoClickListener() {
            @Override
            public void onArchivoClick(Archivo archivo) {
                mostrarOpcionesArchivo(archivo);
            }

            @Override
            public void onArchivoLongClick(Archivo archivo) {
                archivoAdapter.descargar(archivo.getUrlDescarga());
                // Aquí puedes implementar acciones adicionales en caso de una pulsación larga
            }
        });

        btnSelectFile.setOnClickListener(v -> selectFile());
        btnUploadFile.setOnClickListener(v -> uploadFile());

        // Escuchar cambios en la base de datos de Firebase y actualizar la lista de archivos
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaArchivos.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Archivo archivo = snapshot.getValue(Archivo.class);
                    listaArchivos.add(archivo);
                }
                archivoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FileManagementActivity2.this, "Error al leer archivos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Permitir cualquier tipo de archivo
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadFile() {
        if (fileUri != null) {
            StorageReference storageRef = storage.getReference();
            String fileName = fileUri.getLastPathSegment();
            String userId = mAuth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child("archivos/" + userId + "/" + fileName);

            UploadTask uploadTask = fileRef.putFile(fileUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(FileManagementActivity2.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();

                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    Archivo archivo = new Archivo(fileName, downloadUrl);
                    String uploadId = databaseRef.push().getKey();
                    databaseRef.child(uploadId).setValue(archivo);
                }).addOnFailureListener(e ->
                        Toast.makeText(FileManagementActivity2.this, "Error al obtener URL de descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

            }).addOnFailureListener(exception -> {
                Toast.makeText(FileManagementActivity2.this, "Error al subir el archivo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(FileManagementActivity2.this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarOpcionesArchivo(Archivo archivo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones de Archivo")
                .setMessage("Selecciona una acción para el archivo: " + archivo.getNombre())
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarArchivo(archivo))
                .setNeutralButton("Ver Archivo", (dialog, which) -> archivoAdapter.ver(archivo.getUrlDescarga()))
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void eliminarArchivo(Archivo archivo) {
        String userId = mAuth.getCurrentUser().getUid();
        String archivoId = archivo.getId();

        Toast.makeText(FileManagementActivity2.this, ""+archivoId, Toast.LENGTH_SHORT).show();

        /*databaseRef.child(archivoId).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(FileManagementActivity2.this, "Referencia de archivo eliminada correctamente", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(FileManagementActivity2.this, "Error al eliminar referencia en Firebase Database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });*/
    }

    private void eliminarDato() {
        // Por ejemplo, vamos a eliminar un usuario con un ID específico

    }

    private void descargar(){

    }

}