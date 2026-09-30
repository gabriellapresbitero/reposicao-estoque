package io.github.gabriellapresbitero.estoque.modelo;

import java.time.LocalDate;
import java.util.Objects;

/** Uma venda (ou saída) de um produto num dia. */
public record Venda(LocalDate data, String sku, int quantidade) {

    public Venda {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(sku, "sku");
        if (quantidade < 0) {
            throw new IllegalArgumentException("quantidade negativa");
        }
    }
}
