package com.henriquemartins.cardapio.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * A semana e sempre identificada pela sua segunda-feira. Qualquer data recebida do
 * cliente e normalizada aqui, para que "2026-09-03" e "2026-09-01" caiam na mesma semana.
 */
public final class WeekRef {

    private WeekRef() {
    }

    public static LocalDate normalize(LocalDate date) {
        return date == null
                ? null
                : date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate currentWeek() {
        return normalize(LocalDate.now());
    }
}
