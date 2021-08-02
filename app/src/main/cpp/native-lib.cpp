#include<cmath>
#include<opencv2/dnn/dnn.hpp>
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <android/log.h>

using namespace cv;
using namespace std;

void decode_maps(Mat& textmap, Mat& linkmap, std::vector<std::vector<Point2f>>& sort_box, float text_threshold, float link_threshold, float region_threshold,int dbg = 0);

extern "C" JNIEXPORT jfloatArray
Java_com_aimonk_ocrmodelsampleapp_MainActivity_textDetector(JNIEnv * env, jobject instance, jlong incomingImage)
{

	__android_log_print(ANDROID_LOG_INFO, "scanner", "Function Cpp start");

	jlong points[8];
	Mat im = *(Mat*)incomingImage;
	vector<Mat> imgs;
	vector<vector<Point2f>> sort_box; //Point2f(4) reserve space beforehand for better perf
	float dummyf = 1.0f;
	int length2D = 8,length1D;
	float text_threshold = 0.7, link_threshold = 0.3, region_threshold = 0.35;

	imgs.reserve(2);
	split(incomingImage,imgs);
	decode_maps(textmap,linkmap,sort_box, float text_threshold, float link_threshold, float region_threshol);
	length1D = sort_box.size();
	//    str = env->NewStringUTF(s.c_str());
//	    LOGD( "This is a number from JNI: %w", s );
	__android_log_print(ANDROID_LOG_INFO, "scanner", "Function succesfully run ");

	 // Get the float array class
//    jclass floatArrayClass = (*env)->FindClass(env, "[F");
//
//    // Check if we properly got the float array class
//    if (floatArrayClass == NULL)
//    {
//        // Ooops
//        return NULL;
//    }
//	// Create the returnable 2D array
//    jobjectArray rect_points = (*env)->NewObjectArray(env, (jsize) length1D, floatArrayClass, NULL);
//
//    // Go through the firs dimension and add the second dimension arrays
//    for (unsigned int i = 0; i < length1D; i++)
//    {
//    	//think of a better way
//        array<float,8> pts= {sort_box[i][0].x,sort_box[i][0].y,sort_box[i][1].x,sort_box[i][1].y,sort_box[i][2].x,sort_box[i][2].y,sort_box[i][3].x,sort_box[i][3].y}
//        jfloatArray floatArray = (*env)->NewFloatArray(env, length2D);
//        (*env)->SetFloatArrayRegion(env, floatArray, (jsize) 0, (jsize) length2D, (jfloat*) pts);
//        (*env)->SetObjectArrayElement(env, rect_points, (jsize) i, floatArray);
//        (*env)->DeleteLocalRef(env, floatArray);
//    }
	jfloatArray resultJNIarr = env->NewFloatArray( sort_box.length() * 8);
//	 allocate 4 points as 8 integers
	 if (NULL == resultJNIarr) return  NULL ;
//	 exception Handling
	 env->SetLongArrayRegion(resultJNIarr, 0, 8, points);
//	 copy
	 return resultJNIarr;

    // Return a Java consumable 2D float array
//    return rect_points;

    //old method
    //jlongArray resultJNIarr = env->NewLongArray(8);// allocate 4 points as 8 integers
    //if (NULL == resultJNIarr) return  NULL; //exception Handling
    //env->SetLongArrayRegion(resultJNIarr, 0, 8, points); //copy
    //return resultJNIarr;
}
//extern "C" JNIEXPORT jstring

extern "C" jstring
Java_com_aimonk_ocrmodelsampleapp_MainActivity_stringFromJNI(
		JNIEnv* env,
		jobject /* this */) {
//	cv::Mat im = cv::imread("drawable://testimg14.jpg");
	std::string hello = "Hello I am running CPP";
	return env->NewStringUTF(hello.c_str());
}

void decode_maps(Mat& textmap, Mat& linkmap, std::vector<std::vector<Point2f>>& sort_box, float text_threshold, float link_threshold, float region_threshold,int dbg)
{
	//if(dbg) std::cout << "decodeing\n";

	int img_h = textmap.rows;
	int img_w = textmap.cols;
	
	Mat text_score(textmap.size(),CV_8UC1), link_score(linkmap.size(),CV_8UC1);
	threshold(textmap, text_score, region_threshold, 1, 0);
	threshold(linkmap, link_score, link_threshold, 1, 0);
	text_score.convertTo(text_score, CV_8UC1);
	link_score.convertTo(link_score, CV_8UC1);
	Mat text_score_comb(textmap.size(),CV_8UC1), text_dil_flag(textmap.size(),CV_8UC1);
	add(text_score, link_score,text_score_comb);
	threshold(text_score_comb, text_score_comb, 0, 1, THRESH_BINARY);
	bitwise_not(link_score,text_dil_flag);
	bitwise_and(text_dil_flag, text_score, text_dil_flag);
	Mat labels, stats, centroids;
	//return;
	int nLables = connectedComponentsWithStats(text_score_comb, labels, stats, centroids, 4);
	
	//return;
	sort_box.reserve(nLables-1);
	for (int k = 1; k < nLables; ++k)
	{
		
		//if(dbg) std::cout << k << " loop\n";
		//size filtering
		int size = stats.at<int>(k, CC_STAT_AREA);
		if (size < 10) continue;
		int x = stats.at<int>(k, CC_STAT_LEFT), y = stats.at<int>(k, CC_STAT_TOP);
		int w = stats.at<int>(k, CC_STAT_WIDTH), h = stats.at<int>(k, CC_STAT_HEIGHT);
		int niter = sqrt(size * min(w, h) / (w * h)) * 2;
		
		//Extending axis alinged box according to determin   e kernel size
		int sx = x - niter, ex = x + w + niter + 1, sy = y - niter, ey = y + h + niter + 1;

		//boundry check
		if (sx < 0)  sx = 0;
		if (sy < 0)  sy = 0;
		if (ex >= img_w) ex = img_w;
		if (ey >= img_h) ey = img_h;

		//if(dbg) std::cout << "label_flag\n";
		double max_text_val;
		Mat label_flag = (labels(Rect(sx, sy, ex - sx, ey - sy)) == k) / 255;
		Mat flagged_textmap;
		multiply(textmap(Rect(sx, sy, ex - sx, ey - sy)), label_flag, flagged_textmap, 1, CV_32FC1);
		minMaxIdx(flagged_textmap, NULL, &max_text_val, NULL, NULL);
		if (max_text_val < text_threshold) continue;
		Mat dilation_inp = text_dil_flag(Rect(sx, sy, ex - sx, ey - sy)).mul(label_flag) * 255;
		//if (dbg) std::cout << "max\n";
		//if (dbg) std::cout << "dilation\n";
		Mat kernel = getStructuringElement(MORPH_RECT, Size(1 + niter, 1 + niter));
		Mat crop_seg;
		dilate(dilation_inp,crop_seg, kernel);
		std::vector<Point> crop_non_zero_index_points;
		findNonZero(crop_seg, crop_non_zero_index_points); // make sure that this does not clears the out vector
		Mat crop_non_zero_index = Mat(crop_non_zero_index_points.size(), 2, CV_32SC1, crop_non_zero_index_points.data());
		//if (dbg) std::cout << crop_non_zero_index.size() << " crop size\n";
		crop_non_zero_index.colRange(0, 1) += sx;
		crop_non_zero_index.colRange(1, 2) += sy;
		RotatedRect rectangle = minAreaRect(crop_non_zero_index);
		Point2f boxtemp[4];
		rectangle.points(boxtemp);
		std::vector<Point2f> box;
		box.reserve(4);
		box.push_back(boxtemp[1]); //top left
		box.push_back(boxtemp[2]); //top right
		box.push_back(boxtemp[3]); //bottom right
		box.push_back(boxtemp[0]); //bottom left
		//if (dbg) std::cout << "boxed\n";
		
		//align diamond-shape
		
		w = norm(box[0]-box[1]);
		h = norm(box[1]-box[2]);
		float box_ratio = max(w, h) / (min(w, h) + 1e-5);
		
		//if (dbg) std::cout << "ratio check\n";
		if (abs(1 - box_ratio) <= 0.1)
		{
			double min_x, min_y, max_x, max_y;
			minMaxIdx(crop_non_zero_index.colRange(0, 1), &min_x, &max_x, NULL, NULL);
			minMaxIdx(crop_non_zero_index.colRange(1, 2), &min_y, &max_y, NULL, NULL);
			int l = (int) min_x, r = (int) max_x, t = (int) min_y, b = (int) max_y;
			box[0] = Point2f(l, t);
			box[1] = Point2f(r, t);
			box[2] = Point2f(r, b);
			box[3] = Point2f(l, b);
		}
		sort_box.push_back(box);
	}
}
