package io.github.gabriellapresbitero.estoque.analise;

import io.github.gabriellapresbitero.estoque.modelo.ClasseAbc;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CurvaAbcTest {

    private static BigDecimal r(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    public void classificaPeloAcumulado() {
        // total 1000: X=700 (0% antes), Y=150 (70% antes), Z=100 (85% antes), W=40 (95% antes), K=10
        Map<String, ClasseAbc> classes = CurvaAbc.classificar(Map.of(
                "X", r("700"), "Y", r("150"), "Z", r("100"), "W", r("40"), "K", r("10")));
        assertEquals(ClasseAbc.A, classes.get("X"));
        assertEquals(ClasseAbc.A, classes.get("Y")); // cruza os 80%, então ainda é A
        assertEquals(ClasseAbc.B, classes.get("Z"));
        assertEquals(ClasseAbc.C, classes.get("W"));
        assertEquals(ClasseAbc.C, classes.get("K"));
    }

    @Test
    public void umProdutoDominante() {
        Map<String, ClasseAbc> classes = CurvaAbc.classificar(Map.of("X", r("900"), "Y", r("100")));
        assertEquals(ClasseAbc.A, classes.get("X"));
        assertEquals(ClasseAbc.B, classes.get("Y")); // 90% acumulado antes dele: faixa B (80% a 95%)
    }

    @Test
    public void semVendasEClasseC() {
        Map<String, ClasseAbc> classes = CurvaAbc.classificar(Map.of("X", r("100"), "Y", BigDecimal.ZERO));
        assertEquals(ClasseAbc.C, classes.get("Y"));
    }

    @Test
    public void nenhumaVenda() {
        Map<String, ClasseAbc> classes = CurvaAbc.classificar(Map.of("X", BigDecimal.ZERO));
        assertEquals(ClasseAbc.C, classes.get("X"));
    }
}
