package com.albertoalegria.japt;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.albertoalegria.japt.Constants.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean isWorking = true;

    private Vibrator mVibrator;

    private TextView tvTaskName;
    private TextView tvTimeLeft;

    private ProgressBar timeProgress;

    private AnimatorSet progressAnimatorSet;

    private ObjectAnimator longProgressAnimator;
    private ObjectAnimator shortProgressAnimator;

    private FloatingActionButton stopButton;
    private FloatingActionButton startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeElements();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_task_name:
                createInputDialog();
                break;

            case R.id.startButton:
                startButtonEvent();
                break;

            case R.id.stopButton:
                stopButtonEvent();
                break;
        }
    }

    private void initializeElements() {
        mVibrator = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);

        tvTaskName = (TextView) findViewById(R.id.tv_task_name);
        tvTaskName.setOnClickListener(this);

        tvTimeLeft = (TextView) findViewById(R.id.tv_timer);
        tvTimeLeft.setOnClickListener(this);

        startButton = (FloatingActionButton) findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        stopButton = (FloatingActionButton) findViewById(R.id.stopButton);
        stopButton.setVisibility(View.INVISIBLE);

        timeProgress = (ProgressBar) findViewById(R.id.time_progress);
        timeProgress.setMax(MAX_DURATION);

        longProgressAnimator = ObjectAnimator.ofInt(timeProgress, "Progress", 0, MAX_DURATION);
        shortProgressAnimator = ObjectAnimator.ofInt(timeProgress, "Progress", 0, MAX_DURATION);

        longProgressAnimator.setInterpolator(new LinearInterpolator());
        shortProgressAnimator.setInterpolator(new LinearInterpolator());

        longProgressAnimator.setDuration(WORK_DURATION);
        shortProgressAnimator.setDuration(BREAK_DURATION);

        progressAnimatorSet = new AnimatorSet();
        progressAnimatorSet.playSequentially(longProgressAnimator, shortProgressAnimator);

    }

    private void startButtonEvent() {
        isWorking = true;

        startAnimation();

        startButton.setVisibility(View.INVISIBLE);
        startButton.setOnClickListener(null);

        stopButton.setVisibility(View.VISIBLE);
        stopButton.setOnClickListener(this);
    }

    private void stopButtonEvent() {
        isWorking = false;

        handleAnimationStop();

        stopButton.setVisibility(View.INVISIBLE);
        stopButton.setOnClickListener(null);

        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(this);
    }

    private void handleAnimationStop() {
        if (shortProgressAnimator.isRunning()) {
            shortProgressAnimator.end();
            shortProgressAnimator.cancel();
            longProgressAnimator.end();
            longProgressAnimator.cancel();
        }

        if (longProgressAnimator.isRunning()) {
            longProgressAnimator.end();
            longProgressAnimator.cancel();
            shortProgressAnimator.end();
            shortProgressAnimator.cancel();
        }

        progressAnimatorSet.removeAllListeners();
        progressAnimatorSet.end();
        progressAnimatorSet.cancel();
        timeProgress.setProgress(0);
    }

    private void startAnimation() {
        addListenersToAnimators();

        progressAnimatorSet.start();
    }

    private void addListenersToAnimators() {
        progressAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isWorking) {
                    progressAnimatorSet.start();
                }
            }
        });

        longProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                createNotification(WORK_NOTIFICATION_ID, R.drawable.ic_work, getString(R.string.notification_work_title), getString(R.string.notification_work_body));

                mVibrator.vibrate(1000);

                int resourceColor = ContextCompat.getColor(MainActivity.this, R.color.colorAccent);

                tvTaskName.setTextColor(resourceColor);
                tvTimeLeft.setTextColor(resourceColor);
                timeProgress.getProgressDrawable().setColorFilter(resourceColor, PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                deleteNotification(WORK_NOTIFICATION_ID);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                deleteNotification(WORK_NOTIFICATION_ID);
            }
        });

        shortProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                createNotification(BREAK_NOTIFICATION_ID, R.drawable.ic_free, getString(R.string.notification_break_title), getString(R.string.notification_break_body));

                mVibrator.vibrate(500);

                int resourceColor = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);

                tvTaskName.setTextColor(resourceColor);
                tvTimeLeft.setTextColor(resourceColor);
                timeProgress.getProgressDrawable().setColorFilter(resourceColor, PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                deleteNotification(BREAK_NOTIFICATION_ID);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                deleteNotification(BREAK_NOTIFICATION_ID);
            }
        });

        longProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                tvTimeLeft.setText(getFormattedTime(animation.getCurrentPlayTime()));
            }
        });

        shortProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                tvTimeLeft.setText(getFormattedTime(animation.getCurrentPlayTime()));
            }
        });
    }

    private void createNotification(int notificationId, int iconResource, String notificationTitle, String notificationBody) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(iconResource);
        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(notificationBody);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void deleteNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    private void createInputDialog() {
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("Task Name");

        final EditText taskNameInput = new EditText(MainActivity.this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        taskNameInput.setLayoutParams(layoutParams);
        taskNameInput.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);

        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(30);
        taskNameInput.setFilters(filterArray);

        inputDialog.setView(taskNameInput);


        inputDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String taskName = taskNameInput.getText().toString();
                if (!taskName.isEmpty()) {
                    tvTaskName.setText(getTaskName(taskName));
                }
            }
        });

        inputDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        inputDialog.show();
    }

    private String getFormattedTime(long timeInMilis) {
        int milis = (int) timeInMilis;
        int secs = milis / 1000;
        int secs2 = secs % 60;
        int min = secs / 60;
        String strSecs = String.valueOf(secs2);
        String strMins = String.valueOf(min);
        if (secs2 < 10) {
            strSecs = "0" + String.valueOf(secs2);
        }

        if (min < 10) {
            strMins = "0" + String.valueOf(min);
        }
        return strMins + "m " + strSecs + "s";
    }

    private String getTaskName(String taskName) {
        if (taskName.length() > 20) {
            taskName = taskName.substring(0, 17);
            return taskName + "...";
        } else {
            return taskName;
        }
    }
}
