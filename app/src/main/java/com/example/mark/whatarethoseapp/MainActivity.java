package com.example.mark.whatarethoseapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;

    ImageView shoe_image_view;
    Button take_photo_button;

    static String photo_id = "";
    static String download_url = "";

    String mCurrentPhotoPath;
    Uri file;

    StorageReference storage_reference;
    StorageReference photo_reference;

    UploadTask upload_task;

    FirebaseAuth authorization;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Context lookup
        shoe_image_view = findViewById(R.id.shoe_image_view);
        take_photo_button = findViewById(R.id.take_photo_button);

        // Photo permission lookup
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            take_photo_button.setEnabled(false);
            ActivityCompat.requestPermissions(this,
                    new String[] {  android.Manifest.permission.CAMERA,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                    REQUEST_TAKE_PHOTO);

        }

        // Firebase setup
        authorization = FirebaseAuth.getInstance();
        storage_reference = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        // Grant photo permission
        if (requestCode == REQUEST_TAKE_PHOTO) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                take_photo_button.setEnabled(true);

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Set the photo preview to the photo that was just taken
        if (requestCode == REQUEST_TAKE_PHOTO) shoe_image_view.setImageURI(file);

    }


    public void takePhoto(View view) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = getOutputMediaFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("MainActivity", "Error Creating Camera File");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {

                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                file = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }

        }

    }

    private File getOutputMediaFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        mCurrentPhotoPath = image.getAbsolutePath();
        photo_id = image.getName();
        return image;

    }

    public void toSearchActivity(View view) {

        // create reference to photo in Firebase
        photo_reference = storage_reference.child(photo_id);

        // Convert photo to byte array
        shoe_image_view.setDrawingCacheEnabled(true);
        shoe_image_view.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) shoe_image_view.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Upload photo as byte array to Firebase
        upload_task = photo_reference.putBytes(data);
        upload_task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getBaseContext(), "Upload failure!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photo_reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Uri downloadUrl = uri;
                        Toast.makeText(getBaseContext(),
                                "Upload success!", Toast.LENGTH_SHORT).show();
                        download_url = downloadUrl.toString();
                    }
                });
            }
        });

        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("photo_path", mCurrentPhotoPath);
        intent.putExtra("download_url", download_url);

        startActivity(intent);

    }

}