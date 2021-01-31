package me.ccrama.redditslide.test;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import me.ccrama.redditslide.Activities.MainActivity;
import me.ccrama.redditslide.CaseInsensitiveArrayList;
import me.ccrama.redditslide.LastComments;
import me.ccrama.redditslide.UserSubscriptions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { MainActivity.class })
public class MainActivityTest {
    @Test
    public void doStuff() {
        boolean testedStatement = true;

        testedStatement = false;

        assertEquals(Mockito.anyBoolean(), testedStatement);
    }

    @Test
    public void loadSubmission() {
        // arrange
        Context mockedContext = mock(Context.class);
//        SharedPreferences mockedSharedPreferences = mock(SharedPreferences.class);
        LastComments mockedLastComments = mock(LastComments.class);

        UserSubscriptions.pinned = new TestUtils.MockPreferences("pinned,pinned2");

        PowerMockito.mockStatic(UserSubscriptions.class);

//        when(mockedContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockedSharedPreferences);

        BDDMockito.given(UserSubscriptions.getAllSubreddits(mockedContext)).willCallRealMethod();

        // act
        CaseInsensitiveArrayList result = UserSubscriptions.getAllSubreddits(mockedContext);

        // assert
        // CORRECT (E | Existence) - We check if "result" returns something that can be empty/not empty (an array)
        // CORRECT (C | Conformance) - We check if the array that is returned contains atleast 1 element
        assert (!result.isEmpty());
    }
}
