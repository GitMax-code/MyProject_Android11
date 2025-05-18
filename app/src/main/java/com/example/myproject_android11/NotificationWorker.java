package com.example.myproject_android11;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    // Define keys for retrieving data, matching those used in setInputData
    public static final String KEY_NOTIFICATION_TITLE = "notification_title";
    public static final String KEY_NOTIFICATION_CONTENT = "notification_content";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    private static final String DEFAULT_CHANNEL_ID = "default_channel"; // Define a

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // Retrieve the input data
        Data inputData = getInputData();
        String title = inputData.getString(KEY_NOTIFICATION_TITLE);
        String content = inputData.getString(KEY_NOTIFICATION_CONTENT);
        int notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0);

        NotificationHelper.showNotification(getApplicationContext(), title, content,notificationId);
        return Result.success();
    }
}
