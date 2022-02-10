package com.mrrenter.sudokusolver;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Helper {
    CameraBridgeViewBase mOpenCvCameraView;
    double areaMinThreshold, areaMaxThreshold;


    public Helper(CameraBridgeViewBase camera){
        mOpenCvCameraView = camera;

        areaMinThreshold = mOpenCvCameraView.getWidth()*mOpenCvCameraView.getHeight()*0.60;
        areaMaxThreshold = mOpenCvCameraView.getWidth()*mOpenCvCameraView.getHeight()*0.80;
    }

    public Mat prepImage(Mat image){
        Mat modifiedFrame = image;
        Imgproc.GaussianBlur(image, modifiedFrame, new Size(7,7),3);
        Imgproc.adaptiveThreshold(modifiedFrame, modifiedFrame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        Core.bitwise_not(modifiedFrame,modifiedFrame);
        return modifiedFrame;
    }

    public Mat getLargestRect(Mat bit, Mat img) {
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
