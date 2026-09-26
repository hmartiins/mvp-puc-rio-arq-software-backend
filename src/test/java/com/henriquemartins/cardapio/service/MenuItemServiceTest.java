package com.henriquemartins.cardapio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.henriquemartins.cardapio.dto.MealDetail;
import com.henriquemartins.cardapio.dto.MenuItemRequest;
import com.henriquemartins.cardapio.dto.MenuItemResponse;
import com.henriquemartins.cardapio.dto.MenuItemUpdateRequest;
import com.henriquemartins.cardapio.exception.ExternalServiceException;
import com.henriquemartins.cardapio.exception.ResourceNotFoundException;
import com.henriquemartins.cardapio.model.DiaDaSemana;
import com.henriquemartins.cardapio.model.MealType;
import com.henriquemartins.cardapio.model.MenuItem;
import com.henriquemartins.cardapio.repository.MenuItemRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 8, 31);

    @Mock
    private MenuItemRepository repository;

    @Mock
    private TheMealDbClient mealDbClient;

    @InjectMocks
    private MenuItemService service;

    @Test
    void persisteNomeEThumbnailVindosDaExterna() {
        when(mealDbClient.lookup("52771")).thenReturn(new MealDetail("52771",
                "Spicy Arrabiata Penne", "thumb.jpg", "Vegetarian", "Italian", "...", List.of()));
        when(repository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemResponse response = service.create(new MenuItemRequest(
                "52771", DiaDaSemana.SEG, MealType.ALMOCO, 4, WEEK));

        ArgumentCaptor<MenuItem> saved = ArgumentCaptor.forClass(MenuItem.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getMealName()).isEqualTo("Spicy Arrabiata Penne");
        assertThat(saved.getValue().getThumbnailUrl()).isEqualTo("thumb.jpg");
        assertThat(response.mealName()).isEqualTo("Spicy Arrabiata Penne");
    }

    @Test
    void normalizaWeekRefParaASegundaDaSemana() {
        when(mealDbClient.lookup("52771")).thenReturn(new MealDetail("52771", "Penne",
                "thumb.jpg", null, null, null, List.of()));
        when(repository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemResponse response = service.create(new MenuItemRequest(
                "52771", DiaDaSemana.QUI, MealType.JANTAR, 2, WEEK.plusDays(3)));

        assertThat(response.weekRef()).isEqualTo(WEEK);
    }

    @Test
    void naoPersisteQuandoAExternaFalha() {
        when(mealDbClient.lookup("52771")).thenThrow(new ExternalServiceException("fora do ar"));

        assertThatThrownBy(() -> service.create(new MenuItemRequest(
                "52771", DiaDaSemana.SEG, MealType.CAFE, 2, WEEK)))
                .isInstanceOf(ExternalServiceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void atualizaDiaRefeicaoEPorcoes() {
        MenuItem item = new MenuItem(DiaDaSemana.SEG, MealType.ALMOCO, "52771", "Penne",
                "thumb.jpg", 2, WEEK);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        MenuItemResponse response = service.update(1L,
                new MenuItemUpdateRequest(DiaDaSemana.DOM, MealType.JANTAR, 6));

        assertThat(response.dayOfWeek()).isEqualTo(DiaDaSemana.DOM);
        assertThat(response.mealType()).isEqualTo(MealType.JANTAR);
        assertThat(response.servings()).isEqualTo(6);
    }

    @Test
    void atualizarItemInexistenteDa404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new MenuItemUpdateRequest(DiaDaSemana.SEG, MealType.CAFE, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removerItemInexistenteDa404() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void listaSemanaNormalizada() {
        when(repository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK)).thenReturn(List.of());

        assertThat(service.listByWeek(WEEK.plusDays(5))).isEmpty();
        verify(repository).findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK);
    }
}
