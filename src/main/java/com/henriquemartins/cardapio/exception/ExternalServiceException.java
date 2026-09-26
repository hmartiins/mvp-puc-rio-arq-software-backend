package com.henriquemartins.cardapio.exception;

/** A TheMealDB falhou, deu timeout ou devolveu algo inesperado. Vira 503 para o cliente. */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
