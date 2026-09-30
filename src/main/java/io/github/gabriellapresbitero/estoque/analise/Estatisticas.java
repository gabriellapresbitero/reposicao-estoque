package io.github.gabriellapresbitero.estoque.analise;

import java.util.List;

/** Média e desvio padrão, escritos à mão para ficar claro o que cada um significa. */
public final class Estatisticas {

    private Estatisticas() {
    }

    public static double media(double[] valores) {
        if (valores.length == 0) {
            return 0;
        }
        double soma = 0;
        for (double v : valores) {
            soma += v;
        }
        return soma / valores.length;
    }

    /**
     * Desvio padrão amostral: raiz da média dos quadrados das distâncias até a média,
     * dividindo por (n − 1) porque estamos estimando a partir de uma amostra de dias.
     */
    public static double desvioPadrao(double[] valores) {
        if (valores.length < 2) {
            return 0;
        }
        double media = media(valores);
        double somaQuadrados = 0;
        for (double v : valores) {
            somaQuadrados += (v - media) * (v - media);
        }
        return Math.sqrt(somaQuadrados / (valores.length - 1));
    }

    public static double[] paraArray(List<Integer> valores) {
        return valores.stream().mapToDouble(Integer::doubleValue).toArray();
    }
}
