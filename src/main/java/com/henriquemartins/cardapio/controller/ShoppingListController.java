package com.henriquemartins.cardapio.controller;

import com.henriquemartins.cardapio.dto.ShoppingListCheckRequest;
import com.henriquemartins.cardapio.dto.ShoppingListItemResponse;
import com.henriquemartins.cardapio.service.ShoppingListService;
import com.henriquemartins.cardapio.service.WeekRef;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shopping-list")
@Tag(name = "Lista de compras", description = "Ingredientes consolidados da semana")
public class ShoppingListController {

    private final ShoppingListService service;

    public ShoppingListController(ShoppingListService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consolida os ingredientes de todas as receitas da semana")
    public List<ShoppingListItemResponse> byWeek(
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return service.consolidate(week == null ? WeekRef.currentWeek() : week);
    }

    @PatchMapping("/{ingredient}/check")
    @Operation(summary = "Marca ou desmarca um ingrediente como comprado")
    public ShoppingListItemResponse check(
            @PathVariable String ingredient,
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            @Valid @RequestBody ShoppingListCheckRequest request) {
        return service.check(week == null ? WeekRef.currentWeek() : week,
                ingredient, request.checked());
    }
}
