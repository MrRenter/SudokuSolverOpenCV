package com.mrrenter.sudokusolver;

import android.util.Log;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
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
    double slopeThresh = 0.5;

    public Helper(CameraBridgeViewBase camera) {
        mOpenCvCameraView = camera;

        areaMinThreshold = 80000; //mOpenCvCameraView.getWidth()*mOpenCvCameraView.getHeight()*0.50;
        areaMaxThreshold = 600000;//mOpenCvCameraView.getWidth() * mOpenCvCameraView.getHeight() * 0.80;
    }

    public Mat prepImage(Mat image) {
        Mat modifiedFrame = image;
        Imgproc.GaussianBlur(image, modifiedFrame, new Size(7, 7), 3);
        Imgproc.adaptiveThreshold(modifiedFrame, modifiedFrame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
        Core.bitwise_not(modifiedFrame, modifiedFrame);
        return modifiedFrame;
    }

    public Mat getLargestRect(Mat bit, Mat img) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(bit, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Point[] maxpoint = new Point[0];
        Rect max_rect = null;
        for (int i = 0; i < contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));

            MatOfPoint2f c2f = new MatOfPoint2f(contours.get(i).toArray());
            MatOfPoint2f approx = new MatOfPoint2f();

            double peri = Imgproc.arcLength(c2f, true);
            Imgproc.approxPolyDP(c2f, approx, 0.04 * peri, true);
            Point[] points = approx.toArray();

            if (points.length == 4) {
                double area = rect.area();

                if (area > areaMinThreshold && area < areaMaxThreshold) {

                    if (rect.height - rect.width < (rect.height*.1)) {
                        Point min = getMinPoint(points);
                        Point max = getMaxPoint(points);

                        double slope = (max.y - min.y) / (max.x - min.x);
                        if (slope < 0) {
                            slope *= -1;
                        }

                        if (slope > (1 - slopeThresh) && slope < (1 + slopeThresh)) {
                            Log.d("MrRenterDebug", "Area: " + area + " Slope: " + slope);
                            max_rect = rect;
                            maxpoint = points;
                        }
                    }
                }
            }
        }

        if (max_rect != null) {
            Imgproc.rectangle(img, max_rect, new Scalar(255,0,0),5);
        }
        return img;
    }

    public Point getMinPoint(Point[] points) {
        double min = points[0].x + points[0].y;
        int minIndex = 0;

        for (int x = 0; x < points.length; x++) {
            double tmp = points[x].x + points[x].y;
            if (tmp < min) {
                minIndex = x;
                min = tmp;
            }
        }

        return points[minIndex];
    }

    public Point getMaxPoint(Point[] points) {
        double min = points[0].x + points[0].y;
        int maxIndex = 0;

        for (int x = 0; x < points.length; x++) {
            double tmp = points[x].x + points[x].y;
            if (tmp > min) {
                maxIndex = x;
                min = tmp;
            }
        }

        return points[maxIndex];
    }
}
