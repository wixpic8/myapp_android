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
    //INFO: https://stleary.github.io/JSON-java/org/json/JSONTokener.html
    //INFO 2: https://www.javatpoint.com/how-to-convert-string-to-json-object-in-java
    String s = "";

    JSONArray array = null;

    public MyJSONParser(String s){
        this.s = s;
    }

    public MyData getValue() {
        MyData data = new MyData();

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

                // Extraer nombre del remitente
                if (message.has("from")) {
                    JSONObject from = message.getJSONObject("from");
                    data.senderName = from.optString("first_name", "Usuario");
                }

                // Extraer chat_id para autenticación por mensaje
                JSONObject chat = message.getJSONObject("chat");
                long chatId = chat.getLong("id");
                data.chat_id.add(chatId);

                String msg = message.getString("text");
                data.update_id.add(update_id);
                data.msg.add(msg);
            }
        }catch(JSONException e){
            Log.e("JSON-PARSER", e.getMessage());
        }
        return data;
    }
}
