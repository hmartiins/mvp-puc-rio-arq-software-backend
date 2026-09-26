package com.henriquemartins.cardapio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/** Corpo padronizado de erro devolvido por toda a API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(OffsetDateTime.now(), status, error, message,
                details == null || details.isEmpty() ? null : details);
    }
}
