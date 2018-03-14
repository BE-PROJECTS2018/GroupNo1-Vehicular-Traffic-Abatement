package com.beproject.group1.vta;

import com.beproject.group1.vta.activities.LoginActivity;
import com.beproject.group1.vta.activities.SignUpActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Button;
import android.widget.ScrollView;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Dell on 13-03-2018.
 */
@RunWith(AndroidJUnit4.class)
public class SignupTest {

    @Rule
    public ActivityTestRule<SignUpActivity> activityTestRule = new ActivityTestRule<SignUpActivity>(SignUpActivity.class);

    private SignUpActivity signUpActivity;
    private String email;
    private String password;
    private String confirmPassword;
    private Button signUpButton;

    @Before
    public void setUp() throws Exception{
        signUpActivity = activityTestRule.getActivity();
        email = (signUpActivity.findViewById(R.id.email)).toString();
        password = (signUpActivity.findViewById(R.id.password)).toString();
        confirmPassword = (signUpActivity.findViewById(R.id.confirm_password)).toString();
    }

    @Test
    public void testLaunch(){
        ScrollView scrollView = signUpActivity.findViewById(R.id.login_form);
        assertNotNull(scrollView);
    }

    @Test
    public void testInputNotEmpty(){
        assertNotNull("Email is null", email);
        assertNotNull("Password is null", password);
        assertNotNull("Confirm Password is null", confirmPassword);
    }

    @Test
    public void testValidEmail(){
        Boolean expected = true;
        Boolean actual = signUpActivity.isEmailValid("abc@gmail.com");
        assertEquals(expected, actual);
    }

    @Test
    public void testValidPassword(){
        Boolean expected = true;
        Boolean actual = signUpActivity.isPasswordValid("abc123");
        assertEquals(expected, actual);
    }

    @Test
    public void testButtonClick(){
        onView(withId(R.id.email_sign_up_button)).perform(click());
    }

    @After
    public void tearDown() throws Exception{
        signUpActivity = null;
    }
}
