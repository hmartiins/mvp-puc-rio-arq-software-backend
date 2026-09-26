package com.henriquemartins.cardapio.dto;

import jakarta.validation.constraints.NotNull;

public record ShoppingListCheckRequest(
        @NotNull(message = "checked e obrigatorio")
        Boolean checked
) {
}
