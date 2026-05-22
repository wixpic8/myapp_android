package com.example.myapp.data;

import java.util.ArrayList;

public class MyData {
    public ArrayList<Integer> update_id;
    public ArrayList<String> msg;
    public ArrayList<Long> chat_id;
    // PROCESAMIENTO DEL SERVIDOR: Guardamos quién envía la orden
    public String senderName;

    public MyData(){
        initialize();
    }

    public void initialize(){
        update_id = new ArrayList<Integer>();
        msg = new ArrayList<String>();
        chat_id = new ArrayList<Long>();
        senderName = "Usuario Desconocido";
    }
}
