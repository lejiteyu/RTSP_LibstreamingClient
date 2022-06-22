package lyon.lyon.libstreaming.client;

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
import android.widget.Toast;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

import lyon.lyon.libstreaming.client.Permissions.PermissionsActivity;
import lyon.lyon.libstreaming.client.Permissions.PermissionsChecker;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "LibStreaming Clinet";
    ProgressDialog progressDialog;
    private PermissionsChecker mPermissionsChecker; // 权限检测器
    public static final int REQUEST_CODE = 2; // 请求码
    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;

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
    public void onResume() {
        super.onResume();
        if(mSession!=null) {
            if (mSession.isStreaming()) {
                mButton1.setText(R.string.stop);
            } else {
                mButton1.setText(R.string.start);
            }
        }else{
            mButton1.setText(R.string.start);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.release();
        Log.e(TAG,"server is stop");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button1) {
            // Starts/stops streaming
            mSession.setDestination(mEditText.getText().toString());
            if (!mSession.isStreaming()) {
                mSession.configure();
                Log.e(TAG,"server is configured");
            } else {
                mSession.stop();
                Log.e(TAG,"server is stop");
            }
            mButton1.setEnabled(false);
        } else {
            // Switch between the two cameras
            mSession.switchCamera();
        }
    }



    /** Displays a popup to report the eror to the user */
    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private Button mButton1, mButton2;
    private SurfaceView mSurfaceView;
    private EditText mEditText;
    private Session mSession;


    private void init(){
        mButton1 = (Button) findViewById(R.id.button1);
        mButton2 = (Button) findViewById(R.id.button2);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mEditText = (EditText) findViewById(R.id.editText1);

        mSession = SessionBuilder.getInstance()
                .setCallback(new Session.Callback() {
                    @Override
                    public void onBitrateUpdate(long bitrate) {
                        Log.d(TAG,"Bitrate: "+bitrate+", is streaming:"+mSession.isStreaming());
                    }

                    @Override
                    public void onSessionError(int reason, int streamType, Exception e) {
                        mButton1.setEnabled(true);
                        if (e != null) {
                            logError(e.getMessage());
                        }
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
                        Log.d(TAG, "client onSessionConfigured:"+mSession.getSessionDescription());
                        mSession.start();
                    }

                    @Override
                    public void onSessionStarted() {
                        Log.d(TAG,"Session started.");
                        mButton1.setEnabled(true);
                        mButton1.setText(R.string.stop);
                    }

                    @Override
                    public void onSessionStopped() {
                        Log.d(TAG,"Session stopped.");
                        mButton1.setEnabled(true);
                        mButton1.setText(R.string.start);
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

        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mSession.startPreview();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                mSession.stop();
            }
        });
    }
}