package com.society.sunlitindustry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Selects ALI's server palette when present, or its client palette on remote connections. */
public final class AliTooltipCache {
    private static final ClassValue<Method> TOOLTIP_CACHE = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                return type.getMethod("getTooltipCache");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unsupported ALI registry: missing getTooltipCache", e);
            }
        }
    };

    private AliTooltipCache() {}

    public static Object palette(Object serverRegistry, Object clientRegistry) {
        Object registry = serverRegistry != null ? serverRegistry : clientRegistry;
        if (registry == null) throw new IllegalStateException("ALI client and server registries are both absent");
        try {
            return TOOLTIP_CACHE.get(registry.getClass()).invoke(registry);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to read ALI tooltip cache", e);
        }
    }
}
