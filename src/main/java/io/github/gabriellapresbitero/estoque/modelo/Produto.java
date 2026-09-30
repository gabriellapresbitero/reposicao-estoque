package io.github.gabriellapresbitero.estoque.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Um item do estoque.
 *
 * @param leadTimeDias  quantos dias o fornecedor leva para entregar depois do pedido
 * @param loteMinimo    a compra precisa ser múltipla desse número (ex.: caixa com 12 unidades)
 */
public record Produto(String sku, String nome, int estoqueAtual, int leadTimeDias,
                      BigDecimal custoUnitario, int loteMinimo) {

    public Produto {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(custoUnitario, "custoUnitario");
        nome = nome == null ? "" : nome.strip();
        if (estoqueAtual < 0) {
            throw new IllegalArgumentException("estoque negativo no produto " + sku);
        }
        if (leadTimeDias < 1) {
            throw new IllegalArgumentException("lead time precisa ser de pelo menos 1 dia no produto " + sku);
        }
        if (loteMinimo < 1) {
            throw new IllegalArgumentException("lote mínimo precisa ser pelo menos 1 no produto " + sku);
        }
        custoUnitario = custoUnitario.setScale(2, RoundingMode.HALF_EVEN);
    }
}
