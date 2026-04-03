package com.planify.frontend.utils.managers;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SoundManager {

    private static MediaPlayer mediaPlayer;

    // Sound file paths (place these in your resources folder)
    private static final String ALERT_SOUND = "/com/planify/frontend/audio/alert.mp3";
    private static final String NOTIFICATION_SOUND = "/com/planify/frontend/audio/notification.mp3";
    private static final String REMINDER_SOUND = "/com/planify/frontend/audio/reminder.mp3";

    /**
     * Play alert sound (urgent, loud)
     */
    public static void playAlertSound() {
        playSound(ALERT_SOUND);
    }

    /**
     * Play notification sound (gentle)
     */
    public static void playNotificationSound() {
        playSound(NOTIFICATION_SOUND);
    }

    /**
     * Play reminder sound
     */
    public static void playReminderSound() {
        playSound(REMINDER_SOUND);
    }

    /**
     * Play custom sound from file path
     */
    public static void playSound(String soundPath) {
        try {
            URL resource = SoundManager.class.getResource(soundPath);
            if (resource == null) {
                System.err.println("Sound file not found: " + soundPath);
                return;
            }

            Media sound = new Media(resource.toString());

            // Stop any currently playing sound
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.setVolume(0.7); // 70% volume
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    /**
     * Stop current sound
     */
    public static void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    public static void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(Math.max(0, Math.min(1, volume)));
        }
    }
}