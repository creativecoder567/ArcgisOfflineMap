package com.kfd.arcgisofflinemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.net.URI;

public class DownloadActivity extends AppCompatActivity {

    EditText etUrl;
    Button btnDownload;
    private static final int PERMISSION_STORAGE=1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        etUrl = findViewById(R.id.etUrl);
        btnDownload = findViewById(R.id.btnDownload);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){

                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_DENIED){

                        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissions,PERMISSION_STORAGE);
                    }else {
                       startDownloading();
                    }
                }else {

                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSION_STORAGE:{
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                  startDownloading();
                }else {
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void startDownloading() {
        String url = etUrl.getText().toString();

        if (!TextUtils.isEmpty(url)){
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI|DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Download");
            request.setDescription("Downloading File...");

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,System.currentTimeMillis()+".tpk");

            DownloadManager manager  = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        }
    }
}
