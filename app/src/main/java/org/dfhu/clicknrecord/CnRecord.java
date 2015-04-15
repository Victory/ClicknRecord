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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


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
    }

    private void recordNow()
    {
        String fn = getOutputFilename();

        ProgressBar mProgress = (ProgressBar) findViewById(R.id.recordingTimeProgressBar);
        mProgress.setMax(numSeconds);
        mProgress.setProgress(10);

        mr = new MediaRecorder();
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
            Toast.makeText(
                    getApplicationContext(), "Recording Already Stopped", Toast.LENGTH_SHORT);
        }

        mr.release();

    }


    private String getOutputFilename () {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();

        File recordingDir = new File(dir + "/clicknrecord");
        if (!recordingDir.exists()) {
            if (!recordingDir.mkdir()) {
                throw new RuntimeException("Could not create directory in: " + dir);
            }
        }

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
