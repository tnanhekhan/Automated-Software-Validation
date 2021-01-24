package me.ccrama.redditslide.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import me.ccrama.redditslide.Activities.MainActivity;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { MainActivity.class })
public class MainActivityTest {
    @Test
    public void doStuff() {
        boolean testedStatement = true;

        testedStatement = false;

        assertEquals(Mockito.anyBoolean(), testedStatement);
    }
}
