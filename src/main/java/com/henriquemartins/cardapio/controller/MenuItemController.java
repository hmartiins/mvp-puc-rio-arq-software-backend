package com.henriquemartins.cardapio.controller;

import com.henriquemartins.cardapio.dto.MenuItemRequest;
import com.henriquemartins.cardapio.dto.MenuItemResponse;
import com.henriquemartins.cardapio.dto.MenuItemUpdateRequest;
import com.henriquemartins.cardapio.service.MenuItemService;
import com.henriquemartins.cardapio.service.WeekRef;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu-items")
@Tag(name = "Cardapio", description = "Itens do cardapio da semana")
public class MenuItemController {

    private final MenuItemService service;

    public MenuItemController(MenuItemService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Adiciona uma receita a um dia/refeicao da semana")
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/menu-items/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista os itens do cardapio de uma semana (default: semana atual)")
    public List<MenuItemResponse> listByWeek(
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return service.listByWeek(week == null ? WeekRef.currentWeek() : week);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dia, refeicao e porcoes de um item")
    public MenuItemResponse update(@PathVariable Long id,
                                   @Valid @RequestBody MenuItemUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um item do cardapio")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
