package com.henriquemartins.cardapio.service;

import com.henriquemartins.cardapio.dto.MealDetail;
import com.henriquemartins.cardapio.dto.MealIngredient;
import com.henriquemartins.cardapio.dto.ShoppingListItemResponse;
import com.henriquemartins.cardapio.model.MenuItem;
import com.henriquemartins.cardapio.model.ShoppingListCheck;
import com.henriquemartins.cardapio.repository.MenuItemRepository;
import com.henriquemartins.cardapio.repository.ShoppingListCheckRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra de negocio central: transformar o cardapio da semana numa lista de compras
 * consolidada — ingredientes somados entre receitas e ajustados as porcoes pedidas.
 */
@Service
public class ShoppingListService {

    /**
     * A TheMealDB nao informa rendimento das receitas; as quantidades publicadas
     * correspondem, na pratica, a cerca de 4 porcoes. E dessa base que escalamos.
     */
    private static final BigDecimal BASE_SERVINGS = BigDecimal.valueOf(4);

    private final MenuItemRepository menuItemRepository;
    private final ShoppingListCheckRepository checkRepository;
    private final TheMealDbClient mealDbClient;

    public ShoppingListService(MenuItemRepository menuItemRepository,
                               ShoppingListCheckRepository checkRepository,
                               TheMealDbClient mealDbClient) {
        this.menuItemRepository = menuItemRepository;
        this.checkRepository = checkRepository;
        this.mealDbClient = mealDbClient;
    }

    @Transactional(readOnly = true)
    public List<ShoppingListItemResponse> consolidate(LocalDate weekRef) {
        LocalDate week = WeekRef.normalize(weekRef);
        List<MenuItem> items = menuItemRepository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(week);
        if (items.isEmpty()) {
            return List.of();
        }

        // Cache por request: a mesma receita pode aparecer em varios dias da semana
        // e nao ha razao para consultar a externa mais de uma vez.
        Map<String, MealDetail> mealCache = new HashMap<>();
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();

        for (MenuItem item : items) {
            MealDetail meal = mealCache.computeIfAbsent(item.getMealId(), mealDbClient::lookup);
            BigDecimal factor = BigDecimal.valueOf(item.getServings())
                    .divide(BASE_SERVINGS, 4, RoundingMode.HALF_UP);

            for (MealIngredient ingredient : meal.ingredients()) {
                String key = ingredient.name().toLowerCase(Locale.ROOT);
                aggregates.computeIfAbsent(key, k -> new Aggregate(displayName(ingredient.name())))
                        .add(Measure.parse(ingredient.measure()).scaled(factor), meal.mealName());
            }
        }

        Map<String, Boolean> checkedByIngredient = new HashMap<>();
        for (ShoppingListCheck check : checkRepository.findByWeekRef(week)) {
            checkedByIngredient.put(check.getIngredientName().toLowerCase(Locale.ROOT), check.getChecked());
        }

        return aggregates.entrySet().stream()
                .map(entry -> new ShoppingListItemResponse(
                        entry.getValue().displayName,
                        entry.getValue().formatQuantity(),
                        checkedByIngredient.getOrDefault(entry.getKey(), false),
                        List.copyOf(entry.getValue().recipes)))
                .sorted(Comparator.comparing(ShoppingListItemResponse::ingredient,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Marca/desmarca um ingrediente como comprado, criando o registro na primeira vez. */
    @Transactional
    public ShoppingListItemResponse check(LocalDate weekRef, String ingredient, boolean checked) {
        LocalDate week = WeekRef.normalize(weekRef);
        String name = ingredient.trim();

        ShoppingListCheck record = checkRepository
                .findByWeekRefAndIngredientNameIgnoreCase(week, name)
                .orElseGet(() -> new ShoppingListCheck(name, checked, week));
        record.setChecked(checked);
        ShoppingListCheck saved = checkRepository.save(record);

        return new ShoppingListItemResponse(saved.getIngredientName(), null, saved.getChecked(), List.of());
    }

    private static String displayName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    /** Acumula as ocorrencias de um mesmo ingrediente ao longo da semana. */
    private static final class Aggregate {

        private final String displayName;
        /** Somatorio por unidade: "g" e "cup" da mesma cebola nao se misturam. */
        private final Map<String, BigDecimal> totalsByUnit = new LinkedHashMap<>();
        /** Medidas sem numero ("a gosto", "Dash") — nao somam, mas precisam aparecer. */
        private final Set<String> freeFormMeasures = new LinkedHashSet<>();
        private final Set<String> recipes = new LinkedHashSet<>();

        private Aggregate(String displayName) {
            this.displayName = displayName;
        }

        private void add(Measure measure, String recipeName) {
            recipes.add(recipeName);
            if (measure.isNumeric()) {
                totalsByUnit.merge(measure.unit(), measure.amount(), BigDecimal::add);
            } else if (!measure.raw().isBlank()) {
                freeFormMeasures.add(measure.raw());
            }
        }

        private String formatQuantity() {
            List<String> parts = new ArrayList<>();
            totalsByUnit.forEach((unit, total) ->
                    parts.add(new Measure(total, unit, null).format()));
            parts.addAll(freeFormMeasures);
            return parts.isEmpty() ? "a gosto" : String.join(" + ", parts);
        }
    }
}
