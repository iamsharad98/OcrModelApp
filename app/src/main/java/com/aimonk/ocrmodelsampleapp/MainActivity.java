package com.aimonk.ocrmodelsampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private Context mContext = MainActivity.this;
    public static final int RESULT_GALLERY = 0;

    private ImageView imgView;
    private Button select, predict;
    private TextView tv;
    private Bitmap img;
    private String TAG = "MainActivity";
    private Spinner dropdown;
    private String selectedModel;
    private int positionSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgView = (ImageView) findViewById(R.id.image_view);
        tv = (TextView) findViewById(R.id.fetchedText);
        select = (Button) findViewById(R.id.select_image);
        predict = (Button) findViewById(R.id.predict);

        dropdown = findViewById(R.id.spinner);
        String[] items = new String[]{"Float16_320_320", "Float16_480_320", "Float16_480_480", "Float16_640_480",
                "Float16_640_640", "Float16_1280_800",
                "Float32_320_320", "Float32_480_320", "Float32_480_480", "Float32_640_480",
                "Float32_640_640", "Float32_1280_800",};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout
                .simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(this);

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
                predictText();
            }
        });
    }

    private void predictText(){
        Log.d(TAG, "predictText: Position Selected " + positionSelected);
        switch (positionSelected) {
            case 0:
                Toast.makeText(mContext, "Float 16 - 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-320*320";
                model16_320_320();
                break;
            case 1:
                Toast.makeText(mContext, "Float 16 - 480*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*320";
                model16_480_320();
                break;
            case 2:
                Toast.makeText(mContext, "Float 16 - 480*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*480";
                model16_480_480();

                break;
            case 3:
                Toast.makeText(mContext, "Float 16 - 640*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-640*480";
                model16_640_480();
                break;
            case 4:
                Toast.makeText(mContext, "Float 16 - 640*640 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-640*640";
                model16_640_640();
                break;
            case 5:
                Toast.makeText(mContext, "Float 16 - 1280*800 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-1280*800";
                model16_1280_800();
                break;
            case 6:
                Toast.makeText(mContext, "Float 32 - 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-320*320";
                model32_320_320();

                break;
            case 7:
                Toast.makeText(mContext, "Float 32 - 480*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-480*320";
                model32_480_320();
                // Whatever you want to happen when the third item gets selected
                break;
            case 8:
                Toast.makeText(mContext, "Float 32 - 480*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-480*480";
                model32_480_480();
                break;
            case 9:
                Toast.makeText(mContext, "Float 32 - 640*480 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-640*480";
                model32_640_480();
                // Whatever you want to happen when the third item gets selected
                break;
            case 10:
                Toast.makeText(mContext, "Float 32 - 640*640 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-640*640";
                model32_640_640();
                break;
            case 11:
                Toast.makeText(mContext, "Float 32 - 1280*800 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "32-1280*800";
                model32_1280_800();
                // Whatever you want to happen when the third item gets selected
                break;
            default:
                Toast.makeText(mContext, "By Default FLoat 16- 320*320 Selected", Toast.LENGTH_SHORT).show();
                selectedModel = "16-480*480";
                model16_320_320();
        }
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

    @SuppressLint("SetTextI18n")
    private void model16_320_320(){

        img = Bitmap.createScaledBitmap(img, 320, 320, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft320320Float16 model = Craft320320Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "model16_320_320: Before InputFeature" + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 320, 320}, DataType.FLOAT32);
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "model16_320_320: After InputFeature and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft320320Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "model16_320_320: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature " + selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_320_320: Exception " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void model16_480_320(){

        img = Bitmap.createScaledBitmap(img, 480, 320, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft480320Float16 model = Craft480320Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft480320Float16: Before InputFeature " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 320}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft480320Float16: After InputFeature and Before Outputs " + formatter2.format(date2));


            // Runs model inference and gets result.
            Craft480320Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft480320Float16: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+  selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_480_320: Exception " + e.getMessage());
        }

    }

    @SuppressLint("SetTextI18n")
    private void model16_480_480(){

        img = Bitmap.createScaledBitmap(img, 480, 480, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft480480Float16 model = Craft480480Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft480480Float16: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();

            inputFeature0.loadBuffer(byteBuffer);
            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft480480Float16: After Input and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft480480Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft480480Float16: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature " + selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_480_480: Exception " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void model16_640_480(){

        img = Bitmap.createScaledBitmap(img, 640, 480, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft640480Float16 model = Craft640480Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft640480Float16: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft640480Float16: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft640480Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft640480Float16: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_640_480: Exception " + e.getMessage());
        }

    }

    @SuppressLint("SetTextI18n")
    private void model16_640_640(){

        img = Bitmap.createScaledBitmap(img, 640, 640, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft640640Float16 model = Craft640640Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft640640Float16: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 640}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft640640Float16: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft640640Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft640640Float16: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_640_640: Exception " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void model16_1280_800(){

        img = Bitmap.createScaledBitmap(img, 1280, 800, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft1280800Float16 model = Craft1280800Float16.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "model16_1280_800: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 1280, 800}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "model16_1280_800: After Inputs and Before outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft1280800Float16.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "model16_1280_800: After outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+selectedModel+ " "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model16_1280_800: Exception " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void model32_320_320(){

        img = Bitmap.createScaledBitmap(img, 320, 320, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft320320Float32 model = Craft320320Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft320320Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 320, 320}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft320320Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result
            Craft320320Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft320320Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model32_320_320: Exception "  + e.getMessage());
        }

    }

    @SuppressLint("SetTextI18n")
    private void model32_480_320(){

        img = Bitmap.createScaledBitmap(img, 480, 320, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft480320Float32 model = Craft480320Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft480320Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 320}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft480320Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft480320Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft480320Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model32_480_320: Exception " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void model32_480_480(){

        img = Bitmap.createScaledBitmap(img, 480, 480, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft480480Float32 model = Craft480480Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft480480Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 480, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft480480Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft480480Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft480480Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.d(TAG, "model32_480_480: Exception " + e.getMessage());
        }

    }
    @SuppressLint("SetTextI18n")
    private void model32_640_480(){

        img = Bitmap.createScaledBitmap(img, 640, 480, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft640480Float32 model = Craft640480Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft640480Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 480}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft640480Float32: After inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft640480Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft640480Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
            Log.d(TAG, "model32_640_480: Exception " + e.getMessage());
        }

    }
    @SuppressLint("SetTextI18n")
    private void model32_640_640(){

        img = Bitmap.createScaledBitmap(img, 640, 640, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);

        try {
            Craft640640Float32 model = Craft640640Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft640640Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 640, 640}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft640640Float32: After inputs and Before outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft640640Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft640640Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.d(TAG, "model32_640_640: Exception " + e.getMessage());
        }
    }
    @SuppressLint("SetTextI18n")
    private void model32_1280_800(){

        img = Bitmap.createScaledBitmap(img, 1280, 800, true);
        img = img.copy(Bitmap.Config.ARGB_8888, true);


        try {
            Craft1280800Float32 model = Craft1280800Float32.newInstance(mContext);

            //get the current time
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            Log.d(TAG, "Craft1280800Float32: Before Inputs " + formatter.format(date));

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 3, 1280, 800}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            //get the current time
            SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date2 = new Date();
            Log.d(TAG, "Craft1280800Float32: After Inputs and Before Outputs " + formatter2.format(date2));

            // Runs model inference and gets result.
            Craft1280800Float32.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //get the current time
            SimpleDateFormat formatter3 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date3 = new Date();
            Log.d(TAG, "Craft1280800Float32: After Outputs " + formatter3.format(date3));

            Log.d(TAG, "onClick: output feature "+
                    outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            Log.d(TAG, "onClick: length " + outputFeature0.getFloatArray().length);
            tv.setText(outputFeature0.getFloatArray()[0] + "\n"+outputFeature0.getFloatArray()[1]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.d(TAG, "model32_1280_800: Exception " + e.getMessage());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        positionSelected = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}