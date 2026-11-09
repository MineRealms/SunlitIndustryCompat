package com.society.sunlitindustry;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class AliTooltipCacheTest {
    public static final class Registry {
        private final Object palette;

        Registry(Object palette) { this.palette = palette; }
        public Object getTooltipCache() { return palette; }
    }

    @Test
    void keepsServerPaletteWhenBothRegistriesExist() {
        Object serverPalette = new Object();
        assertSame(serverPalette, AliTooltipCache.palette(new Registry(serverPalette), new Registry(new Object())));
    }

    @Test
    void usesClientPaletteWhenRemoteServerRegistryIsAbsent() {
        Object clientPalette = new Object();
        assertSame(clientPalette, AliTooltipCache.palette(null, new Registry(clientPalette)));
    }
}
