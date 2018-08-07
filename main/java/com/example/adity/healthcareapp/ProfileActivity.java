package com.example.adity.healthcareapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;
    private ImageView imageView;
    private EditText editText;
    private Button saveButton;
    private Uri uriProfileImage;
    private ProgressBar progressBar;
    String downloadUrl;
    FirebaseAuth firebaseAuth;
    private TextView verifyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressBar=(ProgressBar)findViewById(R.id.progressBar);

        firebaseAuth=FirebaseAuth.getInstance();

        verifyTextView = (TextView)findViewById(R.id.verifyTextView);

        imageView= (ImageView)findViewById(R.id.imageView);
        editText=(EditText)findViewById(R.id.editText);
        saveButton=(Button)findViewById(R.id.saveButton);



        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showImageShower();

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveUserInfo();

            }
        });
        loadUserInfo();

    }

    private void loadUserInfo() {

        //Use Glide Library for loading image from url
        //(build.gradle-Project)repositories {
        //mavenCentral()
        //maven { url 'https://maven.google.com'}
        //}
        //(build.gradle-app)dependencies{
        //compile 'com.github.bumptech.glide:glide:4.3.1'
        //annotationProcessor 'com.github.bumptech.glide:compiler:4.3.1'
        //}

        final FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user!=null){
            if(user.getPhotoUrl()!=null){
                Glide.with(getApplicationContext())
                        .load(user.getPhotoUrl())
                        .into(imageView);

            }
            if(user.getDisplayName()!=null){
                editText.setText(user.getDisplayName());
            }
            if(!user.isEmailVerified()){
                verifyTextView.setText("User not verified(Click to verify)");
            }
            verifyTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ProfileActivity.this, "Verification Email Sent", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

        }
    }

    private void saveUserInfo() {

        String displayName= editText.getText().toString().trim();

        if(displayName.isEmpty()){

            editText.setError("Display name required");
            editText.requestFocus();
            return;
        }

        FirebaseUser user= firebaseAuth.getCurrentUser();

        if(user!=null && downloadUrl!=null){

            UserProfileChangeRequest profile= new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .setPhotoUri(Uri.parse(downloadUrl))
                    .build();

            user.updateProfile(profile)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PICK_IMAGE && resultCode==RESULT_OK && data!=null && data.getData()!=null){

            uriProfileImage=data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uriProfileImage);
                imageView.setImageBitmap(bitmap);
                uploadImageToFirebaseStorage();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseStorage() {

        StorageReference storageRef=
                FirebaseStorage.getInstance().getReference("profileImages/"+ System.currentTimeMillis()+".jpg");
        if(uriProfileImage!=null){
            progressBar.setVisibility(View.VISIBLE);
            storageRef.putFile(uriProfileImage)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.GONE);
                            downloadUrl= taskSnapshot.getDownloadUrl().toString();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            //
                        }
                    });
        }
    }

    public void showImageShower(){

        Intent intent= new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Please select a profile image"),PICK_IMAGE);

    }

}
