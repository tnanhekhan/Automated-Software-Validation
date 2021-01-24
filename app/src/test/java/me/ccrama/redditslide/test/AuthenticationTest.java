package me.ccrama.redditslide.test;

import android.content.Context;
import android.content.SharedPreferences;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.oauth.OAuthHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import me.ccrama.redditslide.Authentication;
import me.ccrama.redditslide.BuildConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Authentication.class})
public class AuthenticationTest {

    @Test
    public void doVerify() {
        //arrange
        Context mockedContext = mock(Context.class);
        RedditClient mockedRedditClient = mock(RedditClient.class);
        SharedPreferences mockedSharedPreferences = mock(SharedPreferences.class);
        OAuthHelper oAuthHelper = mock(OAuthHelper.class);
        SharedPreferences.Editor mockedEditor = mock(SharedPreferences.Editor.class);
        String lastToken = "token";

        Whitebox.setInternalState(BuildConfig.class, "DEBUG", false);
        Whitebox.setInternalState(Authentication.class, "authentication", mockedSharedPreferences);

        // mock and stub
        when(mockedSharedPreferences.getString(any(), any())).thenReturn(lastToken);
        when(mockedRedditClient.getOAuthHelper()).thenReturn(oAuthHelper);
        when(mockedSharedPreferences.edit()).thenReturn(mockedEditor);
        doNothing().when(mockedEditor).apply();

        //act
        Authentication.doVerify(lastToken, mockedRedditClient, true, mockedContext);

        //assert/verify
        verify(mockedRedditClient).authenticate(any());
    }
}
