package com.mrrenter.sudokusolver;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Button captureImageBtn, solveImageBtn;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initilize aspects
        captureImageBtn = findViewById(R.id.take_picture_btn);
        solveImageBtn = findViewById(R.id.solve_btn);
        imageView = findViewById(R.id.image_view);

        //This is called lambda. No need for all the extra nonsense.
        captureImageBtn.setOnClickListener(view -> dispatchTakePictureIntent());
    }

    private void solveImage() {
        if (OpenCVLoader.initDebug()) {
            Log.d("MrRenterLog", "OpenCV is correctly installed");
        } else {
            Log.d("MrRenterLog", "OpenCV is NOT correctly installed");
        }
    }

    //Method to be called to open the camera
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    //Method called when an image is taken
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Mat imageMat = new Mat(imageBitmap.getHeight(),imageBitmap.getWidth(),0);

            Utils.bitmapToMat(imageBitmap, imageMat);
            Utils.matToBitmap(imageMat, imageBitmap);
            imageView.setImageBitmap(imageBitmap);


            solveImage();
        }
    }
}