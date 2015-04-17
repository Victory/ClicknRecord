package org.dfhu.clicknrecord;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class CnRecord extends ActionBarActivity {


    private MediaRecorder mr = null;
    private Integer numSeconds = 5;
    private boolean recordingStopped = false;
    private Integer fileNumberIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cn_record);

        final Button recordNowButton = (Button) findViewById(R.id.recordNow);

        recordNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence msg = getString(R.string.recording_for_X_seconds);
                Context context = getApplicationContext();
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        recordNow();
                    }
                }).start();

            }
        });


        final Button playback = (Button) findViewById(R.id.playback);

        playback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(getOutputFilename());
                    mPlayer.prepare();
                    mPlayer.start();
                } catch (IOException exc) {
                    Log.e("playback", "prepare() failed:" + exc.getMessage());
                }
            }
        });

        Button stopRecordingButton = (Button) findViewById(R.id.stopRecording);

        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordingStopped = true;
                Toast.makeText(
                        getApplicationContext(),
                        "Stopping Recording",
                        Toast.LENGTH_SHORT).show();
            }
        });

        File[] files = getOutputDir().listFiles();
        if (files.length > 0) {
            ArrayList<RecordedFile> fileList = new ArrayList<>(files.length);
            for (File file: getOutputDir().listFiles()) {
                RecordedFile recorded = new RecordedFile(file.getName(), file.getAbsolutePath());
                fileList.add(recorded);
            }


            final ListView recordingsView = (ListView) findViewById(R.id.recordingsListView);
            RecordingsAdapter songAdapter = new RecordingsAdapter(this, fileList);
            recordingsView.setAdapter(songAdapter);
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
        }
    }

    private void recordNow()
    {
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

    private String getOutputFilename () {
        File recordingDir = getOutputDir();

        if (fileNumberIndex == 2) {
            fileNumberIndex = 1;
        } else {
            fileNumberIndex = 2;
        }

        String fileNum = fileNumberIndex.toString();
        // TODO: add human readable timestamp to filename
        return recordingDir + "/recorded-" + fileNum + ".3gp";
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
