package com.example.pramodgobburi.appuploader;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import tech.gusavila92.websocketclient.WebSocketClient;

public class MainActivity extends AppCompatActivity {

    private WebSocketClient webSocketClient;
    private TextView serviceStatus;
    private EditText websocketEdit;
    private Button webSocketBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceStatus = (TextView) findViewById(R.id.service_status);
        websocketEdit = (EditText) findViewById(R.id.websocket_input);
        webSocketBtn = (Button) findViewById(R.id.websocket_input_btn);

        websocketEdit.setText("ws://192.168.1.180:2500/ws/uploads/");
        websocketEdit.setActivated(false);
        
        webSocketBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!websocketEdit.getText().toString().isEmpty()) {
                    String websocket = websocketEdit.getText().toString();
                    startService(websocket, "");
                }
                else {
                    Toast.makeText(MainActivity.this, "Must provide a websocket url", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadService.NOTIFICATION));
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    private void startService(String websocket, String userEncoded) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.WEBSOCKETPATH, websocket);
        intent.putExtra(DownloadService.ENCODEDUSER, userEncoded);
        startService(intent);
        serviceStatus.setText("Starting connection to service");
    }
    
    

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle != null) {
                String string = bundle.getString(DownloadService.FILEPATH);
                int resultCode = bundle.getInt(DownloadService.RESULT);

                if(resultCode == RESULT_OK) {
                    Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show();
                }
                if(resultCode == 100) {
                    String message = bundle.getString("Message");
                    serviceStatus.setText(message);
                }
            }
        }
    };

    
    private void initWebsocket() {

        URI uri;
        try {
            uri = new URI("ws://192.168.1.180:2500/ws/uploads/");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                System.out.println("onOpen");
                Log.e("MESSAGE", "Websocket connection opened");
                Toast.makeText(MainActivity.this, "Websocket connection opened", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTextReceived(String message) {
                System.out.println("onTextReceived");
                Log.e("MESSAGE", message);
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                System.out.println("onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                System.out.println("onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                System.out.println("onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                System.out.println("onCloseReceived");
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();


    }

    private void sendNotification() {

    }

    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForReadExtertalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void requestPermissionForWriteExtertalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    private void fetchApk(final String strUrl) {


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                InputStream input = null;
                OutputStream output = null;
                try {
                    String PATH = Environment.getExternalStorageDirectory() + "/download";
                    File file = new File(PATH);
                    file.mkdirs();

                    File outputFile = new File(file, "app.apk");
                    output = new FileOutputStream(outputFile);

                    URL url = new URL(strUrl);
                    connection = (HttpURLConnection) url.openConnection();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {

                    }
                    int fileLength = connection.getContentLength();
                    Log.e("FILELENGTH", ""+fileLength);

                    //download file
                    input = connection.getInputStream();
                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        Log.e("DATA", ""+count);
                        output.write(data, 0, count);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    {
                        try {
                            if (output != null) {
                                output.close();
                                installApk();
                                Log.e("DATA", "Output closed");
                            }
                            if (input != null) {
                                input.close();
                                Log.e("DATA", "Input closed");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
            }

        });
        t.start();
    }

    private void installApk() {
        File fileApkToInstall = new File(Environment.getExternalStorageDirectory() + "/download/app.apk");
        File[] list = fileApkToInstall.listFiles();

        Uri photoURI = FileProvider.getUriForFile(this, "com.example.pramodgobburi.appuploader.provider", fileApkToInstall);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(photoURI, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


}

