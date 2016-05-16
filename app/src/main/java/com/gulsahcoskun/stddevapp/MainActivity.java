package com.gulsahcoskun.stddevapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Temperature;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements ServiceConnection {
    private MetaWearBleService.LocalBinder serviceBinder;
    private final String MW_MAC_ADDRESS= "C6:69:79:0D:07:32";
    private MetaWearBoard mwBoard;
    private TextView text;
    ProgressDialog progressDialog;
    private Timer myTimer;
    Bmi160Accelerometer bmi160AccModule;
    public static final String ACCEL_DATA="accel_data";
    private Debug debugModule;
    //private float[] x_arr;
  //  private float[] y_arr;
   // private float[] z_arr;
    ArrayList<Float> x_arr;
    ArrayList<Float> y_arr;
    ArrayList<Float> z_arr;
    ArrayList<Float> gX_arr;
    ArrayList<Float> gY_arr;
    ArrayList<Float> gZ_arr;
    private Temperature tempModule;
    private Float temp_value ;
    private float accel_x = 0;
    private float accel_y = 0;
    private float accel_z = 0;
    private float gyro_x = 0;
    private float gyro_y = 0;
    private float gyro_z = 0;
    private TextView text_temp;

    Bmi160Gyro bmi160GyroModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

        text = (TextView) findViewById(R.id.textView);
        text_temp = (TextView) findViewById(R.id.textView2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MetaWearBleService.LocalBinder binder = (MetaWearBleService.LocalBinder) service;
        // Typecast the binder to the service's LocalBinder class
        //serviceBinder = (MetaWearBleService.LocalBinder) service;

        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice =
                btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        binder.executeOnUiThread();

        // Create a MetaWear board object for the Bluetooth Device
        mwBoard = binder.getMetaWearBoard(remoteDevice);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                // pDialog.hide();
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();

                Log.i("test", "Connected");
                Log.i("test", "MetaBoot? " + mwBoard.inMetaBootMode());

                mwBoard.readDeviceInformation().onComplete(new AsyncOperation.CompletionHandler<MetaWearBoard.DeviceInformation>() {
                    @Override
                    public void success(MetaWearBoard.DeviceInformation result) {
                        Log.i("test", "Device Information: " + result.toString());
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e("test", "Error reading device information", error);
                    }
                });


                callAsynchronousTask();



            }

            @Override
            public void disconnected() {
                // pDialog.hide();
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                Log.i("test", "Disconnected");
            }

            @Override
            public void failure(int status, final Throwable error) {
                //  pDialog.hide();
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e("test", "Error connecting", error);
            }

        });

        mwBoard.connect();
    }

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            temp_value = 0f;

                            BackgroundTask performBackgroundTask = new BackgroundTask();
                            // PerformBackgroundTask this class is the class that extends AsyncTask
                            Log.i("test", "Task begins");
                            performBackgroundTask.execute();

                            x_arr.clear();
                            y_arr.clear();
                            z_arr.clear();
                            gX_arr.clear();
                            gY_arr.clear();
                            gZ_arr.clear();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 10 , 1*60*1000); //2 dk çalışsın
    }



    @Override
    public void onServiceDisconnected(ComponentName name) {

    }



    private void doExit() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                MainActivity.this);

        alertDialog.setPositiveButton("Yeah", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //debugModule.resetDevice();
                mwBoard.disconnect();
                finish();
            }
        });

        alertDialog.setNegativeButton("Nope", null);

        alertDialog.setMessage("Are you sure to exit the app?");
        alertDialog.setTitle("Gulsah Baby Tracking");
        alertDialog.show();
    }
    public void onBackPressed() {
        doExit();
    }


    /*----------------------------------BackgroundTask------------------------------------------------------------*/
    private class BackgroundTask extends AsyncTask<Void, Void, float[]> {

        private Float tempBack=0f;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Ölçüm yapılıyor lütfen 5 dakika boyunca bekleyiniz ve uygulamayı kapatmayınız...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        protected float[] doInBackground(Void... arg0) {
            long startTime = System.currentTimeMillis();
            x_arr = new ArrayList<Float>();
            y_arr = new ArrayList<Float>();
            z_arr = new ArrayList<Float>();
            gX_arr = new  ArrayList<Float>();
            gY_arr = new  ArrayList<Float>();
            gZ_arr = new  ArrayList<Float>();


            try {

                bmi160AccModule = mwBoard.getModule(Bmi160Accelerometer.class);

         /*       bmi160AccModule.configureAxisSampling()
                        .setFullScaleRange(Bmi160Accelerometer.AccRange.AR_2G)
                        .setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_1_5625_HZ)
                        .enableUndersampling((byte) 4)
                        .commit();   */

                bmi160AccModule.configureAxisSampling()
                        .setFullScaleRange(Bmi160Accelerometer.AccRange.AR_2G)
                        .setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_50_HZ)
                        .commit();
                
                bmi160GyroModule= mwBoard.getModule(Bmi160Gyro.class);

                bmi160GyroModule.configure()
                        .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_250)
                        .setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_50_HZ)
                        .commit();



            } catch (UnsupportedModuleException e) {
                e.printStackTrace();
            }

            while (((System.currentTimeMillis() - startTime) / 1000) < 30) {

                bmi160AccModule.enableAxisSampling();
                bmi160AccModule.start();
                bmi160GyroModule.start();
             //   bmi160AccModule.startLowPower();

                bmi160AccModule.routeData().fromAxes().stream(ACCEL_DATA).commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe(ACCEL_DATA, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        Log.i("test", message.getData(CartesianFloat.class).toString());
                                     //   final CartesianFloat axisData = message.getData(CartesianFloat.class);
                                        final CartesianFloat axisData = message.getData(CartesianFloat.class);
                                        Log.i("test", String.valueOf(axisData.x().floatValue()));
                                        accel_x = axisData.x().floatValue();
                                        accel_y = axisData.y().floatValue();
                                        accel_z = axisData.z().floatValue();
                                        x_arr.add(accel_x);
                                        y_arr.add(accel_y);
                                        z_arr.add(accel_z);
                                    }
                                });

                            }
                        });

                //Stream rotation data around the XYZ axes from the gyro sensor
                bmi160GyroModule.routeData().fromAxes().stream("gyro_stream").commit()
                        .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe("gyro_stream", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message msg) {
                                        final CartesianFloat spinData = msg.getData(CartesianFloat.class);
                                        gyro_x= spinData.x().floatValue();
                                        gyro_y= spinData.y().floatValue();
                                        gyro_z= spinData.z().floatValue();

                                        gX_arr.add(gyro_x);
                                        gY_arr.add(gyro_y);
                                        gZ_arr.add(gyro_z);

                                        Log.i("test", spinData.toString());
                                        Log.i("test", String.valueOf(gyro_x));
                                    }
                                });


                            }
                        });

                tempBack = getTemp();


            }

            bmi160AccModule.disableAxisSampling();
            bmi160AccModule.stop();
            bmi160GyroModule.stop();

            float stdX = findDeviation(x_arr);
            Log.i("test", String.valueOf(stdX));
            float stdY = findDeviation(y_arr);
            float stdZ = findDeviation(z_arr);

            float stdGx = findDeviation(gX_arr);
            Log.i("test", String.valueOf(stdGx));
            float stdGy = findDeviation(gY_arr);
            float stdGz = findDeviation(gZ_arr);




            float [] arrStd = new float[7];
            arrStd[0] = stdX;
            arrStd[1] = stdY;
            arrStd[2] = stdZ;
            arrStd[3] = stdGx;
            arrStd[4] = stdGy;
            arrStd[5] = stdGz;
            arrStd[6] = tempBack;



            return arrStd;

        }

        protected void onPostExecute(float[] result) {
            super.onPostExecute(result);
            //     bmi160AccModule.disableAxisSampling();
            //    bmi160AccModule.stop();
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            Log.i("test", String.valueOf(result[0]));
            Log.i("test", String.valueOf(result[1]));
            Log.i("test", String.valueOf(result[2]));
            Log.i("test", String.valueOf(result[3]));
            Log.i("test", String.valueOf(result[4]));
            Log.i("test", String.valueOf(result[5]));
            Log.i("test", String.valueOf(result[6]));

            float stdX_value = result[0];
            float stdY_value = result[1];
            float stdZ_value = result[2];
            float stdGx_value = result[3];
            float stdGy_value = result[4];
            float stdGz_value = result[5];
            float temperature_value = result[6];

            if (stdX_value<0.003 && stdY_value<0.003 && stdZ_value<0.003){
                text.setText("Sleeping");
            }
            else if(stdY_value>0.4){
                text.setText("Wake");
            }
            else if(stdGx_value>10){
                text.setText("Standing/Walking");
            }
            else{
                text.setText("Sitting");
            }

            text_temp.setText(String.valueOf(temperature_value));



        }

        @Override
        protected void onCancelled(float[] result) {
            super.onCancelled(result);
            progressDialog.dismiss();
            Log.i("test", "Task ends");
            cancel(true);
        }
    }


    public static float findDeviation(ArrayList<Float> arr) {
        float sum =0;
        float avg = 0;
        float squareSum = 0;

        for(int i=0; i<arr.size();i++) {
            sum += arr.get(i).floatValue();
        }
        avg=sum/arr.size();


        for (int i = 0; i < arr.size(); i++) {
            squareSum += Math.pow(arr.get(i).floatValue()-avg, 2);
        }

        return (float) Math.sqrt((squareSum)/(arr.size()-1));

    }

    public float getTemp(){
        try {
            tempModule = mwBoard.getModule(Temperature.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
        tempModule.routeData().fromSensor().stream("tempC")
                .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                result.subscribe("tempC", new RouteManager.MessageHandler() {
                    @Override
                    public void process(final Message msg) {
                        Log.i("test", String.format("%.3f C", msg.getData(Float.class)));
                        temp_value = msg.getData(Float.class);
                        // ((TextView) findViewById(R.id.txtTemprature)).setText(String.format("%.3f C", msg.getData(Float.class)).substring(0, 4));
                    }
                });

                tempModule.readTemperature();

            }
        });
        return temp_value;
    }
}
