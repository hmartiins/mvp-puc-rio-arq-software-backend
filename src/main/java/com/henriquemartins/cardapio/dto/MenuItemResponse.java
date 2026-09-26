package com.henriquemartins.cardapio.dto;

import com.henriquemartins.cardapio.model.DiaDaSemana;
import com.henriquemartins.cardapio.model.MealType;
import com.henriquemartins.cardapio.model.MenuItem;
import java.time.LocalDate;

public record MenuItemResponse(
        Long id,
        DiaDaSemana dayOfWeek,
        MealType mealType,
        String mealId,
        String mealName,
        String thumbnailUrl,
        Integer servings,
        LocalDate weekRef
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getDayOfWeek(),
                item.getMealType(),
                item.getMealId(),
                item.getMealName(),
                item.getThumbnailUrl(),
                item.getServings(),
                item.getWeekRef()
        );
    }
}
