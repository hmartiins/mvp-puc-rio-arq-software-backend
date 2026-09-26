package com.henriquemartins.cardapio.repository;

import com.henriquemartins.cardapio.model.ShoppingListCheck;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListCheckRepository extends JpaRepository<ShoppingListCheck, Long> {

    List<ShoppingListCheck> findByWeekRef(LocalDate weekRef);

    Optional<ShoppingListCheck> findByWeekRefAndIngredientNameIgnoreCase(LocalDate weekRef,
                                                                        String ingredientName);
}
