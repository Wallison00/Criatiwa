package com.walli.flexcriatiwa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    existingItems: List<OrderItem>,
    onCancelOrder: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onSendToKitchen: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val cartItems = orderViewModel.currentCartItems
    val destinationType = orderViewModel.destinationType
    val newPayments = orderViewModel.payments // Pagamentos adicionados AGORA nesta tela
    val occupiedTables by kitchenViewModel.occupiedTables.collectAsState(initial = emptySet())
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState(initial = emptyMap())

    val allItemsToShow = existingItems + cartItems
    val totalItems = allItemsToShow.sumOf { it.quantity }
    val totalPrice = allItemsToShow.sumOf { it.singleItemTotalPrice * it.quantity }

    val currentTableId = orderViewModel.tableSelection.firstOrNull()
    val activeOrdersForTable = if (currentTableId != null) ordersByTable[currentTableId] ?: emptyList() else emptyList()

    // Lógica de Pagamento
    val existingPaymentsValue = activeOrdersForTable.sumOf { order -> order.payments.sumOf { it.amount } }
    val newPaymentsValue = newPayments.sumOf { it.amount }
    val totalPaid = existingPaymentsValue + newPaymentsValue
    val remainingAmount = totalPrice - totalPaid
    val isFullyPaid = remainingAmount <= 0.01 // Margem de erro float

    val canCloseBill = activeOrdersForTable.any { it.status == OrderStatus.DELIVERED }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDestinationDialog by remember { mutableStateOf(false) }
    var showCancellationDialog by remember { mutableStateOf(false) }
    var showJustificationDialog by remember { mutableStateOf(false) } // Dialog da justificativa

    val canSendOrder = cartItems.isNotEmpty() && destinationType != null

    // --- DIALOGS ---

    if (showJustificationDialog) {
        var justificationText by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showJustificationDialog = false },
            title = { Text("Justificar Pendência") },
            text = {
                Column {
                    Text("O valor total não foi pago. Restante: R$ ${"%.2f".format(remainingAmount)}", color = Color.Red)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = justificationText,
                        onValueChange = { justificationText = it; isError = false },
                        label = { Text("Motivo do não pagamento") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) Text("Obrigatório informar o motivo.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (justificationText.isBlank()) {
                            isError = true
                        } else {
                            // Fecha com justificativa
                            kitchenViewModel.closeBillWithDetails(activeOrdersForTable, newPayments, justificationText)
                            orderViewModel.clearAll() // Limpa pagamentos locais
                            showJustificationDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirmar Fechamento")
                }
            },
            dismissButton = { TextButton(onClick = { showJustificationDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDestinationDialog) {
        DestinationDialog(
            initialDestinationType = orderViewModel.destinationType,
            initialTableSelection = orderViewModel.tableSelection,
            initialClientName = orderViewModel.clientName,
            occupiedTables = occupiedTables,
            onDismiss = { showDestinationDialog = false },
            onConfirm = { newDest, newTables, newClient ->
                orderViewModel.updateDestination(newDest, newTables, newClient)
                showDestinationDialog = false
            }
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(
            totalOrderPrice = totalPrice,
            currentPayments = newPayments, // Mostra apenas os novos pagamentos para editar
            alreadyPaidAmount = existingPaymentsValue, // Passa o que já foi pago antes
            onDismiss = { showPaymentDialog = false },
            onConfirm = { updatedPayments ->
                orderViewModel.updatePayments(updatedPayments)
                showPaymentDialog = false
            },
            onAddPayment = { payment -> orderViewModel.addPayment(payment) },
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
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                actions = { TextButton(onClick = { showCancellationDialog = true }) { Text("Cancelar") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total: R$ ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (totalPaid > 0) {
                            Text("Pago: R$ ${"%.2f".format(totalPaid)}", style = MaterialTheme.typography.bodyMedium, color = if(isFullyPaid) Color(0xFF4CAF50) else Color.Red)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canSendOrder) {
                            Button(
                                onClick = onSendToKitchen,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Enviar")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Send, null)
                            }
                        }

                        if (canCloseBill && !canSendOrder) {
                            Button(
                                onClick = {
                                    if (isFullyPaid) {
                                        // Fecha direto se estiver tudo pago
                                        kitchenViewModel.closeBillWithDetails(activeOrdersForTable, newPayments, null)
                                        orderViewModel.clearAll()
                                        onNavigateBack()
                                    } else {
                                        // Abre diálogo de justificativa
                                        showJustificationDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isFullyPaid) Color(0xFF1976D2) else Color(0xFFE91E63)) // Azul se pago, Rosa se pendente
                            ) {
                                Text("Fechar Conta")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.AttachMoney, null)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            if (existingItems.isNotEmpty()) {
                item { Text("Na Mesa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                items(existingItems.size) { i -> ExistingOrderItemCard(existingItems[i]) }
                item { HorizontalDivider() }
            }

            if (cartItems.isNotEmpty()) {
                item { Text("Novos Itens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(cartItems.size) { i ->
                    OrderItemCard(
                        orderItem = cartItems[i],
                        onQuantityChange = { qtd -> if (qtd <= 0) orderViewModel.removeItem(cartItems[i]) else orderViewModel.upsertItem(cartItems[i].copy(quantity = qtd), cartItems[i]) },
                        onEdit = { onEditItem(cartItems[i]) }
                    )
                }
            }

            item {
                TextButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AddCircleOutline, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar Item", color = Color(0xFF4CAF50))
                }
            }

            item {
                SummarySectionCard(
                    title = "Local:",
                    info = if (destinationType == "Local" && orderViewModel.tableSelection.isNotEmpty()) "Mesa ${orderViewModel.tableSelection.joinToString()}" else destinationType ?: "Selecione",
                    onEditClick = { showDestinationDialog = true }
                )
            }
            item {
                // Mostra total pago (Antigo + Novo)
                SummarySectionCard(
                    title = "Pagamento:",
                    info = if (totalPaid > 0) "R$ ${"%.2f".format(totalPaid)}" else "Pendente",
                    onEditClick = { showPaymentDialog = true }
                )
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun ExistingOrderItemCard(orderItem: OrderItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${orderItem.quantity}x ${orderItem.menuItem.name}", fontWeight = FontWeight.Bold)
            if (orderItem.removedIngredients.isNotEmpty()) Text("Sem: ${orderItem.removedIngredients.joinToString()}", color = Color.Red, fontSize = 12.sp)
            if (orderItem.additionalIngredients.isNotEmpty()) Text("Add: ${orderItem.additionalIngredients.keys.joinToString()}", color = Color(0xFF388E3C), fontSize = 12.sp)
            orderItem.meatDoneness?.let { Text("Ponto: $it", color = Color.Blue, fontSize = 12.sp) }
            orderItem.observations?.takeIf { it.isNotBlank() }?.let { Text("Obs: $it", fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderItemCard(orderItem: OrderItem, onQuantityChange: (Int) -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onEdit).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("# ${orderItem.menuItem.name}", fontWeight = FontWeight.Bold)
                if (orderItem.removedIngredients.isNotEmpty()) Text("- Sem: ${orderItem.removedIngredients.joinToString()}", color = Color.Red, fontSize = 12.sp)
                if (orderItem.additionalIngredients.isNotEmpty()) Text("+ Com: ${orderItem.additionalIngredients.keys.joinToString()}", color = Color(0xFF388E3C), fontSize = 12.sp)
                orderItem.meatDoneness?.let { Text("Ponto: $it", fontSize = 12.sp) }
                orderItem.observations?.takeIf { it.isNotBlank() }?.let { Text("Obs: $it", fontSize = 12.sp) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R$ ${"%.2f".format(orderItem.singleItemTotalPrice * orderItem.quantity)}", fontWeight = FontWeight.Bold)
                OrderQuantitySelector(
                    quantity = orderItem.quantity,
                    onIncrease = { onQuantityChange(orderItem.quantity + 1) },
                    onDecrease = { onQuantityChange(orderItem.quantity - 1) }
                )
            }
        }
    }
}

@Composable
fun OrderQuantitySelector(quantity: Int, onIncrease: () -> Unit, onDecrease: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Remove, null, Modifier.size(16.dp)) }
        Text("$quantity", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)) }
    }
}

@Composable
fun DestinationDialog(onDismiss: () -> Unit, initialDestinationType: String?, initialTableSelection: Set<Int>, initialClientName: String?, occupiedTables: Set<Int>, onConfirm: (String, Set<Int>, String) -> Unit) {
    var selectedTab by remember { mutableStateOf(initialDestinationType ?: "Local") }
    var selectedTables by remember { mutableStateOf(initialTableSelection) }
    var clientName by remember { mutableStateOf(initialClientName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Destino") },
        text = {
            Column {
                Row {
                    FilterChip(selected = selectedTab == "Local", onClick = { selectedTab = "Local" }, label = { Text("Local") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = selectedTab == "Viagem", onClick = { selectedTab = "Viagem" }, label = { Text("Viagem") })
                }
                Spacer(Modifier.height(16.dp))
                if (selectedTab == "Local") {
                    Text("Mesas:")
                    LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(20) { idx ->
                            val num = idx + 1
                            val isOccupied = occupiedTables.contains(num) && !initialTableSelection.contains(num)
                            val isSelected = selectedTables.contains(num)
                            Button(
                                onClick = { selectedTables = if (isSelected) selectedTables - num else selectedTables + num },
                                enabled = !isOccupied,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFF4CAF50) else if(isOccupied) Color.Red else Color.Gray),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("$num") }
                        }
                    }
                } else {
                    OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Nome Cliente") })
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedTab, selectedTables, clientName) }) { Text("OK") } }
    )
}

@Composable
fun SummarySectionCard(title: String, info: String, onEditClick: () -> Unit) {
    Card(onClick = onEditClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, color = Color.Gray); Text(info, fontWeight = FontWeight.Bold) }
            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PaymentDialog(
    totalOrderPrice: Double,
    currentPayments: List<SplitPayment>,
    alreadyPaidAmount: Double, // Novo parâmetro para saber o que já foi pago no DB
    onDismiss: () -> Unit,
    onConfirm: (List<SplitPayment>) -> Unit,
    onAddPayment: (SplitPayment) -> Unit,
    onClearPayments: () -> Unit
) {
    var paymentFormat by remember { mutableStateOf(if (currentPayments.isNotEmpty()) "Dividir Conta" else "Integral") }
    var selectedPaymentMethod by remember { mutableStateOf("Dinheiro") }
    var amountValue by remember { mutableStateOf("") }

    val totalPaidCurrentSession = currentPayments.sumOf { it.amount }
    val totalPaidGlobally = alreadyPaidAmount + totalPaidCurrentSession

    val remainingAmount = (totalOrderPrice.toBigDecimal() - totalPaidGlobally.toBigDecimal()).toDouble()

    val currentAmountInput = (amountValue.toLongOrNull() ?: 0L) / 100.0
    val isFullyPaid = remainingAmount <= 0.01 // Margem segura

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagamento") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Se já estiver pago ou parcialmente pago, não mostra opção "Integral" confusa
                // Se já pagou tudo no banco, mostra aviso
                if (alreadyPaidAmount >= totalOrderPrice) {
                    item { Text("Este pedido já foi totalmente pago.", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) }
                } else {
                    item {
                        Row {
                            FilterChip(selected = paymentFormat == "Integral", onClick = { paymentFormat = "Integral"; onClearPayments() }, label = { Text("Integral") })
                            Spacer(Modifier.width(8.dp))
                            FilterChip(selected = paymentFormat == "Dividir Conta", onClick = { paymentFormat = "Dividir Conta" }, label = { Text("Dividir") })
                        }
                    }

                    if (!isFullyPaid || paymentFormat == "Integral") {
                        if (paymentFormat == "Dividir Conta") {
                            item {
                                OutlinedTextField(
                                    value = amountValue,
                                    onValueChange = { str -> if (str.all { it.isDigit() }) amountValue = str },
                                    label = { Text("Valor (Restante: R$ ${"%.2f".format(if(remainingAmount < 0) 0.0 else remainingAmount)})") },
                                    visualTransformation = CurrencyVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Dinheiro", "Crédito", "Débito", "Pix").forEach { method ->
                                    FilterChip(selected = selectedPaymentMethod == method, onClick = { selectedPaymentMethod = method }, label = { Text(method) })
                                }
                            }
                        }
                    }
                }

                if (currentPayments.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text("Pagamentos Agora:", fontWeight = FontWeight.Bold)
                        currentPayments.forEach {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(it.method)
                                Text("R$ ${"%.2f".format(it.amount)}")
                            }
                        }
                    }
                }

                if (alreadyPaidAmount > 0) {
                    item {
                        HorizontalDivider()
                        Text("Pagos Anteriormente: R$ ${"%.2f".format(alreadyPaidAmount)}", color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paymentFormat == "Integral") {
                        // Paga apenas o que FALTA (Total - Já Pago)
                        val amountToPay = if(remainingAmount > 0) remainingAmount else 0.0
                        if (amountToPay > 0) {
                            onConfirm(listOf(SplitPayment(amountToPay, selectedPaymentMethod)))
                        } else {
                            onDismiss() // Nada a pagar
                        }
                    } else if (isFullyPaid) {
                        onConfirm(currentPayments)
                    } else {
                        if (currentAmountInput > 0) {
                            onAddPayment(SplitPayment(currentAmountInput, selectedPaymentMethod))
                            amountValue = ""
                        }
                    }
                },
                enabled = paymentFormat == "Integral" || isFullyPaid || (currentAmountInput > 0)
            ) {
                Text(if (isFullyPaid || paymentFormat == "Integral") "Confirmar" else "Adicionar")
            }
        }
    )
}

@Composable
fun CancellationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cancelar?") }, text = { Text("Perder alterações?") }, confirmButton = { Button(onClick = { onConfirm(); onDismiss() }) { Text("Sim") } })
}