package com.henriquemartins.cardapio.dto;

import com.henriquemartins.cardapio.model.DiaDaSemana;
import com.henriquemartins.cardapio.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MenuItemRequest(
        @NotBlank(message = "mealId e obrigatorio")
        String mealId,

        @NotNull(message = "dayOfWeek e obrigatorio")
        DiaDaSemana dayOfWeek,

        @NotNull(message = "mealType e obrigatorio")
        MealType mealType,

        @NotNull(message = "servings e obrigatorio")
        @Min(value = 1, message = "servings deve ser no minimo 1")
        @Max(value = 50, message = "servings deve ser no maximo 50")
        Integer servings,

        @NotNull(message = "weekRef e obrigatorio")
        LocalDate weekRef
) {
}
