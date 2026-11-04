package com.society.sunlitindustry;

/** Fractional mB are prepaid, not discarded or borrowed from an empty tank. */
public final class FuelMeter {
    public record Draw(boolean allowed, int millibuckets, double credit) {}
    private FuelMeter() {}

    public static double validCredit(double value) {
        return Double.isFinite(value) && value >= 0 && value < 1 ? value : 0;
    }

    public static Draw quote(double storedCredit, double demand, int available) {
        double credit = validCredit(storedCredit);
        if (!Double.isFinite(demand) || demand <= 0 || demand > Integer.MAX_VALUE || available < 0)
            return new Draw(false, 0, credit);
        double needed = Math.max(0, demand - credit);
        long draw = (long) Math.ceil(needed);
        if (draw > available) return new Draw(false, 0, credit);
        double left = Math.max(0, Math.min(Math.nextDown(1D), credit + draw - demand));
        return new Draw(true, (int) draw, left);
    }

    public static double demand(int thrusters, int destinationTier, int duration) {
        if (duration <= 0 || thrusters <= 0) return Double.NaN;
        return (thrusters + (double) Math.max(1, destinationTier)) * 2D / (duration / 20D + 1D);
    }
}
