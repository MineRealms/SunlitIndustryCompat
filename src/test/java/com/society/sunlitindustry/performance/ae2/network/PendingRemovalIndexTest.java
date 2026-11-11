package com.society.sunlitindustry.performance.ae2.network;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PendingRemovalIndexTest {
    @Test
    void usesIdentityAndClearsOnlyWhenQueueIsFlushed() {
        String one = new String("same"), two = new String("same");
        var index = new PendingRemovalIndex<String>();
        assertFalse(index.contains(one));
        index.add(one);
        index.add(one);
        assertTrue(index.contains(one));
        assertFalse(index.contains(two));
        index.clear();
        assertFalse(index.contains(one));
        index.add(two);
        assertTrue(index.contains(two));
    }
}
