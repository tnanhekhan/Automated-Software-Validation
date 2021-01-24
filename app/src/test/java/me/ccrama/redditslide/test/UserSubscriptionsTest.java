package me.ccrama.redditslide.test;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;

import me.ccrama.redditslide.CaseInsensitiveArrayList;
import me.ccrama.redditslide.UserSubscriptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by Alex Macleod on 28/03/2016.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserSubscriptions.class})
public class UserSubscriptionsTest {
    private final CaseInsensitiveArrayList subreddits = new CaseInsensitiveArrayList(Arrays.asList(
            "xyy", "xyz", "frontpage", "mod", "friends", "random", "aaa", "pinned", "pinned2"
    ));

    @BeforeClass
    public static void setUp() {
        UserSubscriptions.pinned = new TestUtils.MockPreferences("pinned,pinned2");
    }

    @Test
    public void sortsSubreddits() {
        assertThat(UserSubscriptions.sort(subreddits), is(new ArrayList<>(Arrays.asList(
                "pinned", "pinned2", "frontpage", "all", "random", "friends", "mod", "aaa", "xyy", "xyz"
        ))));
    }

    @Test
    public void sortsSubredditsNoExtras() {
        assertThat(UserSubscriptions.sortNoExtras(subreddits), is(new ArrayList<>(Arrays.asList(
                "pinned", "pinned2", "frontpage", "random", "friends", "mod", "aaa", "xyy", "xyz"
        ))));
    }

    @Test
    public void getAllSubreddits() {
        // arrange
        Context mockedContext = mock(Context.class);
        SharedPreferences mockedSharedPreferences = mock(SharedPreferences.class);

        UserSubscriptions.pinned = new TestUtils.MockPreferences("pinned,pinned2");

        PowerMockito.mockStatic(UserSubscriptions.class);

        when(mockedContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockedSharedPreferences);
        BDDMockito.given(UserSubscriptions.getHistory()).willReturn(subreddits);
        BDDMockito.given(UserSubscriptions.getDefaults(mockedContext)).willReturn(subreddits);
        BDDMockito.given(UserSubscriptions.getSubscriptions(mockedContext)).willReturn(subreddits);
        BDDMockito.given(UserSubscriptions.getAllSubreddits(mockedContext)).willCallRealMethod();

        // act
        CaseInsensitiveArrayList result = UserSubscriptions.getAllSubreddits(mockedContext);

        // assert
        // CORRECT (E | Existence) - We check if "result" returns something that can be empty/not empty (an array)
        // CORRECT (C | Conformance) - We check if the array that is returned contains atleast 1 element
        assert (!result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void getAllSubredditsWithMissingDependencies() {
        // arrange
        Context mockedContext = mock(Context.class);
        SharedPreferences mockedSharedPreferences = mock(SharedPreferences.class);

        UserSubscriptions.pinned = new TestUtils.MockPreferences("pinned,pinned2");

        PowerMockito.mockStatic(UserSubscriptions.class);

        when(mockedContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockedSharedPreferences);

        BDDMockito.given(UserSubscriptions.getAllSubreddits(mockedContext)).willCallRealMethod();

        // act
        CaseInsensitiveArrayList result = UserSubscriptions.getAllSubreddits(mockedContext);

        // assert
        // CORRECT (E | Existence) - We check if "result" returns something that can be empty/not empty (an array)
        // CORRECT (C | Conformance) - We check if the array that is returned contains atleast 1 element
        assert (!result.isEmpty());
    }
}
