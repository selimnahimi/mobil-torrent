package com.mobil.torrent;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Random;

public class DownloadAsyncTask extends AsyncTask<Void,Void, String> {

    private WeakReference<TextView> mTextView;
    private TorrentListActivity context;
    private NotificationHelper mNotificationHelper;
    private String torrentName;

    DownloadAsyncTask(TorrentListActivity context, String torrentName) {
        this.context = context;
        this.torrentName = torrentName;
        this.mNotificationHelper = new NotificationHelper(context);
    }

    /**
     * Runs on the background thread.
     *
     * @param voids No parameters in this use case.
     * @return Returns the string including the amount of time that
     * the background thread slept.
     */
    @Override
    protected String doInBackground(Void... voids) {
        Random r = new Random();
        int n = r.nextInt(5);
        int ms = n * 1000;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "Letöltés kész (" + (ms / 1000) + "s alatt)";
    }

    protected void onPostExecute(String result) {
        context.deductDownload();
        mNotificationHelper.send("[" + this.torrentName + "] " + result);
        Toast.makeText(context, "[" + this.torrentName + "] " + result, Toast.LENGTH_LONG).show();
    }
}
