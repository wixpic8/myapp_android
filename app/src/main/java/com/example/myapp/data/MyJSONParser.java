package com.example.myapp.data;

/*
    REFERENCIA BASE:
        https://www.tutorialspoint.com/json/json_java_example.htm

        DEFINICION: https://www.mclibre.org/consultar/informatica/lecciones/formato-json.html
        ORIGINAL:       https://www.json.org/json-en.html
        UPDATES OBJECTS STRUCTURE:  https://core.telegram.org/bots/api#update
 */
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyJSONParser {
    //JSONObject value = new JSONObject();
    //INFO: https://stleary.github.io/JSON-java/org/json/JSONTokener.html
    //INFO 2: https://www.javatpoint.com/how-to-convert-string-to-json-object-in-java
    String s = "";

    JSONArray array = null;

    public MyJSONParser(String s){
        this.s = s;
    }

    public MyData getValue() {
        //s = "[0,{\"1\":{\"2\":{\"3\":{\"4\":[5,{\"6\":7}]}}}}]";
        MyData data = new MyData();

        int size = -1;
        String msg = "";
        try{
            if( array == null ){
                array = new JSONArray(s);
            }

            JSONObject obj = array.getJSONObject(0);

            String ok = obj.getString("ok");
            JSONArray result = new JSONArray(obj.getString("result"));

            for(int i = 0; i < result.length(); ++i){
                JSONObject update = result.getJSONObject(i);
                int update_id = update.getInt("update_id");
                JSONObject message = update.getJSONObject("message");

                // PROCESAMIENTO: Extraemos metadatos del servidor (quién escribe)
                if (message.has("from")) {
                    JSONObject from = message.getJSONObject("from");
                    data.senderName = from.optString("first_name", "Usuario");
                }

                msg = message.getString("text");
                data.update_id.add(update_id);
                data.msg.add(msg);

//            for(int i = 0; i < result.length(); ++i){
//                // Dentro del for de MyJSONParser.java:
//                JSONObject update = result.getJSONObject(i);
//                JSONObject message = update.getJSONObject("message");
//
//                // Extraemos el nombre desde el objeto 'from' de Telegram
//                JSONObject from = message.getJSONObject("from");
//                data.senderName = from.getString("first_name");
//
//                msg = message.getString("text");


//                JSONObject update = result.getJSONObject(i);
//                int update_id = update.getInt("update_id");
//                JSONObject message = update.getJSONObject("message");
//                msg = message.getString("text");
//                data.update_id.add(update_id);
//                data.msg.add(msg);
            }
        }catch(JSONException e){
            Log.e("JSON-PARSER", e.getMessage());
        }
        return data;
    }
}
