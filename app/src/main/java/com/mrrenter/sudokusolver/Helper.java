package com.mrrenter.sudokusolver;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Helper {
    CameraBridgeViewBase mOpenCvCameraView;
    ImageView transformview;
    double areaMinThreshold, areaMaxThreshold;
    double slopeThresh = 1;

    public Helper(CameraBridgeViewBase camera, ImageView tView) {
        mOpenCvCameraView = camera;
        transformview = tView;
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
        //Best Scenario
        //at least 2 good frames within a certain time. we are getting 22fps when no box. 14-18 when there is
        //use the best square. whatevers height is closest to width



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
            //Imgproc.rectangle(img, max_rect, new Scalar(255,0,0),5);
            return squareUpTransform(img, maxpoint);
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

    public Mat squareUpTransform(Mat img, Point[] unorganizedp){

        Point[] p = organizePoints(unorganizedp);

        Mat srcImage = img;
        Mat destImage = img;
        Mat src = new MatOfPoint2f(new Point(p[0].x, p[0].y), new Point(p[1].x, p[1].y), new Point(p[3].x, p[3].y), new Point(p[2].x, p[2].y));
        Mat dst = new MatOfPoint2f(new Point(0, 0), new Point(destImage.width() - 1, 0), new Point(destImage.width() - 1, destImage.height() - 1), new Point(0, destImage.height() - 1));
        Mat transform = Imgproc.getPerspectiveTransform(src, dst);
        Imgproc.warpPerspective(srcImage, destImage, transform, destImage.size());
        return destImage;
    }

    public Point[] organizePoints(Point[] p){

        Log.d("MrRenterDebug", "Size: " + p.length);
        for (int x=0;x<p.length;x++){
            try {
                Log.d("MrRenterDebug", "UnsortedIndex: " + x + " " + p[x].x + ", " + p[x].y);
            } catch (Exception ex){

            }
        }

        Point[] sortedPoints = new Point[4];
        double min=p[0].x + p[0].y;
        int indexmin=0;

        for (int x=0;x<p.length;x++){
            if (p[x].x + p[x].y < min){
                min = p[x].x + p[x].y;
                indexmin = x;
            }
        }

        double max=0;
        int indexmax=0;

        for (int x=0;x<p.length;x++){
            if (p[x].x + p[x].y > max){
                max = p[x].x + p[x].y;
                indexmax = x;
            }
        }


        sortedPoints[0] = p[indexmin];
        sortedPoints[3] = p[indexmax];

        p[indexmin] = null;
        p[indexmax] = null;

        double maxTL=0;
        int indexTL=0;

        for (int x=0;x<p.length;x++){
            if (p[x] != null){
                if (p[x].y > maxTL){
                    maxTL = p[x].y;
                    indexTL = x;
                }
            }
        }
        sortedPoints[2] = p[indexTL];
        p[indexTL] = null;

        for (int x=0;x<p.length;x++){
            if (p[x] != null){
                sortedPoints[1] = p[x];
            }
        }

        for (int x=0;x<p.length;x++){
            try {
                Log.d("MrRenterDebug", "SortedIndex: " + x + " " + sortedPoints[x].x + ", " + sortedPoints[x].y);
            } catch (Exception ex){

            }
        }

        return sortedPoints;
    }

    public Bitmap convertToBitmap(Mat input){
        Bitmap bmp = null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB, 4);

        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgb, bmp);
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
        return bmp;
    }

}
