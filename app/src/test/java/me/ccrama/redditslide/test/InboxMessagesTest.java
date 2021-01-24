package me.ccrama.redditslide.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import me.ccrama.redditslide.Adapters.InboxAdapter;
import me.ccrama.redditslide.Adapters.InboxMessages;
import me.ccrama.redditslide.Authentication;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest({InboxMessages.class})
public class InboxMessagesTest {
    @Test
    public void loadMessages() {
        //arrange
        String where = "id";
        InboxMessages inboxMessages = new InboxMessages(where);
        InboxAdapter mockedInboxAdapter = mock(InboxAdapter.class);

        // mock and stub

        //act
        inboxMessages.loadMore(mockedInboxAdapter, where, true);

        //assert/verify
        verify(mockedInboxAdapter).notifyDataSetChanged();
    }
}
