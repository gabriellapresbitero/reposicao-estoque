# 📦 Reposição de Estoque

[![Testes](https://github.com/gabriellapresbitero/reposicao-estoque/actions/workflows/testes.yml/badge.svg)](https://github.com/gabriellapresbitero/reposicao-estoque/actions/workflows/testes.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)

Analisa o histórico de vendas de uma loja e responde, produto por produto:
**preciso comprar? Quanto? E onde tem dinheiro parado?**

## O problema

Em pequenos comércios, a compra costuma ser feita "no olho": alguém olha a prateleira e liga para
o fornecedor. Isso gera dois prejuízos opostos:

- **Ruptura:** o produto acaba antes da entrega chegar, e o cliente vai comprar no concorrente;
- **Excesso:** compra-se demais de um item que gira pouco, e o dinheiro fica parado na prateleira
  em vez de pagar contas.

Este projeto usa o **modelo de ponto de pedido com estoque de segurança**, clássico da gestão
de estoques, calculado com o histórico real de vendas de cada produto.

## Como funciona

Para cada produto, com os últimos 90 dias de vendas:

| Conceito | Fórmula | Em português |
|---|---|---|
| Demanda média (**d**) | média das vendas diárias | Quanto vende por dia, **contando os dias sem venda** |
| Variação (**σ**) | desvio padrão das vendas diárias | O quanto um dia costuma fugir da média |
| Estoque de segurança | **Z × σ × √LT** | Colchão para aguentar dias de venda acima do normal durante a entrega |
| Ponto de pedido | **d × LT + estoque de segurança** | Quando o estoque chegar aqui, é hora de pedir |
| Quantidade a comprar | **d × (LT + cobertura) + ES − estoque** | Arredondada para cima no lote do fornecedor |

**LT** é o *lead time*, os dias que o fornecedor leva para entregar. **Z** vem do nível de
serviço escolhido: com 95%, Z = 1,645, e a chance de faltar produto durante a espera é de 5%.

Além disso:

- **Curva ABC:** classifica os produtos pelo valor vendido. Os de classe A (os primeiros 80% do
  faturamento) aparecem primeiro na lista de compras.
- **Situação de cada produto:** ruptura, comprar agora, atenção (vai precisar em até 7 dias),
  OK, excesso (estoque para mais de 90 dias) ou sem giro (tem estoque, mas não vendeu nada).
- **Capital parado:** quanto dinheiro está em estoque acima do necessário.

### Decisões técnicas

- **Dias sem venda contam na média.** Se um produto vendeu 50 unidades num único dia em 10
  dias, a demanda é 5 por dia, não 50. Ignorar os zeros é um erro comum que infla as compras.
- **Venda concentrada pede mais segurança.** Dois produtos com a mesma média podem precisar de
  estoques de segurança bem diferentes. É o desvio padrão que captura isso.
- **`BigDecimal` para dinheiro, `double` para estatística.** Média e desvio padrão são
  estimativas, e `double` basta. Valor de compra e capital parado precisam ser exatos.
- **Desvio padrão amostral (n − 1) implementado à mão**, sem bibliotecas, para deixar a conta explícita.
- **Avisos úteis:** vendas de SKUs que não estão no cadastro são listadas, não ignoradas em silêncio.

## Como rodar

Pré-requisitos: **Java 21** e **Maven**.

```bash
git clone https://github.com/gabriellapresbitero/reposicao-estoque.git
cd reposicao-estoque
mvn package

java -jar target/reposicao.jar --produtos exemplos/produtos.csv --vendas exemplos/vendas.csv
```

Resultado com o exemplo (um mercadinho fictício, 20 produtos e 90 dias de vendas):

```
==============================================================================
REPOSIÇÃO DE ESTOQUE: análise até 30/09/2025
==============================================================================
Ruptura: sem estoque e com demanda                      1 produto(s)
Comprar agora: abaixo do ponto de pedido                5 produto(s)
Atenção: vai precisar comprar em até 7 dias             6 produto(s)
OK                                                      6 produto(s)
Excesso: estoque para mais tempo que o necessário       1 produto(s)
Sem giro: tem estoque, mas não vendeu no período        1 produto(s)

Investimento sugerido em compras: R$ 27.391,12
Capital parado (excesso + sem giro): R$ 1.194,20

LISTA DE COMPRAS (mais urgente primeiro)
------------------------------------------------------------------------------
SKU      Produto                        ABC  Estoque  P.Ped. Comprar         Valor
FEI1     Feijão carioca 1kg               A        0     107     760   R$ 5.692,40  ← em falta!
ARR5     Arroz tipo 1 5kg                 A       60     101     520  R$ 12.948,00
CAF5     Café torrado 500g                A       30      89     372   R$ 6.249,60
BIS4     Biscoito cream cracker 400g      B       35      54     260   R$ 1.375,40
CHO1     Chocolate em pó 200g             C        4      25      72     R$ 640,80
CRE1     Creme dental 90g                 C       60      65     108     R$ 484,92

DINHEIRO PARADO NO ESTOQUE
------------------------------------------------------------------------------
DET5     Detergente 500ml                  R$ 239,00  estoque para 120 dias
PAN5     Panetone 500g                     R$ 955,20  não vendeu no período
```

O arquivo `sugestao_compras.csv` traz a análise completa dos 20 produtos (demanda, desvio,
estoque de segurança, ponto de pedido, cobertura, classe ABC...). Ele abre direto no Excel ou no **Power BI**.

### Arquivos de entrada

**`produtos.csv`**: `sku;nome;estoque_atual;lead_time_dias;custo_unitario;lote_minimo` (o lote é opcional, padrão 1)

**`vendas.csv`**: `data;sku;quantidade`, uma linha por produto por dia (ou por venda, porque as linhas do mesmo dia são somadas)

Os nomes das colunas aceitam acentos e maiúsculas ("Custo Unitário", "Lead Time (dias)").

### Opções

| Opção | Padrão | Para quê |
|---|---|---|
| `--nivel-servico P` | 95 | 80, 85, 90, 95, 97.5, 98, 99 ou 99.9 |
| `--cobertura N` | 30 | Dias de venda que cada compra deve garantir além do lead time |
| `--janela N` | 90 | Dias de histórico usados nos cálculos |
| `--data AAAA-MM-DD` | última venda | Último dia do histórico considerado |
| `--saida ARQUIVO` | `sugestao_compras.csv` | CSV com a análise completa |

## Testes

```bash
mvn test
```

São 30 testes com JUnit. Eles conferem as fórmulas com números calculados à mão, os dias sem
venda entrando na média, as vendas fora da janela sendo ignoradas, cada uma das 6 situações, o
arredondamento por lote, a curva ABC nas bordas (o produto que "cruza" os 80%), a leitura dos CSVs e
que um nível de serviço maior realmente pede mais estoque de segurança.

## Estrutura

```
src/main/java/io/github/gabriellapresbitero/estoque/
├── Main.java                       # linha de comando
├── modelo/                         # Produto, Venda, Sugestao, Situacao, ClasseAbc
├── leitura/                        # Csv (leitor genérico) e Leitores
├── analise/                        # CalculadoraReposicao, CurvaAbc, Estatisticas, NivelServico
└── relatorio/                      # RelatorioCsv e Resumo
exemplos/                           # mercadinho fictício
```

## Próximos passos

- [ ] Considerar sazonalidade (ex.: panetone só vende em dezembro)
- [ ] Variação do lead time do fornecedor no estoque de segurança
- [ ] Juntar as sugestões por fornecedor para montar o pedido

---
Feito por **Gabriella Presbítero** · Licença MIT · Os dados de exemplo são fictícios.
