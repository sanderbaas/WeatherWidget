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
    public String timeZone;
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

        this.timeZone = com.skedgo.converter.TimezoneMapper.latLngToTimezoneString(
                Double.parseDouble(latitude),
                Double.parseDouble(longitude)
        );
    }

    // Constructor to convert JSON object into a Java class instance
    public WeatherStation(JSONObject object){
        try {
            this._id = object.getInt("_id");
            this.name = object.getString("name");
            this.country = object.getString("country");
            this.longitude = "0";
            this.latitude = "0";
        } catch (JSONException e) {
            Log.e("nl.implode.weer", e.toString());
        }
    }

    public JSONObject get5DayForecast() {
        JSONObject jsonResult = new JSONObject();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String apiEntryPoint = "https://api.open-meteo.com/v1/forecast?";

        String url = apiEntryPoint +
                "latitude=" + this.latitude +
                "&longitude=" + this.longitude +
                "&hourly=is_day,temperature_2m,precipitation,rain,snowfall,weather_code,wind_direction_10m,wind_speed_10m,uv_index" +
                "&wind_speed_unit=ms" +
                "&timezone=" + this.timeZone;

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
