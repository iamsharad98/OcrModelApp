package com.aimonk.ocrmodelsampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import com.aimonk.ocrmodelsampleapp.ml.Craft1280800Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft1280800Float32;
import com.aimonk.ocrmodelsampleapp.ml.Craft320320Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft320320Float32;
import com.aimonk.ocrmodelsampleapp.ml.Craft480320Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft480320Float32;
import com.aimonk.ocrmodelsampleapp.ml.Craft480480Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft480480Float32;
import com.aimonk.ocrmodelsampleapp.ml.Craft640480Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft640480Float32;
import com.aimonk.ocrmodelsampleapp.ml.Craft640640Float16;
import com.aimonk.ocrmodelsampleapp.ml.Craft640640Float32;
import com.aimonk.ocrmodelsampleapp.ml.YoloCraft640480Float16;
import com.aimonk.ocrmodelsampleapp.ml.YoloCraft640640Float16;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
//        implements AdapterView.OnItemSelectedListener
{
    private Context mContext = MainActivity.this;
    public static final int RESULT_GALLERY = 0;

    private ImageView imgView;
    private Button select, predict;
    private TextView tv, inputImageTimeText, outputImageTimeText;
    private Bitmap img;
    private String TAG = "MainActivity";
    private Spinner modelSpinner, threadSpinner, hardwareSpinner;
    private String selectedModel;
    private int positionSelected = 0;
    private ProgressBar progressBar;
    // Initialize interpreter with GPU delegate
    private Model.Options options;
    private CompatibilityList compatList = new CompatibilityList();
    private int harWareSelected = -1;//index no.
    private int threadSelected = 0;//index no.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgView = (ImageView) findViewById(R.id.image_view);
        tv = (TextView) findViewById(R.id.fetchedText);
        select = (Button) findViewById(R.id.select_image);
        predict = (Button) findViewById(R.id.predict);
        inputImageTimeText = findViewById(R.id.inputImageTimeText);
        outputImageTimeText = findViewById(R.id.outputImageTimeText);
        progressBar = findViewById(R.id.progressbar);
        threadSpinner = findViewById(R.id.threadSpinner);
        hardwareSpinner = findViewById(R.id.hardwareSpinner);
        modelSpinner = findViewById(R.id.modelSpinner);
//        threadSpinner.setVisibility(View.INVISIBLE);

        //hardware spinner
        String[] hardwareItems = new String[]{"CPU DELEGATE", "GPU DELEGATE", "NNAPI DELEGATE", };
        ArrayAdapter<String> adapterHardware = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, hardwareItems);
        hardwareSpinner.setAdapter(adapterHardware);
        hardwareSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                harWareSelected = position;
                if(harWareSelected == 1 || harWareSelected == 2){
                    if(!compatList.isDelegateSupportedOnThisDevice()){
                        Toast.makeText(mContext, "Gpu Not supported, choose different hardware",
                                Toast.LENGTH_SHORT).show();
                        harWareSelected = 0;
                    }else{
                        Toast.makeText(mContext, "Hurray, your device supports GPU", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //thread spinner
        String[] threadItems = new String[]{"1", "2", "3", "4",};
        ArrayAdapter<String> threadAdapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, threadItems);
        threadSpinner.setAdapter(threadAdapter);
        threadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (harWareSelected ==0){
                    threadSelected = position;
                }else{
                    Toast.makeText(mContext, "Please select Cpu first", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String[] modelItems = new String[]{"Float16_320_320", "Float16_480_320", "Float16_480_480", "Float16_640_480",
                "Float16_640_640", "Float16_1280_800", "YoloCraft_640_480", "YoloCraft_640_640"};
//                "Float32_320_320", "Float32_480_320", "Float32_480_480", "Float32_640_480",
//                "Float32_640_640", "Float32_1280_800",};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, modelItems);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                positionSelected = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callGalleryIntent();
            }
        });

        predict.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
//                progressBar.setVisibility(View.VISIBLE);
//                new AsyncPredictData(mContext).execute();
                try {
                    predictText();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void predictText() throws IOException {
//        Log.d(TAG, "predictText: Position Selected " + positionSelected);

//        if(compatList.isDelegateSupportedOnThisDevice()){
////             if the device has a supported GPU, add the GPU delegate
//            options = new Model.Options.Builder().setDevice(Model.Device.GPU).build();
//            Log.d(TAG, "predictText: Run on GPU ");
//        } else {
//            // if the GPU is not supported, run on 4 threads
//            options = new Model.Options.Builder().setNumThreads(4).build();
//            Log.d(TAG, "predictText: Run on CPU with 4 threads");
//        }
        if (harWareSelected == 0){
            int noOfThread = threadSelected+1;
            Log.d(TAG, "predictText: Runs on Cpu with noOfThreads " + noOfThread);
            options = new Model.Options.Builder().setNumThreads(noOfThread).build();
        }else if (harWareSelected == 1 ){
            Log.d(TAG, "predictText: Runs on GPU ");
            if(!compatList.isDelegateSupportedOnThisDevice()){
                Toast.makeText(mContext, "Gpu Not supported, Switch to cpu",
                        Toast.LENGTH_SHORT).show();
                options = new Model.Options.Builder().setNumThreads(threadSelected+1).build();
            }else{
                Toast.makeText(mContext, "Hurray, your device supports GPU", Toast.LENGTH_SHORT).show();
                options = new Model.Options.Builder().setDevice(Model.Device.GPU).build();
            }
        }else if(harWareSelected == 2){
            Log.d(TAG, "predictText: Runs on NNAPI ");
            if(!compatList.isDelegateSupportedOnThisDevice()){
                Toast.makeText(mContext, "NNAPI Not supported, Switch to cpu",
                        Toast.LENGTH_SHORT).show();
                options = new Model.Options.Builder().setNumThreads(threadSelected+1).build();
            }else{
                Toast.makeText(mContext, "Hurray, your device supports GPU", Toast.LENGTH_SHORT).show();
                options = new Model.Options.Builder().setDevice(Model.Device.NNAPI).build();
            }
        }

        ArrayList<ArrayList<long[]>> resArr = new ArrayList<>();
        switch (positionSelected) {
            case 0:
                ArrayList<long[]> arr0 = new ArrayList<>();

                Bitmap bmp0 = Bitmap.createScaledBitmap(img, 320, 320, true);
                bmp0 = bmp0.copy(Bitmap.Config.ARGB_8888, true);

                Craft320320Float16 model = Craft320320Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr0.add(model16_320_320(model, bmp0));
                }
                model.close();
                resArr.add(arr0);
//                Toast.makeText(mContext, "Float 16 - 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-320*320";

                break;
            case 1:
                ArrayList<long[]> arr1 = new ArrayList<>();
                Bitmap bmp1 = Bitmap.createScaledBitmap(img, 480, 320, true);
                bmp1 = bmp1.copy(Bitmap.Config.ARGB_8888, true);

                Craft480320Float16 model1 = Craft480320Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr1.add(model16_480_320(model1, bmp1));
                }
                model1.close();
                resArr.add(arr1);
//                Toast.makeText(mContext, "Float 16 - 480*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*320";

                break;
            case 2:

                ArrayList<long[]> arr2 = new ArrayList<>();
                Bitmap bmp2 = Bitmap.createScaledBitmap(img, 480, 480, true);
                bmp2 = bmp2.copy(Bitmap.Config.ARGB_8888, true);

                Craft480480Float16 model2 = Craft480480Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr2.add(model16_480_480(model2, bmp2));
                }
                model2.close();
                resArr.add(arr2);
//                Toast.makeText(mContext, "Float 16 - 480*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*480";

                break;
            case 3:
                ArrayList<long[]> arr3 = new ArrayList<>();
                Bitmap bmp3 = Bitmap.createScaledBitmap(img, 640, 480, true);
                bmp3 = bmp3.copy(Bitmap.Config.ARGB_8888, true);

                Craft640480Float16 model3 = Craft640480Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr3.add(model16_640_480(model3, bmp3));
                }
                model3.close();
                resArr.add(arr3);
//                Toast.makeText(mContext, "Float 16 - 640*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-640*480";
                break;
            case 4:
                ArrayList<long[]> arr4 = new ArrayList<>();
                Bitmap bmp4 = Bitmap.createScaledBitmap(img, 640, 640, true);
                bmp4 = bmp4.copy(Bitmap.Config.ARGB_8888, true);

                Craft640640Float16 model4 = Craft640640Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr4.add(model16_640_640(model4, bmp4));
                }
                model4.close();
                resArr.add(arr4);
//                Toast.makeText(mContext, "Float 16 - 640*640 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-640*640";
                break;
            case 5:
                ArrayList<long[]> arr5 = new ArrayList<>();
                Bitmap bmp5 = Bitmap.createScaledBitmap(img, 1280, 800, true);
                bmp5 = bmp5.copy(Bitmap.Config.ARGB_8888, true);

                Craft1280800Float16 model5 = Craft1280800Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr5.add(model16_1280_800(model5, bmp5));
                }
                model5.close();
                resArr.add(arr5);
//                Toast.makeText(mContext, "Float 16 - 1280*800 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-1280*800";
                break;
            case 6:
                Toast.makeText(mContext, "No Model Available here", Toast.LENGTH_SHORT).show();
                ArrayList<long[]> arr6 = new ArrayList<>();
                Bitmap bmp6 = Bitmap.createScaledBitmap(img, 640, 480, true);
                bmp6 = bmp6.copy(Bitmap.Config.ARGB_8888, true);

                YoloCraft640480Float16 model6 = YoloCraft640480Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr6.add(modelYolo640480(model6, bmp6));
                }
                model6.close();
                resArr.add(arr6);
//                ArrayList<long[]> arr6 = new ArrayList<>();
//                Bitmap bmp6 = Bitmap.createScaledBitmap(img, 320, 320, true);
//                bmp6 = bmp6.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft320320Float32 model6 = Craft320320Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr6.add(model32_320_320(model6, bmp6));
//                }
//                model6.close();
//                resArr.add(arr6);
////                Toast.makeText(mContext, "Float 32 - 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "YOLO-640_480";

                break;
            case 7:

                Toast.makeText(mContext, "No Model Available here", Toast.LENGTH_SHORT).show();
                ArrayList<long[]> arr7 = new ArrayList<>();
                Bitmap bmp7 = Bitmap.createScaledBitmap(img, 640, 640, true);
                bmp7 = bmp7.copy(Bitmap.Config.ARGB_8888, true);

                YoloCraft640640Float16 model7 = YoloCraft640640Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr7.add(modelYolo640640(model7, bmp7));
                }
                model7.close();
                resArr.add(arr7);
//                ArrayList<long[]> arr7 = new ArrayList<>();
//                Bitmap bmp7 = Bitmap.createScaledBitmap(img, 480, 320, true);
//                bmp7 = bmp7.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft480320Float32 model7 = Craft480320Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr7.add(model32_480_320(model7, bmp7));
//                }
//                model7.close();
//                resArr.add(arr7);
////                Toast.makeText(mContext, "Float 32 - 480*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "Yolo_640_640";
                // Whatever you want to happen when the third item gets selected
                break;
//            case 8:
//                ArrayList<long[]> arr8 = new ArrayList<>();
//                Bitmap bmp8 = Bitmap.createScaledBitmap(img, 480, 480, true);
//                bmp8 = bmp8.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft480480Float32 model8 = Craft480480Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr8.add(model32_480_480(model8, bmp8));
//                }
//                model8.close();
//                resArr.add(arr8);
////                Toast.makeText(mContext, "Float 32 - 480*480 Selected", Toast.LENGTH_SHORT).show();
//                selectedModel = "32-480*480";
//                break;
//            case 9:
//                ArrayList<long[]> arr9 = new ArrayList<>();
//                Bitmap bmp9 = Bitmap.createScaledBitmap(img, 640, 480, true);
//                bmp9 = bmp9.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft640480Float32 model9 = Craft640480Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr9.add(model32_640_480(model9, bmp9));
//                }
//                model9.close();
//                resArr.add(arr9);
////                Toast.makeText(mContext, "Float 32 - 640*480 Selected", Toast.LENGTH_SHORT).show();
//                selectedModel = "32-640*480";
//                // Whatever you want to happen when the third item gets selected
//                break;
//            case 10:
//                ArrayList<long[]> arr10 = new ArrayList<>();
//                Bitmap bmp10 = Bitmap.createScaledBitmap(img, 640, 640, true);
//                bmp10 = bmp10.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft640640Float32 model10 = Craft640640Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr10.add(model32_640_640(model10, bmp10));
//                }
//                model10.close();
//                resArr.add(arr10);
////                Toast.makeText(mContext, "Float 32 - 640*640 Selected", Toast.LENGTH_SHORT).show();
//                selectedModel = "32-640*640";
//                break;
//            case 11:
//                ArrayList<long[]> arr11 = new ArrayList<>();
//                Bitmap bmp11 = Bitmap.createScaledBitmap(img, 1280, 800, true);
//                bmp11 = bmp11.copy(Bitmap.Config.ARGB_8888, true);
//
//                Craft1280800Float32 model11 = Craft1280800Float32.newInstance(mContext, options);
//
//                for (int i = 0; i< 16; i++){
//                    arr11.add(model32_1280_800(model11, bmp11));
//                }
//                model11.close();
//                resArr.add(arr11);
////                Toast.makeText(mContext, "Float 32 - 1280*800 Selected", Toast.LENGTH_SHORT).show();
//                selectedModel = "32-1280*800";
//                // Whatever you want to happen when the third item gets selected
//                break;
            default:
                ArrayList<long[]> arr12 = new ArrayList<>();
                Bitmap bmp12 = Bitmap.createScaledBitmap(img, 320, 320, true);
                bmp12 = bmp12.copy(Bitmap.Config.ARGB_8888, true);

                Craft320320Float16 model12 = Craft320320Float16.newInstance(mContext, options);

                for (int i = 0; i< 16; i++){
                    arr12.add(model16_320_320(model12, bmp12));
                }
                model12.close();
                resArr.add(arr12);
//                Toast.makeText(mContext, "By Default FLoat 16- 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*480";
        }
        int tempInputTime = 0;
        int tempOutputTime = 0;
        int size = 0;
        for(int i = 0; i< resArr.size(); i++){
            for (int j = 1; j< resArr.get(i).size()-1; j++){
                size = resArr.get(i).size();

                tempInputTime += resArr.get(i).get(j)[0];
                tempOutputTime += resArr.get(i).get(j)[1];

//                Log.d(TAG, "predictText: i " + i + " j " + j + " k " + 1);
                Log.d(TAG, "Model " + selectedModel  + "predictText: ResArr " + resArr.get(i).get(j)[1]);
            }
        }
        size = 14;

        Log.d(TAG, "Model " + selectedModel  + "predictText: Total No. of times model run " + size);

        double meanInputTime = tempInputTime/size;
        double meanOutputTime = tempOutputTime/size;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
                inputImageTimeText.setText("MeanInputImageTimeDifference: "+ meanInputTime);
                outputImageTimeText.setText("MeanOutputImageTimeDifference: " + meanOutputTime);
                Log.d(TAG, "Model " + selectedModel  + "predictText: Mean of time to Input Image  "
                        + meanInputTime + " Mean of time to Output Image " + meanOutputTime);
//                writeLogs("selectedModel " + selectedModel + "\n" + "MeanInputImageTimeDifference: " +
//                        meanInputTime + "\n" +  " MeanOutputImageTimeDifference: " + meanOutputTime);
            }
        });
    }

    private void writeLogs(String data){

        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( getExternalFilesDir(null)  + "/" + "TezzScanner" );
            File logDirectory = new File( appDirectory + "/logs" );
            File logFile = new File( logDirectory, "logcat_file" + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }
//
//            String path = getExternalFilesDir(null)  + "/" + "TezzScanner" + "/logs";
//            File f = new File(path);
//            File fileTo = new File(f.toString() + "/logcat_file.png");

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    private void callGalleryIntent() {
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent , RESULT_GALLERY );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_GALLERY :
                if (data != null) {

                    Uri imageUri = data.getData();
                    Log.d("scanner", "Uri gallery image path " + imageUri.getPath());

                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(mContext.getContentResolver(), imageUri));
                        } else {
                            bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageUri);
                        }
//                        scaleBitmap(bitmap);
                        imgView.setImageBitmap(bitmap);
                        img = bitmap;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            default:
                break;
        }
    }

    public class AsyncPredictData extends AsyncTask<String, Integer, String> {
        Context context;
        ProgressDialog progressDialog;

        public AsyncPredictData(Context context2) {
            context = context2;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            Log.d("MonkVision", "in PreExecute ");
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Please wait...");
            progressDialog.setMessage("Predicting Mean Time...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {

            try {
                predictText();

                publishProgress(100);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "doInBackground: Exception " + e.getMessage());
            }
            String res = "success";

            return res;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

//            Log.d("scanner", "in onProgresUpdate ");

            this.progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String file) {
            super.onPostExecute(file);
            Log.d("scanner", "in onPostExecute ");

            progressDialog.dismiss();
        }
    }


    @SuppressLint("SetTextI18n")
    private long[] modelYolo640480(YoloCraft640480Float16 model, Bitmap bmp) {

        long[] res = new long[2];
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "model16_320_320: Before InputFeature" + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "model16_320_320: After InputFeature and Before Outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;

        // Runs model inference and gets result.
        YoloCraft640480Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
        long outputTimeDiff = date3.getTime() - date2.getTime();
        res[1] = outputTimeDiff;
        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] modelYolo640640(YoloCraft640640Float16 model, Bitmap bmp) {

        long[] res = new long[2];
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "model16_320_320: Before InputFeature" + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "model16_320_320: After InputFeature and Before Outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;

        // Runs model inference and gets result.
        YoloCraft640640Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
        long outputTimeDiff = date3.getTime() - date2.getTime();
        res[1] = outputTimeDiff;
        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model16_320_320(Craft320320Float16 model, Bitmap bmp) {

        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 320, 320, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

        //            Craft320320Float16 model = Craft320320Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "model16_320_320: Before InputFeature" + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 320, 320}, DataType.FLOAT32);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "model16_320_320: After InputFeature and Before Outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
            }
        });

        // Runs model inference and gets result.
        Craft320320Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "model16_320_320: After Outputs " + formatter3.format(date3));

        long outputTimeDiff = date3.getTime() - date2.getTime();
        res[1] = outputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
            }
        });

//            Log.d(TAG, "onClick: output feature " + selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
            }
        });
        // Releases model resources if no longer used.
//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model16_480_320(Craft480320Float16 model, Bitmap bmp){

        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 480, 320, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//        Craft480320Float16 model = Craft480320Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft480320Float16: Before InputFeature " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 320}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft480320Float16: After InputFeature and Before Outputs " + formatter2.format(date2));
        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeText " + inputTimeDiff);
            }
        });

        // Runs model inference and gets result.
        Craft480320Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft480320Float16: After Outputs " + formatter3.format(date3));

        long outputDiff = date3.getTime() - date2.getTime();
        res[1] = outputDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeText " + outputDiff);
            }
        });

//            Log.d(TAG, "onClick: output feature "+  selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

        // Releases model resources if no longer used.
//        model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model16_480_480(Craft480480Float16 model, Bitmap bmp){

        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 480, 480, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//        Craft480480Float16 model = Craft480480Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft480480Float16: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 480}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();

        inputFeature0.loadBuffer(byteBuffer);
        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft480480Float16: After Input and Before Outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeText " + inputTimeDiff);
            }
        });
        // Runs model inference and gets result.
        Craft480480Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft480480Float16: After Outputs " + formatter3.format(date3));

        long outputDiff = date3.getTime() - date2.getTime();
        res[1] = outputDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("outputImageTimeText " + outputDiff);
            }
        });
//            Log.d(TAG, "onClick: output feature " + selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model16_640_480(Craft640480Float16 model, Bitmap bmp){
        long[] res = new long[2];

//        img = Bitmap.createScaledBitmap(img, 640, 480, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//          Craft640480Float16 model = Craft640480Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft640480Float16: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft640480Float16: After Inputs and Before Outputs " + formatter2.format(date2));
        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeText " + inputTimeDiff);
            }
        });


        // Runs model inference and gets result.
        Craft640480Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft640480Float16: After Outputs " + formatter3.format(date3));

        long outputDiff = date3.getTime() - date2.getTime();
        res[1] = outputDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("outputImageTimeText " + outputDiff);
            }
        });


//            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//            model.close();

        return res;

    }

    @SuppressLint("SetTextI18n")
    private long[] model16_640_640(Craft640640Float16 model, Bitmap bmp){
        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 640, 640, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft640640Float16 model = Craft640640Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft640640Float16: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 640}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

//        writeInputData(inputFeature0.getFloatArray());
        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft640640Float16: After Inputs and Before Outputs " + formatter2.format(date2));


        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;

        // Runs model inference and gets result.
        Craft640640Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

//        for (int j = 0; j< outputArr.length; j++){
//
//            Log.d(TAG, "model16_640_640: outputArr " + outputArr[j] + "\n");
//        }
//        writeOutputData(outputFeature0.getFloatArray());
//        Log.d(TAG, "model16_640_640: OutputArr length " + outputArr.length);

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft640640Float16: After Outputs " + formatter3.format(date3));


        long outputDiff = date3.getTime() - date2.getTime();
        res[1] = outputDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("outputImageTimeText " + outputDiff);
            }
        });

//            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

        // Releases model resources if no longer used.
//        model.close();

        return res;
    }


    private void writeInputData(float[] inputArr) {
        StringBuilder outPutString = new StringBuilder();
        Log.d(TAG, "writeDataInFile: Start");
        for (int j = 0; j<inputArr.length; j++){
            outPutString.append(inputArr[j]).append("\n");
        }

        // Get the directory for the user's public pictures directory.
        String path =
                getExternalFilesDir(null) + File.separator  + "TezzScanner";

        // Create the folder.
        File folder = new File(path);
        folder.mkdirs();

        // Create the file.
        File file = new File(folder, "inputConfig.txt");

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(outPutString);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }


    private void writeOutputData(float[] outputArr) {
        StringBuilder outPutString = new StringBuilder();
        Log.d(TAG, "writeDataInFile: Start");
        for (int j = 0; j<outputArr.length; j++){
            outPutString.append(outputArr[j]).append("\n");
        }

        // Get the directory for the user's public pictures directory.
        String path =
                getExternalFilesDir(null) + File.separator  + "TezzScanner";

        // Create the folder.
        File folder = new File(path);
        folder.mkdirs();

        // Create the file.
        File file = new File(folder, "outputConfig.txt");

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(outPutString);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    @SuppressLint("SetTextI18n")
    private long[] model16_1280_800(Craft1280800Float16 model, Bitmap bmp){

        long[] res = new long[2];

//        img = Bitmap.createScaledBitmap(img, 1280, 800, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft1280800Float16 model = Craft1280800Float16.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "model16_1280_800: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 1280, 800}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "model16_1280_800: After Inputs and Before outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
            }
        });

        // Runs model inference and gets result.
        Craft1280800Float16.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "model16_1280_800: After outputs " + formatter3.format(date3));

        long outputTimeDiff = date3.getTime() - date2.getTime();
        res[1] = outputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
            }
        });

//            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

        // Releases model resources if no longer used.
//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model32_320_320(Craft320320Float32 model, Bitmap bmp){

        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 320, 320, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft320320Float32 model = Craft320320Float32.newInstance(mContext);

        //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft320320Float32: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 320, 320}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft320320Float32: After Inputs and Before Outputs " + formatter2.format(date2));
        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeText " + inputTimeDiff);
            }
        });
        // Runs model inference and gets result
        Craft320320Float32.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft320320Float32: After Outputs " + formatter3.format(date3));


        long outputDiff = date3.getTime() - date2.getTime();
        res[1] = outputDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("outputImageTimeText " + outputDiff);
            }
        });
//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

        // Releases model resources if no longer used.
//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model32_480_320(Craft480320Float32 model, Bitmap bmp){
        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 480, 320, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft480320Float32 model = Craft480320Float32.newInstance(mContext);

            //get the current time
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
//            Log.d(TAG, "Craft480320Float32: Before Inputs " + formatter.format(date));

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 320}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bmp);
        ByteBuffer byteBuffer = tensorImage.getBuffer();
        inputFeature0.loadBuffer(byteBuffer);

        //get the current time
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date2 = new Date();
//            Log.d(TAG, "Craft480320Float32: After Inputs and Before Outputs " + formatter2.format(date2));

        long inputTimeDiff = date2.getTime() - date.getTime();
        res[0] = inputTimeDiff;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
            }
        });
        // Runs model inference and gets result.
        Craft480320Float32.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        //get the current time
        SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date3 = new Date();
//            Log.d(TAG, "Craft480320Float32: After Outputs " + formatter3.format(date3));
        long outputTimeDiff = date3.getTime() - date2.getTime();
        res[1] = outputTimeDiff;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
            }
        });
//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

        // Releases model resources if no longer used.
//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model32_480_480(Craft480480Float32 model, Bitmap bmp){
        long[] res = new long[2];

//        img = Bitmap.createScaledBitmap(img, 480, 480, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft480480Float32 model = Craft480480Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
//            Log.d(TAG, "Craft480480Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bmp);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
//            Log.d(TAG, "Craft480480Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            long inputTimeDiff = date2.getTime() - date.getTime();
            res[0] = inputTimeDiff;

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
                }
            });
            // Runs model inference and gets result.
            Craft480480Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
//            Log.d(TAG, "Craft480480Float32: After Outputs " + formatter3.format(date3));

            long outputTimeDiff = date3.getTime() - date2.getTime();
            res[1] = outputTimeDiff;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
                }
            });
//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
//            model.close();

        return res;
    }
    @SuppressLint("SetTextI18n")
    private long[] model32_640_480(Craft640480Float32 model, Bitmap bmp){

        long[] res = new long[2];
//        img = Bitmap.createScaledBitmap(img, 640, 480, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft640480Float32 model = Craft640480Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
//            Log.d(TAG, "Craft640480Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bmp);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
//            Log.d(TAG, "Craft640480Float32: After inputs and Before Outputs " + formatter2.format(date2));

            long inputTimeDiff = date2.getTime() - date.getTime();
            res[0] = inputTimeDiff;

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
                }
            });

            // Runs model inference and gets result.
            Craft640480Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
//            Log.d(TAG, "Craft640480Float32: After Outputs " + formatter3.format(date3));

            long outputTimeDiff = date3.getTime() - date2.getTime();
            res[1] = outputTimeDiff;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
                }
            });

//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
//            model.close();

        return res;

    }
    @SuppressLint("SetTextI18n")
    private long[] model32_640_640(Craft640640Float32 model, Bitmap bmp){
        long[] res = new long[2];

//        img = Bitmap.createScaledBitmap(img, 640, 640, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft640640Float32 model = Craft640640Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
//            Log.d(TAG, "Craft640640Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 640}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bmp);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
//            Log.d(TAG, "Craft640640Float32: After inputs and Before outputs " + formatter2.format(date2));

            long inputTimeDiff = date2.getTime() - date.getTime();
            res[0] = inputTimeDiff;

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
                }
            });

            // Runs model inference and gets result.
            Craft640640Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
//            Log.d(TAG, "Craft640640Float32: After Outputs " + formatter3.format(date3));

            long outputTimeDiff = date3.getTime() - date2.getTime();
            res[1] = outputTimeDiff;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
                }
            });

//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
//            model.close();

        return res;
    }

    @SuppressLint("SetTextI18n")
    private long[] model32_1280_800(Craft1280800Float32 model, Bitmap bmp){
        long[] res = new long[2];

//        img = Bitmap.createScaledBitmap(img, 1280, 800, true);
//        img = img.copy(Bitmap.Config.ARGB_8888, true);

//            Craft1280800Float32 model = Craft1280800Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
//            Log.d(TAG, "Craft1280800Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 1280, 800}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bmp);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
//            Log.d(TAG, "Craft1280800Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            long inputTimeDiff = date2.getTime() - date.getTime();
            res[0] = inputTimeDiff;

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    inputImageTimeText.setText("InputImageTimeDifference: "+ inputTimeDiff);
                }
            });

            // Runs model inference and gets result.
            Craft1280800Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
//            Log.d(TAG, "Craft1280800Float32: After Outputs " + formatter3.format(date3));

            long outputTimeDiff = date3.getTime() - date2.getTime();
            res[1] = outputTimeDiff;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
//                    outputImageTimeText.setText("OutputImageTimeDifference: "+ outputTimeDiff);
                }
            });
//            Log.d(TAG, "onClick: output feature "+
//                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
//
//            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
//            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
//            model.close();

        return res;
    }

}

