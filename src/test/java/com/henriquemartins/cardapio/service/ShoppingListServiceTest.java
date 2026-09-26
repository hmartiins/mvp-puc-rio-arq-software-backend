package com.henriquemartins.cardapio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.henriquemartins.cardapio.dto.MealDetail;
import com.henriquemartins.cardapio.dto.MealIngredient;
import com.henriquemartins.cardapio.dto.ShoppingListItemResponse;
import com.henriquemartins.cardapio.model.DiaDaSemana;
import com.henriquemartins.cardapio.model.MealType;
import com.henriquemartins.cardapio.model.MenuItem;
import com.henriquemartins.cardapio.model.ShoppingListCheck;
import com.henriquemartins.cardapio.repository.MenuItemRepository;
import com.henriquemartins.cardapio.repository.ShoppingListCheckRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    /** Segunda-feira. */
    private static final LocalDate WEEK = LocalDate.of(2026, 8, 31);

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ShoppingListCheckRepository checkRepository;

    @Mock
    private TheMealDbClient mealDbClient;

    @InjectMocks
    private ShoppingListService service;

    private MealDetail penne;
    private MealDetail lasanha;

    @BeforeEach
    void setUp() {
        penne = new MealDetail("52771", "Spicy Arrabiata Penne", "thumb.jpg", "Vegetarian",
                "Italian", "...", List.of(
                new MealIngredient("penne rigate", "1 pound"),
                new MealIngredient("olive oil", "1/4 cup"),
                new MealIngredient("garlic", "3 cloves"),
                new MealIngredient("salt", "to taste")));

        lasanha = new MealDetail("52844", "Lasagne", "thumb2.jpg", "Pasta",
                "Italian", "...", List.of(
                new MealIngredient("Olive oil", "2 tbs"),
                new MealIngredient("garlic", "1 cloves"),
                new MealIngredient("beef", "500 g")));
    }

    @Test
    void semanaSemItensDevolveListaVazia() {
        when(menuItemRepository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK))
                .thenReturn(List.of());

        assertThat(service.consolidate(WEEK)).isEmpty();
    }

    @Test
    void somaIngredientesEntreReceitasEOrdenaAlfabeticamente() {
        stubWeek(
                menuItem("52771", 4, DiaDaSemana.SEG, MealType.ALMOCO),
                menuItem("52844", 4, DiaDaSemana.TER, MealType.JANTAR));
        when(mealDbClient.lookup("52771")).thenReturn(penne);
        when(mealDbClient.lookup("52844")).thenReturn(lasanha);
        when(checkRepository.findByWeekRef(WEEK)).thenReturn(List.of());

        List<ShoppingListItemResponse> list = service.consolidate(WEEK);

        assertThat(list)
                .extracting(ShoppingListItemResponse::ingredient, ShoppingListItemResponse::quantity)
                .containsExactly(
                        tuple("Beef", "500 g"),
                        tuple("Garlic", "4 cloves"),
                        tuple("Olive oil", "0.25 cup + 2 tbs"),
                        tuple("Penne rigate", "1 pound"),
                        tuple("Salt", "to taste"));
    }

    @Test
    void ajustaQuantidadeProporcionalmenteAsPorcoes() {
        stubWeek(menuItem("52844", 8, DiaDaSemana.QUA, MealType.JANTAR));
        when(mealDbClient.lookup("52844")).thenReturn(lasanha);
        when(checkRepository.findByWeekRef(WEEK)).thenReturn(List.of());

        assertThat(service.consolidate(WEEK))
                .extracting(ShoppingListItemResponse::ingredient, ShoppingListItemResponse::quantity)
                .contains(tuple("Beef", "1000 g"), tuple("Garlic", "2 cloves"));
    }

    @Test
    void consultaAExternaUmaVezPorReceitaMesmoRepetidaNaSemana() {
        stubWeek(
                menuItem("52771", 4, DiaDaSemana.SEG, MealType.ALMOCO),
                menuItem("52771", 4, DiaDaSemana.QUI, MealType.JANTAR));
        when(mealDbClient.lookup("52771")).thenReturn(penne);
        when(checkRepository.findByWeekRef(WEEK)).thenReturn(List.of());

        service.consolidate(WEEK);

        verify(mealDbClient, times(1)).lookup("52771");
    }

    @Test
    void preservaOEstadoCompradoJaMarcado() {
        stubWeek(menuItem("52844", 4, DiaDaSemana.SEX, MealType.JANTAR));
        when(mealDbClient.lookup("52844")).thenReturn(lasanha);
        when(checkRepository.findByWeekRef(WEEK))
                .thenReturn(List.of(new ShoppingListCheck("beef", true, WEEK)));

        assertThat(service.consolidate(WEEK))
                .extracting(ShoppingListItemResponse::ingredient, ShoppingListItemResponse::checked)
                .contains(tuple("Beef", true), tuple("Garlic", false));
    }

    @Test
    void normalizaQualquerDiaDaSemanaParaASegunda() {
        LocalDate quinta = WEEK.plusDays(3);
        when(menuItemRepository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK))
                .thenReturn(List.of());

        service.consolidate(quinta);

        verify(menuItemRepository).findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK);
    }

    @Test
    void marcarIngredienteInexistenteCriaORegistroDaSemana() {
        when(checkRepository.findByWeekRefAndIngredientNameIgnoreCase(WEEK, "Beef"))
                .thenReturn(Optional.empty());
        when(checkRepository.save(any(ShoppingListCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListItemResponse response = service.check(WEEK, " Beef ", true);

        assertThat(response.ingredient()).isEqualTo("Beef");
        assertThat(response.checked()).isTrue();
        verify(checkRepository).save(any(ShoppingListCheck.class));
    }

    @Test
    void desmarcarAtualizaORegistroExistente() {
        ShoppingListCheck existing = new ShoppingListCheck("Beef", true, WEEK);
        when(checkRepository.findByWeekRefAndIngredientNameIgnoreCase(eq(WEEK), eq("Beef")))
                .thenReturn(Optional.of(existing));
        when(checkRepository.save(existing)).thenReturn(existing);

        assertThat(service.check(WEEK, "Beef", false).checked()).isFalse();
        assertThat(existing.getChecked()).isFalse();
    }

    private void stubWeek(MenuItem... items) {
        when(menuItemRepository.findByWeekRefOrderByDayOfWeekAscMealTypeAsc(WEEK))
                .thenReturn(List.of(items));
    }

    private MenuItem menuItem(String mealId, int servings, DiaDaSemana day, MealType type) {
        return new MenuItem(day, type, mealId, "Receita " + mealId, "thumb.jpg", servings, WEEK);
    }
}
