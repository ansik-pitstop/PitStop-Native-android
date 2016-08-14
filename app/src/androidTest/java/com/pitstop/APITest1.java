package com.pitstop;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.espresso.contrib.RecyclerViewActions;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class APITest1 {

    @Rule
    public ActivityTestRule<SplashScreen> mActivityTestRule = new ActivityTestRule<>(SplashScreen.class);

    @Test
    public void aPITest1() {
        ViewInteraction appCompatButton = onView(
                allOf(withId(com.pitstop.R.id.log_in_skip), withText("LOG IN"),
                        withParent(allOf(withId(com.pitstop.R.id.splash_layout),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatButton.perform(click());
//
        ViewInteraction appCompatEditText = onView(
                allOf(withId(com.pitstop.R.id.email), isDisplayed()));
        appCompatEditText.perform(click());
//
        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(com.pitstop.R.id.email), isDisplayed()));
        appCompatEditText2.perform(replaceText("ben1@a.ca"));
//
        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(com.pitstop.R.id.password), isDisplayed()));
        appCompatEditText3.perform(click());
//
        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(com.pitstop.R.id.password), isDisplayed()));
        appCompatEditText4.perform(replaceText("aaaaaa"));
//
        pressBack();
//
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(com.pitstop.R.id.forgot_password), withText("Forgot password?"), isDisplayed()));
        appCompatButton2.perform(click());
//
        ViewInteraction editText = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText.perform(click());
//
        ViewInteraction editText2 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText2.perform(replaceText("bwu96412@gmail.com"));
//
        ViewInteraction appCompatButton3 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
                                withParent(withClassName(is("android.widget.LinearLayout"))))),
                        isDisplayed()));
        appCompatButton3.perform(click());
//
        ViewInteraction appCompatButton4 = onView(
                allOf(withId(R.id.sign_log_switcher_button), withText("Sign Up"), isDisplayed()));
        appCompatButton4.perform(click());
//
        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(com.pitstop.R.id.firstName), isDisplayed()));
        appCompatEditText5.perform(replaceText("ben ?"));
//
        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(com.pitstop.R.id.lastName), isDisplayed()));
        appCompatEditText6.perform(replaceText("butt"));
//
        ViewInteraction appCompatEditText7 = onView(
                allOf(withId(com.pitstop.R.id.phone), isDisplayed()));
        appCompatEditText7.perform(replaceText("123"));
//
        ViewInteraction appCompatEditText8 = onView(
                allOf(withId(com.pitstop.R.id.phone), withText("123"), isDisplayed()));
        appCompatEditText8.perform(replaceText("123654"));
//
        ViewInteraction appCompatButton5 = onView(
                allOf(withId(R.id.sign_log_switcher_button), withText("Sign Up"), isDisplayed()));
        appCompatButton5.perform(click());
//
        ViewInteraction appCompatEditText9 = onView(
                allOf(withId(com.pitstop.R.id.lastName), withText("butt"), isDisplayed()));
        appCompatEditText9.perform(replaceText(""));
//
        pressBack();
//
        ViewInteraction appCompatButton6 = onView(
                allOf(withId(R.id.sign_log_switcher_button), withText("Sign Up"), isDisplayed()));
        appCompatButton6.perform(click());
//
        ViewInteraction appCompatEditText10 = onView(
                allOf(withId(com.pitstop.R.id.lastName), isDisplayed()));
        appCompatEditText10.perform(click());
//
        ViewInteraction appCompatEditText11 = onView(
                allOf(withId(com.pitstop.R.id.lastName), isDisplayed()));
        appCompatEditText11.perform(replaceText("adss"));
//
        ViewInteraction appCompatEditText12 = onView(
                allOf(withId(com.pitstop.R.id.phone), withText("123654"), isDisplayed()));
        appCompatEditText12.perform(replaceText("1236542365"));
//
        pressBack();
//
        ViewInteraction appCompatButton7 = onView(
                allOf(withId(com.pitstop.R.id.sign_log_switcher_button), withText("Sign Up"), isDisplayed()));
        appCompatButton7.perform(click());
//
        pressBack();
//
        ViewInteraction appCompatButton8 = onView(
                allOf(withId(com.pitstop.R.id.login_btn), withText("Log In"), isDisplayed()));
        appCompatButton8.perform(click());

        //ViewInteraction overflowMenuButton = onView(
        //        allOf(withContentDescription("More options"), isDisplayed()));
        //overflowMenuButton.perform(click());
//
        //ViewInteraction appCompatTextView = onView(
        //        allOf(withId(com.pitstop.R.id.title), withText("Settings"), isDisplayed()));
        //appCompatTextView.perform(click());
//
        //ViewInteraction linearLayout = onView(
        //        allOf(withClassName(is("android.widget.LinearLayout")),
        //                withParent(withId(android.R.id.list)),
        //                isDisplayed()));
//
        //DataInteraction linear = onData(allOf(hasToString("Porche 911"), ))
//
        //linearLayout.perform(click());
//
        //ViewInteraction appCompatCheckedTextView = onView(
        //        allOf(withId(android.R.id.text1), withText("GB Autos"),
        //                withParent(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
        //                        withParent(withClassName(is("android.widget.FrameLayout"))))),
        //                isDisplayed()));
        //appCompatCheckedTextView.perform(click());

        //ViewInteraction linearLayout2 = onView(
        //        allOf(withClassName(is("android.widget.LinearLayout")),
        //                withParent(withId(android.R.id.list)),
        //                isDisplayed()));
        //linearLayout2.perform(click());

        //ViewInteraction appCompatCheckedTextView2 = onView(
        //        allOf(withId(android.R.id.text1), withText("Kia Forte"),
        //                withParent(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
        //                        withParent(withClassName(is("android.widget.FrameLayout"))))),
        //                isDisplayed()));
        //appCompatCheckedTextView2.perform(click());

        //ViewInteraction appCompatButton9 = onView(
        //        allOf(withId(android.R.id.button1), withText("OK"),
        //                withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
        //                        withParent(withClassName(is("android.widget.LinearLayout"))))),
        //                isDisplayed()));
        //appCompatButton9.perform(click());

        //pressBack();

        ViewInteraction overflowMenuButton2 = onView(
                allOf(withContentDescription("More options"), isDisplayed()));
        overflowMenuButton2.perform(click());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(com.pitstop.R.id.title), withText("Settings"), isDisplayed()));
        appCompatTextView2.perform(click());

        //ViewInteraction linearLayout3 = onView(
        //        allOf(withClassName(is("android.widget.LinearLayout")),
        //                withParent(withId(android.R.id.list)),
        //                isDisplayed()));
        //linearLayout3.perform(click());

        //ViewInteraction appCompatCheckedTextView3 = onView(
        //        allOf(withId(android.R.id.text1), withText("GB Autos"),
        //                withParent(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
        //                        withParent(withClassName(is("android.widget.FrameLayout"))))),
        //                isDisplayed()));
        //appCompatCheckedTextView3.perform(click());

        pressBack();

        ViewInteraction appCompatButton10 = onView(
                allOf(withId(com.pitstop.R.id.request_service_btn), withText("Request Service"),
                        withParent(withId(com.pitstop.R.id.main_view)),
                        isDisplayed()));
        appCompatButton10.perform(click());

        ViewInteraction editText3 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText3.perform(click());

        ViewInteraction editText4 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText4.perform(replaceText("woooooooo ???"));

        ViewInteraction appCompatButton11 = onView(
                allOf(withId(android.R.id.button1), withText("SEND"),
                        withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
                                withParent(withClassName(is("android.widget.LinearLayout"))))),
                        isDisplayed()));
        appCompatButton11.perform(click());

        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.add_car_root), withContentDescription("Add car"), isDisplayed()));
        actionMenuItemView.perform(click());

        //ViewInteraction cardView = onView(
        //        allOf(withId(com.pitstop.R.id.dealership_row_item),
        //                withParent(allOf(withId(com.pitstop.R.id.dealership_row_layout),
        //                        withParent(withId(com.pitstop.R.id.dealership_list)))),
        //                isDisplayed()));
        //onData(is(instanceOf(Dealership.class)))
        //        .perform(click());
        //DataInteraction cardView = onData(anything()).inAdapterView(withId(R.id.dealership_list)).atPosition(0);

        onView(withId(R.id.dealership_list)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        //cardView.perform(click());

        ViewInteraction toggleButton = onView(
                allOf(withId(com.pitstop.R.id.no_i_dont_button), withText("NO"), isDisplayed()));
        toggleButton.perform(click());

        ViewInteraction appCompatEditText13 = onView(
                allOf(withId(com.pitstop.R.id.mileage),
                        withParent(withId(com.pitstop.R.id.linearLayout)),
                        isDisplayed()));
        appCompatEditText13.perform(click());

        ViewInteraction appCompatEditText14 = onView(
                allOf(withId(com.pitstop.R.id.mileage),
                        withParent(withId(com.pitstop.R.id.linearLayout)),
                        isDisplayed()));
        appCompatEditText14.perform(replaceText("1888"));

        ViewInteraction appCompatEditText15 = onView(
                allOf(withId(com.pitstop.R.id.VIN), isDisplayed()));
        appCompatEditText15.perform(replaceText("2hgfb2f76dh033278"));

        ViewInteraction appCompatButton12 = onView(
                allOf(withId(com.pitstop.R.id.button), withText("ADD CAR"), isDisplayed()));
        appCompatButton12.perform(click());

        ViewInteraction appCompatButton13 = onView(
                allOf(withId(com.pitstop.R.id.car_scan_btn), withText("Scan"),
                        withParent(allOf(withId(com.pitstop.R.id.car_details_components_layout),
                                withParent(withId(R.id.car_info_layout)))),
                        isDisplayed()));
        appCompatButton13.perform(click());

        ViewInteraction appCompatButton14 = onView(
                allOf(withId(com.pitstop.R.id.update_mileage), withText("Update"), isDisplayed()));
        appCompatButton14.perform(click());

        ViewInteraction editText5 = onView(
                allOf(withText("1888"),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText5.perform(click());

        ViewInteraction editText6 = onView(
                allOf(withText("1888"),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText6.perform(replaceText("3000"));

        ViewInteraction appCompatButton15 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
                                withParent(withClassName(is("android.widget.LinearLayout"))))),
                        isDisplayed()));
        appCompatButton15.perform(click());

        pressBack();

        ViewInteraction appCompatButton16 = onView(
                allOf(withId(com.pitstop.R.id.request_service_btn), withText("Request Service"),
                        withParent(withId(com.pitstop.R.id.main_view)),
                        isDisplayed()));
        appCompatButton16.perform(click());

        ViewInteraction editText7 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText7.perform(click());

        ViewInteraction editText8 = onView(
                allOf(withClassName(is("android.widget.EditText")),
                        withParent(allOf(withId(android.R.id.custom),
                                withParent(withClassName(is("android.widget.FrameLayout"))))),
                        isDisplayed()));
        editText8.perform(replaceText("car"));

        ViewInteraction appCompatButton17 = onView(
                allOf(withId(android.R.id.button1), withText("SEND"),
                        withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
                                withParent(withClassName(is("android.widget.LinearLayout"))))),
                        isDisplayed()));
        appCompatButton17.perform(click());

        ViewInteraction overflowMenuButton3 = onView(
                allOf(withContentDescription("More options"), isDisplayed()));
        overflowMenuButton3.perform(click());

        ViewInteraction appCompatTextView3 = onView(
                allOf(withId(com.pitstop.R.id.title), withText("History"), isDisplayed()));
        appCompatTextView3.perform(click());

        pressBack();

        //ViewInteraction appCompatButton18 = onView(
        //        allOf(withId(android.R.id.button1), withText("OK"),
        //                withParent(allOf(withClassName(is("com.android.internal.widget.ButtonBarLayout")),
        //                        withParent(withClassName(is("android.widget.LinearLayout"))))),
        //                isDisplayed()));
        //appCompatButton18.perform(click());

        ViewInteraction overflowMenuButton4 = onView(
                allOf(withContentDescription("More options"), isDisplayed()));
        overflowMenuButton4.perform(click());

        ViewInteraction appCompatTextView4 = onView(
                allOf(withId(com.pitstop.R.id.title), withText("History"), isDisplayed()));
        appCompatTextView4.perform(click());

        pressBack();

        ViewInteraction overflowMenuButton5 = onView(
                allOf(withContentDescription("More options"), isDisplayed()));
        overflowMenuButton5.perform(click());

        ViewInteraction appCompatTextView5 = onView(
                allOf(withId(com.pitstop.R.id.title), withText("Settings"), isDisplayed()));
        appCompatTextView5.perform(click());

    }
}
