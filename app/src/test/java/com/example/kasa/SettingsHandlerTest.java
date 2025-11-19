package com.example.kasa;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import com.example.Kasa.SettingsHandler;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { SettingsHandlerTest.ShadowSettingsSystemDenied.class })
public class SettingsHandlerTest {

    private Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class)
                .create()
                .start()
                .resume()
                .get();
    }

    /**
     * If WRITE_SETTINGS is not allowed, SettingsHandler should launch
     * the ACTION_MANAGE_WRITE_SETTINGS intent.
     */
    @Test
    public void setScreenBrightness_withoutPermission_launchesManageSettings() {
        // by default, our Denied shadow makes canWrite(...) return false
        SettingsHandler.setScreenBrightness(activity, 40);

        ShadowActivity shadow = shadowOf(activity);
        Intent next = shadow.getNextStartedActivity();
        assertNotNull("Expected an intent to be started", next);
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, next.getAction());
        assertTrue(next.getDataString().contains(activity.getPackageName()));
    }

    /**
     * When WRITE_SETTINGS is allowed, brightness should be applied to the window.
     */
    @Test
    @Config(shadows = { ShadowSettingsSystemAllowed.class })
    public void setScreenBrightness_withPermission_appliesToWindow() {
        // our Allowed shadow makes canWrite(...) return true
        SettingsHandler.setScreenBrightness(activity, 60);

        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        assertEquals("Window brightness should be 0.6",
                0.6f, lp.screenBrightness, 0.01f);
    }

    /**
     * Shadow that makes Settings.System.canWrite(...) return false.
     */
    @Implements(Settings.System.class)
    public static class ShadowSettingsSystemDenied {
        @Implementation
        protected static boolean canWrite(android.content.Context ctx) {
            return false;
        }
    }

    /**
     * Shadow that makes Settings.System.canWrite(...) return true.
     */
    @Implements(Settings.System.class)
    public static class ShadowSettingsSystemAllowed {
        @Implementation
        protected static boolean canWrite(android.content.Context ctx) {
            return true;
        }
    }
}
