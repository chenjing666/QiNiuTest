package com.biaoke.qiniutest;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.KeyGenerator;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class VideoActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 8090;
    private Button button1;
    private Button button2;
    private Button button3;
    private ImageView imageview;
    private Uri imageUri;
    private TextView textview,tv_path;
    private ProgressBar progressbar;
    public static final int RESULT_LOAD_IMAGE = 1;
    private volatile boolean isCancelled = false;
    UploadManager uploadManager;
    String videopath=null;

    public VideoActivity() {
        //断点上传
        String dirPath = "/storage/emulated/0/Download";
        Recorder recorder = null;
        try {
            File f = File.createTempFile("qiniu_xxxx", ".tmp");
            Log.d("qiniu", f.getAbsolutePath().toString());
            dirPath = f.getParent();
            //设置记录断点的文件的路径
            recorder = new FileRecorder(dirPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String dirPath1 = dirPath;
        //默认使用 key 的url_safe_base64编码字符串作为断点记录文件的文件名。
        //避免记录文件冲突（特别是key指定为null时），也可自定义文件名(下方为默认实现)：
        KeyGenerator keyGen = new KeyGenerator() {
            public String gen(String key, File file) {
                // 不必使用url_safe_base64转换，uploadManager内部会处理
                // 该返回值可替换为基于key、文件内容、上下文的其它信息生成的文件名
                String path = key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                Log.d("qiniu", path);
                File f = new File(dirPath1, UrlSafeBase64.encodeToString(path));
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f));
                    String tempString = null;
                    int line = 1;
                    try {
                        while ((tempString = reader.readLine()) != null) {
//                          System.out.println("line " + line + ": " + tempString);
                            Log.d("qiniu", "line " + line + ": " + tempString);
                            line++;
                        }

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return path;
            }
        };

        Configuration config = new Configuration.Builder()
                // recorder 分片上传时，已上传片记录器
                // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
                .recorder(recorder, keyGen)
                .zone(Zone.httpsAutoZone)
                .build();
        // 实例化一个上传的实例
        uploadManager = new UploadManager(config);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        button1 = (Button) findViewById(R.id.bt1);
        button2 = (Button) findViewById(R.id.bt2);
        button3 = (Button) findViewById(R.id.bt3);
        imageview = (ImageView) findViewById(R.id.iv);
        textview = (TextView) findViewById(R.id.tv);
        tv_path= (TextView) findViewById(R.id.tv_path);
        progressbar = (ProgressBar) findViewById(R.id.pb);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectUploadFile(v);
            }
        });
    }
    private Handler mhandler=new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    tv_path.setText(videopath);
                    break;
            }
        }
    };

    public void selectUploadFile(View view) {
        Intent target = FileUtils.createGetContentIntent();
        Intent intent = Intent.createChooser(target,
                this.getString(R.string.choose_file));
        try {
            this.startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException ex) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        // Get the file path from the URI
                        videopath = FileUtils.getPath(this, uri);
                        Message message=new Message();
                        message.what = 1;
                        mhandler.sendMessage(message);
                        Log.d("pathpath---------",videopath);
                    }
                }

                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
        final String token = getIntent().getStringExtra("uptoken");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //设定需要添加的自定义变量为Map<String, String>类型 并且放到UploadOptions第一个参数里面
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("x:phone", "12345678");

                Log.d("qiniu", "click upload");
                isCancelled = false;
                uploadManager.put(videopath, null, token,
                        new UpCompletionHandler() {
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                                if(info.isOK()){
                                    textview.setText(res.toString());
                                }
                            }
                        }, new UploadOptions(map, null, false,
                                new UpProgressHandler() {
                                    public void progress(String key, double percent){
                                        Log.i("qiniu", key + ": " + percent);
                                        progressbar.setVisibility(View.VISIBLE);
                                        int progress = (int)(percent*1000);
//                                          Log.d("qiniu", progress+"");
                                        progressbar.setProgress(progress);
                                        if(progress==1000){
                                            progressbar.setVisibility(View.GONE);
                                        }
                                    }

                                }, new UpCancellationSignal(){
                            @Override
                            public boolean isCancelled() {
                                return isCancelled;
                            }
                        }));
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCancelled = true;
            }
        });
    }

}
