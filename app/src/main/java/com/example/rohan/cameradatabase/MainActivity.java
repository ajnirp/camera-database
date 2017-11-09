package com.example.rohan.cameradatabase;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    String mCurrentPhotoPath;
    private File mImagesDir;
    private Bitmap mCurrentBitmap;

    private final String DIR_NAME = "Camera Database";

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        File sdCardDir = Environment.getExternalStorageDirectory();
//        mImagesDir = new File(sdCardDir, DIR_NAME);
//        mImagesDir.mkdirs();
        mImagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    public void save(View v) {
    }

    public void load(View v) {}

    private File createPhotoFile() throws IOException {
        Log.v("tag", "started createPhotoFile");
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = "temp";
        File photoFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                mImagesDir      /* directory */
        );

        mCurrentPhotoPath = photoFile.getAbsolutePath();
        Log.v("tag", "current photo path " + mCurrentPhotoPath);
        return photoFile;
    }

    public void capture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createPhotoFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.v("tag", "stored photo in " + mCurrentPhotoPath);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            ImageView imageView = (ImageView) findViewById(R.id.thumbnail_view);
            mCurrentBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(mCurrentBitmap);
        }
    }
}
