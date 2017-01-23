package nl.implode.weer;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sander on 20-1-17.
 */
public class WeatherStationAutoCompleteAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 10;
    private Context mContext;
    private List<WeatherStation> resultList = new ArrayList<WeatherStation>();

    public WeatherStationAutoCompleteAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public WeatherStation getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        //TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
        // Populate the data into the template view using the data object
        text1.setText(getItem(position).name + ", "+getItem(position).country);
        //text2.setText(String.valueOf(getItem(position)._id));
        return convertView;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                Filter.FilterResults filterResults = new Filter.FilterResults();
                if (constraint != null) {
                    List<WeatherStation> weatherStations = findWeatherStations(mContext, constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = weatherStations;
                    filterResults.count = weatherStations.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                if (results != null && results.count > 0) {
                    resultList = (List<WeatherStation>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }

    /**
     * Returns a search result for the given book title.
     */
    private List<WeatherStation> findWeatherStations(Context context, String name) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String apiEntryPoint = "http://api.openweathermap.org/data/2.5/find?";
        String appId = "fbc3d19917801786e46dbacd55d2ee9c";
        Integer maxResults = 10;

        String url = apiEntryPoint + "appid=" + appId + "&cnt=" + String.valueOf(maxResults) + "&q=" + name;
        String result = "";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        List<WeatherStation> searchResults = new ArrayList<WeatherStation>();
        try {
            Response response = client.newCall(request).execute();
            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            if (!jsonObject.isNull("list")) {
                JSONArray stations = jsonObject.getJSONArray("list");
                for (Integer i=0; i < stations.length(); i++) {
                    Log.d("nl.implode.weer",String.valueOf(stations.getJSONObject(i).getInt("id")));
                    searchResults.add(new WeatherStation(
                            stations.getJSONObject(i).getInt("id"),
                            stations.getJSONObject(i).getString("name"),
                            stations.getJSONObject(i).getJSONObject("sys").getString("country"),
                            "",
                            ""
                    ));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return searchResults;


        // GoogleBooksProtocol is a wrapper for the Google Books API
        /*GoogleBooksProtocol protocol = new GoogleBooksProtocol(context, MAX_RESULTS);
        return protocol.findBooks(bookTitle);*/
        /*List<WeatherStation> searchResults = new ArrayList<WeatherStation>();

        WeatherStation station1 = new WeatherStation(1, name + "Dordrecht","NL","0","0");
        WeatherStation station2 = new WeatherStation(2, name + "Rotterdam","NL","0","0");
        WeatherStation station3 = new WeatherStation(3, name + "Zwijndrecht","NL","0","0");
        WeatherStation station4 = new WeatherStation(4, name + "Delft","NL","0","0");

        searchResults.add(station1);
        searchResults.add(station2);
        searchResults.add(station3);
        searchResults.add(station4);

        return searchResults;*/
        /*String readData = readData("https://www.implode.nl/weatherimplode.php?action=getstations&city="+query);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String result = "";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Url)
                .build();
        try {
            Response response = client.newCall(request).execute();
                    result = response.body().string();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;*/
    }
}

