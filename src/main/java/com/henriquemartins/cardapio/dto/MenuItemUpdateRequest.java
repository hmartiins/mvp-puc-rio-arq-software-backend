package com.henriquemartins.cardapio.dto;

import com.henriquemartins.cardapio.model.DiaDaSemana;
import com.henriquemartins.cardapio.model.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Atualiza dia / refeicao / porcoes de um item ja existente. */
public record MenuItemUpdateRequest(
        @NotNull(message = "dayOfWeek e obrigatorio")
        DiaDaSemana dayOfWeek,

        @NotNull(message = "mealType e obrigatorio")
        MealType mealType,

        @NotNull(message = "servings e obrigatorio")
        @Min(value = 1, message = "servings deve ser no minimo 1")
        @Max(value = 50, message = "servings deve ser no maximo 50")
        Integer servings
) {
}
