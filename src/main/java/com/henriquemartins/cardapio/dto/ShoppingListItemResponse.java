package com.henriquemartins.cardapio.dto;

import java.util.List;

/**
 * Um ingrediente consolidado da semana.
 *
 * @param ingredient nome canonico do ingrediente
 * @param quantity   quantidade somada e ja ajustada as porcoes (ex.: "300 g", "2 tbsp + a gosto")
 * @param checked    se o usuario ja marcou como comprado
 * @param recipes    receitas da semana que usam o ingrediente
 */
public record ShoppingListItemResponse(
        String ingredient,
        String quantity,
        boolean checked,
        List<String> recipes
) {
}
