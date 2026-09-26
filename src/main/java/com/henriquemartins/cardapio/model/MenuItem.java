package com.henriquemartins.cardapio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Uma receita atribuida a um dia/refeicao de uma semana. */
@Entity
@Table(name = "menu_item")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private DiaDaSemana dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MealType mealType;

    @Column(nullable = false)
    private String mealId;

    @Column(nullable = false)
    private String mealName;

    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer servings;

    /** Segunda-feira da semana a que o item pertence. */
    @Column(nullable = false)
    private LocalDate weekRef;

    protected MenuItem() {
    }

    public MenuItem(DiaDaSemana dayOfWeek, MealType mealType, String mealId, String mealName,
                    String thumbnailUrl, Integer servings, LocalDate weekRef) {
        this.dayOfWeek = dayOfWeek;
        this.mealType = mealType;
        this.mealId = mealId;
        this.mealName = mealName;
        this.thumbnailUrl = thumbnailUrl;
        this.servings = servings;
        this.weekRef = weekRef;
    }

    public Long getId() {
        return id;
    }

    public DiaDaSemana getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DiaDaSemana dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }

    public String getMealId() {
        return mealId;
    }

    public String getMealName() {
        return mealName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public LocalDate getWeekRef() {
        return weekRef;
    }
}
