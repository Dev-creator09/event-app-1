package com.example.event_app.admin;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminBrowseEventsActivity;

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
import static org.hamcrest.Matchers.allOf;

/**
 * Instrumented tests for AdminHomeActivity
 * Tests all button clicks and navigation
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminHomeActivityTest {

    @Rule
    public ActivityScenarioRule<AdminHomeActivity> activityRule =
            new ActivityScenarioRule<>(AdminHomeActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testAdminHomeActivity_DisplaysCorrectTitle() {
        // dashboard verification
        onView(withId(R.id.tvEventsCount))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAdminHomeActivity_StatisticsAreDisplayed() {
        // on view stats display testing
        onView(withId(R.id.tvEventsCount))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.tvUsersCount))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.tvOrganizersCount))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.tvActiveCount))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAdminHomeActivity_BrowseEventsButton_Click() {
        // clicker test for browse events
        onView(withId(R.id.btnBrowseEvents))
                .check(matches(isDisplayed()))
                .perform(click());
        
        //AdminBrowseEventsActivity verification
        intended(hasComponent(AdminBrowseEventsActivity.class.getName()));
    }

    @Test
    public void testAdminHomeActivity_BrowseUsersButton_Click() {
        // browse user clicker test
        onView(withId(R.id.btnBrowseUsers))
                .check(matches(isDisplayed()))
                .perform(click());
        
        // intent ver
        intended(hasComponent("com.example.event_app.activities.admin.AdminBrowseUsersActivity"));
    }

    @Test
    public void testAdminHomeActivity_BrowseImagesButton_Click() {
        onView(withId(R.id.btnBrowseImages))
                .check(matches(isDisplayed()))
                .perform(click());
        intended(hasComponent("com.example.event_app.activities.admin.AdminBrowseImagesActivity"));
    }

    @Test
    public void testAdminHomeActivity_GenerateReportsButton_Click() {
        onView(withId(R.id.btnGenerateReports))
                .check(matches(isDisplayed()))
                .perform(click());
        //IMP - Firebase verification required
        onView(withId(R.id.btnGenerateReports))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAdminHomeActivity_FlaggedItemsButton_Click() {
        // flagged items
        onView(withId(R.id.btnFlaggedItems))
                .check(matches(isDisplayed()))
                .perform(click());

        intended(allOf(
                hasComponent(AdminBrowseEventsActivity.class.getName()),
                IntentMatchers.hasExtra("showFlaggedOnly", true)
        ));
    }

    @Test
    public void testAdminHomeActivity_AllButtonsAreDisplayed() {
        // Verify if all action buttons are displayed
        onView(withId(R.id.btnBrowseEvents))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.btnBrowseUsers))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.btnBrowseImages))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.btnGenerateReports))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.btnFlaggedItems))
                .check(matches(isDisplayed()));
    }
}
