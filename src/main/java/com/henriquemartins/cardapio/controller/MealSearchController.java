package com.henriquemartins.cardapio.controller;

import com.henriquemartins.cardapio.dto.MealSearchResponse;
import com.henriquemartins.cardapio.service.TheMealDbClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meals")
@Validated
@Tag(name = "Receitas", description = "Busca de receitas (proxy tratado da TheMealDB)")
public class MealSearchController {

    private final TheMealDbClient mealDbClient;

    public MealSearchController(TheMealDbClient mealDbClient) {
        this.mealDbClient = mealDbClient;
    }

    @GetMapping("/search")
    @Operation(summary = "Busca receitas por nome na TheMealDB e devolve o contrato proprio da API")
    public List<MealSearchResponse> search(
            @RequestParam("q") @NotBlank(message = "q e obrigatorio") String query) {
        return mealDbClient.search(query.trim());
    }
}
