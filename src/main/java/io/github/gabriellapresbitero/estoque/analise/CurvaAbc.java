package io.github.gabriellapresbitero.estoque.analise;

import io.github.gabriellapresbitero.estoque.modelo.ClasseAbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Classificação ABC pelo valor vendido. */
public final class CurvaAbc {

    static final BigDecimal LIMITE_A = new BigDecimal("80");
    static final BigDecimal LIMITE_B = new BigDecimal("95");

    private CurvaAbc() {
    }

    /**
     * Ordena os produtos do mais vendido (em R$) para o menos vendido e vai somando.
     * Um produto é A enquanto o acumulado ANTES dele ainda não chegou a 80%.
     * Assim, o produto que "cruza" os 80% também é A, e um único produto
     * responsável por 90% das vendas fica corretamente na classe A.
     */
    public static Map<String, ClasseAbc> classificar(Map<String, BigDecimal> valorPorSku) {
        List<Map.Entry<String, BigDecimal>> ordenados = new ArrayList<>(valorPorSku.entrySet());
        ordenados.sort(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));

        BigDecimal total = ordenados.stream().map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, ClasseAbc> classes = new HashMap<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> item : ordenados) {
            BigDecimal percentualAntes = total.signum() == 0 ? new BigDecimal("100")
                    : acumulado.multiply(new BigDecimal("100")).divide(total, 4, RoundingMode.HALF_EVEN);
            ClasseAbc classe;
            if (item.getValue().signum() == 0) {
                classe = ClasseAbc.C; // não vendeu nada
            } else if (percentualAntes.compareTo(LIMITE_A) < 0) {
                classe = ClasseAbc.A;
            } else if (percentualAntes.compareTo(LIMITE_B) < 0) {
                classe = ClasseAbc.B;
            } else {
                classe = ClasseAbc.C;
            }
            classes.put(item.getKey(), classe);
            acumulado = acumulado.add(item.getValue());
        }
        return classes;
    }
}
