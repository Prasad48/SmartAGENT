package com.bhavaniprasad.smartagent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BGService extends Service {
    DatabaseManager mDatabase;
    List<description> datalistfrmdb;
    private int found;
    private String filename;

    String TAG = "BGservice";
    public BGService() {
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }

    public int counter=0;

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = new DatabaseManager(this);
        datalistfrmdb = new ArrayList<>();
        filename="";

        if(mDatabase.getcount().getInt(0)!=0)
        this.getAlldata();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        getapidata();
        startTimer();
        return START_STICKY;
    }

    private void getAlldata() {
        //we are here using the DatabaseManager instance to get all employees
        Cursor cursor = mDatabase.getAlldata();
        if (cursor.moveToFirst()) {
            do {
                datalistfrmdb.add(new description(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4)
                ));
            } while (cursor.moveToNext());

        }
    }

    public void getapidata(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //Here we are using the GsonConverterFactory to directly convert json data to object
                .build();

        Api api = retrofit.create(Api.class);

        Call<Dbhandler> call = api.getjson();
        try{
            call.enqueue(new Callback<Dbhandler>() {
                @Override
                public void onResponse(Call<Dbhandler> call, Response<Dbhandler> response) {
                    String insertid,insertname,inserttype,insertcdn_path;
                    int insertsizeInBytes;

                    Dbhandler resp = response.body();

                    List<description> descarr = resp.getDescriptionArray();
                    for (description responsedesc : descarr) {
                        found=0;
                        insertid = responsedesc.getId();
                        insertname = responsedesc.getName();
                        inserttype= responsedesc.getType();

                        insertsizeInBytes=  responsedesc.getSizeInBytes();
                        insertcdn_path = responsedesc.getCdn_path();
                        String file_url = responsedesc.getCdn_path();
//                            "http://www.qwikisoft.com/demo/ashade/20001.kml";

                        if(!datalistfrmdb.isEmpty()){
                            for (description descfrmdb : datalistfrmdb) {
                                String nn=descfrmdb.getName();
                                int dd= descfrmdb.getSizeInBytes();
                                if (descfrmdb.getName().equals(insertname) && descfrmdb.getSizeInBytes() == insertsizeInBytes) {
                                    found = 1;
                                    break;
                                }
                            }
                            if(found!=1){             //got a new record not matches with record in localdb
                                if (mDatabase.adddata(insertid,insertname, inserttype,insertsizeInBytes,insertcdn_path)) {
                                    Log.e(TAG,"successfully inserted");
                                    Toast.makeText(getApplicationContext(),"successfully inserted for existing",Toast.LENGTH_SHORT).show();
                                    found=0;
                                }
                                else {
                                    Log.e(TAG,"couldnt insert");
                                    Toast.makeText(getApplicationContext(),"couldnt insert data",Toast.LENGTH_SHORT).show();
                                }
                            }

                            if(!fileExist(insertname)){
                                new DownloadFileFromURL().execute(file_url);
                                Toast.makeText(getApplicationContext(),"File Downloaded",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"Already Downloaded",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            if (mDatabase.adddata(insertid,insertname, inserttype,insertsizeInBytes,insertcdn_path)) {   //got a new record
//                        Toast.makeText(this, "Employee Added", Toast.LENGTH_SHORT).show();
                                Log.e(TAG,"successfully inserted");
                                Toast.makeText(getApplicationContext(),"successfully inserted",Toast.LENGTH_SHORT).show();

                            }
                            else {
                                Log.e(TAG,"couldnt insert data");
                                Toast.makeText(getApplicationContext(),"couldnt insert data",Toast.LENGTH_SHORT).show();
                            }
                            new DownloadFileFromURL().execute(file_url);
                        }
                    }
                    Log.e("Response","res is"+response.body());
                }

                @Override
                public void onFailure(Call<Dbhandler> call, Throwable t) {
                    Log.e("FAiled","faileddd"+t);


                }
            });
        }
        catch (Exception e){
            Log.e(TAG,"api call exception"+e);
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stoptimertask();

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }


    public boolean fileExist(String fname){
        File file = new File(Environment.getExternalStorageDirectory(),fname);
        boolean exist= file.exists();
        return exist;
    }


    private Timer timer;
    private TimerTask timerTask;
    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                Log.i("Count", "=========  "+ (counter++));
            }
        };
        timer.schedule(timerTask, 1000, 1000); //
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);

                Uri uri = Uri.parse(f_url[0]);
//                String protocol = uri.getScheme();
//                String server = uri.getAuthority();
                String path = uri.getPath();
                File theFile = new File(path);
                String[] filewithtype=theFile.getName().split("\\.");
                String filename=filewithtype[0];

                String filetyp=filewithtype[1];
                URLConnection conection = url.openConnection();
                conection.connect();
                File mediaStorageDir,mediaStorageDirp;

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);


                if(filetyp.equals("mp4")){
                    new File(Environment.getExternalStorageDirectory(), filename);
                    mediaStorageDirp = new File(Environment.getExternalStorageDirectory().toString()+"/"+filename.concat(".mp4"));
                }
                else{
                    new File(Environment.getExternalStorageDirectory(), filename);
                    mediaStorageDirp = new File(Environment.getExternalStorageDirectory().toString()+"/"+filename.concat(".svg"));
                }

                // Output stream
                OutputStream output = new FileOutputStream(mediaStorageDirp);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", "download exception"+e);
            }

            return null;
        }
    }
}
