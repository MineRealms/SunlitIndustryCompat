package com.society.sunlitindustry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelMeterTest {
    @Test void fractionalUseIsPrepaidAndAccumulates() {
        double credit = 0; int total = 0;
        for (int i = 0; i < 300; i++) {
            var q = FuelMeter.quote(credit, 2D / 3D, 1000);
            assertTrue(q.allowed());
            credit = q.credit(); total += q.millibuckets();
        }
        assertTrue(total >= 200 && total <= 201);
        assertEquals(200D, total - credit, 1e-8);
    }
    @Test void emptyTankDoesNotFinanceFirstTick() {
        assertFalse(FuelMeter.quote(0, 0.1, 0).allowed());
    }
    @Test void alreadyPaidFractionCanBeUsed() {
        var q = FuelMeter.quote(0.8, 0.2, 0);
        assertTrue(q.allowed()); assertEquals(0, q.millibuckets()); assertEquals(0.6, q.credit(), 1e-12);
    }
    @Test void shortageDoesNotSpendOrIncreaseCredit() {
        var q = FuelMeter.quote(0.2, 4.4, 4);
        assertFalse(q.allowed()); assertEquals(0, q.millibuckets()); assertEquals(0.2, q.credit());
    }
    @Test void corruptStateCannotCreateCredit() {
        assertEquals(0, FuelMeter.validCredit(Double.NaN));
        assertEquals(0, FuelMeter.validCredit(Double.POSITIVE_INFINITY));
        assertEquals(0, FuelMeter.validCredit(-1));
        assertEquals(0, FuelMeter.validCredit(10));
        assertFalse(FuelMeter.quote(0, Double.NaN, 10).allowed());
        assertFalse(FuelMeter.quote(0, 0, 10).allowed());
    }
    @Test void preserveOriginalContinuousFormula() {
        assertEquals(2D/3D, FuelMeter.demand(1, 1, 100), 1e-12);
        assertEquals(4D/2.25D, FuelMeter.demand(1, 1, 25), 1e-12);
        assertTrue(Double.isNaN(FuelMeter.demand(0,1,100)));
    }
}
