package com.warmcare.thyroidtalk;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class SoundSpeakerHelper {
    private final Context appContext;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SoundSpeakerHelper(Context context) {
        this.appContext = context.getApplicationContext();
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = tts.setLanguage(Locale.TRADITIONAL_CHINESE);
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.CHINESE);
                }
                tts.setSpeechRate(0.92f);
                tts.setPitch(1.02f);
                ttsReady = true;
            }
        });
    }

    public void announcePhrase(String text, boolean doubleChime) {
        if (PhraseStorage.isMuted(appContext)) {
            return;
        }
        final String speakText = text.replace("／", "，");
        new Thread(() -> {
            playWarmDingDong(doubleChime);
            mainHandler.post(() -> speakOnly(speakText));
        }).start();
    }

    public void speakOnly(String text) {
        if (PhraseStorage.isMuted(appContext)) {
            return;
        }
        String cleanText = text.replace("／", "，");
        if (tts != null && ttsReady) {
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "warmcare_utterance");
        }
    }

    private void playWarmDingDong(boolean doubleChime) {
        try {
            int sampleRate = 22050;
            double[] freqs = doubleChime
                    ? new double[]{659.25, 523.25, 659.25, 523.25}
                    : new double[]{659.25, 523.25};
            double noteDurationSec = 0.22;
            int samplesPerNote = (int) (sampleRate * noteDurationSec);
            int totalSamples = samplesPerNote * freqs.length;
            short[] buffer = new short[totalSamples];

            for (int n = 0; n < freqs.length; n++) {
                double freq = freqs[n];
                int offset = n * samplesPerNote;
                for (int i = 0; i < samplesPerNote; i++) {
                    double t = (double) i / sampleRate;
                    double attack = Math.min(1.0, t / 0.015);
                    double decay = Math.exp(-5.5 * t);
                    double wave = Math.sin(2.0 * Math.PI * freq * t)
                            + 0.25 * Math.sin(2.0 * Math.PI * (freq * 2.0) * t);
                    double sampleVal = wave * attack * decay * 14000.0;
                    if (sampleVal > Short.MAX_VALUE) sampleVal = Short.MAX_VALUE;
                    if (sampleVal < Short.MIN_VALUE) sampleVal = Short.MIN_VALUE;
                    buffer[offset + i] = (short) sampleVal;
                }
            }

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();

            AudioTrack track = new AudioTrack(
                    attrs,
                    format,
                    buffer.length * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );
            track.write(buffer, 0, buffer.length);
            track.play();
            Thread.sleep((long) (freqs.length * noteDurationSec * 1000) + 40);
            track.release();
        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}