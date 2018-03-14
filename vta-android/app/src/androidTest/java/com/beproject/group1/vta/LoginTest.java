package com.beproject.group1.vta;

import com.beproject.group1.vta.activities.LoginActivity;
import com.beproject.group1.vta.activities.MapsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.TouchUtils;
import android.widget.Button;
import android.widget.ScrollView;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
/**
 * Created by Dell on 13-03-2018.
 */
@RunWith(AndroidJUnit4.class)
public class LoginTest {

    @Rule
    public ActivityTestRule<LoginActivity> activityTestRule = new ActivityTestRule<LoginActivity>(LoginActivity.class);

    private LoginActivity loginActivity;
    private String email;
    private String password;
    private Button signinButton;
    Instrumentation.ActivityMonitor activityMonitor = getInstrumentation().addMonitor(MapsActivity.class.getName(), null, false);


    @Before
    public void setUp(){
        loginActivity = activityTestRule.getActivity();
    }

    @Test
    public void testLaunch(){
        ScrollView scrollView = loginActivity.findViewById(R.id.login_form);
        assertNotNull(scrollView);
    }

    @Test
    public void testInputNotEmpty(){
        email = (loginActivity.findViewById(R.id.email)).toString();
        password = (loginActivity.findViewById(R.id.password)).toString();
        assertNotNull("Email is null", email);
        assertNotNull("Password is null", password);
    }

    @Test
    public void testButtonClick(){
       signinButton = loginActivity.findViewById(R.id.email_sign_in_button);
        Instrumentation.ActivityMonitor activityMonitor = getInstrumentation()
                .addMonitor(MapsActivity.class.getName(), null, false);
        onView(withId(R.id.email_sign_in_button)).perform(click());
        MapsActivity targetActivity = (MapsActivity) activityMonitor.waitForActivity();
        assertNotNull("Target Activity is not launched", targetActivity);

    }

    @Test
    public void testValidEmail(){
        Boolean expected = true;
        Boolean actual = loginActivity.isEmailValid("abc@gmail.com");
        assertEquals(expected, actual);
    }

    @Test
    public void testValidPassword(){
        Boolean expected = true;
        Boolean actual = loginActivity.isPasswordValid("abc123");
        assertEquals(expected, actual);
    }

}
