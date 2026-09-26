package com.henriquemartins.cardapio.dto;

/** Resultado de busca ja mapeado para o contrato proprio da API (nunca o JSON cru da TheMealDB). */
public record MealSearchResponse(
        String mealId,
        String mealName,
        String thumbnailUrl,
        String category,
        String area
) {
}
