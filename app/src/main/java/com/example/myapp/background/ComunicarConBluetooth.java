package com.example.myapp.background;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Hilo encargado de mantener la conexion y realizar las lecturas y escrituras
// de los mensajes intercambiados entre dispositivos.
public class ComunicarConBluetooth {
    private final InputStream inputStream;    // Flujo de entrada (lecturas)
    private final OutputStream outputStream;   // Flujo de salida (escrituras)


    @SuppressLint("MissingPermission")
    public ComunicarConBluetooth(BluetoothSocket socket)
    {
        Log.e("ON-ComunicarConBluetooth", "Constructor(): Iniciando metodo");

        // Se generan los flujos de entrada y salida
        InputStream tmpInputStream = null;
        OutputStream tmpOutputStream = null;

        try {
            tmpInputStream = socket.getInputStream();
            tmpOutputStream = socket.getOutputStream();
        }
        catch(IOException e){
            Log.e("ON-ComunicarConBluetooth", "Constructor(): IOException", e);
        }
        inputStream = tmpInputStream;
        outputStream = tmpOutputStream;
    }

    // Metodo principal del hilo, encargado de realizar las lecturas
    public void write(String s)
    {
        Log.e("ON-ComunicarConBluetooth", "write()");
        //byte[] buffer = new byte[1024]; // Variable sin uso eliminada
        int bytes;

        byte[] out_buffer = s.getBytes();
        bytes = out_buffer.length;

        try{
            outputStream.write(out_buffer);
        }
        catch(IOException e) {
            Log.e("ON-ComunicarConBluetooth", "write(): IOException", e);
        }
    }

    // AHORA RETORNA UN STRING CON LO QUE LEA DEL ARDUINO
    public String read()
    {
        Log.e("ON-ComunicarConBluetooth", "read()");
        byte[] buffer = new byte[1024];
        int bytes;
        String resultado = "";

        try {
            // Leemos del flujo de entrada del socket. Esto bloquea hasta recibir respuesta.
            bytes = inputStream.read(buffer);
            resultado = new String(buffer, 0, bytes).trim(); // Convertimos bytes a String y limpiamos espacios
            Log.e("ON-ComunicarConBluetooth", "Recibido del Arduino: " + resultado);
        }
        catch(IOException e) {
            Log.e("ON-ComunicarConBluetooth", "read(): IOException", e);
        }

        return resultado;
    }
}