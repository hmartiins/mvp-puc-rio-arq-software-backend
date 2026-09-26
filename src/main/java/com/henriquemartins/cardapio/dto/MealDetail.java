package com.henriquemartins.cardapio.dto;

import java.util.List;

/** Detalhe completo de uma receita, ja mapeado a partir do JSON da TheMealDB. */
public record MealDetail(
        String mealId,
        String mealName,
        String thumbnailUrl,
        String category,
        String area,
        String instructions,
        List<MealIngredient> ingredients
) {
}
