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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderScreen(
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    existingItems: List<OrderItem>,
    onCancelOrder: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onSendToKitchen: () -> Unit,
    onNavigateBack: () -> Unit // <--- Parâmetro para o botão voltar
) {
    val cartItems = orderViewModel.currentCartItems
    val destinationType = orderViewModel.destinationType
    val payments = orderViewModel.payments

    val occupiedTables by kitchenViewModel.occupiedTables.collectAsState(initial = emptySet())

    val allItemsToShow = existingItems + cartItems
    val totalItems = allItemsToShow.sumOf { it.quantity }
    val totalPrice = allItemsToShow.sumOf { it.singleItemTotalPrice * it.quantity }

    var showCancellationDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDestinationDialog by remember { mutableStateOf(false) }

    val isButtonEnabled = if (existingItems.isNotEmpty()) {
        cartItems.isNotEmpty() || payments.isNotEmpty()
    } else {
        cartItems.isNotEmpty() && destinationType != null
    }

    // --- DIÁLOGOS (Mantidos iguais) ---
    if (showDestinationDialog) {
        DestinationDialog(
            initialDestinationType = orderViewModel.destinationType,
            initialTableSelection = orderViewModel.tableSelection,
            initialClientName = orderViewModel.clientName,
            occupiedTables = occupiedTables,
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
            onAddPayment = { newPayment -> orderViewModel.addPayment(newPayment) },
            onClearPayments = { orderViewModel.clearPayments() }
        )
    }

    if (showCancellationDialog) {
        CancellationDialog(onDismiss = { showCancellationDialog = false }, onConfirm = onCancelOrder)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pedido", fontWeight = FontWeight.Bold) },
                // --- BOTÃO DE VOLTAR CONFIGURADO ---
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = { TextButton(onClick = { showCancellationDialog = true }) { Text("Cancelar") } }
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
                        Text(text = "R$ ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.alignByBaseline())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "/ $totalItems itens", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.alignByBaseline())
                    }

                    Button(
                        onClick = onSendToKitchen,
                        enabled = isButtonEnabled,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("Enviar", color = Color.White)
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
            // --- ITENS JÁ ENVIADOS (MESA) ---
            if (existingItems.isNotEmpty()) {
                item {
                    Text(
                        "Itens na Mesa (Já enviados)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(existingItems) { item ->
                    // Agora usamos o visual detalhado para itens existentes também
                    ExistingOrderItemCard(orderItem = item)
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // --- NOVOS ITENS ---
            item { Text(if (existingItems.isNotEmpty()) "Adicionando Agora" else "Itens do Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

            items(cartItems) { orderItem ->
                OrderItemCard(
                    orderItem = orderItem,
                    onQuantityChange = { newQuantity ->
                        if (newQuantity <= 0) orderViewModel.removeItem(orderItem)
                        else orderViewModel.upsertItem(orderItem.copy(quantity = newQuantity), originalItem = orderItem)
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

            item { Spacer(modifier = Modifier.height(16.dp)); Text("Resumo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

            item {
                val isDestinationDefined = destinationType != null
                val destinationInfo = when (destinationType) {
                    "Local" -> if (orderViewModel.tableSelection.isNotEmpty()) "Local - Mesa(s) ${orderViewModel.tableSelection.sorted().joinToString(", ")}" else "Local"
                    "Viagem" -> if (!orderViewModel.clientName.isNullOrBlank()) "Viagem - Cliente: ${orderViewModel.clientName}" else "Viagem"
                    else -> "Não definido"
                }
                SummarySectionCard(title = "Local de Consumo:", info = destinationInfo, isDefined = isDestinationDefined, onEditClick = { showDestinationDialog = true })
            }

            item {
                val isPaymentDefined = payments.isNotEmpty()
                val paymentInfo = when {
                    !isPaymentDefined -> "Pendente"
                    payments.size == 1 -> payments.first().method
                    else -> "Múltiplos (${payments.size} pagamentos)"
                }
                SummarySectionCard(title = "Forma de Pagamento:", info = paymentInfo, isDefined = isPaymentDefined, onEditClick = { showPaymentDialog = true })
            }
        }
    }
}

// --- CARD DETALHADO PARA ITENS EXISTENTES ---
@Composable
fun ExistingOrderItemCard(orderItem: OrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Título
            Text(
                text = "${orderItem.quantity}x ${orderItem.menuItem.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Detalhes (Sem, Com, Ponto, Obs) - MESMA LÓGICA DO CARD NOVO
            val labelStyle = SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            val valueStyle = SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)

            if (orderItem.removedIngredients.isNotEmpty()) {
                Text(buildAnnotatedString {
                    withStyle(labelStyle) { append("Sem: ") }
                    withStyle(valueStyle.copy(color = Color.Red)) { append(orderItem.removedIngredients.joinToString(", ")) }
                })
            }
            if (orderItem.additionalIngredients.isNotEmpty()) {
                val addText = orderItem.additionalIngredients.map { (name, qtd) -> if (qtd > 1) "$name (x$qtd)" else name }.joinToString(", ")
                Text(buildAnnotatedString {
                    withStyle(labelStyle) { append("Com: ") }
                    withStyle(valueStyle.copy(color = Color(0xFF388E3C))) { append(addText) }
                })
            }
            orderItem.meatDoneness?.let {
                Text(buildAnnotatedString {
                    withStyle(labelStyle) { append("Ponto: ") }
                    withStyle(valueStyle) { append(it) }
                })
            }
            orderItem.observations?.takeIf { it.isNotBlank() }?.let {
                Text(buildAnnotatedString {
                    withStyle(labelStyle) { append("Obs: ") }
                    withStyle(valueStyle) { append(it) }
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemCard(orderItem: OrderItem, onQuantityChange: (Int) -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Column(
                modifier = Modifier.clickable(onClick = onEdit).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "# ${orderItem.menuItem.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val labelStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                val valueStyle = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)

                if (orderItem.removedIngredients.isNotEmpty()) {
                    Text(buildAnnotatedString { withStyle(labelStyle) { append("- Sem: ") }; withStyle(valueStyle.copy(color = Color.Red)) { append(orderItem.removedIngredients.joinToString(", ")) } })
                }
                if (orderItem.additionalIngredients.isNotEmpty()) {
                    val addText = orderItem.additionalIngredients.map { (name, qtd) -> if (qtd > 1) "$name (x$qtd)" else name }.joinToString(", ")
                    Text(buildAnnotatedString { withStyle(labelStyle) { append("+ Com: ") }; withStyle(valueStyle.copy(color = Color(0xFF388E3C))) { append(addText) } })
                }
                orderItem.meatDoneness?.let { Text(buildAnnotatedString { withStyle(labelStyle) { append("Ponto: ") }; withStyle(valueStyle.copy(color = labelStyle.color)) { append(it) } }) }
                orderItem.observations?.takeIf { it.isNotBlank() }?.let { Text(buildAnnotatedString { withStyle(labelStyle) { append("Observação: ") }; withStyle(valueStyle.copy(color = labelStyle.color)) { append("\"$it\"") } }) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "R$ ${"%.2f".format(orderItem.singleItemTotalPrice * orderItem.quantity)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(50), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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

// Mantenha as outras funções auxiliares (DestinationDialog, PaymentDialog, CancellationDialog, etc.) que já estavam no arquivo original.
// Se precisar, posso reenviá-las, mas elas não mudaram.
@Composable
fun DestinationDialog(
    onDismiss: () -> Unit,
    initialDestinationType: String?,
    initialTableSelection: Set<Int>,
    initialClientName: String?,
    occupiedTables: Set<Int>,
    onConfirm: (destinationType: String, tables: Set<Int>, clientName: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialDestinationType ?: "Local") }
    var selectedTables by remember { mutableStateOf(initialTableSelection) }
    var clientName by remember { mutableStateOf(initialClientName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Destino", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Filled.Close, "Fechar") }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("Área de Consumo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedTab == "Local", onClick = { selectedTab = "Local" }, label = { Text("Local") }, leadingIcon = if (selectedTab == "Local") { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null)
                    FilterChip(selected = selectedTab == "Viagem", onClick = { selectedTab = "Viagem" }, label = { Text("Viagem") }, leadingIcon = if (selectedTab == "Viagem") { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null)
                }
                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    "Local" -> {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text("Selecionar Mesa(s)", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(16, key = { "$it-${selectedTables.contains(it + 1)}-${occupiedTables.contains(it + 1)}" }) { index ->
                                    val number = index + 1
                                    val isSelected = selectedTables.contains(number)
                                    val isOccupied = occupiedTables.contains(number)
                                    val isClickable = !isOccupied || isSelected
                                    Button(
                                        onClick = { if (isClickable) selectedTables = if (isSelected) selectedTables - number else selectedTables + number },
                                        enabled = isClickable,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = when {
                                            isSelected -> ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            isOccupied -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            else -> ButtonDefaults.buttonColors()
                                        }
                                    ) { Text("%02d".format(number)) }
                                }
                            }
                        }
                    }
                    "Viagem" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Identificação do Cliente", fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Nome do Cliente") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = if (clientName.isNotEmpty()) { { IconButton(onClick = { clientName = "" }) { Icon(Icons.Filled.Close, "Limpar") } } } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { onConfirm(selectedTab, selectedTables, clientName) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.fillMaxWidth(0.6f)) { Text("Continuar") }
            }
        },
        dismissButton = null
    )
}

@Composable
fun SummarySectionCard(title: String, info: String, isDefined: Boolean, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = info, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onEditClick) {
                if (isDefined) { Icon(Icons.Filled.Edit, "Editar", Modifier.size(18.dp), tint = Color(0xFF4CAF50)); Spacer(modifier = Modifier.width(4.dp)) }
                Text(if (isDefined) "Editar" else "Adicionar", color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun PaymentDialog(
    totalOrderPrice: Double,
    currentPayments: List<SplitPayment>,
    onDismiss: () -> Unit,
    onConfirm: (List<SplitPayment>) -> Unit,
    onAddPayment: (SplitPayment) -> Unit,
    onClearPayments: () -> Unit
) {
    var paymentFormat by remember { mutableStateOf(if (currentPayments.size == 1 && currentPayments.first().amount == totalOrderPrice) "Integral" else if (currentPayments.isNotEmpty()) "Dividir Conta" else "Integral") }
    var selectedPaymentMethod by remember { mutableStateOf("Dinheiro") }
    var amountValue by remember { mutableStateOf("") }

    val totalPaid = currentPayments.sumOf { it.amount }
    val remainingAmount = (totalOrderPrice.toBigDecimal() - totalPaid.toBigDecimal()).toDouble()
    val currentAmountInput = (amountValue.toLongOrNull() ?: 0L) / 100.0
    val isFullyPaid = remainingAmount <= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Pagamento(s)", fontWeight = FontWeight.Bold); IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Filled.Close, "Fechar") } } },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item { SectionWithChips(title = "Formato de Pagamento", options = listOf("Integral", "Dividir Conta"), selectedOption = paymentFormat, onOptionSelected = { paymentFormat = it; if (it == "Integral") onClearPayments() }, enabled = currentPayments.isEmpty()) }

                if (!isFullyPaid || paymentFormat == "Integral") {
                    if (paymentFormat == "Dividir Conta") {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Valor", fontWeight = FontWeight.Bold); Text("Restante: R$ ${"%.2f".format(remainingAmount)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                OutlinedTextField(
                                    value = amountValue,
                                    onValueChange = { newText -> val digits = newText.filter { it.isDigit() }; val newValue = (digits.toLongOrNull() ?: 0L) / 100.0; if (newValue <= remainingAmount) amountValue = digits },
                                    placeholder = { Text("R$ 0,00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = CurrencyVisualTransformation()
                                )
                            }
                        }
                    }
                    if (!isFullyPaid) {
                        item { SectionWithChips(title = "Forma de Pagamento", options = listOf("Dinheiro", "Crédito", "Débito", "Pix"), selectedOption = selectedPaymentMethod, onOptionSelected = { selectedPaymentMethod = it }, enabled = !isFullyPaid) }
                    }
                }

                if (currentPayments.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Pagamentos Adicionados", fontWeight = FontWeight.Bold)
                            currentPayments.forEach { payment -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${payment.method}:"); Text("R$ ${"%.2f".format(payment.amount)}") } }
                            Divider(modifier = Modifier.padding(top = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Pago:", fontWeight = FontWeight.Bold); Text("R$ ${"%.2f".format(totalPaid)}", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = {
                        when {
                            isFullyPaid -> onConfirm(currentPayments)
                            paymentFormat == "Dividir Conta" -> { onAddPayment(SplitPayment(currentAmountInput, selectedPaymentMethod)); amountValue = "" }
                            paymentFormat == "Integral" -> onConfirm(listOf(SplitPayment(totalOrderPrice, selectedPaymentMethod)))
                        }
                    },
                    enabled = paymentFormat == "Integral" || isFullyPaid || (currentAmountInput > 0 && currentAmountInput <= remainingAmount),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) { Text(if (isFullyPaid) "Confirmar" else "Adicionar Pagamento") }
            }
        },
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionWithChips(title: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                FilterChip(
                    selected = isSelected,
                    onClick = { if (enabled) onOptionSelected(option) },
                    label = { Text(option) },
                    enabled = enabled,
                    leadingIcon = if (isSelected) { { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                    border = if (isSelected) null else BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
                )
            }
        }
    }
}

@Composable
fun CancellationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancelar Pedido", fontWeight = FontWeight.Bold) },
        text = { Text("Você tem certeza que deseja cancelar este pedido? Todos os itens serão perdidos.") },
        confirmButton = { Button(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Sim, Cancelar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Não") } }
    )
}