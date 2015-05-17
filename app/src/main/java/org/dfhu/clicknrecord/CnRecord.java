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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class CnRecord extends ActionBarActivity {

    final private MediaRecorder mr = new MediaRecorder();
    private Integer numSeconds = 15;
    volatile private AtomicBoolean isRecording = new AtomicBoolean(false);
    volatile private boolean currentlyPlaying = false;
    final private MediaPlayer mPlayer = new MediaPlayer();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private RecordingsAdapter recordingsAdapter = null;
    final ArrayList<RecordedFile> fileList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cn_record);

        setupRecordingsView();

        updateAdapter();

        bindRecordNowButton();

        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
            }
        });

        bindPlaybackButton();

        bindStopButton();
    }


    /**
     * set up the list of currently recorded files and bind short click to play
     * and long click to delete
     */
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


    /**
     * bind the stop recording and top playing action button
     */
    private void bindStopButton() {
        Button stopButton = (Button) findViewById(R.id.stopButton);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                stopPlaying();
            }
        });
    }


    /**
     * bind the button that plays back the latest recording
     */
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

                        playLatestRecording();

                        currentlyPlaying = false;
                    }
                }).start();

            }
        });
    }

    /**
     * play the latest recorded file
     */
    private void playLatestRecording() {

        if (mPlayer.isPlaying()) {
            return;
        }

        try {
            mPlayer.reset();
            mPlayer.setDataSource(getLatestFilename());
            mPlayer.prepareAsync();
        } catch (IOException exc) {
            Log.e("playback", "prepare() failed:" + exc.getMessage());
        }

    }


    /**
     * stop the currently playing file
     */
    private void stopPlaying() {
        if (!mPlayer.isPlaying()) {
            return;
        }

        try {
            mPlayer.stop();
        } catch (IllegalStateException e) {
            Log.w("RECORDING", "IllegalStateException");
        }
    }


    /**
     * bind to the record button
     */
    private void bindRecordNowButton() {
        final Button recordNowButton = (Button) findViewById(R.id.recordNow);

        recordNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence msg = getString(R.string.recording_for_X_seconds, numSeconds);
                Context context = getApplicationContext();

                if (!isRecording.get()) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }

                handleLocation();

                AsyncTask<Integer, Integer, Integer> task =
                        new AsyncTask<Integer, Integer, Integer>() {
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


    /**
     * update the recorded file list
     */
    private void updateAdapter ()
    {
        File[] files = getOutputDir().listFiles();
        Arrays.sort(files);
        fileList.clear();
        if (files.length > 0) {

            for (File file : files) {
                RecordedFile recorded = new RecordedFile(file.getName(), file.getAbsolutePath());
                fileList.add(recorded);
            }
        }
        recordingsAdapter.notifyDataSetChanged();
    }


    /**
     * handle showing location information
     */
    private void handleLocation () {

        final LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        final LocationListener locationListener = new LocationListener() {
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

                try {
                    mr.setLocation((float) lon, (float) lat);
                } catch (IllegalStateException e) {
                    // tried and failed
                }
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

        Timer stopPolling = new Timer();
        TimerTask stopPollingTask = new TimerTask() {
            @Override
            public void run() {
                locationManager.removeUpdates(locationListener);
            }
        };
        stopPolling.schedule(stopPollingTask, numSeconds * 1000);

    }

    /**
     * start recording if you are not recording yet
     */
    private void recordNow() {

        if (isRecording.getAndSet(true)) {
            return;
        }
        String fn = getOutputFilename();

        final ProgressBar mProgress = (ProgressBar) findViewById(R.id.recordingTimeProgressBar);
        mProgress.setMax(numSeconds);
        mProgress.setProgress(1);

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

        final Runnable progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (!isRecording.get()) {
                    mProgress.setProgress(0);
                    return;
                }
                int ii = mProgress.getProgress();
                mProgress.setProgress(ii + 1);
            }
        };

        final ScheduledFuture<?> progressHandle =
                scheduler.scheduleAtFixedRate(progressUpdater, 0, 1, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            public void run() {
                progressHandle.cancel(true);
                stopRecording();
            }
        }, numSeconds, TimeUnit.SECONDS);
    }

    /**
     * stop the mediarecorder if its running
     */
    private void stopRecording () {
        if (isRecording.getAndSet(false)) {
            mr.stop();
            Toast.makeText(
                    getApplicationContext(),
                    "Stopping Recording",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * return the output directory for recorded files
     * @return File
     */
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


    /**
     * get the name of the next file to record to
     * @return String
     */
    private String getOutputFilename () {
        File recordingDir = getOutputDir();

        Date now = new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd-HH:mm:ss", Locale.US);

        return recordingDir + "/cnr-" + dateFormat.format(now) + ".mp4";
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
