package com.android.support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private String savePath;
    private String currentCommitSha;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog updateDialog;
    private ProgressBar dialogProgressBar;
    private TextView dialogProgressText;
    private TextView dialogStatusText;
    
    private String LIBRARY_NAME = "libMagicMods.so";
    private String GITHUB_USER_NAME = "Retired-Gamer1";
    private String REPO_NAME = "libs";
    private String GAME_ACTIVITY = "com.tencent.tmgp.cod.CODMainActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout blankLayout = new RelativeLayout(this);
        blankLayout.setBackgroundColor(Color.rgb(245, 245, 245));
        setContentView(blankLayout);

        requestOverlayPermission();
        savePath = getFilesDir().getAbsolutePath() + "/" + LIBRARY_NAME;

        performCommitCheck("https://api.github.com/repos/" + GITHUB_USER_NAME + "/" + REPO_NAME + "/commits?path="+ LIBRARY_NAME);
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                           Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void performCommitCheck(final String urlString) {
        executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(urlString);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent",
                                                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        conn.connect();

                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            Scanner scanner = new Scanner(conn.getInputStream());
                            StringBuilder data = new StringBuilder();
                            while (scanner.hasNext()) {
                                data.append(scanner.nextLine());
                            }
                            scanner.close();

                            JSONArray commitsArray = new JSONArray(data.toString());
                            if (commitsArray.length() > 0) {
                                JSONObject latestCommit = commitsArray.getJSONObject(0);
                                String commitSha = latestCommit.getString("sha");
                                mainHandler.post(new CommitResultRunnable(commitSha));
                                return;
                            }
                        }
                    } catch (Exception e) {
                    }
                    mainHandler.post(new CommitResultRunnable(null));
                }
            });
    }

    private void performLibraryDownload(String libUrl, String commitSha) {
        currentCommitSha = commitSha;
        executorService.execute(new LibraryDownloadRunnable(libUrl, commitSha));
    }

    private void createLibraryInfo(String commitSha) {
        try {
            JSONObject libraryInfo = new JSONObject();
            libraryInfo.put("commit_sha", commitSha);
            libraryInfo.put("library_name", LIBRARY_NAME);
            libraryInfo.put("download_timestamp", System.currentTimeMillis());
            libraryInfo.put("file_size", new File(savePath).length());

            saveLibraryInfo(libraryInfo.toString());
        } catch (Exception e) {
        }
    }

    private void handleCommitResult(String commitSha) {
        String storedCommitSha = getStoredCommitSha();

        if (commitSha != null) {
            if (!commitSha.equals(storedCommitSha)) {
                showUpdateDialog();
                performLibraryDownload("https://raw.githubusercontent.com/" + GITHUB_USER_NAME + "/" + REPO_NAME + "/main/"+ LIBRARY_NAME, commitSha);
            } else {
                Toast.makeText(MainActivity.this, "Mod is up to date", Toast.LENGTH_SHORT).show();
                loadAndStartGame();
            }
        } else {
            Toast.makeText(MainActivity.this, "Failed to check for updates", Toast.LENGTH_SHORT).show();
            loadAndStartGame();
        }
    }

    private void showUpdateDialog() {

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(60, 60, 60, 60);
        dialogLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(24);
        dialogLayout.setBackground(cardBg);

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText("Update Available");
        dialogTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        dialogTitle.setTextColor(Color.BLACK);
        dialogTitle.setTypeface(null, Typeface.BOLD);
        dialogTitle.setGravity(Gravity.CENTER);
        dialogLayout.addView(dialogTitle);

        dialogStatusText = new TextView(this);
        dialogStatusText.setText("Connecting to server...");
        dialogStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dialogStatusText.setTextColor(Color.rgb(80, 80, 80));
        dialogStatusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 30, 0, 30);
        dialogStatusText.setLayoutParams(statusParams);
        dialogLayout.addView(dialogStatusText);
        
        dialogProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        dialogProgressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dialogProgressBar.setLayoutParams(progressParams);
        dialogLayout.addView(dialogProgressBar);

        dialogProgressText = new TextView(this);
        dialogProgressText.setText("");
        dialogProgressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dialogProgressText.setTextColor(Color.DKGRAY);
        dialogProgressText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        percentParams.setMargins(0, 20, 0, 0);
        dialogProgressText.setLayoutParams(percentParams);
        dialogLayout.addView(dialogProgressText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogLayout);
        builder.setCancelable(false);

        updateDialog = builder.create();

        if (updateDialog.getWindow() != null) {
            updateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        updateDialog.show();
    }

    private void updateDownloadProgress(int progress, int downloadedBytes, int totalBytes) {
        String downloadedSize = formatFileSize(downloadedBytes);
        String totalSize = formatFileSize(totalBytes);

        if (dialogProgressBar.isIndeterminate()) {
            dialogProgressBar.setIndeterminate(false);
            dialogProgressBar.setMax(100);
            dialogStatusText.setText("Downloading update...");
        }

        if (dialogProgressBar != null) {
            dialogProgressBar.setProgress(progress);
        }
        if (dialogProgressText != null) {
            dialogProgressText.setText(String.format("%d%% (%s / %s)",
                                                     progress, downloadedSize, totalSize));
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private void loadAndStartGame() {
        File libFile = new File(savePath);
        if (libFile.exists()) {
            try {
                System.load(libFile.getAbsolutePath());
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load mod", Toast.LENGTH_LONG).show();
            }
       }

        try {
            Intent intent = new Intent(MainActivity.this,
                                       Class.forName(GAME_ACTIVITY));
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Log.e("StartGame", "Game activity not found", e);
            Toast.makeText(this, "Game activity not found", Toast.LENGTH_LONG).show();
        }


        finish();
    }

    private String getStoredCommitSha() {
        return getSharedPreferences("library_prefs", MODE_PRIVATE)
            .getString("last_commit", "");
    }

    private void saveCommitSha(String commitSha) {
        Log.d("UpdateCheck", "Saving new SHA: " + commitSha);
        getSharedPreferences("library_prefs", MODE_PRIVATE)
            .edit().putString("last_commit", commitSha).apply();
    }

    private void saveLibraryInfo(String libraryInfo) {
        getSharedPreferences("library_prefs", MODE_PRIVATE)
            .edit().putString("library_info", libraryInfo).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
    }

    private class CommitResultRunnable implements Runnable {
        private String commitSha;

        public CommitResultRunnable(String commitSha) {
            this.commitSha = commitSha;
        }

        @Override
        public void run() {
            handleCommitResult(commitSha);
        }
    }

    private class LibraryDownloadRunnable implements Runnable {
        private String libUrl;
        private String commitSha;

        public LibraryDownloadRunnable(String libUrl, String commitSha) {
            this.libUrl = libUrl;
            this.commitSha = commitSha;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                URL url = new URL(libUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent",
                                        "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    long totalFileSize = conn.getContentLength();
                    InputStream inputStream = conn.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(savePath);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long lastProgressUpdate = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) {
                            inputStream.close();
                            outputStream.close();
                            new File(savePath).delete();
                            mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (updateDialog != null) updateDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "Download cancelled", Toast.LENGTH_SHORT).show();
                                        loadAndStartGame();
                                    }
                                });
                            return;
                        }

                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (totalFileSize > 0) {
                            int progress = (int) ((totalBytesRead * 100L) / totalFileSize);
                            if (progress > lastProgressUpdate || progress == 0) {
                                final int finalProgress = progress;
                                final int finalDownloadedBytes = (int) totalBytesRead;
                                final int finalTotalBytes = (int) totalFileSize;

                                mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateDownloadProgress(finalProgress, finalDownloadedBytes, finalTotalBytes);
                                        }
                                    });
                                lastProgressUpdate = progress;
                            }
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    createLibraryInfo(this.commitSha); 
                    success = true;
                }
            } catch (Exception e) {
                new File(savePath).delete();
                success = false;
            }

            final boolean finalSuccess = success;
            mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (updateDialog != null) {
                            updateDialog.dismiss();
                        }

                        if (finalSuccess) {
                            saveCommitSha(commitSha); 
                            Toast.makeText(MainActivity.this, "Mod updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Update failed, using existing mod", Toast.LENGTH_LONG).show();
                        }

                        loadAndStartGame(); 
                    }
                });
        }
    }
}

