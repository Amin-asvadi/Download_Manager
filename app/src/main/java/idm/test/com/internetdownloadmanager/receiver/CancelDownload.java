package idm.test.com.internetdownloadmanager.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.liulishuo.filedownloader.FileDownloader;

import java.util.Calendar;

/**
 * Created by erfan on 11/15/2017.
 */

public class CancelDownload extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("CANCEL_DOWNLOAD"));

    }

}
