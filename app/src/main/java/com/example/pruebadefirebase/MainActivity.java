package com.example.pruebadefirebase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
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

public class MainActivity extends AppCompatActivity {

    //FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final int REQUEST_CODE_PICK_FILE = 101;

    private Button btnSelectFile;
    private Button btnUploadFile;
    private RecyclerView recyclerViewFiles;
    private ArchivoAdapter archivoAdapter;
    private List<Archivo> listaArchivos;

    private Uri fileUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);

        listaArchivos = new ArrayList<>();
        archivoAdapter = new ArchivoAdapter(this, listaArchivos);
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(archivoAdapter);

        btnSelectFile.setOnClickListener(v -> {
            selectFile();
        });

        btnUploadFile.setOnClickListener(v -> {
            uploadFile();
        });

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("archivos");
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
                Toast.makeText(MainActivity.this, "Error al leer archivos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        // Get a non-default Storage bucket
        //FirebaseStorage storage = FirebaseStorage.getInstance("gs://my-custom-bucket");

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

    private void uploadFile() {
        if (fileUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference fileRef = storageRef.child("archivos/" + fileUri.getLastPathSegment());

            UploadTask uploadTask = fileRef.putFile(fileUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Éxito al subir el archivo
                Toast.makeText(MainActivity.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();

                // Guardar información del archivo en Firebase Database
                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("archivos");
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Archivo archivo = new Archivo(fileUri.getLastPathSegment(), uri.toString());
                    databaseRef.push().setValue(archivo);
                });

            }).addOnFailureListener(exception -> {
                // Error al subir el archivo
                Toast.makeText(MainActivity.this, "Error al subir el archivo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(MainActivity.this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
        }
    }

}