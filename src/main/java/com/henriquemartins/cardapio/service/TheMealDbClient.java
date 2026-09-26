package com.henriquemartins.cardapio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.henriquemartins.cardapio.dto.MealDetail;
import com.henriquemartins.cardapio.dto.MealIngredient;
import com.henriquemartins.cardapio.dto.MealSearchResponse;
import com.henriquemartins.cardapio.exception.ExternalServiceException;
import com.henriquemartins.cardapio.exception.ResourceNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unico ponto do sistema que fala com a TheMealDB. Nenhuma resposta crua da externa
 * escapa daqui: tudo sai mapeado para DTOs proprios, e qualquer falha vira
 * {@link ExternalServiceException} (503) em vez de exception de transporte.
 */
@Component
public class TheMealDbClient {

    private static final Logger log = LoggerFactory.getLogger(TheMealDbClient.class);

    /** A TheMealDB expoe ate 20 pares ingrediente/medida por receita. */
    private static final int MAX_INGREDIENTS = 20;

    private final WebClient webClient;
    private final Duration timeout;

    public TheMealDbClient(WebClient.Builder builder,
                           @Value("${themealdb.base-url}") String baseUrl,
                           @Value("${themealdb.timeout-seconds:5}") long timeoutSeconds) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /** {@code search.php?s=} — busca receitas por nome. */
    public List<MealSearchResponse> search(String query) {
        JsonNode root = get(UriComponentsBuilder.fromPath("/search.php")
                .queryParam("s", query)
                .toUriString());

        JsonNode meals = root.path("meals");
        if (!meals.isArray()) {
            // A TheMealDB devolve {"meals": null} quando nao ha resultado.
            return List.of();
        }

        List<MealSearchResponse> results = new ArrayList<>();
        for (JsonNode meal : meals) {
            results.add(new MealSearchResponse(
                    text(meal, "idMeal"),
                    text(meal, "strMeal"),
                    text(meal, "strMealThumb"),
                    text(meal, "strCategory"),
                    text(meal, "strArea")
            ));
        }
        return Collections.unmodifiableList(results);
    }

    /** {@code lookup.php?i=} — detalhe de uma receita, incluindo ingredientes e medidas. */
    public MealDetail lookup(String mealId) {
        return findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receita nao encontrada na TheMealDB: " + mealId));
    }

    /** Igual ao {@link #lookup(String)}, mas devolve vazio em vez de estourar 404. */
    public Optional<MealDetail> findById(String mealId) {
        JsonNode root = get(UriComponentsBuilder.fromPath("/lookup.php")
                .queryParam("i", mealId)
                .toUriString());

        JsonNode meals = root.path("meals");
        if (!meals.isArray() || meals.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toMealDetail(meals.get(0)));
    }

    private MealDetail toMealDetail(JsonNode meal) {
        List<MealIngredient> ingredients = new ArrayList<>();
        for (int i = 1; i <= MAX_INGREDIENTS; i++) {
            String name = text(meal, "strIngredient" + i);
            if (name == null || name.isBlank()) {
                continue;
            }
            String measure = text(meal, "strMeasure" + i);
            ingredients.add(new MealIngredient(name.trim(), measure == null ? "" : measure.trim()));
        }

        return new MealDetail(
                text(meal, "idMeal"),
                text(meal, "strMeal"),
                text(meal, "strMealThumb"),
                text(meal, "strCategory"),
                text(meal, "strArea"),
                text(meal, "strInstructions"),
                Collections.unmodifiableList(ingredients)
        );
    }

    private JsonNode get(String uri) {
        try {
            JsonNode body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout)
                    .block();

            if (body == null) {
                throw new ExternalServiceException("TheMealDB devolveu uma resposta vazia");
            }
            return body;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Falha ao consultar a TheMealDB em {}: {}", uri, e.toString());
            throw new ExternalServiceException(
                    "Servico externo de receitas indisponivel no momento", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
