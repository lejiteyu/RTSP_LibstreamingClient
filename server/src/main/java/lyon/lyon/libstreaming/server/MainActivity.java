package lyon.lyon.libstreaming.server;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import org.w3c.dom.Text;

import lyon.lyon.libstreaming.server.Permissions.PermissionsActivity;
import lyon.lyon.libstreaming.server.Permissions.PermissionsChecker;

public class MainActivity extends AppCompatActivity {


    String TAG = "LibStreaming Server";
    ProgressDialog progressDialog;
    private PermissionsChecker mPermissionsChecker; // 权限检测器
    public static final int REQUEST_CODE = 2; // 请求码
    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private SurfaceView mSurfaceView;
    TextView textView;
    String s_port= "1234";
    Intent intent;
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
    protected void onStop() {
        super.onStop();
        if(intent!=null) {
            this.stopService(intent);
            Log.e(TAG,"server is stop");
        }
    }

    private void init(){
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        textView =(TextView) findViewById(R.id.text);
        textView.setText(getLocalIpAddress(this));
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, s_port);
        editor.commit();

        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264);
        intent = new Intent(this,RtspServer.class);
        // Starts the RTSP server
        this.startService(intent);
        Log.e(TAG,"server is start");
    }
    int wifiLevel =-1;
    int wifiLinkSpeed = -1;
    public String getLocalIpAddress(Context context) {

        String ip =  "no connect wifi!";
        WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        ip=String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        wifiLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        wifiLinkSpeed = wifiInfo.getLinkSpeed();
        long currentTotalTxBytes = TrafficStats.getTotalRxBytes();
        long nowTimeStampTotalUp = System.currentTimeMillis();
        Log.d(TAG,"20220118 wifiLevel:"+wifiLevel+" ,wifiLinkSpeed:"+wifiLinkSpeed);
//      Log.e(TAG, "20190610 ***** IP="+ ip);
        return ""+ip+":"+s_port;
    }
}