package lyon.lyon.libstreaming.sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lyon.lyon.libstreaming.sample.Permissions.PermissionsActivity;
import lyon.lyon.libstreaming.sample.Permissions.PermissionsChecker;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener ,
        RadioGroup.OnCheckedChangeListener {
    String TAG = MainActivity.class.getSimpleName();
    ProgressDialog progressDialog;
    private PermissionsChecker mPermissionsChecker; // 权限检测器
    public static final int REQUEST_CODE = 2; // 请求码
    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;

    private Button mButtonSave;
    private Button mButtonVideo;
    private ImageButton mButtonStart;
    private ImageButton mButtonFlash;
    private ImageButton mButtonCamera;
    private ImageButton mButtonSettings;
    private RadioGroup mRadioGroup;
    private FrameLayout mLayoutVideoSettings;
    private FrameLayout mLayoutServerSettings;
    private SurfaceView mSurfaceView;
    private TextView mTextBitrate;
    private EditText mEditTextURI;
    private EditText mEditTextPassword;
    private EditText mEditTextUsername;
    private ProgressBar mProgressBar;
    private Session mSession;
    private RtspClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mPermissionsChecker = new PermissionsChecker(this);
        if (mPermissionsChecker.lacksPermissions(PermissionsActivity.PERMISSIONS)) {
            //Toast.makeText(getBaseContext(),"start ask!!",Toast.LENGTH_SHORT).show();
            PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PermissionsActivity.PERMISSIONS);
        }else{
            init();
        }

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mLayoutVideoSettings.setVisibility(View.GONE);
        mLayoutServerSettings.setVisibility(View.VISIBLE);
        selectQuality();
    }

    private void enableUI() {
        mButtonStart.setEnabled(true);
        mButtonCamera.setEnabled(true);
        mButtonStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // SYSTEM_ALERT_WINDOW permission not granted...
                    Toast.makeText(this, "Permission Denieddd by user.Please Check it in Settings",Toast.LENGTH_LONG);
                }
            }
            finish();
        }else if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }else if(requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_GRANTED){
            init();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                mLayoutServerSettings.setVisibility(View.GONE);
                toggleStream();
                break;
            case R.id.flash:
                if (mButtonFlash.getTag().equals("on")) {
                    mButtonFlash.setTag("off");
                    mButtonFlash.setImageResource(R.drawable.ic_flash_on_holo_light);
                } else {
                    mButtonFlash.setImageResource(R.drawable.ic_flash_off_holo_light);
                    mButtonFlash.setTag("on");
                }
                mSession.toggleFlash();
                break;
            case R.id.camera:
                mSession.switchCamera();
                break;
            case R.id.settings:
                if (mLayoutVideoSettings.getVisibility() == View.GONE &&
                        mLayoutServerSettings.getVisibility() == View.GONE) {
                    mLayoutServerSettings.setVisibility(View.VISIBLE);
                } else {
                    mLayoutServerSettings.setVisibility(View.GONE);
                    mLayoutVideoSettings.setVisibility(View.GONE);
                }
                break;
            case R.id.video:
                mRadioGroup.clearCheck();
                mLayoutServerSettings.setVisibility(View.GONE);
                mLayoutVideoSettings.setVisibility(View.VISIBLE);
                break;
            case R.id.save:
                mLayoutServerSettings.setVisibility(View.GONE);
                break;
        }
    }

    private void init(){

        mButtonVideo = (Button) findViewById(R.id.video);
        mButtonSave = (Button) findViewById(R.id.save);
        mButtonStart = (ImageButton) findViewById(R.id.start);
        mButtonFlash = (ImageButton) findViewById(R.id.flash);
        mButtonCamera = (ImageButton) findViewById(R.id.camera);
        mButtonSettings = (ImageButton) findViewById(R.id.settings);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mEditTextURI = (EditText) findViewById(R.id.uri);
        mEditTextUsername = (EditText) findViewById(R.id.username);
        mEditTextPassword = (EditText) findViewById(R.id.password);
        mTextBitrate = (TextView) findViewById(R.id.bitrate);
        mLayoutVideoSettings = (FrameLayout) findViewById(R.id.video_layout);
        mLayoutServerSettings = (FrameLayout) findViewById(R.id.server_layout);
        mRadioGroup =  (RadioGroup) findViewById(R.id.radio);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mRadioGroup.setOnCheckedChangeListener(this);
        mRadioGroup.setOnClickListener(this);

        mButtonStart.setOnClickListener(this);
        mButtonSave.setOnClickListener(this);
        mButtonFlash.setOnClickListener(this);
        mButtonCamera.setOnClickListener(this);
        mButtonVideo.setOnClickListener(this);
        mButtonSettings.setOnClickListener(this);
        mButtonFlash.setTag("off");
        mButtonStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        if (mPrefs.getString("uri", null) != null) mLayoutServerSettings.setVisibility(View.GONE);
        mEditTextURI.setText(mPrefs.getString("uri", getString(R.string.default_stream)));
        mEditTextPassword.setText(mPrefs.getString("password", ""));
        mEditTextUsername.setText(mPrefs.getString("username", ""));
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSession = SessionBuilder.getInstance()
                .setCallback(new Session.Callback() {
                    @Override
                    public void onBitrateUpdate(long bitrate) {
                        // Informs you of the bandwidth consumption of the streams
                        Log.d(TAG,"Bitrate: "+bitrate);
                    }

                    @Override
                    public void onSessionError(int reason, int streamType, Exception e) {
                        // Might happen if the streaming at the requested resolution is not supported
                        // or if the preview surface is not ready...
                        // Check the Session class for a list of the possible errors.
                        Log.e(TAG, "An error occured", e);
                    }

                    @Override
                    public void onPreviewStarted() {
                        Log.d(TAG,"Preview started.");

                    }

                    @Override
                    public void onSessionConfigured() {
                        Log.d(TAG,"Preview configured.");
                        // Once the stream is configured, you can get a SDP formated session description
                        // that you can send to the receiver of the stream.
                        // For example, to receive the stream in VLC, store the session description in a .sdp file
                        // and open it with VLC while streming.
                        Log.d(TAG, mSession.getSessionDescription());
                        mSession.start();
                    }

                    @Override
                    public void onSessionStarted() {
                        Log.d(TAG,"Streaming session started.");
                    }

                    @Override
                    public void onSessionStopped() {
                        Log.d(TAG,"Streaming session stopped.");
                    }
                })
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setAudioQuality(new AudioQuality(16000, 32000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(320,240,20,500000))
                .build();

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // Starts the preview of the Camera
                mSession.startPreview();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                // Stops the streaming session
                mSession.stop();
            }
        });

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(new RtspClient.Callback() {
            @Override
            public void onRtspUpdate(int message, Exception e) {
                switch (message) {
                    case RtspClient.ERROR_CONNECTION_FAILED:
                    case RtspClient.ERROR_WRONG_CREDENTIALS:
                        mProgressBar.setVisibility(View.GONE);
                        enableUI();
                        logError(e.getMessage());
                        e.printStackTrace();
                        break;
                }
            }
        });



        selectQuality();
    }



    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        // Displays a popup to report the eror to the user
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Connects/disconnects to the RTSP server and starts/stops the stream
    public void toggleStream() {
        mProgressBar.setVisibility(View.VISIBLE);
        if (!mClient.isStreaming()) {
            String ip="",port="",path="";
            mButtonStart.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            // We save the content user inputs in Shared Preferences
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("uri", mEditTextURI.getText().toString());
            editor.putString("password", mEditTextPassword.getText().toString());
            editor.putString("username", mEditTextUsername.getText().toString());
            editor.commit();

            // We parse the URI written in the Editext
            Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
            Matcher m = uri.matcher(mEditTextURI.getText());
            m.find();
            if( m.groupCount()>=3)
                ip = m.group(1);
            if( m.groupCount()>=3)
                port = m.group(2);
            if( m.groupCount()>=3)
                path = m.group(3);

            mClient.setCredentials(mEditTextUsername.getText().toString(), mEditTextPassword.getText().toString());
            mClient.setServerAddress(ip, Integer.parseInt(port));
            mClient.setStreamPath("/"+path);
            mClient.startStream();

        } else {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
            mProgressBar.setVisibility(View.GONE);
            mButtonStart.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
    }



    private void selectQuality() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        RadioButton button = (RadioButton) findViewById(id);
        if (button == null) return;

        String text = button.getText().toString();
        Pattern pattern = Pattern.compile("(\\d+)x(\\d+)\\D+(\\d+)\\D+(\\d+)");
        Matcher matcher = pattern.matcher(text);

        matcher.find();
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));
        int framerate = Integer.parseInt(matcher.group(3));
        int bitrate = Integer.parseInt(matcher.group(4))*1000;

        mSession.setVideoQuality(new VideoQuality(width, height, framerate, bitrate));
        Toast.makeText(this, ((RadioButton)findViewById(id)).getText(), Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Selected resolution: "+width+"x"+height);
    }
}