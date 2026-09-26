package com.henriquemartins.cardapio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * Estado "comprado" de um ingrediente numa semana. A lista de compras em si e sempre
 * recalculada a partir dos MenuItem; aqui so mora o que o usuario marcou.
 */
@Entity
@Table(name = "shopping_list_check",
        uniqueConstraints = @UniqueConstraint(columnNames = {"week_ref", "ingredient_name"}))
public class ShoppingListCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(nullable = false)
    private Boolean checked = false;

    @Column(name = "week_ref", nullable = false)
    private LocalDate weekRef;

    protected ShoppingListCheck() {
    }

    public ShoppingListCheck(String ingredientName, Boolean checked, LocalDate weekRef) {
        this.ingredientName = ingredientName;
        this.checked = checked;
        this.weekRef = weekRef;
    }

    public Long getId() {
        return id;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public LocalDate getWeekRef() {
        return weekRef;
    }
}
