package com.henriquemartins.cardapio.service;

import com.henriquemartins.cardapio.dto.MealDetail;
import com.henriquemartins.cardapio.dto.MenuItemRequest;
import com.henriquemartins.cardapio.dto.MenuItemResponse;
import com.henriquemartins.cardapio.dto.MenuItemUpdateRequest;
import com.henriquemartins.cardapio.exception.ResourceNotFoundException;
import com.henriquemartins.cardapio.model.MenuItem;
import com.henriquemartins.cardapio.repository.MenuItemRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {

    private final MenuItemRepository repository;
    private final TheMealDbClient mealDbClient;

    public MenuItemService(MenuItemRepository repository, TheMealDbClient mealDbClient) {
        this.repository = repository;
        this.mealDbClient = mealDbClient;
    }

    /**
     * Busca o detalhe da receita na TheMealDB e persiste o item ja com nome e thumbnail,
     * para que a listagem da semana nao precise bater na externa.
     */
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        MealDetail meal = mealDbClient.lookup(request.mealId());

        MenuItem item = new MenuItem(
                request.dayOfWeek(),
                request.mealType(),
                meal.mealId(),
                meal.mealName(),
                meal.thumbnailUrl(),
                request.servings(),
                WeekRef.normalize(request.weekRef())
        );
        return MenuItemResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listByWeek(LocalDate weekRef) {
        return repository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WeekRef.normalize(weekRef))
                .stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemUpdateRequest request) {
        MenuItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de cardapio nao encontrado: " + id));

        item.setDayOfWeek(request.dayOfWeek());
        item.setMealType(request.mealType());
        item.setServings(request.servings());

        return MenuItemResponse.from(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Item de cardapio nao encontrado: " + id);
        }
        repository.deleteById(id);
    }
}
