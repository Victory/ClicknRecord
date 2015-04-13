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
import android.widget.Toast;

import java.io.IOException;


public class CnRecord extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cn_record);

        final Button recordNow = (Button) findViewById(R.id.recordNow);

        recordNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence msg = getString(R.string.recording_for_X_seconds);
                Context context = getApplicationContext();
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                recordNow();
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
    }

    private void recordNow()
    {
        String fn = getOutputFilename();

        MediaRecorder mr = new MediaRecorder();
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

        try {
            Thread.sleep(3000);
        } catch (InterruptedException exc) {
            // ok
        }

        mr.stop();
        mr.release();
    }


    private String getOutputFilename () {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();

        // TODO: add human readable timestamp to filename
        return dir + "/recorded.3gp";
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
