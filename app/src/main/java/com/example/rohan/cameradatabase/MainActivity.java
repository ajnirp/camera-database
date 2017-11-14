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

    private Cursor mCursor;
    private int mNumResults;
    private int mCurrentResult;

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

        mCurrentID = 0;
        mCurrentSize = -1;
        mNumResults = -1;
    }

    public void save(View v) {
        Log.v("wonkity wonk", "Save button clicked");
        if (mCurrentSize < 0) {
            displayToast("Please take a picture first");
            return;
        }
        mCurrentID++; // Update ID
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
        String report = "Saved image with tags: ";
        for (int i = 0; i < tagsList.length; i++) {
            report += tagsList[i];
            if (i != tagsList.length-1) {
                report += ", ";
            }
        }
        displayToast(report);
        mThumbnailView.setImageBitmap(null);
        mTagsListView.setText("");
        mSizeView.setText("");

        // Reset mCurrentSize to avoid saving multiple images in a row
        mCurrentSize = -1;
    }

    private void displayToast(String report) {
        Toast toast = Toast.makeText(getApplicationContext(), report, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void load(View v) {
        Log.v("wonkity wonk", "Load button clicked");
        String tagString = mTagsListView.getText().toString();
        String sizeString = mSizeView.getText().toString();
        if (tagString.length() == 0 && sizeString.length() == 0) {
            displayToast("No tags or size specified");
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

        mCursor = mDatabase.rawQuery(query, null);

        if (mCursor.getCount() == 0) {
            displayToast("No matching images found");
            mThumbnailView.setImageBitmap(null);
            return;
        }

        mCursor.moveToFirst();
        mNumResults = mCursor.getCount();
        mCurrentResult = 0;

        updateThumbnail();
    }

    private void updateThumbnail() {
        String path = mCursor.getString(1);
        File photoFile = new File(path);
        String absolutePath = photoFile.getAbsolutePath();
        if (photoFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(absolutePath);
            mThumbnailView.setImageBitmap(bmp);
        } else {
            displayToast("Error retrieving image: " + absolutePath);
        }
        String str = "";
        for (int j = 0; j < mCursor.getColumnCount(); j++) {
            str = str + mCursor.getString(j) + "\n";
        }
        Log.v("wonkity wonk", str);
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
        mSizeView.setText(Integer.toString(photoW * photoH));

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mThumbnailView.setImageBitmap(bitmap);
    }

    public void nextMatch(View v) {
        if (mCursor == null || mNumResults < 0) {
            displayToast("Please run a search query first");
            return;
        }
        mCurrentResult = (mCurrentResult + 1) % mNumResults;
        if (mCurrentResult == 0) {
            mCursor.moveToFirst();
        } else {
            mCursor.moveToNext();
        }
        updateThumbnail();
    }

    public void prevMatch(View v) {
        if (mCursor == null || mNumResults < 0) {
            displayToast("Please run a search query first");
            return;
        }
        if (mCurrentResult == 0) {
            mCurrentResult = mNumResults - 1;
            mCursor.moveToLast();
        } else {
            mCurrentResult = mCurrentResult - 1;
            mCursor.moveToPrevious();
        }
        updateThumbnail();
    }
}
