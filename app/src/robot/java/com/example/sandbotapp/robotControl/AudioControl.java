package com.example.sanbotapp.robotControl;

import android.media.AudioManager;

public class AudioControl {
    private AudioManager audioManager;

    public AudioControl(AudioManager audioManager){
        this.audioManager = audioManager;
    }

    // Función utilizada para guardar el volumen de los altavoces del robot (0-100)
    public void setVolumen(int porcentaje) {
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int level = Math.round(porcentaje * max / 100f);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
    }

    // Función utilizada para obtener el volumen en porcentaje (0-100)
    public int getVolumen() {
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max == 0) return 0;
        int actual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return Math.round(actual * 100f / max);
    }

}
