package org.dfhu.clicknrecord;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


public class CnRecord extends ActionBarActivity {


    private MediaRecorder mr = null;
    private Integer numSeconds = 45;
    volatile private boolean recordingStopped = false;
    volatile private boolean stopPlaying = false;
    volatile private boolean currentlyPlaying = false;

    private RecordingsAdapter recordingsAdapter = null;
    final ArrayList<RecordedFile> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cn_record);

        setupRecordingsView();

        updateAdapter();

        bindRecordNowButton();

        bindPlaybackButton();

        bindStopButton();

    }

    private void setupRecordingsView() {
        final ListView recordingsView = (ListView) findViewById(R.id.recordingsListView);
        recordingsAdapter = new RecordingsAdapter(this, fileList);
        recordingsView.setAdapter(recordingsAdapter);

        recordingsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RecordedFile rf = (RecordedFile) recordingsView.getItemAtPosition(position);

                MediaPlayer mPlayer = new MediaPlayer();

                try {
                    mPlayer.setDataSource(rf.absolutePath);
                    mPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mPlayer.start();
            }
        });

        recordingsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(
                    AdapterView<?> parent, View view, int position, long id) {
                RecordedFile rf = (RecordedFile) recordingsView.getItemAtPosition(position);
                File file = new File(rf.absolutePath);
                if (file.delete()) {
                    fileList.remove(position);
                    updateAdapter();
                    return true;
                }
                return false;
            }
        });
    }

    private void bindStopButton() {
        Button stopRecordingButton = (Button) findViewById(R.id.stopButton);

        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordingStopped = true;
                stopPlaying = true;
                Toast.makeText(
                        getApplicationContext(),
                        "Stopping Recording",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindPlaybackButton() {
        final Button playback = (Button) findViewById(R.id.playback);

        playback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentlyPlaying) {
                            return;
                        }

                        currentlyPlaying = true;
                        String lastFileName = getLatestFilename();
                        if (lastFileName == null) {
                            return;
                        }

                        playRecording();
                        stopPlaying = false;
                        currentlyPlaying = false;
                    }
                }).start();

            }
        });
    }

    private void playRecording() {
        MediaPlayer mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(getLatestFilename());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException exc) {
            Log.e("playback", "prepare() failed:" + exc.getMessage());
        }

        while(mPlayer.isPlaying()) {
            if (stopPlaying) {
                try {
                    mPlayer.stop();
                } catch (IllegalStateException e) {
                    Log.w("RECORDING", "IllegalStateException");
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void bindRecordNowButton() {
        final Button recordNowButton = (Button) findViewById(R.id.recordNow);

        recordNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence msg = getString(R.string.recording_for_X_seconds, numSeconds);
                Context context = getApplicationContext();
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                handleLocation();

                AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {

                    @Override
                    protected Integer doInBackground(Integer... params) {
                        recordNow();
                        return 1;
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        updateAdapter();
                    }
                };
                task.execute(0);

            }
        });
    }

    private void updateAdapter ()
    {
        File[] files = getOutputDir().listFiles();
        fileList.clear();
        if (files.length > 0) {

            for (File file : getOutputDir().listFiles()) {
                RecordedFile recorded = new RecordedFile(file.getName(), file.getAbsolutePath());
                fileList.add(recorded);
            }
        }
        recordingsAdapter.notifyDataSetChanged();
    }

    private void handleLocation () {

        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double lon = location.getLongitude();
                double lat = location.getLatitude();
                Float accuracy = location.getAccuracy();
                TextView lonText = (TextView) findViewById(R.id.lonText);
                TextView latText = (TextView) findViewById(R.id.latText);
                TextView accuracyText = (TextView) findViewById(R.id.accuracyText);

                lonText.setText(String.format("lon: %.3f", lon));
                latText.setText(String.format("lat: %.3f", lat));
                accuracyText.setText(String.format("acc: %.2f", accuracy));


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

    private void recordNow() {
        String fn = getOutputFilename();

        ProgressBar mProgress = (ProgressBar) findViewById(R.id.recordingTimeProgressBar);
        mProgress.setMax(numSeconds);
        mProgress.setProgress(10);

        mr = new MediaRecorder();
        mr.setMaxFileSize(1 << 20);
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
        mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mr.setOutputFile(fn);
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mr.prepare();
        } catch (IOException exc) {
            Log.e("record", "prepare() failed " + exc.getMessage());
            return;
        }


        mr.start();

        recordingStopped = false;
        for (int ii = 1; ii <= numSeconds + 1; ii++) {
            if (recordingStopped) {
                mProgress.setProgress(numSeconds);


                // little extra time after stop button
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                break;
            }

            try {
                mProgress.setProgress(ii);
                Thread.sleep(1000);
            } catch (InterruptedException exc) {
                // ok
            }
        }


        try {
            mr.stop();
        } catch (IllegalStateException e) {
            // can't toast here throws
            // java.lang.RuntimeException: Can't create handler
            // inside thread that has not called Looper.prepare()
        }



        mr.release();
    }

    private File getOutputDir ()
    {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();

        File recordingDir = new File(dir + "/clicknrecord");
        if (!recordingDir.exists()) {
            if (!recordingDir.mkdir()) {
                throw new RuntimeException("Could not create directory in: " + dir);
            }
        }
        return recordingDir;
    }

    /**
     * get the last file recorded
     * @return String
     */
    private String getLatestFilename () {
        File recordingDir = getOutputDir();

        File[] files = recordingDir.listFiles();
        Arrays.sort(files);

        if (files.length > 0) {
            return files[files.length - 1].getAbsolutePath();
        }
        return null;
    }

    private String getOutputFilename () {
        File recordingDir = getOutputDir();

        Date now = new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd-HH:mm:ss");

        return recordingDir + "/cnr-" + dateFormat.format(now) + ".3gp";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cn_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
