package com.example.myapp.background;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.example.myapp.SimpleActivity;
import com.example.myapp.data.MyData;
import com.example.myapp.data.MyJSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MyService extends IntentService {
    MyJSONParser parser = null;
    int offset = -1;

    ConectarMiBluetooth bt_connect = null;
    ComunicarConBluetooth bt_comm = null;

    public MyService(){
        super("MyService");
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onHandleIntent(Intent workIntent) {
        BluetoothDevice bt = workIntent.getParcelableExtra((SimpleActivity.TAG_BLUETOOTH_DEVICE));
        Log.e("ON-MyService", "onHandleIntent(): [" + bt.getName() + "]" );

        bt_connect = new ConectarMiBluetooth(bt);
        bt_connect.execute();
        bt_comm = new ComunicarConBluetooth(bt_connect.getSocket());

        MyData data = null;

        do {
            data = this.get_updates();
        }while( data == null || this.process(data) );
    }

    private String get(){
        Log.e("ON-MyService", "get()");
        String resultadoLocal = "Error al leer BD";
        try {
            URL url = new URL("http://172.16.186.33:3010/access");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.connect();

            int status = conn.getResponseCode();
            if ( status == 200 ) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                resultadoLocal = sb.toString();
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e("ON-MyService", "get(): Exception", e);
        }
        return resultadoLocal;
    }

    private void send(String info) {
        Log.e("ON-MyService", "send()");
        try {
            String textoCodificado = java.net.URLEncoder.encode(info, "UTF-8");
            URL url = new URL("https://api.telegram.org/bot8328851822:AAFVpl6jWu4ueK_1-LsFyghdUbse6qwk7w0/sendMessage?chat_id=5676215823&text=" + textoCodificado);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.connect();

            int status = conn.getResponseCode();
            if(status == 200) {
                conn.getInputStream().close();
            } else {
                conn.getErrorStream().close();
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e("ON-MyService", "send(): Exception", e);
        }
    }

    private void solicitarGrafica() {
        Log.e("ON-MyService", "solicitarGrafica()");
        try {
            URL url = new URL("http://172.16.186.33:3010/solicitar-grafica");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            String input = "{}";
            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            int status = conn.getResponseCode();

            BufferedReader br;
            if (status == 200) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                if (conn.getErrorStream() != null) {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                } else {
                    this.send("Error: El servidor respondió con estado " + status + " pero sin detalles.");
                    conn.disconnect();
                    return;
                }
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();

            String respuestaNode = sb.toString();

            if (status == 200 && respuestaNode.contains("true")) {
                this.send("¡Éxito! El script de Python recibió la orden y está generando la gráfica.");
            } else {
                this.send("Error: El cliente de Python no está conectado al servidor.");
            }

        } catch (Exception e) {
            Log.e("ON-MyService", "solicitarGrafica(): Exception", e);
            this.send("Error de conexión: No se pudo alcanzar el servidor Node.js.");
        }
    }

    private MyData get_updates() {
        Log.e("ON-MyService", "get_updates()");
        MyData data = null;
        HttpURLConnection conn = null;
        try {
            String my_url = "https://api.telegram.org/bot8328851822:AAFVpl6jWu4ueK_1-LsFyghdUbse6qwk7w0/getUpdates?offset=" + offset + "&timeout=2";
            URL url = new URL(my_url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            if ( conn.getResponseCode() == 200 ) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                String resLocal = sb.toString();

                this.parser = new MyJSONParser("[" + resLocal +"]");
                data = this.parser.getValue();
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e("ON-MyService", "get_updates(): Exception", e);
            if( conn != null ) conn.disconnect();
        }
        return data;
    }

    private boolean process(MyData data){
        Log.e("ON-MyService", "process()");
        int update_id = -1;
        String msg = "";

        if(data == null || data.msg == null) return true;

        for (int i = 0; i < data.msg.size(); ++i) {
            update_id = data.update_id.get(i);
            msg = data.msg.get(i);
            this.offset = update_id + 1;

            if(msg == null || msg.isEmpty()) {
                continue;
            }

            String msgUpper = msg.toUpperCase();

            if( msgUpper.contains("ENCENDER") ){
                bt_comm.write("1");
                this.send("Sensor Encendido " + this.offset);
            }

            if( msgUpper.contains("APAGAR") ) {
                bt_comm.write("0");
                this.send("Sensor Apagado " + this.offset);
            }

            if( msgUpper.contains("LEER") ){
                String r = get();
                this.send("Info del servidor: \n" + r);
            }

            if( msgUpper.contains("REGISTRAR:") ) {
                String[] partes = msg.split(":");
                if(partes.length > 1) {
                    String nombre = partes[1].replace("\"", "").trim();
                    bt_comm.write("1000," + nombre + "\n");
                    this.send("Registro iniciado para: " + nombre + ". Mira a la cámara...");
                }
            }

            if (msgUpper.contains("BORRAR:")) {
                String[] partes = msg.split(":");
                if (partes.length > 1) {
                    String id_str = partes[1].replace("\"", "").trim();
                    try {
                        int id = Integer.parseInt(id_str);
                        int comando_borrar = 2000 + id;
                        bt_comm.write(comando_borrar + "\n");
                        this.send("Petición enviada: Borrando el ID " + id + " de la cámara.");
                    } catch (NumberFormatException e) {
                        this.send("Error: Por favor envía un ID numérico válido. (Ej. Borrar:5)");
                    }
                }
            }

            if (msgUpper.contains("BORRARTODO") || msgUpper.contains("FORMATEAR")) {
                bt_comm.write("3000\n");
                this.send("Petición enviada: Formateando base de datos y borrando todos los rostros.");
            }

            if (msgUpper.contains("GRAFICA")) {
                this.send("Enviando orden al servidor para comunicarse con Python...");
                this.solicitarGrafica();
            }

            if( msgUpper.contains("TERMINAR") ) {
                return false;
            }
        }
        return true;
    }
}