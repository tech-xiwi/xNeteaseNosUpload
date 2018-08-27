package com.tech.xiwi.xneteasenosupload;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.netease.cloud.nos.android.core.CallRet;
import com.netease.vcloudnosupload.NOSUpload;
import com.netease.vcloudnosupload.NOSUploadHandler;
import com.tech.xiwi.xnosupload.RxNosUploadManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText filepath_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filepath_tv = findViewById(R.id.filepath_tv);
        findViewById(R.id.upload_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload(filepath_tv.getText().toString());
            }
        });
    }

    private void upload(final String filepath) {
        NOSUpload.Config config = new NOSUpload.Config();

        config.appKey = "55f3fcee14db4682a11e1c633739d314";
        config.accid = "test_accid_0505";
        config.token = "b99d5baf7afd461e8b1ca747f112bee80854adf2";

        RxNosUploadManager.getInstance().httpsUploadFile(this, config, filepath, new NOSUploadHandler.UploadCallback() {
            @Override
            public void onUploadContextCreate(String oldUploadContext, String newUploadContext) {
                Log.d(TAG, "onUploadContextCreate() called with: oldUploadContext = [" + oldUploadContext + "], newUploadContext = [" + newUploadContext + "]");
            }

            @Override
            public void onProcess(long current, long total) {
                Log.d(TAG, "onProcess() called with: current = [" + current + "], total = [" + total + "]");
            }

            @Override
            public void onSuccess(CallRet ret) {
                Log.d(TAG, "onSuccess() called with: ret = [" + ret + "]");
            }

            @Override
            public void onFailure(CallRet ret) {
                Log.d(TAG, "onFailure() called with: ret = [" + ret + "]");
                if (ret != null) {
                    Log.e(TAG, "onFailure: " + ret.getResponse() + ", " + ret.getHttpCode() + ", " + ret.getCallbackRetMsg());
                }
            }

            @Override
            public void onCanceled(CallRet ret) {
                Log.d(TAG, "onCanceled() called with: ret = [" + ret + "]");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        RxNosUploadManager.getInstance().cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
