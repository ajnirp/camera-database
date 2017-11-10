package com.example.rohan.cameradatabase;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private String mCurrentPhotoPath;
    private File mImagesDir;
    private ImageView mThumbnailView;
    private TextView mPhotoSizeView;
    private TextView mTagsListView;
    private TextView mSizeView;
    private SQLiteDatabase mDatabase;

    private final String DIR_NAME = "Camera Database";

    private int mCurrentID;
    private int mCurrentSize;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        mThumbnailView = (ImageView) findViewById(R.id.thumbnail_view);
        mPhotoSizeView = (TextView) findViewById(R.id.photo_size_view);
        mTagsListView = (TextView) findViewById(R.id.tag_field_view);
        mSizeView = (TextView) findViewById(R.id.size_field_view);

        mDatabase = openOrCreateDatabase("MyDatabase", Context.MODE_PRIVATE, null);

        mDatabase.execSQL("DROP TABLE IF EXISTS Photos");
        mDatabase.execSQL("CREATE TABLE Photos (ID INTEGER, location TEXT, size INTEGER)");
        mDatabase.execSQL("DROP TABLE IF EXISTS Tags");
        mDatabase.execSQL("CREATE TABLE Tags (ID INTEGER, tag TEXT)");

        mDatabase.execSQL("INSERT INTO Photos VALUES (2, 'sdcard/p2.jpg', 200)");
        mDatabase.execSQL("INSERT INTO Photos VALUES (3, 'sdcard/p3.jpg', 300)");
        mDatabase.execSQL("INSERT INTO Photos VALUES (4, 'sdcard/p4.jpg', 200)");

        mCurrentID = 0;
    }

    public void save(View v) {
        Log.v("wonkity wonk", "Save button clicked");
        String idString = Integer.toString(mCurrentID);
        String sizeString = Integer.toString(mCurrentSize);
        mDatabase.execSQL("INSERT INTO Photos VALUES (" +
                idString + ", '" + mCurrentPhotoPath + "', " +
                sizeString + ")");
        String tagString = mTagsListView.getText().toString();
        String[] tagsList = tagString.split(";");
        for (String tag: tagsList) {
            mDatabase.execSQL("INSERT INTO Tags VALUES (" + idString + ", '" + tag + "')");
        }
    }

    public void load(View v) {
        Log.v("wonkity wonk", "Load button clicked");
        String tagString = mTagsListView.getText().toString();
        String sizeString = mSizeView.getText().toString();
        if (tagString.length() == 0 && sizeString.length() == 0) {
            CharSequence report = "No tags or size specified";
            Toast toast = Toast.makeText(getApplicationContext(), report, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        String query = "SELECT Photos.ID, location, size, tag FROM " +
                "Photos, Tags WHERE Photos.ID = Tags.ID";
        if (tagString.length() > 0) {
            String[] tagsList = tagString.split(";");
            query += " AND (";
            for (int i = 0; i < tagsList.length; i++) {
                if (i != tagsList.length - 1) {
                    query += "tag = '" + tagsList[i] + "' OR ";
                } else {
                    query += "tag = '" + tagsList[i] + "')";
                }
            }
        }
        if (sizeString.length() > 0) {
            int minSize = (int) (0.75 * Integer.parseInt(sizeString));
            int maxSize = (int) (1.25 * Integer.parseInt(sizeString));
            query += " AND (size <= " + maxSize + " AND size >= " + minSize + ")";
        }

        Cursor c = mDatabase.rawQuery(query, null);
        c.moveToFirst();

        String str = "";

        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < c.getColumnCount(); j++) {
                str = str + c.getString(j) + "\n";
            }
            Log.v("wonkity wonk", str);
            c.moveToNext();
        }
    }

    private File createPhotoFile() throws IOException {
        Log.v("tag", "started createPhotoFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File photoFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                mImagesDir      /* directory */
        );

        mCurrentPhotoPath = photoFile.getAbsolutePath();
        Log.v("wonkity wonk", "current photo path " + mCurrentPhotoPath);
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
        int targetW = mThumbnailView.getWidth();
        int targetH = mThumbnailView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        mCurrentSize = photoW * photoH;

        // Set the photo size text view
        mPhotoSizeView.setText(Integer.toString(photoW) + " x " + Integer.toString(photoH));

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mThumbnailView.setImageBitmap(bitmap);
    }
}
