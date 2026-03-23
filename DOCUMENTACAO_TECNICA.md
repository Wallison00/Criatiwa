# 📘 FlexCriatiwa - Documentação Técnica e Arquitetural (Guia para IA)

> **ATENÇÃO IA:** Este documento é o **MAIOR PONTO DE VERDADE** do sistema. Sempre que for fazer qualquer alteração, inclusão de nova funcionalidade ou refatoração, consulte este arquivo para garantir que não haja quebra de dependências cruzadas.

## 1. Visão Geral do Sistema
O **FlexCriatiwa** é um sistema completo e moderno para gestão de estabelecimentos comerciais (focado em restaurantes, bares e lanchonetes). Foi desenhado para rodar nativamente em Android utilizando tecnologias estado-da-arte, operando de maneira unificada entre diferentes perfis de usuários (Gestor/Dono, Balcão/Caixa e Cozinha) sob um modelo "Offline-First".

### 🛠️ Tecnologias Principais:
1.  **Linguagem**: Kotlin (Totalmente nativo).
2.  **Interface de Usuário**: Jetpack Compose (Design responsivo e dinâmico, totalmente guiado por estados).
3.  **Banco de Dados & Backend**: Google Firebase (Firestore para dados em tempo real + Firebase Auth).
4.  **Integração de Câmera/Scanner**: Biblioteca GMS Vision / CodeScanner (para leitura ágil de código de barras e QR Codes).
5.  **Impressão de Cupom**: `EscPosPrinter` nativo (Integração com impressoras térmicas via rede IP / Bluetooth).
6.  **Gerenciamento de Imagens**: Coil (exibição assíncrona) e CâmeraX/Intents para captura. **O sistema não faz upload de arquivos binários para o Storage**. Todas as imagens são convertidas e comprimidas localmente para Base64 (`data:image/jpeg;base64,...`) e transferidas diretamente como *String* dentro dos documentos do Firestore.

---

## 2. Modus Operandi "Offline-First"
A arquitetura baseia-se fortemente em "Pessimistic UI" para regras cruciais ou "Optimistic UI" para entradas rápidas. Como a conexão da rede em cozinhas e salões costuma oscilar:
*  **Firestore Cache**: Todos o sistema usa os `SnapshotListeners` locais do Firebase. Operações de salvamento direto usam de métodos sem travadores de `await()`. **JAMAIS USE `.await()` no Firebase em fluxos de criação como Produtos ou Insumos**, pois interrupções momentâneas de rede causarão suspensão infinita (travando modais com a animação de loading girando eternamente).
*  No Firebase enviamos as requisições (Ex: `ref.add(data).addOnFailureListener{}`) e deixamos os snapshots se encarregarem de atualizar as listas de `LazyColumn`.

---

## 3. Modelo de Dados e Banco (Firestore Rules)
Tudo é centralizado sob a hierarquia de `Companies` (Empresas).

### Estrutura do Banco `databases/{database}/documents`
*   `/users/{uid}` -> Cada usuário (funcionário ou gerente). Cada usuário aponta de qual empresa ele é através do campo `companyId`.
*   `/companies/{companyId}` -> O "nó central" de uma empresa.
    *   `/products` -> Cadastro de Produtos de venda (Burger, Pizza). Usam categoria, ingredientes e opcionais em lista. A foto é Base64.
    *   `/stock` -> Estoque de insumos base (Lata de óleo, KG de Farinha). Possui unidade de medida, código de barras e alerta de mínimo. A foto é Base64.
    *   `/orders` -> Central de pedidos (Pendentes, Em Preparo, Prontos). Os itens lidos pela "Cozinha" e enviados pelo "Balcão".
    *   `/settings` -> Estrutura e categorias visuais do menu, lista condicional de produtos.
    *   `/config/payments` -> Parâmetros de máquina de cartão/Mercado Pago.

### Regras de Negócio de Permissão
* Nenhum dispositivo avulso pode entrar em uma empresa. Ele deve ler um **QR Code** gerado no painel do administrador ou informar o código da empresa. 
* Após isso, ele precisará fazer "Login" logando com Google, caindo num modo "PENDENTE". O Administrador deve ir na tela de Equipe e **autorizar o membro**, mudando o `status` dele para `active` na coleção `users`.

---

## 4. O Coração: Módulos do Sistema

### A) Design do Salão (`TableScreen` & `TableLayoutConfigScreen`)
* A planta baixa interativa.
* **Componentes**: Mesas arrastáveis (`square`, `round`, `rectangle`).
* **Visual Premium**: Foi criado um desenho em Canvas muito complexo chamado **Chair Arcade** onde as cadeiras possuem extremidades de couro preto e encosto curvado de madeira encaixando magneticamente (exatamente nas bordas) de tampos de madeira polida.
*   **Dependência Cuidado**: Não mude o tamanho fixo da visualização ou a lógica de matemática do clique no canvas. As posições (X e Y) são mapeadas de forma relativa no grid.

### B) Gestão de Insumos (`StockManagementScreen`)
* Focado em controlar produtos base.
* Possui leitor de código de barras no mesmo modal, mudando a visão em tempo real.
*   **Dependência Cuidado**: O formulário nunca trava em espera da nuvem. Ele é assíncrono. Imagens acima de 60% de qualidade geram Base64 massivos que invalidam a cota do Firebase. 

### C) Gestão de Cardápio (`ProductManagementScreen`)
*   Produtos possuem dependência intrínseca de `Categoria`. 
*   **Atenção**: Se as categorias não existirem no `/settings/menu_structure`, o usuário tem seu acesso à adição daquele produto revogados preventivamente.

### D) Fila da Cozinha (`KitchenScreen`)
* Sistema "Kanban" automatizado (A Fazer -> Fazendo -> Pronto).
* Reage via Socket/Listener disparando imediatamente quando qualquer dispositivo no salão/balcão envia o pedido para a cozinha. 

### E) Impressora Externa (`PrinterConfigScreen` / `EscPosPrinter`)
* Responsável pela via do cliente e da cozinha.
* Envia protocolos `ESC/POS` de impressoras térmicas (EPSON, Daruma, etc) via conexão por debrifing em Socket TCP/IP (Endereço e Porta).

---

## 5. Práticas e Condutas para IA no Projeto
1.  **Modificação Sensível**: Se for alterar uma `DataClass` (como `Company`, `ManagedProduct`, `StockItem`), verifique onde ELA é instanciada e principalmente nos `update()` ou `set()` diretos pro Firestore, caso contrário o banco ficará inconsistente.
2.  **Separação de Contexto**: A UI de Composable não possui lógicas bloqueantes, tudo roda em `remember` ou derivado de um *ViewModel* (como `ManagementViewModel` e `AuthViewModel`).
3.  **Cor: Material 3**: Respeitar o `MaterialTheme.colorScheme` e priorizar designs premium (`CardDefaults.cardElevation`) ao gerar novas interfaces para manter o estado premium da aplicação. 

***

## 6. Dicionário de Dados (Campos e Tipos)
Abaixo estão os principais modelos do Firebase armazenados nas sub-coleções de `companies/{companyId}`:

### 🧩 `products` (ManagedProduct)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | String | ID do documento auto-gerado pelo Firebase. |
| `code` | Number (Int) | Código numérico sequencial do produto. |
| `name` | String | Nome de exibição principal. |
| `price` | Number (Double) | Preço de venda. |
| `imageUrl` | String | Foto convertida para string `data:image/jpeg;base64...`. |
| `isActive` | Boolean | Se o produto aparece para venda. |
| `category` | String | Chave estrangeira ligando ao `settings/menu_structure`. |
| `ingredients` | Array / List<String> | Ingredientes pertencentes ao produto. |
| `optionals` | Array de Maps | Lista contendo `name` (String) e `price` (Number). |

### 📦 `stock` (StockItem)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | String | ID do documento auto-gerado. |
| `name` | String | Nome interno do insumo. |
| `barcode` | String | Código de barras global para leitura por câmera. |
| `quantity` | Number (Double) | Quantidade atual no restaurante. |
| `minQuantity` | Number (Double) | Alerta. Se `quantity` <= este valor, dispara visual na tela. |
| `unit` | String | "Unidade", "Kg", "Grama", "Litro", etc. |
| `imageUrl` | String | Foto convertida para string `data:image/jpeg;base64...`. |

### 🗺️ `tablePositions` dentro de Company (TablePosition)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `x` / `y` | Number (Float) | Posições relativas no grid do Canvas. |
| `shape` | String | Formato: `square`, `round` ou `rectangle`. |
| `seats` | Number (Int) | Quantos assentos estão ativos ao redor da mesa. |
| `rotation` | Number (Float) | Grau de rotação da mesa (`0`, `90`, `180`, `270`, etc). |

---

## 7. Fluxos do Sistema (Workflows Principais)

### ⏩ Fluxo 1: Onboarding de Funcionário
1. O Usuário acessa sua conta Google na Home.
2. É perguntado "Você deseja Ingressar em uma empresa ou Criar uma?". O usuário seleciona **Ingressar**.
3. Abre-se a `QRScannerScreen` habilitando a Câmera.
4. O usuário aponta o aparelho para o celular de um funcionário autorizado e escaneia o código da empresa.
5. O estado do usuário é definido globalmente como `pending_approval`.
6. Um gestor acessa `ManagementHubScreen -> Equipe`, localiza o nome do usuário e pressiona Aprovar (atribuindo a _role_ "employee" ou "manager").
7. A tela do funcionário atualiza via Snapshots e destrava o aplicativo.

### ⏩ Fluxo 2: Construção do Salão de Mesas
1. O Gestor acessa a Aba **Desenhar / Configuração**.
2. Acessa o Menu Retrátil na esquerda.
3. Arrastar a forma de Mesa Desejada (Drag and Drop) para o espaço quadriculado.
4. Ao clicar na mesa na tela:
   - Pode **Girar** o ângulo.
   - Pode **Adicionar/Remover Cadeiras**.
   - Pode **Trocar o Formato**. 
5. As edições são manipuladas de forma volátil dentro do `OrderViewModel` e publicadas no nó `companies/tablePositions` sem recarregar a tela.

### ⏩ Fluxo 3: Salvar Imagens Nativamente e Offline
1. O Gestor ou Funcionário tira a Foto do prato ou insumo.
2. A foto preenche um State do composable como `Uri`.
3. Ao clicar em **Salvar**, o software chama a função auxiliar `compressUriToBase64` (disponibilizada no `ManagementViewModel.kt`).
4. O dado Bitmap é cortado sem gerar arquivo físico local.
5. A string de Bytes é enviada diretamente no payload `.add()` ou `.set()`. 
6. Como não há bloqueio com invocação de `.await()`, o usuário vê o item salvo instantaneamente (por causa do cachê offline do Firebase) e o próprio SDK do banco resolve o handshake com os servidores na nuvem em background.

***
*Documento vivo. Se novas infraestruturas vitais entrarem (ex: Migração Global de Banco), atualize esta página imediatamente.*
