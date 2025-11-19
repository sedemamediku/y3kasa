package com.example.kasa;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import androidx.test.core.app.ApplicationProvider;
import android.content.Context;

import com.example.Kasa.AudioRecorder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AudioRecorderTest {
    private AudioRecorder recorder;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        recorder = new AudioRecorder(ctx);
    }

    @Test
    public void initialState_notRecording() {
        assertFalse(recorder.isRecording());
    }

    @Test
    public void getAudioFilePath_returnsNonEmptyPath() {
        String path = recorder.getAudioFilePath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    public void stopRecording_withoutStart_safeAndNotRecording() {
        recorder.stopRecording();
        assertFalse(recorder.isRecording());
    }
}
