package io.github.gabriellapresbitero.estoque.modelo;

/**
 * Classificação ABC (princípio de Pareto): poucos produtos respondem pela maior parte do dinheiro.
 * <ul>
 *   <li>A: produtos que somam os primeiros 80% do valor vendido. Merecem controle rigoroso.</li>
 *   <li>B: os próximos 15% (até 95%).</li>
 *   <li>C: os 5% restantes, geralmente a maioria dos itens.</li>
 * </ul>
 */
public enum ClasseAbc {
    A, B, C
}
