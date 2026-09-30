package io.github.gabriellapresbitero.estoque.analise;

import java.util.Map;
import java.util.TreeMap;

/**
 * Nível de serviço = probabilidade de NÃO faltar produto durante a espera da entrega.
 *
 * <p>Cada nível corresponde a um fator Z da distribuição normal: com 95%, Z = 1,645.
 * Ou seja, o estoque de segurança cobre a demanda até 1,645 desvios padrão acima da média.
 * Os valores vêm da tabela da distribuição normal padrão.
 */
public final class NivelServico {

    private static final Map<Double, Double> FATOR_Z = new TreeMap<>(Map.of(
            80.0, 0.842,
            85.0, 1.036,
            90.0, 1.282,
            95.0, 1.645,
            97.5, 1.960,
            98.0, 2.054,
            99.0, 2.326,
            99.9, 3.090));

    private NivelServico() {
    }

    public static double fatorZ(double percentual) {
        Double z = FATOR_Z.get(percentual);
        if (z == null) {
            throw new IllegalArgumentException("nível de serviço " + percentual
                    + "% não suportado. Use um destes: " + FATOR_Z.keySet());
        }
        return z;
    }
}
