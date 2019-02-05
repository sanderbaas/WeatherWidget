package nl.implode.weer;

import android.os.StrictMode;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sander on 19-1-17.
 */
public class WeatherStation {
    public Integer _id;
    public String name;
    public String country;
    public String latitude;
    public String longitude;

    public WeatherStation(Integer id, String name, String country, String latitude, String longitude) {
        this._id = id;
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Constructor to convert JSON object into a Java class instance
    public WeatherStation(JSONObject object){
        try {
            Log.d("nl.implode.weer", object.getString("name"));
            this._id = object.getInt("_id");
            this.name = object.getString("name");
            this.country = object.getString("country");
            this.longitude = "0";
            this.latitude = "0";
            //this.latitude = String.valueOf(object.getJSONObject("coords").getDouble("lat"));
            //this.longitude = String.valueOf(object.getJSONObject("coords").getDouble("lon"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject get5DayForecast() {
        JSONObject jsonResult = new JSONObject();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String apiEntryPoint = "https://api.openweathermap.org/data/2.5/forecast?";
        String appId = "fbc3d19917801786e46dbacd55d2ee9c";
        Integer maxResults = 10;

        String url = apiEntryPoint + "appid=" + appId + "&id=" + _id;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        String result = "";
        try {
            Response response = client.newCall(request).execute();
            result = response.body().string();
            jsonResult = new JSONObject(result);
        } catch(Exception e) {
             e.printStackTrace();
        }

        return jsonResult;
    }

    // Factory method to convert an array of JSON objects into a list of objects
    // User.fromJson(jsonArray);
    public static ArrayList<WeatherStation> fromJson(JSONArray jsonObjects) {
        ArrayList<WeatherStation> weatherStations = new ArrayList<WeatherStation>();
        for (int i = 0; i < jsonObjects.length(); i++) {
            try {
                weatherStations.add(new WeatherStation(jsonObjects.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return weatherStations;
    }
}
