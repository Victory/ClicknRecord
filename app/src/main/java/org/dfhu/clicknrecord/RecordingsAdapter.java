package org.dfhu.clicknrecord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class RecordingsAdapter extends ArrayAdapter<RecordedFile> {
    public RecordingsAdapter(Context context, List<RecordedFile> recordedFiles) {
        super(context, R.layout.recording_item, recordedFiles);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        RecordedFile recording = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.recording_item, parent, false);
        }
        TextView fn = (TextView) convertView.findViewById(R.id.recordingPath);
        fn.setText(recording.filename);

        return convertView;
    }
}
