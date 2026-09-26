package com.henriquemartins.cardapio.exception;

/** Recurso inexistente (item de cardapio, receita). Vira 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
