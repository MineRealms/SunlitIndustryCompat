package com.society.sunlitindustry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class ServerPerformanceStats {
    public enum Event { EMPTY_IO, SIMULATION, AND_CONDITIONS, GROUP_HIT, GROUP_BUILD, GROUP_FALLBACK }
    private static final LongAdder[] COUNTERS = new LongAdder[Event.values().length];
    private static volatile boolean recording;

    static {
        for (int i = 0; i < COUNTERS.length; i++) COUNTERS[i] = new LongAdder();
    }

    public static void record(Event event) {
        if (recording) COUNTERS[event.ordinal()].increment();
    }

    public static void setRecording(boolean enabled) { recording = enabled; }

    public static void reset() {
        for (var counter : COUNTERS) counter.reset();
    }

    public static Map<String, Long> snapshot() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (var event : Event.values()) result.put(event.name(), COUNTERS[event.ordinal()].sum());
        return result;
    }

    private ServerPerformanceStats() {}
}
