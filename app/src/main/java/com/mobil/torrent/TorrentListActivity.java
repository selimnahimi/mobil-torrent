package com.mobil.torrent;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobil.torrent.model.TorrentItem;
import com.mobil.torrent.model.TorrentItemAdapter;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TorrentListActivity extends AppCompatActivity {
    private static final String LOG_TAG = TorrentListActivity.class.getName();
    private FirebaseUser user;

    private FrameLayout redCircle;
    private TextView countTextView;
    private int downloadingItems = 0;
    private int gridNumber = 1;
    private Integer itemLimit = 5;

    // Member variables.
    private RecyclerView mRecyclerView;
    private ArrayList<TorrentItem> mItemsData;
    private TorrentItemAdapter mAdapter;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private NotificationHelper mNotificationHelper;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;
    private SharedPreferences preferences;

    private boolean viewRow = true;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_list);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        // recycle view
        mRecyclerView = findViewById(R.id.recyclerView);
        // Set the Layout Manager.
        mRecyclerView.setLayoutManager(new GridLayoutManager(
                this, gridNumber));
        // Initialize the ArrayList that will contain the data.
        mItemsData = new ArrayList<>();
        // Initialize the adapter and set it to the RecyclerView.
        mAdapter = new TorrentItemAdapter(this, mItemsData);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        Log.d(LOG_TAG, "getting firebase instance");
        mItems = mFirestore.collection("Torrents");
        Log.d(LOG_TAG, "getting firebase collection");
        queryData();
        Log.d(LOG_TAG, "firebase query complete");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver, filter);

        mNotificationHelper = new NotificationHelper(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        // setAlarmManager();
        setJobScheduler();
    }

    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            if (intentAction == null)
                return;

            switch (intentAction) {
                case Intent.ACTION_POWER_CONNECTED:
                    itemLimit = 10;
                    queryData();
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    itemLimit = 5;
                    queryData();
                    break;
            }
        }
    };

    private void initializeData() {
        // Get the resources from the XML file.
        String[] itemsList = getResources()
                .getStringArray(R.array.torrent_item_names);
        String[] itemsInfo = getResources()
                .getStringArray(R.array.torrent_item_desc);
        String[] itemsSize = getResources()
                .getStringArray(R.array.torrent_item_size);
        TypedArray itemsImageResources =
                getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemRate = getResources().obtainTypedArray(R.array.shopping_item_rates);

        for (int i = 0; i < itemsList.length; i++) {
            mItems.add(new TorrentItem(
                 itemsList[i],
                 itemsInfo[i],
                 itemsSize[i],
                 itemRate.getFloat(i, 0),
                 itemsImageResources.getResourceId(i, 0),
                 0));
        }

        // Recycle the typed array.
        itemsImageResources.recycle();
    }

    private void queryData() {
        mItemsData.clear();
        mItems.orderBy("downloadCount", Query.Direction.DESCENDING).limit(itemLimit).get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Log.d(LOG_TAG, "NEW TORRENT!!!!!!!!!!!!!!!");
                TorrentItem item = document.toObject(TorrentItem.class);
                item.setId(document.getId());
                mItemsData.add(item);
                Log.d(LOG_TAG, item.getName());
            }

            if (mItemsData.size() == 0) {
                initializeData();
                queryData();
            }

            // Notify the adapter of the change.
            mAdapter.notifyDataSetChanged();
        });
    }

    public void deleteItem(TorrentItem item) {
        DocumentReference ref = mItems.document(item._getId());
        ref.delete()
            .addOnSuccessListener(success -> {
                Log.d(LOG_TAG, "Item is successfully deleted: " + item._getId());
            })
            .addOnFailureListener(fail -> {
                Toast.makeText(this, "Item " + item._getId() + " cannot be deleted.", Toast.LENGTH_LONG).show();
            });

        queryData();
        mNotificationHelper.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out_button:
                Log.d(LOG_TAG, "Logout clicked!");
                FirebaseAuth.getInstance().signOut();
                finish();
                return true;
            case R.id.downloading:
                Log.d(LOG_TAG, "Cart clicked!");
                return true;
            case R.id.view_selector:
                if (viewRow) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                } else {
                    changeSpanCount(item, R.drawable.ic_view_row, 2);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.downloading);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        countTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    public void deductDownload() {
        downloadingItems = (downloadingItems - 1);

        if (0 < downloadingItems) {
            countTextView.setText(String.valueOf(downloadingItems));
        } else {
            countTextView.setText("");
        }

        redCircle.setVisibility((downloadingItems > 0) ? VISIBLE : GONE);
    }

    public void updateAlertIcon(TorrentItem item) {
        downloadingItems = (downloadingItems + 1);
        if (0 < downloadingItems) {
            countTextView.setText(String.valueOf(downloadingItems));
        } else {
            countTextView.setText("");
        }

        redCircle.setVisibility((downloadingItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("downloadCount", item.getDownloadCount() + 1)
            .addOnFailureListener(fail -> {
                Toast.makeText(this, "Item " + item._getId() + " cannot be changed.", Toast.LENGTH_LONG).show();
            });

        mNotificationHelper.send("[ " + item.getName() + "] Download started");
        DownloadAsyncTask downloadAsyncTask = new DownloadAsyncTask(this, item.getName());
        downloadAsyncTask.execute();

        queryData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(powerReceiver);

    }

    private void setAlarmManager() {
        long repeatInterval = 60000; // AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                repeatInterval,
                pendingIntent);


        mAlarmManager.cancel(pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setJobScheduler() {
        // SeekBar, Switch, RadioButton
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        Boolean isDeviceCharging = true;
        int hardDeadline = 5000; // 5 * 1000 ms = 5 sec.

        ComponentName serviceName = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceName)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(isDeviceCharging)
                .setOverrideDeadline(hardDeadline);

        JobInfo jobInfo = builder.build();
        mJobScheduler.schedule(jobInfo);

        // mJobScheduler.cancel(0);
        // mJobScheduler.cancelAll();

    }
}
