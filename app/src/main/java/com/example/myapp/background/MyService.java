package com.example.myapp.background;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.example.myapp.SimpleActivity;
import com.example.myapp.data.MyData;
import com.example.myapp.data.MyJSONParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/* REFERENCES
 * url: Service vs IntentService https://www.geeksforgeeks.org/difference-between-service-and-intentservice-in-android/
 * URL: Using Service: https://www.geeksforgeeks.org/services-in-android-with-example/
 *
 * java.lang.IllegalAccessException : la causa es un constructor que no se puede invocar (el del IntentService) asegurate que sea declarado "public"
 */
public class MyService extends IntentService {
    MyJSONParser parser = null;
    int offset = -1;
    String res;
    private String serverIp = "172.16.184.191";

    private static final long AUTHORIZED_CHAT_ID = 8306537431L;

    ConectarMiBluetooth bt_connect = null;
    ComunicarConBluetooth bt_comm = null;

    public MyService() {
        super("MyService");
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onHandleIntent(Intent workIntent) {
        BluetoothDevice bt = workIntent.getParcelableExtra(SimpleActivity.TAG_BLUETOOTH_DEVICE);
        Log.e("ON-MyService", "onHandleIntent(): [" + bt.getName() + "]");

        bt_connect = new ConectarMiBluetooth(bt);
        bt_connect.execute();
        bt_comm = new ComunicarConBluetooth(bt_connect.getSocket());

        // Cargar IP del servidor desde SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        serverIp = prefs.getString("server_ip", "172.16.184.191");
        Log.e("ON-MyService", "IP del servidor cargada: " + serverIp);

        MyData data = null;

        do {
            data = this.get_updates();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        } while (data == null || this.process(data));
    }

    private String get() {
        Log.e("ON-MyService", "get()");
        try {
            URL url = new URL("http://" + serverIp + ":3010/registros");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-length", "0");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();

            int status = conn.getResponseCode();

            if (status == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(reader);

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                res = sb.toString();
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            Log.e("ON-MyService", "get(): MalformedURLException", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("ON-MyService", "get(): IOException", e);
            e.printStackTrace();
        }

        return res;
    }

    private void send(String info) {
        Log.e("ON-MyService", "send()");
        try {
            URL url = new URL(
                    "https://api.telegram.org/bot7973168904:AAFQlRJel7QexwLpsn6upTeyTPzrHmbqahM/sendMessage?chat_id=8306537431&text="
                            + info);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-length", "0");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();

            int status = conn.getResponseCode();

            if (status == 200) {
                res = "Message Send as BOT";
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            Log.e("ON-MyService", "send(): MalformedURLException", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("ON-MyService", "send(): IOException", e);
            e.printStackTrace();
        }
    }

    private int postRegistroServidor(String nombre) {
        try {
            URL url = new URL("http://" + serverIp + ":3010/registro");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", "12345sia");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            String body = "{\"nombre\":\"" + nombre + "\"}";
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
                br.close();
                conn.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                return json.optInt("id_camara", -1);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e("ON-MyService", "postRegistroServidor(): " + e.getMessage());
        }
        return -1;
    }

    private MyData get_updates() {
        Log.e("ON-MyService", "get_updates()");
        MyData data = null;

        HttpURLConnection conn = null;
        try {
            String my_url = "https://api.telegram.org/bot7973168904:AAFQlRJel7QexwLpsn6upTeyTPzrHmbqahM/getUpdates?offset="
                    + offset + "&timeout=1000";
            URL url = new URL(my_url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-length", "0");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();

            int status = conn.getResponseCode();

            if (status == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(reader);

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                res = sb.toString();

                this.parser = new MyJSONParser("[" + res + "]");
                data = this.parser.getValue();
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            Log.e("ON-MyService", "get_updates(): MalformedURLException", e);
            e.printStackTrace();

            if (conn != null)
                conn.disconnect();
        } catch (IOException e) {
            Log.e("ON-XXXXXX", "get_updates(): IOException", e);
            e.printStackTrace();

            if (conn != null)
                conn.disconnect();
        }

        return data;
    }

    private boolean process(MyData data) {
        Log.e("ON-MyService", "process()");
        int update_id = -1;
        String msg = "";
        for (int i = 0; i < data.msg.size(); ++i) {
            update_id = data.update_id.get(i);
            msg = data.msg.get(i);
            long chatId = data.chat_id.get(i);

            this.offset = update_id + 1;

            // Validar chat autorizado
            if (chatId != AUTHORIZED_CHAT_ID) {
                Log.w("ON-MyService", "Mensaje ignorado de chat no autorizado: " + chatId);
                continue;
            }

            // Interpretar comandos
            if (msg.contains("ENCENDER")) {
                bt_comm.write("1");
                this.send("Sensor Encendido " + this.offset);
            }

            if (msg.contains("APAGAR")) {
                bt_comm.write("0");
                this.send("Sensor Apagado " + this.offset);
            }

            if (msg.contains("LEER")) {
                String r = get();
                this.send("Info del servidor: \n" + r);
            }

            // CONFIGURACIÓN DINÁMICA DE IP (Ej. IP:172.16.184.191)
            if (msg.contains("IP:")) {
                String[] partes = msg.split(":");
                if (partes.length > 1) {
                    String nuevaIp = partes[1].trim();
                    serverIp = nuevaIp;
                    // Guardar en SharedPreferences
                    android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putString("server_ip", nuevaIp).apply();
                    this.send("[INFO] IP del servidor actualizada a: http://" + serverIp + ":3010");
                }
            }

            // REGISTRO DE ROSTROS
            if (msg.contains("REGISTRAR:")) {
                String[] partes = msg.split(":");
                if (partes.length > 1) {
                    String nombre = partes[1].replace("\"", "").trim();
                    int idAsignado = postRegistroServidor(nombre);
                    if (idAsignado > 0) {
                        bt_comm.write("1000\n");
                        this.send("Registro iniciado para: " + nombre + " (ID: " + idAsignado + "). Mira a la camara...");
                    } else {
                        this.send("Error al registrar en servidor. Intenta de nuevo.");
                    }
                }
            }

            // BORRADO ESPECÍFICO (Base 2000)
            if (msg.contains("BORRAR:")) {
                String[] partes = msg.split(":");
                if (partes.length > 1) {
                    String id_str = partes[1].replace("\"", "").trim();
                    try {
                        int id = Integer.parseInt(id_str);
                        int comando_borrar = 2000 + id;
                        bt_comm.write(comando_borrar + "\n");
                        this.send("Peticion enviada: Borrando el ID " + id + " de la camara.");
                    } catch (NumberFormatException e) {
                        this.send("Error: Por favor envia un ID numerico valido. (Ej. Borrar:5)");
                    }
                }
            }

            // BORRADO TOTAL (Base 3000)
            if (msg.contains("BORRARTODO") || msg.contains("Formatear")) {
                bt_comm.write("3000\n");
                this.send("Peticion enviada: Formateando base de datos y borrando todos los rostros.");
            }

            if (msg.contains("TERMINAR")) {
                return false;
            }
        }
        return true;
    }
}