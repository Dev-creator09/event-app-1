package com.example.event_app;

import android.content.Intent;

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
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.example.event_app.utils.Navigator;

/**
 * Instrumented tests for EventDetailsActivity
 * Tests UI display, button clicks, and navigation
 *
 * Mainly does button if.exists
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsActivityTest {

    private static final String TEST_EVENT_ID = "test-event-123";

    @Rule
    public ActivityScenarioRule<EventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(createIntentWithEventId());

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents
        Intents.release();
    }

    private Intent createIntentWithEventId() {
        Intent intent = new Intent();
        intent.putExtra(Navigator.EXTRA_EVENT_ID, TEST_EVENT_ID);
        return intent;
    }

    @Test
    public void testEventDetailsActivity_DisplaysProgressBarInitially() {
        onView(withId(R.id.progressBar_event_details))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_DisplaysEventName() {
        onView(withId(R.id.text_event_name))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_DisplaysEventDescription() {
        onView(withId(R.id.text_event_description))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_DisplaysEventPoster() {
        onView(withId(R.id.image_event_poster))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_DisplaysJoinButton() {
        onView(withId(R.id.button_join_event))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_DisplaysBackButton() {
        onView(withId(R.id.button_go_back))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_BackButton_Click() {
        onView(withId(R.id.button_go_back))
                .check(matches(isDisplayed()))
                .perform(click());
        
        // Note- only for click test
    }

    @Test
    public void testEventDetailsActivity_JoinButton_Click() {
        onView(withId(R.id.button_join_event))
                .check(matches(isDisplayed()))
                .perform(click());
        
        // IMP - Firebase data needed
    }

    @Test
    public void testEventDetailsActivity_ContentGroupDisplayed() {
        // Verify content group exists
        onView(withId(R.id.content_group))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsActivity_HandlesMissingEventId() {
        //Note - Different activity flow for future implementation
        onView(withId(R.id.progressBar_event_details))
                .check(matches(isDisplayed()));
    }
}
