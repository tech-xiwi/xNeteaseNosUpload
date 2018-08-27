package com.tech.xiwi.xnosupload;

import com.netease.vcloudnosupload.NOSUpload;

public class UploadResult {
    //init
    String nosToken;
    String bucket;
    String object;

    //result
    int code = -1;
    String msg;

    NOSUpload.UploadExecutor executor;
}
