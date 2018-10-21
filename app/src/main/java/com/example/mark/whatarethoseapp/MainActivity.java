package com.example.mark.whatarethoseapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    static final int TAKE_PHOTO_PERMISSION = 1;
    static final int REQUEST_TAKE_PHOTO = 2;

    ImageView shoe_image_view;
    Button take_photo_button;

    static String photo_path = "";
    static String photo_id = "";
    static String download_url = "";

    Uri file;

    StorageReference storage_reference;
    StorageReference photo_reference;
    StorageReference photo_images_reference;

    UploadTask upload_task;

    private FirebaseAuth authorization;


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
                                    TAKE_PHOTO_PERMISSION);

        }

        authorization = FirebaseAuth.getInstance();

        storage_reference = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser current_user = authorization.getCurrentUser();
    }

    private void signInAnonymously() {
        // [START signin_anonymously]
        authorization.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = authorization.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        // [END_EXCLUDE]
                    }
                });
        // [END signin_anonymously]
    }

//    private void signOut() {
//        authorization.signOut();
//    }
//
//    private void linkAccount() {
//
//        // Create EmailAuthCredential with email and password
//        AuthCredential credential = EmailAuthProvider.getCredential("mmm5ja@virginia.edu", "firebase");
//
//        // [START link_credential]
//        authorization.getCurrentUser().linkWithCredential(credential)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//                            FirebaseUser user = task.getResult().getUser();
//                        } else {
//                            Toast.makeText(MainActivity.this, "Authentication failed.",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//
//                        // [START_EXCLUDE]
//                        // [END_EXCLUDE]
//                    }
//                });
//        // [END link_credential]
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        // Grant photo permission
        if (requestCode == TAKE_PHOTO_PERMISSION) {

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

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        file = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, REQUEST_TAKE_PHOTO);

    }

    private static File getOutputMediaFile(){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists())
            if (!mediaStorageDir.mkdirs())
                return null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        photo_path = mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg";

        photo_id = "IMG_" + timeStamp + ".jpg";

        return new File(photo_path);

    }

    public void toSearchActivity(View view) {

        signInAnonymously();

        photo_reference = storage_reference.child(photo_id);
        photo_images_reference = storage_reference.child("images/" + photo_id);

        shoe_image_view.setDrawingCacheEnabled(true);
        shoe_image_view.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) shoe_image_view.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

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
                                "Upload success! URL - " + downloadUrl.toString(),
                                Toast.LENGTH_SHORT).show();
                        download_url = downloadUrl.toString();
                    }
                });
            }
        });

        Intent intent = new Intent(this, SearchActivity.class);

        intent.putExtra("photo_path", photo_path);

        startActivity(intent);

    }

}