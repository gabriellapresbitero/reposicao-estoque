package io.github.gabriellapresbitero.estoque.modelo;

import java.math.BigDecimal;

/**
 * Resultado da análise de um produto.
 *
 * <p>Quantidades e estatísticas usam {@code double} (são estimativas).
 * Valores em dinheiro usam {@link BigDecimal} (precisam ser exatos).
 *
 * @param demandaMedia       unidades vendidas por dia, em média
 * @param desvioPadrao       o quanto a venda diária costuma variar em torno da média
 * @param estoqueSeguranca   "colchão" para aguentar dias de venda acima da média durante a entrega
 * @param pontoPedido        quando o estoque chegar aqui, é hora de pedir
 * @param coberturaDias      quantos dias o estoque atual dura na média de vendas
 * @param comprar            quantidade sugerida para comprar agora (0 se não precisa)
 * @param capitalParado      dinheiro em estoque acima do necessário (excesso ou sem giro)
 */
public record Sugestao(Produto produto, double demandaMedia, double desvioPadrao, int estoqueSeguranca,
                       int pontoPedido, double coberturaDias, int comprar, BigDecimal valorCompra,
                       BigDecimal valorVendido, ClasseAbc classe, Situacao situacao, BigDecimal capitalParado) {
}
