package com.example.testalgorithm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ScannerActivity extends AppCompatActivity {

    ImageView imageView;
    String errorMsg;
    private static final String TAG="MainActivity";
    Toolbar toolbar;
    File file;
    Uri uri;
    Intent CamIntent,GalIntent;
    final int RequestPermissionCode=1;
    Bitmap selectedImage;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case BaseLoaderCallback.SUCCESS:{
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Scanner");  //----------------   Name   -----------------------------------------------
        setSupportActionBar(toolbar);

        imageView = (ImageView)findViewById(R.id.imageView);

        int permissionCheck = ContextCompat.checkSelfPermission(ScannerActivity.this, Manifest.permission.CAMERA);
        if(permissionCheck == PackageManager.PERMISSION_DENIED)
            RequestRuntimePermission();
    }

    private void RequestRuntimePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(ScannerActivity.this,Manifest.permission.CAMERA))
            Toast.makeText(this,"CAMERA permission allows us to access CAMERA app",Toast.LENGTH_SHORT).show();
        else
        {
            ActivityCompat.requestPermissions(ScannerActivity.this,new String[]{Manifest.permission.CAMERA},RequestPermissionCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.btn_camera)
            CameraOpen();
        else if(item.getItemId() == R.id.btn_gallery)
            GalleryOpen();
        return true;
    }

    private void GalleryOpen() {
        //GalIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        GalIntent = new Intent();
        GalIntent.setType("image/*");
        GalIntent.setAction(Intent.ACTION_GET_CONTENT);
        GalIntent.addCategory(Intent.CATEGORY_OPENABLE);
        errorMsg = null;
        startActivityForResult(Intent.createChooser(GalIntent,"Select Image from Gallery"),2);

    }

    private void CameraOpen() {
        CamIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagesFolder = new File(Environment.getExternalStorageDirectory() +"/TestAlgorithm/");
        if(!imagesFolder.exists())     //check if file already exists
        {
            imagesFolder.mkdirs();     //if not, create it
        }
        file = new File(imagesFolder,
                "file"+String.valueOf(System.currentTimeMillis())+".jpg");
        uri = Uri.fromFile(file);
        errorMsg = null;
        CamIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        CamIntent.putExtra("return-data",true);
        startActivityForResult(CamIntent,0);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "Opencv succesfully loaded");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else{
            Log.i(TAG, "Opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && resultCode == RESULT_OK)
            try {
                final InputStream imageStream =
                        getContentResolver().
                                openInputStream(uri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                //----------------------------------------------------------------------------------
                Scanner scanner = new Scanner(selectedImage);
                boolean success = scanner.calcScannedImage(false);

                if( success ) {
                    Log.d(TAG,"Display Image Result");
                    imageView.setImageBitmap(scanner.getScannedImage());
                } else {
                    errorMsg = scanner.getErrorMsg();
                    Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    imageView.setImageBitmap(scanner.getScannedImage());
                }
                //----------------------------------------------------------------------------------
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        else if(requestCode == 2) {
            try {
                if(data != null) {
                    uri = data.getData();
                    InputStream stream = getContentResolver().openInputStream(uri);
                    selectedImage = BitmapFactory.decodeStream(stream);
                    //------------------------------------------------------------------------------
                    Scanner scanner = new Scanner(selectedImage);
                    boolean success = scanner.calcScannedImage(false);

                    if( success ) {
                        Log.d(TAG,"Display Image Result");
                        imageView.setImageBitmap(scanner.getScannedImage());
                    } else {
                        errorMsg = scanner.getErrorMsg();
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        imageView.setImageBitmap(scanner.getScannedImage());
                    }
                    //------------------------------------------------------------------------------
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
        else if (requestCode == 1)
        {
            if(data != null)
            {
                Bundle bundle = data.getExtras();
                Bitmap bitmap = bundle.getParcelable("data");
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case RequestPermissionCode:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this,"Permission Canceled",Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }
}
