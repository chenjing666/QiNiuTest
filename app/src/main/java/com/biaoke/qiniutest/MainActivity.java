package com.biaoke.qiniutest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //指定upToken, 强烈建议从服务端提供get请求获取, 这里为了掩饰直接指定key
    public String uptoken = null;
    private String url = "http://172.16.1.144/BK/qiniu-sdk-7.1.3/examples/token.php";
    private Button btnUpload, btn_putpicture;
    private EditText textView;
    private UploadManager uploadManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okhttpget(url);
        textView = (EditText) findViewById(R.id.textView);
        btnUpload = (Button) findViewById(R.id.button_put);
        btnUpload.setOnClickListener(this);
        btn_putpicture = (Button) findViewById(R.id.button_putpicture);
        btn_putpicture.setOnClickListener(this);
        uploadManager = new UploadManager(config);
        //new一个uploadManager类

//        uploadManager = new UploadManager(config);

    }
//    Configuration configuration=new Configuration.Builder().zone(Zone.zone1).build();
//boolean https = true;
// Zone z1 = new AutoZone(https, null);
// Configuration config = new Configuration.Builder().zone(z1).build();
Configuration config = new Configuration.Builder()
//        .chunkSize(256 * 1024)  //分片上传时，每片的大小。 默认256K
//        .putThreshhold(512 * 1024)  // 启用分片上传阀值。默认512K
//        .connectTimeout(10) // 链接超时。默认10秒
//        .responseTimeout(60) // 服务器响应超时。默认60秒
//        .recorder(recorder)  // recorder分片上传时，已上传片记录器。默认null
//        .recorder(recorder, keyGen)  // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
        .zone(Zone.httpsAutoZone) // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
        .build();





    private void okhttpget(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("错误:", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                phoneJson(response.body().string());
//                Message msg = new Message();
//                msg.what = 0;
//                msg.obj = response.body().string();
            }
        });
    }

    private void phoneJson(String json) {
        try {
            JSONObject jsonobject = new JSONObject(json);
            uptoken = jsonobject.getString("token");
            String bucket = jsonobject.getString("bucket");
            Log.d("用户uptoken获取" + bucket, uptoken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_put:
                byte[] data = new byte[]{0, 1, 2, 3};
                //设置上传后文件的key
                String upkey = "uploadtest.txt";
                uploadManager.put(data, upkey, uptoken, new UpCompletionHandler() {
                    public void complete(String key, ResponseInfo rinfo, JSONObject response) {
                        btnUpload.setVisibility(View.INVISIBLE);
                        String s = key + ", " + rinfo + ", " + response;
                        Log.i("qiniutest", s);
                        textView.setTextSize(10);
                        String o = textView.getText() + "\r\n\r\n";
                        //显示上传后文件的url
                        textView.setText(o + s + "\n" + "http://xm540.com1.z0.glb.clouddn.com/" + key);
                    }
                }, new UploadOptions(null, "test-type", true, null, null));
                break;
            case R.id.button_putpicture:
                Intent intent=new Intent(this, PhotoActivity.class);
                intent.putExtra("uptoken",uptoken);
                startActivity(intent);
                break;
        }
    }
}
