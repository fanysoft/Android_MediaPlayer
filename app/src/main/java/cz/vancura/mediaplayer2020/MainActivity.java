package cz.vancura.mediaplayer2020;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener {

    private static final String TAG = "myTAG-MainActivity";

    private static MediaPlayer mediaPlayer;
    private static SurfaceHolder sufaceVideoHolder;
    private static SurfaceView surfaceVideoView;

    private static ImageView button;
    private static TextView currentTimeTextView, endTimeTextView, subtitleTextView;
    private static SeekBar seekBar;
    private static ProgressBar progressBar;

    private static Handler mHandlerTimer = new Handler();
    private static Handler mHandlerSubtitles = new Handler();

    private static View view;
    private static Context context;

    androidx.appcompat.app.ActionBar actionBar;

    static int mediaDuration;
    int currentPlayheadMilisec = 0;
    String currentPlayheadMinSecString = "";
    int lastPositionOneSecondAgo = 0;

    String  deviceLanguage = "eng"; // or sk or cz

    ArrayList<String> subtitlesArrayList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "App started");

        // view an context reference
        view = findViewById(R.id.mainView);
        context = this;

        // SurfaceView
        surfaceVideoView = findViewById(R.id.surfaceView);
        sufaceVideoHolder = surfaceVideoView.getHolder();
        sufaceVideoHolder.addCallback(this);

        // seekBar
        seekBar = findViewById(R.id.seekBar);

        // ActionBar
        actionBar = this.getSupportActionBar();

        // progressBar
        progressBar = findViewById(R.id.progressBar);

        // TextView
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        endTimeTextView = findViewById(R.id.endTimeTextView);
        subtitleTextView = findViewById(R.id.subtitleTextView);

        // init subtitles array
        InsertSubtitlesToArray();

        // Stop/Play button onClick
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "button pressed ..");

                // video is not playing - start it
                if (!mediaPlayer.isPlaying()) {

                    play();

                }else{
                    // video is playing - pause it

                    pause();

                }
            }
        });


        // Handler - every second
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                //Log.d(TAG, "Handler on UiThread - every second ..");

                if(mediaPlayer != null && mediaPlayer.isPlaying()){

                    currentPlayheadMilisec = mediaPlayer.getCurrentPosition();
                    currentPlayheadMinSecString = convertMilisecToMins(currentPlayheadMilisec);
                    Log.d(TAG, "media position is currentPlayheadMilisec=" + currentPlayheadMilisec + " currentPlayheadMinSecString=" + currentPlayheadMinSecString);

                    currentTimeTextView.setText(currentPlayheadMinSecString);

                    // SeekBar
                    seekBar.setMax(mediaDuration);
                    seekBar.setProgress(currentPlayheadMilisec);


                    // Subtitles - my custom solution as original MediaPlayers was not reliable with seeking
                    for (int counter = 0; counter < subtitlesArrayList.size(); counter++) {

                        String oneSubtitleComplete = subtitlesArrayList.get(counter);
                        List<String> oneSubtitleList = Arrays.asList(oneSubtitleComplete.split(","));

                        String sub_start = oneSubtitleList.get(0);
                        String sub_end = oneSubtitleList.get(1);
                        String sub_line1 = oneSubtitleList.get(2);
                        String sub_line2 = oneSubtitleList.get(3);

                        Date currentPlayheadDate = null;
                        Date sub_startDate = null;
                        Date sub_endDate = null;

                        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                        try {
                            currentPlayheadDate = sdf.parse(currentPlayheadMinSecString);
                            sub_startDate = sdf.parse(sub_start);
                            sub_endDate = sdf.parse(sub_end);
                        } catch (ParseException e) {
                            Log.e(TAG, "ERROR converting Sting to Date " + e.getLocalizedMessage());
                        }

                        // compare times
                        // if sub_end >= currentPlayheadDate >= sub_start - show sub
                        if ((sub_endDate.compareTo(currentPlayheadDate) >= 0 ) && (currentPlayheadDate.compareTo(sub_startDate) >= 0 )){

                            String subtitle = sub_line1 + "\n" + sub_line2;
                            Log.d(TAG,"subtitles - showing subtitle - " + subtitle);
                            subtitleTextView.setText(subtitle);

                        }

                    }


                    // progressBar - detection of still video
                    if (currentPlayheadMilisec == lastPositionOneSecondAgo) {

                        Log.d(TAG, "video not playing but it should be - buffering ? - show progressBar");
                        progressBar.setVisibility(View.VISIBLE);

                    }else{

                        if (progressBar.getVisibility() == View.VISIBLE) {
                            Log.d(TAG, "video resumed - hide progressBar");
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                    }

                    lastPositionOneSecondAgo = currentPlayheadMilisec;

                }
                // delay 1 sec
                mHandlerTimer.postDelayed(this, 1000);
            }

        });


         // seekBar changeListener - user moved position
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(mediaPlayer != null && mediaPlayer.isPlaying() && fromUser){

                    // seeking and buffering - show progressBar
                    Log.d(TAG, "progressBar show after seek");
                    progressBar.setVisibility(View.VISIBLE);

                    Log.d(TAG, "user used seekBar .. - progress=" + progress);

                    Log.d(TAG, "mediaPlayer.seekTo() newPosition=" + progress);
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }


        });

    }

    private void InsertSubtitlesToArray() {

        // subtitles pole String
        // - sub_start
        // - sub_end
        // - String sub_line1
        // - String sub_line2

        // according to device language
        if (deviceLanguage.equals("cz") || deviceLanguage.equals("sk")) {

            // Czech
            subtitlesArrayList.add("0:01,0:03,Titulky 1, Radka 2");
            subtitlesArrayList.add("0:10,0:23,Titulky 2, Radka 2");
            subtitlesArrayList.add("0:24,0:28,Titulky 3, Radka 2");
            subtitlesArrayList.add("0:29,0:40,Titulky 4, Radka 2");

        }else{

            // English
            subtitlesArrayList.add("0:01,0:03,Subtitle 1, row 2");
            subtitlesArrayList.add("0:05,0:08,Subtitle 2, row 2");
            subtitlesArrayList.add("0:14,0:20,Subtitle 3, row 2");
            subtitlesArrayList.add("0:21,0:33,Subtitle 4, row 2");

        }
    }


    // MediaPlayer callback - player is ready
    @Override
    public void onPrepared(MediaPlayer mp) {

        Log.d(TAG, "MediaPlayer onPrepared");

        // hide ProgressBar
        progressBar.setVisibility(View.INVISIBLE);
        Log.d(TAG, "progressBar hide");

        // get media duration for GUI
        mediaDuration = mediaPlayer.getDuration();
        Log.d(TAG, "media duration=" + mediaDuration);
        endTimeTextView.setText(convertMilisecToMins(mediaDuration));

        // auto-play
        play();

    }

    private static void play() {

        Log.d(TAG, "play() - mediaPlayer.start()");
        mediaPlayer.start();

        // change icon to pause
        button.setImageResource(R.drawable.ic_pause_circle_outline_24px);

        // keep video aspect ratio - see https://stackoverflow.com/questions/8240051/android-mediaplayer-video-aspect-ratio-issue
        MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {

            @Override
            public void onVideoSizeChanged(MediaPlayer player, int width, int height) {

                if(player != null) {

                    Integer screenWidth = ((Activity) context).getWindowManager().getDefaultDisplay().getWidth();
                    Integer screenHeight = ((Activity) context).getWindowManager().getDefaultDisplay().getHeight();
                    android.view.ViewGroup.LayoutParams videoParams = surfaceVideoView.getLayoutParams();

                    if (width > height) {
                        videoParams.width = screenWidth;
                        videoParams.height = screenWidth * height / width;
                    } else {
                        videoParams.width = screenHeight * width / height;
                        videoParams.height = screenHeight;
                    }

                    surfaceVideoView.setLayoutParams(videoParams);
                }

            }
        };
        mediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);

    }

    private static void pause() {

        Log.d(TAG, "pause - mediaPlayer.pause()");
        mediaPlayer.pause();

        // change icon to play
        button.setImageResource(R.drawable.ic_play_circle_outline_24px);

    }

    // SurfaceView callbacks
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceView surfaceCreated holder=" + holder.toString());

        // show ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "progressBar show");

        if (mediaPlayer == null){

            try {

                Log.d(TAG, "creating MediaPayer ..");

                // setup MediaPlayer
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDisplay(sufaceVideoHolder);


                // Video source - at Url
                // PROBLEM - if throws error and it is posssible to play as local file - than check your video file format
                // see https://stackoverflow.com/questions/11540076/android-mediaplayer-error-1-2147483648
                String videoUrl = "";
                videoUrl = "https://www.vancura.cz/programing/Android/Apps/Dochazka/Help/Big_Buck_Bunny_360_10s_2MB.mp4";
                actionBar.setTitle("Playing video from internet");
                Log.d(TAG, "video source from internet - videoUrl=" + videoUrl);
                mediaPlayer.setDataSource(videoUrl);


    /*
                // Video source - in local /res/raw
                Uri myUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.big_buck_bunny);
                Log.d(TAG, "video source local /res/raw - myUri=" + myUri);

                actionBar.setTitle("Playing Local video");
                mediaPlayer.setDataSource(context, myUri);
*/


                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); // why MUSIC ? I am using video ..

                // call prepreAsync out of UI threat
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
                        } catch (Exception e) {
                            Log.e(TAG, "Error prepareAsync" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();


                // MediaPlayer ErrorListener
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {

                        Log.e(TAG, "mediaPlayer.setOnErrorListener ERROR what=" + what + " extra=" + extra);
                        // if bad video file format
                        // what=1 extra=-2147483648 - MEDIA_ERROR_SYSTEM = corresponds to hexadecimal 0x80000000 which is defined as UNKNOWN_ERROR in frameworks/native/include/utils/Errors.h
                        // what=-38 extra=0 - na emulatoru
                        // see https://stackoverflow.com/questions/11540076/android-mediaplayer-error-1-2147483648

                        // hide ProgressBar
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "progressBar hide after error 1");

                        String errorDetail = "Is internet on ?";

                        switch(extra){
                            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                                errorDetail = "MediaPlayer.MEDIA_ERROR_UNKNOWN";
                                break;
                            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                                errorDetail = "MediaPlayer.MEDIA_ERROR_SERVER_DIED";
                                // Do Something - eg. reset the media player and restart
                                break;
                            case MediaPlayer.MEDIA_ERROR_IO:
                                errorDetail = "MediaPlayer.MEDIA_ERROR_IO";
                                // Do Something - eg. Show dialog to user indicating bad connectivity or attempt to restart the player
                                break;
                            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                errorDetail = "MediaPlayer.MEDIA_ERROR_TIMED_OUT";
                                //Do Something - eg. Show dialog that there was error in connecting to the server or attempt some retries
                                break;
                        }

                        Log.e(TAG, errorDetail);

                        // GUI - show error to user
                        String errorText = "ERROR MediaPlayer - " + errorDetail;
                        showError(errorText);

                        return true;
                    }
                });


            } catch (IOException e) {

                Log.e(TAG, "Error creating MediaPlayer " + e.toString());

                String errorText = "ERROR creating MediaPlayer";
                showError(errorText);

                // hide ProgressBar
                progressBar.setVisibility(View.INVISIBLE);
                Log.d(TAG, "progressBar hide after error 2");

            }

        }else{
            Log.d(TAG, "Player was already created - looks like screen orientation change ..");
        }


    }

    // SurfaceView callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged - format=" + format + " width=" + width + " height=" + height);
    }

    // SurfaceView callbacks
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    // SurfaceView callbacks
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.d(TAG, "onPointerCaptureChanged");
    }


    // GUI - show error to user
    private void showError(String message) {

        final Snackbar mySnackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mySnackbar.dismiss();
            }
        });
        mySnackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        mySnackbar.show();

    }

    // sec to min:sec
    private static String convertMilisecToMins(int seconds){

        int mins = (seconds/1000)/ 60;
        int secs = (seconds/1000)% 60;

        NumberFormat formatter = new DecimalFormat("00");
        String mins2d = formatter.format(mins);
        String sec2d = formatter.format(secs);

        return (mins2d + ":" + sec2d);

    }


}
