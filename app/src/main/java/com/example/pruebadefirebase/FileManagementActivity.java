package com.example.pruebadefirebase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManagementActivity extends AppCompatActivity implements ArchivoAdapter.OnItemClickListener {

    private static final int REQUEST_CODE_PICK_FILE = 101;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    private Button btnSelectFile, btnUploadFile;
    private RecyclerView recyclerViewFiles;
    private ProgressBar progressBar;

    private Uri fileUri;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private ArchivoAdapter archivoAdapter;
    private List<Archivo> listaArchivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_management);

        // Inicialización de Firebase
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        String userId = mAuth.getCurrentUser().getUid(); // Obtener el ID del usuario autenticado
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios").child(userId).child("archivos");

        // Referencias de vistas
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);
        progressBar = findViewById(R.id.progressBar);

        // Configuración del RecyclerView
        listaArchivos = new ArrayList<>();
        archivoAdapter = new ArchivoAdapter(this, listaArchivos);
        archivoAdapter.setOnItemClickListener(this);
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(archivoAdapter);

        // Botón para seleccionar archivo
        btnSelectFile.setOnClickListener(v -> selectFile());

        // Botón para subir archivo
        btnUploadFile.setOnClickListener(v -> uploadFile());

        // Verificar y solicitar permisos si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        }

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
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FileManagementActivity.this, "Error al leer archivos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
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
            progressBar.setVisibility(View.VISIBLE);

            StorageReference storageRef = storage.getReference();
            String fileName = fileUri.getLastPathSegment();
            String userId = mAuth.getCurrentUser().getUid(); // Obtener el ID del usuario autenticado
            StorageReference fileRef = storageRef.child("archivos/" + userId + "/" + fileName);

            UploadTask uploadTask = fileRef.putFile(fileUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Éxito al subir el archivo
                Toast.makeText(FileManagementActivity.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();

                // Guardar información del archivo en Firebase Database
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    Archivo archivo = new Archivo(fileName, downloadUrl);
                    String uploadId = databaseRef.push().getKey();
                    databaseRef.child(uploadId).setValue(archivo);
                }).addOnFailureListener(e ->
                        Toast.makeText(FileManagementActivity.this, "Error al obtener URL de descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

                progressBar.setVisibility(View.GONE);
            }).addOnFailureListener(exception -> {
                // Error al subir el archivo
                Toast.makeText(FileManagementActivity.this, "Error al subir el archivo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
        } else {
            Toast.makeText(FileManagementActivity.this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(int position) {
        Archivo archivo = listaArchivos.get(position);
        archivoAdapter.descargarArchivo(archivo.getNombre(), archivo.getUrlDescarga());
    }
}
