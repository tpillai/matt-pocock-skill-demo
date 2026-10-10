package com.example.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FeeCalculator {
    static final BigDecimal DAILY_LATE_FEE = new BigDecimal("0.25");

    public BigDecimal lateFee(LocalDate dueDate, LocalDate returnDate) {
        if (returnDate.isBefore(dueDate)) {
            return BigDecimal.ZERO;
        }
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate) + 1;
        return DAILY_LATE_FEE.multiply(BigDecimal.valueOf(daysLate));
    }
}
