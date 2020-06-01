/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idm.test.com.internetdownloadmanager;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import idm.test.com.internetdownloadmanager.controller.TasksManager;
import idm.test.com.internetdownloadmanager.view.TaskItemAdapter;
import idm.test.com.internetdownloadmanager.view.TaskItemViewHolder;

public class InternetDownloadManagerActivity extends AppCompatActivity implements FileSizeInterface {

    Button addUrlButton;
    TaskItemAdapter taskItemAdapter;
    RecyclerView recyclerView;
    Context context;
    int currentPosition;

    EstimateFileSize estimateFileSize;
    FileSizeInterface fileSizeInterface;

    Dialog dialog;
    int okButtonCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internet_download_manager);

        context = getApplicationContext();
        currentPosition = 0;

        FileDownloadUtils.setDefaultSaveRootPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "IDM");

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        taskItemAdapter = new TaskItemAdapter(TasksManager.getImpl().getModelList());
        taskItemAdapter.setSpeed(-1);
        recyclerView.setAdapter(taskItemAdapter);

        checkForPermissionsMAndAboveBlocking(this);

        ////////////////////////////////////////////////// Download Scheduler

        okButtonCounter = 0;

        dialog = new Dialog(this);


        ////////////////////////////////////////////////////////

        addUrlButton = findViewById(R.id.add_url_btn);

        addUrlButton.setOnClickListener(addUrlOnClickListener);

        TasksManager.getImpl().onCreate(new WeakReference<>(this));
        fileSizeInterface = this;

        registerReceiver(broadcastReceiver, new IntentFilter("START_DOWNLOAD"));
        registerReceiver(broadcastReceiver2, new IntentFilter("CANCEL_DOWNLOAD"));
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Download Started Automatically...!", Toast.LENGTH_LONG).show();
        }
    };

    BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FileDownloader.getImpl().pauseAll();
            for (int i = 0; i < TasksManager.getImpl().getModelList().size(); i++) {
                taskItemAdapter.notifyItemChanged(i);
            }
            Toast.makeText(context, "All Downloads Paused Automatically...!", Toast.LENGTH_LONG).show();
        }
    };

    public void postNotifyDataChanged() {
        if (taskItemAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (taskItemAdapter != null) {
                        taskItemAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        TasksManager.getImpl().onDestroy();
        taskItemAdapter = null;
        FileDownloader.getImpl().pauseAll();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(broadcastReceiver2);
        super.onDestroy();
    }

    View.OnClickListener addUrlOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("URL:");

            estimateFileSize = new EstimateFileSize();
            estimateFileSize.delegate = fileSizeInterface;

            final EditText input = new EditText(v.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().toString().isEmpty()) {
                        currentPosition = TasksManager.getImpl().getTaskCounts();
                        estimateFileSize.execute(input.getText().toString());
                        TasksManager.getImpl().addTask(input.getText().toString());
                        taskItemAdapter.notifyDataSetChanged();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    };

    @Override
    public void postResult(double asyncresult) {
        taskItemAdapter.updateFileSize(asyncresult, currentPosition);
    }

    private class EstimateFileSize extends AsyncTask<String, String, Double> {

        FileSizeInterface delegate = null;

        protected Double doInBackground(String... urls) {
            HttpURLConnection ucon = null;
            try {
                if (android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();
                final URL url = new URL(urls[0]);
                ucon = (HttpURLConnection) url.openConnection();
                ucon.connect();
                return (double) ucon.getContentLength() / 1024.0;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ucon.disconnect();
            }
            return 0.0;
        }

        @Override
        protected void onPostExecute(Double result) {
            if (delegate != null) {
                delegate.postResult(result);
            } else {
                Log.e("ApiAccess", "You have not assigned IApiAccessResponse delegate");
            }
        }
    }




    @TargetApi(23)
    public static void checkForPermissionsMAndAboveBlocking(Activity act) {
        Log.i("Permission", "checkForPermissions() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Here, thisActivity is the current activity
            if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // No explanation needed, we can request the permission.
                act.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                while (true) {
                    if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Permission", "Got permissions, exiting block loop");
                        break;
                    }
                    Log.i("Permission", "Sleeping, waiting for permissions");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (act.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                act.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                while (true) {
                    if (act.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Permission", "Got permissions, exiting block loop");
                        break;
                    }
                    Log.i("Permission", "Sleeping, waiting for permissions");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (act.checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {

                act.requestPermissions(new String[]{Manifest.permission.INTERNET}, 0);
                while (true) {
                    if (act.checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Permission", "Got permissions, exiting block loop");
                        break;
                    }
                    Log.i("Permission", "Sleeping, waiting for permissions");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (act.checkSelfPermission(Manifest.permission.SET_ALARM) != PackageManager.PERMISSION_GRANTED) {

                act.requestPermissions(new String[]{Manifest.permission.SET_ALARM}, 0);
                while (true) {
                    if (act.checkSelfPermission(Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Permission", "Got permissions, exiting block loop");
                        break;
                    }
                    Log.i("Permission", "Sleeping, waiting for permissions");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (act.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                act.requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 0);
                while (true) {
                    if (act.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                        Log.i("Permission", "Got permissions, exiting block loop");
                        break;
                    }
                    Log.i("Permission", "Sleeping, waiting for permissions");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // permission already granted
            else {
                Log.i("Permission", "permission already granted");
            }
        } else {
            Log.i("Permission", "Below M, permissions not via code");
        }
    }
}
