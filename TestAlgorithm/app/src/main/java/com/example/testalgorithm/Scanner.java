package com.example.testalgorithm;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by l on 22/05/17.
 */
public class Scanner {

    //--------------------------       Variables        --------------------------------------------
    private Bitmap srcImage;
    private String errorMsg;
    private Bitmap scannedImage;

    //--------------------------      Constructor       --------------------------------------------
    Scanner(Bitmap image){
        srcImage = image;
        errorMsg = null;
    }

    //--------------------------       Functions         -------------------------------------------

    //Getters
    public Bitmap getSrcImage(){
        return srcImage;
    }

    public String getErrorMsg(){
        return errorMsg;
    }

    public Bitmap getScannedImage(){ return scannedImage; }

    //Setters
    public void setSrcImage(Bitmap image){
        srcImage = image;
    }

    private void setErrorMsg(String msg){
        errorMsg = msg;
    }

    private void setScannedImage(Bitmap image){ scannedImage = image; }

    //Other functions
    private static int calcScaleFactor(int rows, int cols){
        int idealRow, idealCol;
        if(rows<cols){
            idealRow = 240;
            idealCol = 320;
        } else {
            idealCol = 240;
            idealRow = 320;
        }
        int val = Math.min(rows / idealRow, cols / idealCol);
        if(val<=0){
            return 1;
        } else {
            return val;
        }
    }

    private static double calcWhiteDist(double r, double g, double b){
        return Math.sqrt(Math.pow(255 - r, 2) +
                Math.pow(255 - g, 2) + Math.pow(255 - b, 2));
    }

    private static Point findIntersection(double[] line1, double[] line2) {
        double start_x1 = line1[0], start_y1 = line1[1],
                end_x1 = line1[2], end_y1 = line1[3], start_x2 =
                line2[0], start_y2 = line2[1], end_x2 = line2[2],
                end_y2 = line2[3];
        double denominator = ((start_x1 - end_x1) * (start_y2 -
                end_y2)) - ((start_y1 - end_y1) * (start_x2 - end_x2));
        if (denominator!=0)
        {
            Point pt = new Point();
            pt.x = ((start_x1 * end_y1 - start_y1 * end_x1) *
                    (start_x2 - end_x2) - (start_x1 - end_x1) *
                    (start_x2 * end_y2 - start_y2 * end_x2)) /
                    denominator;
            pt.y = ((start_x1 * end_y1 - start_y1 * end_x1) *
                    (start_y2 - end_y2) - (start_y1 - end_y1) *
                    (start_x2 * end_y2 - start_y2 * end_x2)) /
                    denominator;
            return pt;
        }
        else
            return new Point(-1, -1);
    }

    private static boolean exists(ArrayList<Point> corners, Point pt){
        for(int i=0; i<corners.size(); i++){
            if(Math.sqrt(Math.pow(corners.get(i).x-pt.x,
                    2)+Math.pow(corners.get(i).y-pt.y, 2)) < 10){
                return true;
            }
        }
        return false;
    }

    private static void sortCorners(ArrayList<Point> corners, int scaleFactor) {
        ArrayList<Point> top, bottom;
        top = new ArrayList<Point>();
        bottom = new ArrayList<Point>();
        Point center = new Point();
        for(int i=0; i<corners.size(); i++){
            center.x += corners.get(i).x/corners.size();
            center.y += corners.get(i).y/corners.size();
        }
        for (int i = 0; i < corners.size(); i++)
        {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }
        corners.clear();
        if (top.size() == 2 && bottom.size() == 2){
            Point top_left = top.get(0).x > top.get(1).x ?
                    top.get(1) : top.get(0);
            Point top_right = top.get(0).x > top.get(1).x ?
                    top.get(0) : top.get(1);
            Point bottom_left = bottom.get(0).x > bottom.get(1).x
                    ? bottom.get(1) : bottom.get(0);
            Point bottom_right = bottom.get(0).x > bottom.get(1).x
                    ? bottom.get(0) : bottom.get(1);
            top_left.x *= scaleFactor;
            top_left.y *= scaleFactor;
            top_right.x *= scaleFactor;
            top_right.y *= scaleFactor;
            bottom_left.x *= scaleFactor;
            bottom_left.y *= scaleFactor;
            bottom_right.x *= scaleFactor;
            bottom_right.y *= scaleFactor;
            corners.add(top_left);
            corners.add(top_right);
            corners.add(bottom_right);
            corners.add(bottom_left);
        }
    }

    public static Bitmap drawSelection(ArrayList<Point> corners, Bitmap image, int scaleFactor, Scalar color){
        Bitmap resultBitmap;
        Mat imageRGBA = Mat.zeros(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
        Mat imageRGB = Mat.zeros(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        Utils.bitmapToMat(image, imageRGBA);
        Imgproc.cvtColor(imageRGBA, imageRGB, Imgproc.COLOR_BGRA2BGR);
        sortCorners(corners, scaleFactor);
        Imgproc.line(imageRGB,corners.get(0),corners.get(1),color,2,8,0);
        Imgproc.line(imageRGB,corners.get(1),corners.get(2),color,2,8,0);
        Imgproc.line(imageRGB,corners.get(2),corners.get(3),color,2,8,0);
        Imgproc.line(imageRGB,corners.get(3),corners.get(0),color,2,8,0);
        resultBitmap = Bitmap.createBitmap(imageRGB.cols(), imageRGB.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageRGB, resultBitmap);
        return resultBitmap;
    }

    private static Mat binaryImageKMeans(Mat image){
        /*
        First of all, we will make our image in the desired form to perform a k-means
        clustering with two clusters.
        */
        Mat samples = new Mat(image.rows() * image.cols(), 3, CvType.CV_32F);
        for( int y = 0; y < image.rows(); y++ ) {
            for( int x = 0; x < image.cols(); x++ ) {
                for( int z = 0; z < 3; z++) {
                    samples.put(x + y*image.cols(), z, image.get(y,x)[z]);
                }
            }
        }
        /*
        Then, we will apply the k-means algorithm as follows:
        */
        int clusterCount = 2;
        int attempts = 5;
        Mat labels = new Mat();
        Mat centers = new Mat();
        Core.kmeans(samples, clusterCount, labels, new
                        TermCriteria(TermCriteria.MAX_ITER |
                        TermCriteria.EPS, 10000, 0.0001), attempts,
                Core.KMEANS_PP_CENTERS, centers);
        /*
        Now, we have the two cluster centers and the labels for each pixel in the original
        image. We will use the two cluster centers to detect which one corresponds to the
        paper. For this, we will find the Euclidian distance between the color of both the
        centers and the color pure white. The one which is closer to the color pure white will
        be considered as the foreground:
        */

        double dstCenter0 = calcWhiteDist(centers.get(0,
                0)[0], centers.get(0, 1)[0], centers.get(0, 2)[0]);
        double dstCenter1 = calcWhiteDist(centers.get(1,
                0)[0], centers.get(1, 1)[0], centers.get(1, 2)[0]);
        int paperCluster = (dstCenter0 < dstCenter1)?0:1;
        /*
        We need to define two Mat objects that we will use in the next step:
        */
        Mat srcRes = new Mat( image.size(), image.type() );
        /*
        Now, we will perform a segmentation where we will display all the foreground
        pixels as white and all the background pixels as black:
        */
        for( int y = 0; y < image.rows(); y++ ) {
            for( int x = 0; x < image.cols(); x++ )
            {
                int cluster_idx = (int)labels.get(x + y*image.cols(),0)[0];
                if(cluster_idx != paperCluster){
                    srcRes.put(y,x, 0, 0, 0, 255);
                } else {
                    srcRes.put(y,x, 255, 255, 255, 255);
                }
            }
        }
        return srcRes;

    }

    private static Mat biggestContour(Mat image){
        Mat srcGray = new Mat();
        /*
        Now, we will move on to the next step; that is, detecting contours in this image.
        First, we will apply the Canny edge detector to detect just the edges and then
        apply a contouring algorithm:
        */
        Imgproc.cvtColor(image, srcGray, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.GaussianBlur(srcGray, srcGray, new Size(5, 5), 0);
        Imgproc.Canny(srcGray, srcGray, 50, 150);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(srcGray, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

                /*
                We now make an assumption that the page occupies the biggest part of the
                foreground and so it corresponds to the biggest contour we find:
                 */

        int index = 0;
        double maxim = Imgproc.contourArea(contours.get(0));
        for (int contourIdx = 1; contourIdx < contours.size(); contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contours.get(contourIdx));
            if(maxim<temp) {
                maxim=temp;
                index=contourIdx;
            }
        }

        Mat drawing = Mat.zeros(image.size(), CvType.CV_8UC1);
        Imgproc.drawContours(drawing, contours, index, new Scalar(255), 1);
        return drawing;
    }

    private static ArrayList<Point> reduceToFourCorners(ArrayList<Point> corners){
        ArrayList<Point> boundaries = new ArrayList<Point>();
        //Calculate the geometric center of points
        //X coordinate
        Point center=new Point();
        for (int i=0; i<corners.size(); i++){
            center.x += corners.get(i).x;
            center.y += corners.get(i).y;
        }
        center.x = center.x/corners.size();
        center.y = center.y/corners.size();
        //Create an array with de distances beetwen the center and each point
        ArrayList<Double> distances = new ArrayList<Double>();
        for (int i=0; i<corners.size(); i++){
            distances.add(Math.sqrt(Math.pow(corners.get(i).x - center.x, 2) + Math.pow(corners.get(i).y - center.y, 2)));
        }
        //Get the four biggest distances
        //Distance 1
        double biggestDist1 = -1;
        int biggestDistIndex1 = -1;
        for (int i=0; i<distances.size(); i++){
            if(biggestDist1==-1){
                biggestDist1 = distances.get(i);
                biggestDistIndex1 = i;
            }else{
                if(biggestDist1 < distances.get(i)){
                    biggestDist1 = distances.get(i);
                    biggestDistIndex1 = i;
                }
            }
        }
        boundaries.add(corners.get(biggestDistIndex1));
        //Distance 2
        double biggestDist2 = -1;
        int biggestDistIndex2 = -1;
        for (int i=0; i<distances.size(); i++){
            if(i != biggestDistIndex1){
                if(biggestDist2==-1){
                    biggestDist2 = distances.get(i);
                    biggestDistIndex2 = i;
                }else{
                    if(biggestDist2 < distances.get(i)){
                        biggestDist2 = distances.get(i);
                        biggestDistIndex2 = i;
                    }
                }
            }
        }
        boundaries.add(corners.get(biggestDistIndex2));
        //Distance 3
        double biggestDist3 = -1;
        int biggestDistIndex3 = -1;
        for (int i=0; i<distances.size(); i++){
            if(i != biggestDistIndex1 && i != biggestDistIndex2){
                if(biggestDist3==-1){
                    biggestDist3 = distances.get(i);
                    biggestDistIndex3 = i;
                }else{
                    if(biggestDist3 < distances.get(i)){
                        biggestDist3 = distances.get(i);
                        biggestDistIndex3 = i;
                    }
                }
            }
        }
        boundaries.add(corners.get(biggestDistIndex3));
        //Distance 4
        double biggestDist4 = -1;
        int biggestDistIndex4 = -1;
        for (int i=0; i < distances.size(); i++){
            if(i != biggestDistIndex1 && i != biggestDistIndex2 && i != biggestDistIndex3){
                if(biggestDist4 == -1){
                    biggestDist4 = distances.get(i);
                    biggestDistIndex4 = i;
                }else{
                    if(biggestDist4 < distances.get(i)){
                        biggestDist4 = distances.get(i);
                        biggestDistIndex4 = i;
                    }
                }
            }
        }
        boundaries.add(corners.get(biggestDistIndex4));
        return boundaries;
    }

    private static Mat cornersImage(ArrayList<Point> corners, Mat image, boolean debug){
        //Create image with all the corners
        Mat cornersImg = Mat.zeros(image.size(), CvType.CV_8UC1);
        for(int i=0; i<corners.size(); i++){
            if(debug){
                Log.d("Scanner Class","Corners "+Integer.toString(i)+" = "+corners.get(i).toString());
            }
            Imgproc.circle(cornersImg, corners.get(i), 1, new Scalar(255));
        }
        return cornersImg;
    }

    private static Mat perspectiveTransformation(ArrayList<Point> corners, Mat imageOriginal, int scaleFactor){
        /*
        Now that we have detected the four corners, we will try to identify their locations
        on a quadrilateral. For this, we will compare the location of each corner with the
        center of the quadrilateral, which we obtain by taking the average of the coordinates
        of each of the corners
        */
        sortCorners(corners, scaleFactor);
        /*
        We need to determine the size of the resulting image.
        For this, we will use the coordinates of the corners calculated in the earlier step:
        */
        double top = Math.sqrt(Math.pow(corners.get(0).x -
                corners.get(1).x, 2) + Math.pow(corners.get(0).y -
                corners.get(1).y, 2));
        double right = Math.sqrt(Math.pow(corners.get(1).x -
                corners.get(2).x, 2) + Math.pow(corners.get(1).y -
                corners.get(2).y, 2));
        double bottom = Math.sqrt(Math.pow(corners.get(2).x -
                corners.get(3).x, 2) + Math.pow(corners.get(2).y -
                corners.get(3).y, 2));
        double left = Math.sqrt(Math.pow(corners.get(3).x -
                corners.get(1).x, 2) + Math.pow(corners.get(3).y -
                corners.get(1).y, 2));
        Mat quad = Mat.zeros(new Size(Math.max(top, bottom),
                Math.max(left, right)), CvType.CV_8UC3);

        /*
        Now, we need to use a perspective transformation to warp the image in order to
        occupy the entire image. For this, we need to create reference corners, corresponding
        to each corner in the corners array:
        */
        ArrayList<Point> result_pts = new ArrayList<Point>();
        result_pts.add(new Point(0, 0));
        result_pts.add(new Point(quad.cols(), 0));
        result_pts.add(new Point(quad.cols(), quad.rows()));
        result_pts.add(new Point(0, quad.rows()));
        /*
        Notice how the elements in the corners are in the same order as they are in
        result_pts. This is required so as to perform a proper perspective transformation.
        Next, we will perform the perspective transformation:
        */

        Mat cornerPts = Converters.vector_Point2f_to_Mat(corners);
        Mat resultPts = Converters.vector_Point2f_to_Mat(result_pts);
        Mat transformation = Imgproc.getPerspectiveTransform(cornerPts,
                resultPts);
        Imgproc.warpPerspective(imageOriginal, quad, transformation,
                quad.size());
        Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA);
        return quad;
    }

    private static ArrayList<Point> getCornersFromLines(Mat image, boolean debug){
        /*
        Now, we will detect the lines in this image, which contain only the biggest contours.
        We will try to find the point of intersection of these lines, and use this to detect the
        corners of the page in the image:
        */

        Mat lines = new Mat();
        Imgproc.HoughLinesP(image, lines, 1, Math.PI/180, 70, 30, 10);
        ArrayList<Point> corners = new ArrayList<Point>();
        if(debug){
            Log.d("Scanner Class","Lines Cols = "+Integer.toString(lines.cols()));
            Log.d("Scanner Class","Lines Rows = "+Integer.toString(lines.rows()));
        }
        for (int i = 0; i < lines.rows(); i++) {
            for (int j = i+1; j < lines.rows(); j++) {
                double[] line1 = lines.get(i, 0);
                double[] line2 = lines.get(j, 0);
                //double[] line1 = lines.get(i, 0);
                //double[] line2 = lines.get(j, 0);
                Point pt = findIntersection(line1, line2);
                if(debug){
                    Log.d("MainActivity", "Line 1 = "+line1[0]+" "+line1[1]);
                    Log.d("MainActivity", "Line 2 = "+line2[0]+" "+line2[1]);
                    Log.d("MainActivity", "Intersección = "+pt.x+" "+pt.y);
                }
                if (pt.x >= 0 && pt.y >= 0 && pt.x <= image.cols() && pt.y <= image.rows()){
                    if(!exists(corners, pt)){
                        corners.add(pt);
                    }
                }
            }
        }
        return corners;
    }

    public boolean calcScannedImage(boolean debug){
        boolean success = false;
        Mat srcOrig = new Mat(getSrcImage().getHeight(), getSrcImage().getWidth(), CvType.CV_8UC4);
        Imgproc.cvtColor(srcOrig, srcOrig, Imgproc.COLOR_BGR2RGB);
        Utils.bitmapToMat(getSrcImage(), srcOrig);
        int scaleFactor = calcScaleFactor(srcOrig.rows(), srcOrig.cols());
        Mat src = new Mat();
        Imgproc.resize(srcOrig, src, new Size(srcOrig.cols()/scaleFactor, srcOrig.rows()/scaleFactor));
        Imgproc.GaussianBlur(src, src, new Size(5,5), 1);

        Bitmap bitmap;
        if(debug)Log.w("Scanner Class","Get Page");
        //We create a binary image (type CV_8UC4) using kmeans with 2 clusters
        //Mat srcRes = binaryImageKMeans(src);

        //Get the biggest contour (binary image with that contour)
        Mat drawing = biggestContour(src);
        //Mat drawing = biggestContour(srcRes);

        // Get the corners using line intersections (Hough)
        ArrayList<Point> corners = getCornersFromLines(drawing, debug);

        /*
        Now, we will check whether we were able to detect the four corners perfectly.
        If not, the algorithm returns an error message:
        */
        ArrayList<Point> boundaries = new ArrayList<Point>();
        if(debug)Log.d("Scanner Class","Corners Size = "+Integer.toString(corners.size()));
        if(corners.size() > 4){
            boundaries = reduceToFourCorners(corners);
        }else if(corners.size() < 4){
            errorMsg = "Cannot detect perfect corners";
            Mat cornersImg = cornersImage(corners, src, debug);
            bitmap = Bitmap.createBitmap(cornersImg.cols(), cornersImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cornersImg, bitmap);
            //bitmap = Bitmap.createBitmap(srcRes.cols(), srcRes.rows(), Bitmap.Config.ARGB_8888);
            //Utils.matToBitmap(srcRes, bitmap);
            setScannedImage(bitmap);
            //success = false;
            return success;

        }else if(corners.size() == 4){
            boundaries = corners;

        }
        corners = boundaries;

        //Prueba de la funcion drawSelection
        //Bitmap srcB = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(src,srcB);
        //Bitmap resultB = drawSelection(boundaries,srcB, scaleFactor, new Scalar(0,255,0));

        Mat quad = perspectiveTransformation(corners,srcOrig,scaleFactor);
        bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(quad, bitmap);

        //return resultB; //Prueba de la función drawSelection
        setScannedImage(bitmap);
        success = true;
        return success;
    }

     /*
     Implementation
     Bitmap selectedImage;
     String errorMsg;

     Scanner scanner = new Scanner(selectedImage);
     boolean success = scanner.calcScannedImage(false);

     if( success == true ) {
        Log.d(TAG,"Display Image Result");
        ivImage.setImageBitmap(scanner.getScannedImage());
     } else {
        errorMsg = scanner.getErrorMsg();
        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
        ivImage.setImageBitmap(scanner.getScannedImage());
     }
     */



}
