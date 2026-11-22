package com.eams.dto.response;

import java.math.BigDecimal;

/** One bar/slice in a chart: {"label": "Laptop", "value": 12}. */
public record ChartPoint(String label, BigDecimal value) {

    public static ChartPoint of(Object label, Object value) {
        BigDecimal v = value instanceof BigDecimal b ? b
                : value instanceof Number n ? new BigDecimal(n.toString())
                : BigDecimal.ZERO;
        return new ChartPoint(label == null ? "Unknown" : label.toString(), v);
    }
}
