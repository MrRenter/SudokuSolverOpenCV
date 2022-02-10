package com.mrrenter.sudokusolver;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import org.opencv.core.Scalar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    static CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    private Button captureImageBtn, solveImageBtn;
    Mat savedImage;
    View cameraView;

    Boolean freezePicture = false;
    Boolean invertImage = false;
    static Boolean lastFrame = false;

    static double areaMinThreshold;
    static double areaMaxThreshold;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        captureImageBtn = findViewById(R.id.take_picture_btn);
        solveImageBtn = findViewById(R.id.solve_btn);
        cameraView = findViewById(R.id.image_view);

        captureImageBtn.setOnClickListener(view -> getPicture());
        solveImageBtn.setOnClickListener(view -> testFunctions());

        areaMinThreshold = mOpenCvCameraView.getWidth()*mOpenCvCameraView.getHeight()*0.60;
        areaMaxThreshold = mOpenCvCameraView.getWidth()*mOpenCvCameraView.getHeight()*0.80;
    }

    public void getPicture(){
        freezePicture = !freezePicture;
    }

    public void testFunctions(){
        invertImage = !invertImage;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat modifiedFrame = inputFrame.gray();
        if (invertImage){
            Imgproc.GaussianBlur(inputFrame.gray(), modifiedFrame, new Size(7,7),3);
            Imgproc.adaptiveThreshold(modifiedFrame, modifiedFrame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
            Core.bitwise_not(modifiedFrame,modifiedFrame);

            return getLargestRect(modifiedFrame, inputFrame.rgba());
        }
        if (!freezePicture){
            savedImage = inputFrame.rgba();
        }
        return savedImage;
    }

    public static Mat getLargestRect(Mat bit, Mat img) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(bit, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double inf = 0;
        Rect max_rect = null;
        for(int i=0; i< contours.size();i++){
            Rect rect = Imgproc.boundingRect(contours.get(i));

            MatOfPoint2f c2f = new MatOfPoint2f(contours.get(i).toArray());
            MatOfPoint2f approx = new MatOfPoint2f();

            double peri = Imgproc.arcLength(c2f, true);
            Imgproc.approxPolyDP(c2f, approx, 0.04*peri, true);
            Point[] points = approx.toArray();
            if (points.length == 4) {
                //Not making sure its actually a square
                double area = rect.area();
                if (area > areaMinThreshold) {
                    if (inf < area) {
                        max_rect = rect;
                        inf = area;
                    }
                }
            }
        }
        if (max_rect != null) {
            Imgproc.rectangle(img, new Point(max_rect.x, max_rect.y), new Point(max_rect.x + max_rect.width, max_rect.y + max_rect.height), new Scalar(0, 255, 0), 5);
        }
        return img;
    }
}