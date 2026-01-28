package com.walli.flexcriatiwa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


// --- TELA DE RESUMO DO PEDIDO ---

// Em OrderScreen.kt
// SUBSTITUA A FUNÇÃO INTEIRA

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderScreen(
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel, // <-- Adicionado para acessar as mesas
    existingItems: List<OrderItem>,
    onCancelOrder: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onSendToKitchen: () -> Unit
) {
    // --- OS ESTADOS AGORA VÊM DO VIEWMODEL ---
    val cartItems = orderViewModel.currentCartItems
    val destinationType = orderViewModel.destinationType
    val payments = orderViewModel.payments

    // Coleta o estado das mesas ocupadas
    val occupiedTables by kitchenViewModel.occupiedTables.collectAsState()

    val allItemsToShow = existingItems + cartItems // Combina com os itens existentes
    val totalItems = allItemsToShow.sumOf { it.quantity }
    val totalPrice = allItemsToShow.sumOf { it.singleItemTotalPrice * it.quantity }


    // Estados dos diálogos (continuam locais)
    var showCancellationDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDestinationDialog by remember { mutableStateOf(false) }

    // Lógica de validação do botão "Enviar para Cozinha"
    val isButtonEnabled = if (existingItems.isNotEmpty()) {
        cartItems.isNotEmpty() || payments.isNotEmpty()
    } else {
        cartItems.isNotEmpty() && destinationType != null
    }

    // --- CHAMADAS AOS DIÁLOGOS ATUALIZADAS ---
    if (showDestinationDialog) {
        DestinationDialog(
            initialDestinationType = orderViewModel.destinationType,
            initialTableSelection = orderViewModel.tableSelection,
            initialClientName = orderViewModel.clientName,
            occupiedTables = occupiedTables, // Passa o estado coletado
            onDismiss = { showDestinationDialog = false },
            onConfirm = { newDestinationType, newTables, newClientName ->
                orderViewModel.updateDestination(newDestinationType, newTables, newClientName)
                showDestinationDialog = false
            }
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(
            totalOrderPrice = totalPrice,
            currentPayments = payments,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { newPayments ->
                orderViewModel.updatePayments(newPayments)
                showPaymentDialog = false
            },
            onAddPayment = { newPayment ->
                orderViewModel.addPayment(newPayment)
            },
            onClearPayments = {
                orderViewModel.clearPayments()
            }
        )
    }

    if (showCancellationDialog) {
        CancellationDialog(
            onDismiss = { showCancellationDialog = false },
            onConfirm = onCancelOrder
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pedido", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showCancellationDialog = true }) {
                        Text("Cancelar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "R$ ${"%.2f".format(totalPrice)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "/ $totalItems itens",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.alignByBaseline()
                        )
                    }

                    Button(
                        onClick = onSendToKitchen, // Apenas chama a lambda
                        enabled = isButtonEnabled,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("Enviar para Cozinha", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- LÓGICA DE EXIBIÇÃO DOS ITENS ---
            if (existingItems.isNotEmpty()) {
                item {
                    Text(
                        "Itens na Mesa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                items(existingItems) { item ->
                    ExistingOrderItemCard(orderItem = item)
                }
                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            item {
                Text(
                    if (existingItems.isNotEmpty()) "Novos Itens" else "Itens",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(cartItems) { orderItem -> // Itera sobre os itens do carrinho
                OrderItemCard(
                    orderItem = orderItem,
                    onQuantityChange = { newQuantity ->
                        if (newQuantity <= 0) {
                            orderViewModel.removeItem(orderItem)
                        } else {
                            orderViewModel.upsertItem(orderItem.copy(quantity = newQuantity), originalItem = orderItem)
                        }
                    },
                    onEdit = { onEditItem(orderItem) }
                )
            }

            item {
                TextButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Adicionar", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Item", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Resumo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                val isDestinationDefined = destinationType != null
                val destinationInfo = when (destinationType) {
                    "Local" -> if (orderViewModel.tableSelection.isNotEmpty()) "Local - Mesa(s) ${orderViewModel.tableSelection.joinToString(", ")}" else "Local"
                    "Viagem" -> if (!orderViewModel.clientName.isNullOrBlank()) "Viagem - Cliente: ${orderViewModel.clientName}" else "Viagem"
                    else -> "Não definido"
                }

                SummarySectionCard(
                    title = "Local de Consumo:",
                    info = destinationInfo,
                    isDefined = isDestinationDefined,
                    onEditClick = { showDestinationDialog = true }
                )
            }

            item {
                val isPaymentDefined = payments.isNotEmpty()
                val paymentInfo = when {
                    !isPaymentDefined -> "Pendente"
                    payments.size == 1 -> payments.first().method
                    else -> "Múltiplos (${payments.size} pagamentos)"
                }

                SummarySectionCard(
                    title = "Forma de Pagamento:",
                    info = paymentInfo,
                    isDefined = isPaymentDefined,
                    onEditClick = { showPaymentDialog = true }
                )
            }
        }
    }
}

// ADICIONE ESTE NOVO COMPONENTE no final do arquivo OrderScreen.kt
@Composable
fun ExistingOrderItemCard(orderItem: OrderItem) {
    // Card simplificado, cinza, não interativo, apenas para visualização
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${orderItem.quantity}x ${orderItem.menuItem.name}",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge
        )
        // Você pode adicionar mais detalhes aqui se quiser (sem, com, etc.)
    }
}


// No seu arquivo OrderScreen.kt, substitua toda a função OrderItemCard por esta:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemCard(
    orderItem: OrderItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        // O onClick foi removido daqui!
    ) {
        // Column principal que divide o card em duas seções
        Column {
            // --- 1. ÁREA SUPERIOR (CLICÁVEL PARA EDIÇÃO) ---
            Column(
                modifier = Modifier
                    .clickable(onClick = onEdit) // Apenas esta área é clicável
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "# ${orderItem.menuItem.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val labelStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                val valueStyle = SpanStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (orderItem.removedIngredients.isNotEmpty()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(labelStyle) { append("- Sem: ") }
                            withStyle(valueStyle.copy(color = Color.Red)) {
                                append(orderItem.removedIngredients.joinToString(", "))
                            }
                        }
                    )
                }

                if (orderItem.additionalIngredients.isNotEmpty()) {
                    val additionalIngredientsText = orderItem.additionalIngredients.map { (name, quantity) ->
                        if (quantity > 1) "$name (x$quantity)" else name
                    }.joinToString(", ")
                    Text(
                        buildAnnotatedString {
                            withStyle(labelStyle) { append("+ Com: ") }
                            withStyle(valueStyle.copy(color = Color(0xFF388E3C))) {
                                append(additionalIngredientsText)
                            }
                        }
                    )
                }

                orderItem.meatDoneness?.let {
                    Text(
                        buildAnnotatedString {
                            withStyle(labelStyle) { append("Ponto: ") }
                            withStyle(valueStyle.copy(color = labelStyle.color)) { append(it) }
                        }
                    )
                }

                orderItem.observations?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        buildAnnotatedString {
                            withStyle(labelStyle) { append("Observação: ") }
                            withStyle(valueStyle.copy(color = labelStyle.color)) { append("\"$it\"") }
                        }
                    )
                }
            }

            // --- 2. ÁREA INFERIOR (PREÇO E CONTADOR) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R$ ${"%.2f".format(orderItem.singleItemTotalPrice * orderItem.quantity)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    CompactQuantitySelector(
                        quantity = orderItem.quantity,
                        onIncrease = { onQuantityChange(orderItem.quantity + 1) },
                        onDecrease = { onQuantityChange(orderItem.quantity - 1) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}




// --- PREVIEW PARA VISUALIZAÇÃO ---

@Preview(showBackground = true)
@Composable
fun OrderScreenPreview() {
    val previewOrderViewModel = viewModel<OrderViewModel>()
    val previewKitchenViewModel = viewModel<KitchenViewModel>()

    OrderScreen(
        orderViewModel = previewOrderViewModel,
        kitchenViewModel = previewKitchenViewModel,
        existingItems = emptyList(), // Fornece uma lista vazia para o preview
        onCancelOrder = {},
        onAddItem = {},
        onEditItem = {},
        onSendToKitchen = {} // A chamada simplificada
    )
}

// No final do arquivo OrderScreen.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDialog(
    onDismiss: () -> Unit,
    initialDestinationType: String?,
    initialTableSelection: Set<Int>,
    initialClientName: String?,
    occupiedTables: Set<Int>,
    onConfirm: (destinationType: String, tables: Set<Int>, clientName: String) -> Unit // TODO: Passar os dados do destino para a confirmação
) {
    // --- ESTADOS INTERNOS DO DIALOG ---
    var selectedTab by remember { mutableStateOf(initialDestinationType ?: "Local") }
    var selectedTables by remember { mutableStateOf(initialTableSelection) }
    var clientName by remember { mutableStateOf(initialClientName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        // --- CONTEÚDO DO DIALOG ---
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center // Centraliza o título
            ) {
                Text("Destino", fontWeight = FontWeight.Bold)
                // Botão de fechar no canto direito
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar"
                    )
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start // Alinha filhos à direita
                ){
                    // CÓDIGO CORRIGIDO
                    // --- SELEÇÃO COM CHIPS ---
                    Text(
                        text = "Área de Consumo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Uma cor mais suave
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    // Chip para "Local"
                    FilterChip(
                        selected = selectedTab == "Local",
                        onClick = { selectedTab = "Local" },
                        label = { Text("Local") },
                        leadingIcon = if (selectedTab == "Local") {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Selecionado",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )

                    // Chip para "Viagem"
                    FilterChip(
                        selected = selectedTab == "Viagem",
                        onClick = { selectedTab = "Viagem" },
                        label = { Text("Viagem") },
                        leadingIcon = if (selectedTab == "Viagem") {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Selecionado",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- CONTEÚDO QUE MUDA CONFORME A ABA ---
                when (selectedTab) {
                    "Local" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start // Alinha filhos à direita
                        ) {
                            Text("Selecionar Mesa(s)", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                items(16, key = { tableNumber ->
                                    val number = tableNumber + 1
                                    val isSelected = selectedTables.contains(number)
                                    val isOccupied = occupiedTables.contains(number)
                                    // A chave única que força a recomposição
                                    "$number-$isSelected-$isOccupied"
                                }) { tableNumber ->
                                    val number = tableNumber + 1
                                    val isSelected = selectedTables.contains(number)
                                    val isOccupied = occupiedTables.contains(number)
                                    val isClickable = !isOccupied || isSelected

                                    val colors = when {
                                        isSelected -> ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        isOccupied -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        else -> ButtonDefaults.buttonColors()
                                    }

                                    Button(
                                        onClick = {
                                            if (isClickable) {
                                                selectedTables = if (isSelected) selectedTables - number else selectedTables + number
                                            }
                                        },
                                        enabled = isClickable,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = colors
                                    ) {
                                        Text(text = "%02d".format(number), maxLines = 1)
                                    }
                                }

                            }
                        }
                    }
                    "Viagem" -> {
                        // Campos de texto para viagem
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Identificação do Cliente",
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Nome do Cliente") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                // --- MUDANÇA AQUI: ADICIONANDO O ÍCONE DE LIMPAR ---
                                trailingIcon = {
                                    // Mostra o ícone apenas se o campo não estiver vazio
                                    if (clientName.isNotEmpty()) {
                                        IconButton(onClick = { clientName = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Limpar campo"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        // --- BOTÕES DE AÇÃO DO DIALOG ---
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center // Centraliza o botão na Row
            ) {
                Button(
                    onClick = {
                        onConfirm(selectedTab, selectedTables, clientName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth(0.6f) // Ocupa 60% da largura
                ) {
                    Text("Continuar")
                }
            }
        },
        dismissButton = null
    )
}

// No final do seu arquivo, substitua a função SummarySectionCard por esta versão mais simples:

@Composable
fun SummarySectionCard(
    title: String,
    info: String, // Voltamos a usar um único parâmetro 'info'
    isDefined: Boolean,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // Alinhamento central fica melhor com uma linha só
        ) {
            // Coluna com as informações
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Exibe a string de informação única
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            // --- LÓGICA DO BOTÃO DINÂMICO ---
            val buttonText = if (isDefined) "Editar" else "Adicionar"
            val buttonColor = Color(0xFF4CAF50) // Verde para ambos, para consistência

            TextButton(onClick = onEditClick) {
                // Mostra o ícone apenas se a informação já foi definida
                if (isDefined) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(18.dp),
                        tint = buttonColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(buttonText, color = buttonColor)
            }
        }
    }
}


// No seu arquivo OrderScreen.kt, substitua a função PaymentDialog por esta:

@Composable
fun PaymentDialog(
    totalOrderPrice: Double,
    currentPayments: List<SplitPayment>,
    onDismiss: () -> Unit,
    onConfirm: (payments: List<SplitPayment>) -> Unit,
    // --- NOVO PARÂMETRO PARA ADICIONAR PAGAMENTOS ---
    onAddPayment: (payment: SplitPayment) -> Unit,
    // --- NOVO PARÂMETRO PARA LIMPAR PAGAMENTOS ---
    onClearPayments: () -> Unit
) {
    // --- ESTADOS INTERNOS DO DIÁLOGO ---
    var paymentFormat by remember {
        mutableStateOf(
            if (currentPayments.size == 1 && currentPayments.first().amount == totalOrderPrice) "Integral"
            else if (currentPayments.isNotEmpty()) "Dividir Conta"
            else "Integral"
        )
    }
    var selectedPaymentMethod by remember { mutableStateOf("Dinheiro") }
    var amountValue by remember { mutableStateOf("") }

    // --- LÓGICA DE VALIDAÇÃO (agora usa currentPayments) ---
    val totalPaid = currentPayments.sumOf { it.amount }
    val remainingAmount = (totalOrderPrice.toBigDecimal() - totalPaid.toBigDecimal()).toDouble()
    val currentAmountInput = (amountValue.toLongOrNull() ?: 0L) / 100.0
    val isFullyPaid = remainingAmount <= 0.0

    val isConfirmButtonEnabled = when {
        paymentFormat == "Integral" -> true
        isFullyPaid -> true
        else -> currentAmountInput > 0 && currentAmountInput <= remainingAmount
    }

    val paymentFormats = listOf("Integral", "Dividir Conta")
    val paymentMethods = listOf("Dinheiro", "Crédito", "Débito", "Pix")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Pagamento(s)", fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    SectionWithChips(
                        title = "Formato de Pagamento",
                        options = paymentFormats,
                        selectedOption = paymentFormat,
                        onOptionSelected = {
                            paymentFormat = it
                            if (it == "Integral") {
                                // --- MUDANÇA AQUI: CHAMA A FUNÇÃO PARA LIMPAR ---
                                onClearPayments()
                            }
                        },
                        // A seção só é habilitada se NENHUM pagamento tiver sido feito ainda.
                        enabled = currentPayments.isEmpty()
                    )
                }

                // Oculta os campos de valor e forma de pagamento se a conta já estiver paga
                if (!isFullyPaid || paymentFormat == "Integral") {
                    if (paymentFormat == "Dividir Conta") {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Valor", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Restante: R$ ${"%.2f".format(remainingAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                OutlinedTextField(
                                    value = amountValue,
                                    onValueChange = { newText ->
                                        val digits = newText.filter { it.isDigit() }
                                        val newValue = (digits.toLongOrNull() ?: 0L) / 100.0
                                        if (newValue <= remainingAmount) {
                                            amountValue = digits
                                        }
                                    },
                                    placeholder = { Text("R$ 0,00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = CurrencyVisualTransformation()
                                )
                            }
                        }
                    }

                    // Oculta a seção de forma de pagamento se a conta estiver paga
                    if (!isFullyPaid) {
                        item {
                            SectionWithChips(
                                title = "Forma de Pagamento",
                                options = paymentMethods,
                                selectedOption = selectedPaymentMethod,
                                onOptionSelected = { selectedPaymentMethod = it },
                                // --- E MUDANÇA AQUI ---
                                enabled = !isFullyPaid
                            )
                        }
                    }
                }

                // --- MUDANÇA AQUI: A LISTA DE PAGAMENTOS AGORA APARECE SEMPRE QUE HOUVER PAGAMENTOS ---
                if (currentPayments.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Pagamentos Adicionados", fontWeight = FontWeight.Bold)
                            currentPayments.forEach { payment ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${payment.method}:")
                                    Text("R$ ${"%.2f".format(payment.amount)}")
                                }
                            }
                            Divider(modifier = Modifier.padding(top = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pago:", fontWeight = FontWeight.Bold)
                                Text("R$ ${"%.2f".format(totalPaid)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        when {
                            // Se a conta está paga, a única ação possível é confirmar e fechar.
                            isFullyPaid -> onConfirm(currentPayments)

                            paymentFormat == "Dividir Conta" -> {
                                val newPayment = SplitPayment(amount = currentAmountInput, method = selectedPaymentMethod)
                                // --- MUDANÇA CRÍTICA AQUI ---
                                // Em vez de modificar um estado local, chama a função para elevar o estado.
                                onAddPayment(newPayment)
                                amountValue = "" // Limpa o campo de valor local
                            }

                            paymentFormat == "Integral" -> {
                                val integralPayment = SplitPayment(amount = totalOrderPrice, method = selectedPaymentMethod)
                                onConfirm(listOf(integralPayment))
                            }
                        }
                    },
                    enabled = isConfirmButtonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    // --- MUDANÇA AQUI: TEXTO DO BOTÃO TOTALMENTE DINÂMICO ---
                    Text(text = if (isFullyPaid) "Confirmar" else "Adicionar Pagamento")
                }
            }
        },
        dismissButton = null
    )
}




// No final do seu arquivo, substitua a função SectionWithChips por esta versão corrigida:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionWithChips(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true // --- NOVO PARÂMETRO, PADRÃO É HABILITADO ---
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                FilterChip(
                    selected = isSelected,
                    onClick = { if (enabled) onOptionSelected(option) },
                    label = { Text(text = option) },
                    enabled = enabled,
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Selecionado",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        // Cores para o estado desabilitado
                        disabledContainerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f)
                    ),
                    // --- CORREÇÃO APLICADA AQUI ---
                    border = if (isSelected) {
                        null // Sem borda quando selecionado
                    } else {
                        // Criamos o BorderStroke diretamente
                        BorderStroke(
                            width = 1.dp,
                            color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                        )
                    }
                )
            }
        }
    }
}

// Adicione esta classe no final do seu arquivo OrderScreen.kt

class CurrencyVisualTransformation(
    private val currencySymbol: String = "R$ ",
    private val decimalSeparator: Char = ',',
    private val thousandsSeparator: Char = '.'
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // O estado armazena apenas os dígitos. Ex: "12345"
        val digitsOnly = text.text.filter { it.isDigit() }

        // Converte para um número para formatação. Se vazio, é 0.
        val intValue = digitsOnly.toLongOrNull() ?: 0L

        // Formata o número para ter pelo menos 3 dígitos (para incluir os centavos)
        // Ex: 1 -> "001", 12 -> "012", 123 -> "123"
        val formattedNumber = intValue.toString().padStart(3, '0')

        // Separa a parte inteira dos centavos
        val integerPart = formattedNumber.dropLast(2)
        val decimalPart = formattedNumber.takeLast(2)

        // Adiciona os separadores de milhar à parte inteira
        val formattedIntegerPart = integerPart
            .reversed()
            .chunked(3)
            .joinToString(separator = thousandsSeparator.toString())
            .reversed()

        // Monta a string final
        val maskedText = currencySymbol + formattedIntegerPart + decimalSeparator + decimalPart

        // Mapeia as posições do texto original para o texto formatado
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Mapeia a posição do cursor para o final do texto formatado
                return maskedText.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Mapeia a posição do cursor do texto formatado de volta para o original
                return digitsOnly.length
            }
        }

        return TransformedText(AnnotatedString(maskedText), offsetMapping)
    }
}

// Adicione esta data class no final do arquivo
data class SplitPayment(val amount: Double, val method: String)

// Adicione este novo Composable no final do arquivo

@Composable
fun CancellationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // Título do diálogo
        title = {
            Text(
                text = "Cancelar Pedido",
                fontWeight = FontWeight.Bold
            )
        },
        // Texto principal com a pergunta
        text = {
            Text("Você tem certeza que deseja cancelar este pedido? Todos os itens serão perdidos.")
        },
        // Botão de confirmação ("Sim")
        confirmButton = {
            Button(
                onClick = {
                    onConfirm() // Executa a ação de confirmação (navegar para trás)
                    onDismiss() // Fecha o diálogo
                },
                // Usamos cores destrutivas (vermelho) para indicar uma ação perigosa
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Sim, Cancelar")
            }
        },
        // Botão para dispensar ("Não")
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Não")
            }
        }
    )
}
