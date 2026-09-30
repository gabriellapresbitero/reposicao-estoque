package io.github.gabriellapresbitero.estoque.modelo;

/** Situação de um produto, em ordem de urgência. */
public enum Situacao {
    RUPTURA("Ruptura: sem estoque e com demanda"),
    COMPRAR("Comprar agora: abaixo do ponto de pedido"),
    ATENCAO("Atenção: vai precisar comprar em até 7 dias"),
    OK("OK"),
    EXCESSO("Excesso: estoque para mais tempo que o necessário"),
    SEM_GIRO("Sem giro: tem estoque, mas não vendeu no período");

    private final String descricao;

    Situacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean precisaComprar() {
        return this == RUPTURA || this == COMPRAR;
    }
}
