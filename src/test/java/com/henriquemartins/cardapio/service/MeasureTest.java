package com.henriquemartins.cardapio.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MeasureTest {

    @ParameterizedTest
    @CsvSource({
            "'1 pound',      1,    pound",
            "'200g',         200,  g",
            "'1/2 cup',      0.5,  cup",
            "'1 1/2 tbs',    1.5,  tbs",
            "'2.5 kg',       2.5,  kg",
            "'½ tsp',        0.5,  tsp",
            "'1 ½ cups',     1.5,  cups",
            "'3',            3,    ''"
    })
    void extraiQuantidadeEUnidade(String raw, String amount, String unit) {
        Measure measure = Measure.parse(raw);

        assertThat(measure.isNumeric()).isTrue();
        assertThat(measure.amount()).isEqualByComparingTo(new BigDecimal(amount));
        assertThat(measure.unit()).isEqualTo(unit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Dash", "To taste", "a gosto"})
    void medidasSemNumeroFicamComoTexto(String raw) {
        Measure measure = Measure.parse(raw);

        assertThat(measure.isNumeric()).isFalse();
        assertThat(measure.format()).isEqualTo(raw);
    }

    @Test
    void medidaVaziaNaoQuebra() {
        Measure measure = Measure.parse("  ");

        assertThat(measure.isNumeric()).isFalse();
        assertThat(measure.raw()).isEmpty();
    }

    @Test
    void escalaProporcionalmenteAsPorcoes() {
        Measure dobrada = Measure.parse("200 g").scaled(BigDecimal.valueOf(2));

        assertThat(dobrada.format()).isEqualTo("400 g");
    }

    @Test
    void formataSemCasasDecimaisDesnecessarias() {
        assertThat(Measure.parse("1/2 cup").scaled(BigDecimal.valueOf(4)).format()).isEqualTo("2 cup");
        assertThat(Measure.parse("1/3 cup").format()).isEqualTo("0.33 cup");
    }

    @Test
    void naoEscalaMedidaSemNumero() {
        assertThat(Measure.parse("Dash").scaled(BigDecimal.TEN).format()).isEqualTo("Dash");
    }
}
