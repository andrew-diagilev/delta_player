 package com.example.myrtspplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.example.myrtspplayer.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_URL_CONFIG = 100;
    private ActivityMainBinding binding;
    private LibVLC libVLC;
    private org.videolan.libvlc.MediaPlayer mediaPlayer;
    private String url = "rtsp://192.168.10.75:8554/stream";
    /*private String url = "rtsp://192.168.150.115:8554/stream";*/
/*    private int maxRetryAttempts = 100; // Максимальное количество попыток подключения
    private int currentRetryAttempt = 0; // Текущее количество попыток подключения*/
    private Handler handler;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler = new Handler();
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        url = sharedPreferences.getString("streamUrl", "rtsp://192.168.10.75:8554/stream");

        // Инициализация VLC и медиа!
        initializePlayer();

        FloatingActionButton fabSettings = findViewById(R.id.fabSettings);
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the URL configuration activity
                Intent intent = new Intent(MainActivity.this, UrlConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        // Обновление URL при каждом возвращении на главный экран
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        url = sharedPreferences.getString("streamUrl", "rtsp://192.168.10.75:8554/stream");
        // Повторная инициализация плеера только если mediaPlayer еще не инициализирован
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
        initializePlayer();
    }

    /*protected void onResume() {
        super.onResume();
        if (mediaPlayer == null) {
            initializePlayer();
        }
        else mediaPlayer.play();
    }*/

    private void showLoader() {
        binding.loaderLayout.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        binding.loaderLayout.setVisibility(View.GONE);
    }


    private void initializePlayer() {
        showLoader();
        binding.loaderLayout.setVisibility(View.VISIBLE);
        libVLC = new LibVLC(this);
        Media media = new Media(libVLC, Uri.parse(url));
        media.addOption("--aout=opensles");
        media.addOption("--audio-time-stretch");
        media.addOption("-vvv"); // verbosity

        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        mediaPlayer.setMedia(media);
        mediaPlayer.getVLCVout().setVideoSurface(binding.contentMain.videoView.getHolder().getSurface(), binding.contentMain.videoView.getHolder());
        mediaPlayer.getVLCVout().setWindowSize(binding.contentMain.videoView.getWidth(), binding.contentMain.videoView.getHeight());
        mediaPlayer.getVLCVout().attachViews();

        // Установка слушателя событий для обработки ошибок воспроизведения
        mediaPlayer.setEventListener(event -> {
            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
                    isPlaying = true;
                    hideLoader();
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.EncounteredError:
                    Log.e("MediaPlayer", "Error encountered while playing media");
                        // Если произошла ошибка и попыток меньше максимального количества, пытаемся подключиться еще раз
                        handler.postDelayed(() -> {
                            // Повторная попытка подключения через некоторое время
                            Log.d("MediaPlayer", "Retrying connection...");
                            mediaPlayer.play();
                            showLoader();
                        }, 5000); // Пауза перед повторной попыткой в миллисекундах (здесь 5000 мс = 5 секунд)
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                    // Воспроизведение завершилось, попробуем перезапустить его
                    Log.d("MediaPlayer", "End of media reached. Restarting playback...");
                    handler.postDelayed(() -> {
                        // Перезапуск воспроизведения после задержки
                        /*mediaPlayer.stop();*/
                        showLoader();
                        mediaPlayer.play();
                    }, 5000); // Задержка перед перезапуском в миллисекундах (здесь 5000 мс = 5 секунд)
                    break;
            }
        });

        // Начало воспроизведения
        mediaPlayer.play();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_URL_CONFIG && resultCode == Activity.RESULT_OK) {
            // Update URL with the newly saved URL from UrlConfigActivity
            String newUrl = data.getStringExtra("newUrl");
            url = newUrl;

            // Save the new URL to SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("streamUrl", newUrl);
            editor.apply();

            // Reinitialize player with the new URL
            initializePlayer();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Освобождение ресурсов при уничтожении активности
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }
}