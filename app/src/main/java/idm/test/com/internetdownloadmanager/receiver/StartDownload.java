package idm.test.com.internetdownloadmanager.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import idm.test.com.internetdownloadmanager.InternetDownloadManagerActivity;


public class StartDownload extends BroadcastReceiver {
    InternetDownloadManagerActivity idmActivity = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("START_DOWNLOAD"));

    }
}