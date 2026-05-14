package com.example.myapp.background;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MiPeticionREST extends AsyncTask<String,String,String> {
    private TextView output;
    String resultado = "";
    int offset;

    public MiPeticionREST(TextView output){
        offset = 0;
        this.output = output;
    }

    public MiPeticionREST(){
        offset = 0;
        this.output = null;
    }

    @Override
    public void onPreExecute(){
    }

    @Override
    protected String doInBackground(String... info) {
        String res = "";

        try {
            if( info[0].contains("POST")) {
                URL url = new URL("http://172.16.186.33:3010/users1");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String nombre = info[1];
                String id_camara = info[2];
                String input = "{\"id\": \"" + id_camara + "\",\"nombre\": \"" + nombre + "\"}";

                OutputStream os = conn.getOutputStream();
                os.write(input.getBytes());
                os.flush();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                    res = "error";
                }
                conn.disconnect();
            }

            if( info[0].contains("GET-SEND")){
                String msgCodificado = java.net.URLEncoder.encode(info[1], "UTF-8");
                URL url = new URL("https://api.telegram.org/bot8328851822:AAFVpl6jWu4ueK_1-LsFyghdUbse6qwk7w0/sendMessage?chat_id=5676215823&text=" + msgCodificado);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                if ( conn.getResponseCode() == 200 ) { res = "Message Send as BOT"; }
                conn.disconnect();
            }

            if( info[0].contains("GET-UPDATES")){
                URL url = new URL("https://api.telegram.org/bot8328851822:AAFVpl6jWu4ueK_1-LsFyghdUbse6qwk7w0/getUpdates?offset=" + offset + "timeout=5");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                if ( conn.getResponseCode() == 200 ) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) { sb.append(line).append("\n"); }
                    br.close();
                    res = sb.toString();
                }
                conn.disconnect();
            }
        } catch (MalformedURLException e) {
            Log.e("ENVIOREST", "[MalformedURLException]=>" + e.getMessage());
        } catch (IOException e) {
            Log.e("ENVIOREST", "[IOException]=>" + e.getMessage());
        }

        return res;
    }

    @Override
    protected void onPostExecute(String result) {
        this.resultado = result;
        if( this.output != null ) {
            this.output.setText("[" + result + "]");
        }
    }
}