package io.github.gabriellapresbitero.estoque.analise;

/**
 * Parâmetros da análise.
 *
 * @param nivelServico   % de chance de não faltar produto durante a entrega (ex.: 95)
 * @param diasCobertura  para quantos dias, além do lead time, cada compra deve durar
 * @param janelaDias     quantos dias de histórico usar para calcular a demanda
 * @param diasExcesso    acima de quantos dias de cobertura o estoque é considerado excessivo
 */
public record Parametros(double nivelServico, int diasCobertura, int janelaDias, int diasExcesso) {

    public static final Parametros PADRAO = new Parametros(95, 30, 90, 90);

    public Parametros {
        NivelServico.fatorZ(nivelServico); // valida
        if (diasCobertura < 1 || janelaDias < 7 || diasExcesso < 1) {
            throw new IllegalArgumentException("parâmetros inválidos: cobertura >= 1, janela >= 7 e excesso >= 1");
        }
    }
}
