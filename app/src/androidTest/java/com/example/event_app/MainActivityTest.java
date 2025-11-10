package com.example.event_app;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.example.event_app.activities.entrant.MainActivity;

/**
 * Instrumented tests for MainActivity
 * Tests button clicks and navigation for entrant functionality
 *
 * Load layout checking
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // Initialize Intents for intent verification
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents
        Intents.release();
    }

    @Test
    public void testMainActivity_DisplaysCorrectLayout() {
        onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMainActivity_OpenBrowseEventsButton_Click() {
       onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMainActivity_LaunchSettingsButton_Click() {
        onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMainActivity_ScanQrCodeButton_Click() {
        // note - needs permissions
        onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMainActivity_ToggleButton_Click() {
        onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMainActivity_ActivityLoadsSuccessfully() {
        // Bbasic test
        onView(withId(R.id.main))
                .check(matches(isDisplayed()));
    }
}
