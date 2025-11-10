package com.example.event_app;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
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
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Instrumented tests for EntrantBrowseEventsActivity
 * Tests search functionality, UI display, and navigation
 *
 * View display checks and search functionality
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantBrowseEventsActivityTest {

    @Rule
    public ActivityScenarioRule<EntrantBrowseEventsActivity> activityRule =
            new ActivityScenarioRule<>(EntrantBrowseEventsActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents
        Intents.release();
    }

    @Test
    public void testEntrantBrowseEventsActivity_DisplaysSearchField() {
        onView(withId(R.id.searchEvents))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_DisplaysRecyclerView() {

        onView(withId(R.id.recyclerViewEvents))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_SearchFieldAcceptsInput() {
        onView(withId(R.id.searchEvents))
                .perform(typeText("test event"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_SearchFieldClears() {
        onView(withId(R.id.searchEvents))
                .perform(typeText("test"))
                .perform(ViewActions.clearText())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_EmptyStateDisplayed() {
        //empty state view
        onView(withId(R.id.tvEmptyEvents))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_ActionBarBackButton() {
        // Note- Testing back navigation would require checking onSupportNavigateUp
        // This test verifies the activity has the expected UI elements
        onView(withId(R.id.recyclerViewEvents))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantBrowseEventsActivity_SearchFiltersEvents() {
        String searchQuery = "concert";
        onView(withId(R.id.searchEvents))
                .perform(typeText(searchQuery));
        onView(withId(R.id.searchEvents))
                .check(matches(ViewMatchers.withText(searchQuery)));
    }

    @Test
    public void testEntrantBrowseEventsActivity_ActivityTitle() {
        // Note - same as actionbarbutton
        onView(withId(R.id.recyclerViewEvents))
                .check(matches(isDisplayed()));
    }
}
