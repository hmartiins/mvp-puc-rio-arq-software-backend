package com.henriquemartins.cardapio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uma medida de receita da TheMealDB ("1 pound", "1/2 cup", "200g", "Dash").
 * O texto nao e estruturado na origem, entao separamos quantidade de unidade
 * na marra: o que der para somar vira numero, o resto fica como observacao.
 *
 * @param amount quantidade, ou {@code null} quando a medida nao tem numero ("a gosto", "Dash")
 * @param unit   unidade normalizada em minusculas ("g", "cup", "tbs"), possivelmente vazia
 * @param raw    texto original, preservado para medidas nao numericas
 */
public record Measure(BigDecimal amount, String unit, String raw) {

    /** Fracoes unicode que aparecem nas receitas ("½ cup"). */
    private static final Map<Character, String> UNICODE_FRACTIONS = Map.of(
            '½', "1/2", '¼', "1/4", '¾', "3/4",
            '⅓', "1/3", '⅔', "2/3",
            '⅛', "1/8", '⅜', "3/8", '⅝', "5/8", '⅞', "7/8"
    );

    /**
     * Numero inteiro, fracao ou misto ("2", "1/2", "1 1/2"), seguido da unidade.
     * As alternativas sao testadas do mais especifico para o mais generico: sem isso
     * "1/4 cup" casaria como inteiro 1 com unidade "/4 cup".
     */
    private static final Pattern QUANTITY = Pattern.compile(
            "^\\s*(?:(?<whole>\\d+)\\s+(?<mixNum>\\d+)\\s*/\\s*(?<mixDen>\\d+)"
                    + "|(?<num>\\d+)\\s*/\\s*(?<den>\\d+)"
                    + "|(?<int>\\d+))\\s*(?<unit>.*)$");

    private static final Pattern DECIMAL = Pattern.compile(
            "^\\s*(?<value>\\d+[.,]\\d+)\\s*(?<unit>.*)$");

    public static Measure parse(String rawMeasure) {
        String raw = rawMeasure == null ? "" : rawMeasure.trim();
        if (raw.isEmpty()) {
            return new Measure(null, "", "");
        }

        String expanded = expandUnicodeFractions(raw);

        Matcher decimal = DECIMAL.matcher(expanded);
        if (decimal.matches()) {
            BigDecimal value = new BigDecimal(decimal.group("value").replace(',', '.'));
            return new Measure(value, normalizeUnit(decimal.group("unit")), raw);
        }

        Matcher m = QUANTITY.matcher(expanded);
        if (m.matches()) {
            BigDecimal amount;
            if (m.group("int") != null) {
                amount = new BigDecimal(m.group("int"));
            } else if (m.group("whole") != null) {
                amount = new BigDecimal(m.group("whole"))
                        .add(fraction(m.group("mixNum"), m.group("mixDen")));
            } else {
                amount = fraction(m.group("num"), m.group("den"));
            }
            return new Measure(amount, normalizeUnit(m.group("unit")), raw);
        }

        // Medidas sem numero ("Dash", "To taste") continuam valendo, so nao somam.
        return new Measure(null, "", raw);
    }

    public boolean isNumeric() {
        return amount != null;
    }

    public Measure scaled(BigDecimal factor) {
        return isNumeric()
                ? new Measure(amount.multiply(factor), unit, raw)
                : this;
    }

    /** "300 g", "2 1/2 cup" -> aqui simplificado para decimal enxuto: "2.5 cup". */
    public String format() {
        if (!isNumeric()) {
            return raw;
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        String number = rounded.scale() <= 0 ? rounded.toBigInteger().toString() : rounded.toPlainString();
        return unit.isEmpty() ? number : number + " " + unit;
    }

    private static BigDecimal fraction(String numerator, String denominator) {
        BigDecimal den = new BigDecimal(denominator);
        return den.signum() == 0
                ? BigDecimal.ZERO
                : new BigDecimal(numerator).divide(den, 4, RoundingMode.HALF_UP);
    }

    private static String expandUnicodeFractions(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 4);
        for (char c : value.toCharArray()) {
            String fraction = UNICODE_FRACTIONS.get(c);
            if (fraction == null) {
                sb.append(c);
            } else {
                if (!sb.isEmpty() && Character.isDigit(sb.charAt(sb.length() - 1))) {
                    sb.append(' ');
                }
                sb.append(fraction);
            }
        }
        return sb.toString();
    }

    private static String normalizeUnit(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase();
    }
}
