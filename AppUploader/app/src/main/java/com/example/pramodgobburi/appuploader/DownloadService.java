package com.example.pramodgobburi.appuploader;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.ArrayList;

import tech.gusavila92.websocketclient.WebSocketClient;

public class DownloadService extends IntentService {
    private int result = Activity.RESULT_CANCELED;
    public static final String NOTIFICATION = "com.example.pramodgobburi.appuploader";
    private String websocketPath;
    private String encoded_user_token;
    private WebSocketClient webSocketClient;
    public static final String RESULT = "result";
    public static final String FILEPATH = "filepath";
    private String outputPath = "";
    private ArrayList<DownloadObjects> filesToFetch = new ArrayList<>();
    private boolean isCurrentlyDownloading = false;
    public static final String WEBSOCKETPATH = "websocket_path";
    public static final String ENCODEDUSER = "user_encoded";
    private String downloadIP = "http://192.168.1.180:2500/download/";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        websocketPath = intent.getStringExtra(WEBSOCKETPATH);
        encoded_user_token = intent.getStringExtra(ENCODEDUSER);
        initWebsocket(websocketPath);
    }

    private void initWebsocket(String websocketPath) {

        URI uri;
        try {
            uri = new URI(websocketPath);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                System.out.println("onOpen");
                Log.e("SERVICE_MESSAGE", "Websocket connection opened");
                sendMessage("Connected to websocket", 100);
            }

            @Override
            public void onTextReceived(String message) {
                System.out.println("onTextReceived");
                Log.e("SERVICE_MESSAGE", message);
                try {
                    JSONObject object = new JSONObject(message);
                    String link = object.getString("message");
                    String filename = object.getString("file_name");
                    link = downloadIP+link+"/";

                    if(isCurrentlyDownloading) {
                        filesToFetch.add(new DownloadObjects(link, filename));
                    }
                    else {
                        downloadFile(link, filename);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


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
                Log.e("APP_DOWNLOADER_SERVICES", "Connection closed");
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();

    }

    private void downloadFile(String link, String filename) {
        sendMessage("Got a message from websocket", 100);
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            String PATH = Environment.getExternalStorageDirectory() + "/download/Instant_Downloader";
            File file = new File(PATH);
            file.mkdirs();

            File outputFile = new File(file, filename);
            output = new FileOutputStream(outputFile);

            outputPath = outputFile.getPath();

            URL url = new URL(link);
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

            isCurrentlyDownloading = true;
            sendMessage("Downloading files", 100);
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

                isCurrentlyDownloading = false;
                sendMessage("Finished downloading", 100);
                publishResults(outputPath, result);

                getNextInQueue();

            }
        }
    }

    private void getNextInQueue() {
        if(!isCurrentlyDownloading) {
            if(filesToFetch.size() > 0) {
                downloadFile(filesToFetch.get(1).getUrl(), filesToFetch.get(1).getFilename());
                filesToFetch.remove(1);
            }
        }
    }

    public void sendMessage(String message, int result) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, result);
        intent.putExtra("Message", message);
        sendBroadcast(intent);
    }
    public void publishResults(String outputPath, int result) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(FILEPATH, outputPath);
        intent.putExtra(RESULT, result);
        sendBroadcast(intent);
    }

    private class DownloadObjects {
        private String url;
        private String filename;

        public DownloadObjects(String url, String filename) {
            this.url = url;
            this.filename = filename;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }
}
