package com.henriquemartins.cardapio.dto;

/**
 * Par ingrediente/medida como vem da receita (ex.: "penne rigate" / "1 pound").
 * A medida e mantida como texto porque a TheMealDB nao a estrutura.
 */
public record MealIngredient(String name, String measure) {
}
