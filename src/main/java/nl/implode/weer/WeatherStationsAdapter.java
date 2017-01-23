package nl.implode.weer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by sander on 19-1-17.
 */
public class WeatherStationsAdapter extends ArrayAdapter<WeatherStation> {
    public WeatherStationsAdapter(Context context, ArrayList<WeatherStation> weatherstations) {
        super(context, 0, weatherstations);
        }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        WeatherStation weatherstation = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
        }
        // Lookup view for data population
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(R.id.text2);
        // Populate the data into the template view using the data object
        text1.setText(weatherstation._id + ", "+weatherstation.country);
        text2.setText(weatherstation.name);
        // Return the completed view to render on screen
        return convertView;
    }
}
