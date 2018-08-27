package com.tech.xiwi.xnosupload;

import android.content.Context;
import android.util.Log;

import com.netease.vcloudnosupload.AcceleratorConfig;
import com.netease.vcloudnosupload.NOSUpload;
import com.netease.vcloudnosupload.NOSUploadHandler;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class RxNosUploadManager {
    private static final String TAG = "RxNosUploadManager";
    private AcceleratorConfig acceleratorConf = new AcceleratorConfig();

    private static RxNosUploadManager sInstance = null;

    private RxNosUploadManager() {
    }

    public static RxNosUploadManager getInstance() {
        if (sInstance == null) {
            synchronized (RxNosUploadManager.class) {
                if (sInstance == null) {
                    sInstance = new RxNosUploadManager();
                }
            }
        }
        return sInstance;
    }

    private void loadDefaultAcceleratorConf() throws Exception {
        acceleratorConf.setChunkSize(1024 * 32);
        acceleratorConf.setChunkRetryCount(2);
        acceleratorConf.setConnectionTimeout(10 * 1000);
        acceleratorConf.setSoTimeout(30 * 1000);
        acceleratorConf.setLbsConnectionTimeout(10 * 1000);
        acceleratorConf.setLbsSoTimeout(10 * 1000);
        acceleratorConf.setRefreshInterval(2 * 3600);
        acceleratorConf.setMonitorInterval(120 * 1000);
        acceleratorConf.setMonitorThread(true);
    }

    public void httpsUploadFile(final Context context, final NOSUpload.Config config, final String filename, final NOSUploadHandler.UploadCallback callback) {
        Flowable.create(new FlowableOnSubscribe<UploadResult>() {
            @Override
            public void subscribe(final FlowableEmitter<UploadResult> emitter) throws Exception {
                loadDefaultAcceleratorConf();
                NOSUpload nosUpload = NOSUpload.getInstace(context);
                nosUpload.setAcceConfig(acceleratorConf);
                nosUpload.setConfig(config);
                Log.d(TAG, "start init");
                nosUpload.fileUploadInit(filename, null, -1, -1, null, null, -1, null, new NOSUploadHandler.UploadInitCallback() {
                    @Override
                    public void onSuccess(String nosToken, String bucket, String object) {
                        UploadResult result = new UploadResult();
                        result.nosToken = nosToken;
                        result.bucket = bucket;
                        result.object = object;
                        emitter.onNext(result);
                        emitter.onComplete();
                        Log.d(TAG, "init success");
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        UploadResult result = new UploadResult();
                        result.code = code;
                        result.msg = msg;
                        emitter.onNext(result);
                        emitter.onComplete();
                        Log.d(TAG, "init fail");
                    }
                });
            }
        }, BackpressureStrategy.DROP).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).map(new Function<UploadResult, UploadResult>() {
            @Override
            public UploadResult apply(UploadResult uploadResult) throws Exception {
                if (uploadResult != null && uploadResult.code == -1) {
                    File file = new File(filename);
                    uploadResult.executor = NOSUpload.getInstace(context).putFileByHttps(file, null, uploadResult.bucket, uploadResult.object, uploadResult.nosToken, callback);
                } else {
                    uploadResult.executor = null;
                }
                return uploadResult;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<UploadResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(UploadResult uploadResult) {
                if (uploadResult != null) {
                    mUploadResult = uploadResult;
                    if (mUploadResult.executor != null) {
                        synchronized (lock) {
                            cancelWorkThread();
                            workThread = new Thread() {
                                @Override
                                public void run() {
                                    mUploadResult.executor.join();
                                }
                            };
                        }
                        Log.d(TAG, "start upload");
                    } else {
                        Log.e(TAG, "upload code = " + uploadResult.code + ", msg = " + uploadResult.msg);
                        if (callback != null) {
                            callback.onFailure(null);
                        }
                    }
                } else {
                    Log.e(TAG, "error");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (callback != null) {
                    callback.onFailure(null);
                }
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void cancelWorkThread() {
        if (workThread != null && !workThread.isInterrupted()) {
            workThread.interrupt();
            workThread = null;
        }
    }

    public void cancel() {
        if (mUploadResult != null && mUploadResult.executor != null) {
            synchronized (lock) {
                mUploadResult.executor.cancel();
                cancelWorkThread();
                mUploadResult.executor = null;
            }
        }
    }

    private Thread workThread;
    private Object lock = new Object();
    private UploadResult mUploadResult;
}
