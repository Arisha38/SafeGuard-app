package com.safeguard.womensafety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Arrays;

public class VideoRecordingService extends Service {
    public static final String ACTION_START = "com.safeguard.womensafety.action.START_VIDEO";
    public static final String ACTION_STOP = "com.safeguard.womensafety.action.STOP_VIDEO";

    private static final String CHANNEL_ID = "safeguard_video_recording_channel";
    private static final int NOTIFICATION_ID = 4201;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private Surface recorderSurface;
    private SurfaceTexture dummySurfaceTexture;
    private Surface dummySurface;
    private String cameraId;
    private boolean isRecording = false;
    private boolean isShuttingDown = false;
    private Uri outputUri;
    private ParcelFileDescriptor outputPfd;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        if (ACTION_STOP.equals(action)) {
            stopRecordingAndShutdown(true);
            return START_NOT_STICKY;
        }
        startForegroundServiceAndRecording();
        return START_STICKY;
    }

    private void startForegroundServiceAndRecording() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("SafeGuard")
                .setContentText("Recording for safety...")
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        startCameraRecording();
    }

    private void startCameraRecording() {
        if (isRecording) return;
        try {
            setupBackgroundThread();
            setupMediaRecorder();

            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            cameraId = getBackCameraId(cameraManager);
            if (cameraId == null) {
                showToast("No camera available");
                stopRecordingAndShutdown(false);
                return;
            }

            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showToast("Camera/Mic permission denied");
                stopRecordingAndShutdown(false);
                return;
            }

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    showToast("Camera disconnected");
                    stopRecordingAndShutdown(false);
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    showToast("Camera unavailable");
                    stopRecordingAndShutdown(false);
                }
            }, cameraHandler);
        } catch (Exception e) {
            showToast("Unable to start recording");
            stopRecordingAndShutdown(false);
        }
    }

    private void createCaptureSession() {
        try {
            dummySurfaceTexture = new SurfaceTexture(10);
            dummySurfaceTexture.setDefaultBufferSize(1280, 720);
            dummySurface = new Surface(dummySurfaceTexture);

            captureSession = null;
            cameraDevice.createCaptureSession(
                    Arrays.asList(recorderSurface, dummySurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                builder.addTarget(recorderSurface);
                                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                session.setRepeatingRequest(builder.build(), null, cameraHandler);
                                mediaRecorder.start();
                                isRecording = true;
                                showToast("Recording started");
                            } catch (Exception e) {
                                showToast("Recording failed");
                                stopRecordingAndShutdown(false);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            showToast("Camera configuration failed");
                            stopRecordingAndShutdown(false);
                        }
                    },
                    cameraHandler
            );
        } catch (CameraAccessException e) {
            showToast("Camera access failed");
            stopRecordingAndShutdown(false);
        }
    }

    private void setupMediaRecorder() throws IOException {
        String fileName = "safeguard_video_" + System.currentTimeMillis() + ".mp4";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SafeGuard");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        outputUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (outputUri == null) throw new IOException("Unable to create MediaStore entry");

        outputPfd = getContentResolver().openFileDescriptor(outputUri, "w");
        if (outputPfd == null) throw new IOException("Unable to open output file");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(outputPfd.getFileDescriptor());
        mediaRecorder.setVideoEncodingBitRate(5_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.prepare();
        recorderSurface = mediaRecorder.getSurface();
    }

    private String getBackCameraId(CameraManager cameraManager) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
            Integer lens = c.get(CameraCharacteristics.LENS_FACING);
            if (lens != null && lens == CameraCharacteristics.LENS_FACING_BACK) return id;
        }
        String[] ids = cameraManager.getCameraIdList();
        return ids.length > 0 ? ids[0] : null;
    }

    private void stopRecordingAndShutdown(boolean userInitiated) {
        if (isShuttingDown) return;
        isShuttingDown = true;
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            }
        } catch (Exception ignored) {
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {
            }
        }
        isRecording = false;

        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (outputPfd != null) {
            try {
                outputPfd.close();
            } catch (IOException ignored) {
            }
            outputPfd = null;
        }
        if (outputUri != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            try {
                getContentResolver().update(outputUri, values, null, null);
            } catch (Exception ignored) {
            }
        }

        if (recorderSurface != null) {
            recorderSurface.release();
            recorderSurface = null;
        }
        if (dummySurface != null) {
            dummySurface.release();
            dummySurface = null;
        }
        if (dummySurfaceTexture != null) {
            dummySurfaceTexture.release();
            dummySurfaceTexture = null;
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
            cameraThread = null;
            cameraHandler = null;
        }

        if (outputUri != null) {
            VideoRepository.add(outputUri);
        }
        if (userInitiated) {
            showToast("Video saved to Gallery");
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void setupBackgroundThread() {
        cameraThread = new HandlerThread("SafeGuardVideoThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for safety background video recording");
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void showToast(String message) {
        new Handler(getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecordingAndShutdown(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
