package com.walli.flexcriatiwa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    existingOrders: List<KitchenOrder>,
    onCancelOrder: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (OrderItem) -> Unit,
    onSendToKitchen: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val cartItems = orderViewModel.currentCartItems
    val destinationType = orderViewModel.destinationType
    val newPayments = orderViewModel.payments
    val occupiedTables by kitchenViewModel.occupiedTables.collectAsState(initial = emptySet())
    val ordersByTable by kitchenViewModel.ordersByTable.collectAsState(initial = emptyMap())

    // Calcula itens totais para exibição de preço (Itens já feitos + Carrinho)
    val existingItemsFlat = existingOrders.flatMap { it.items }
    val allItemsToShow = existingItemsFlat + cartItems
    val totalPrice = allItemsToShow.sumOf { it.singleItemTotalPrice * it.quantity }

    val currentTableId = orderViewModel.tableSelection.firstOrNull()
    val activeOrdersForTable = if (currentTableId != null) ordersByTable[currentTableId] ?: emptyList() else emptyList()

    // Lógica de Pagamento
    val existingPaymentsValue = activeOrdersForTable.sumOf { order -> order.payments.sumOf { it.amount } }
    val newPaymentsValue = newPayments.sumOf { it.amount }
    val totalPaid = existingPaymentsValue + newPaymentsValue
    val remainingAmount = (totalPrice.toBigDecimal() - totalPaid.toBigDecimal()).toDouble()
    val isFullyPaid = remainingAmount <= 0.01

    val canCloseBill = activeOrdersForTable.any { it.status != OrderStatus.FINISHED }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDestinationDialog by remember { mutableStateOf(false) }
    var showCancellationDialog by remember { mutableStateOf(false) }
    var showJustificationDialog by remember { mutableStateOf(false) }

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
                    Text("Falta pagar: R$ ${"%.2f".format(if(remainingAmount < 0) 0.0 else remainingAmount)}", color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = justificationText, onValueChange = { justificationText = it; isError = false },
                        label = { Text("Motivo do não pagamento") }, isError = isError, modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) Text("Obrigatório informar o motivo.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (justificationText.isBlank()) { isError = true } else {
                        kitchenViewModel.closeBillWithDetails(activeOrdersForTable, newPayments, justificationText)
                        orderViewModel.clearAll()
                        showJustificationDialog = false
                        onNavigateBack()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Confirmar Fechamento") }
            },
            dismissButton = { TextButton(onClick = { showJustificationDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDestinationDialog) {
        // CORREÇÃO AQUI: Usando argumentos nomeados para garantir a ordem correta
        DestinationDialog(
            onDismiss = { showDestinationDialog = false },
            initialDestinationType = orderViewModel.destinationType,
            initialTableSelection = orderViewModel.tableSelection,
            initialClientName = orderViewModel.clientName,
            occupiedTables = occupiedTables,
            onConfirm = { newDest, newTables, newClient ->
                orderViewModel.updateDestination(newDest, newTables, newClient)
                showDestinationDialog = false
            }
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(totalPrice, newPayments, existingPaymentsValue, { showPaymentDialog = false }, { orderViewModel.updatePayments(it); showPaymentDialog = false }, { orderViewModel.addPayment(it) }, { orderViewModel.clearPayments() })
    }

    if (showCancellationDialog) { CancellationDialog({ showCancellationDialog = false }, onCancelOrder) }

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
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total: R$ ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (totalPaid > 0) Text("Pago: R$ ${"%.2f".format(totalPaid)}", style = MaterialTheme.typography.bodyMedium, color = if(isFullyPaid) Color(0xFF4CAF50) else Color.Red)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canSendOrder) {
                            Button(onClick = onSendToKitchen, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                                Text("Enviar"); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Send, null)
                            }
                        }
                        if (canCloseBill && !canSendOrder) {
                            Button(onClick = {
                                if (isFullyPaid) { kitchenViewModel.closeBillWithDetails(activeOrdersForTable, newPayments, null); orderViewModel.clearAll(); onNavigateBack() }
                                else { showJustificationDialog = true }
                            }, colors = ButtonDefaults.buttonColors(containerColor = if (isFullyPaid) Color(0xFF1976D2) else Color(0xFFE91E63))) {
                                Text("Fechar Conta"); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.AttachMoney, null)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // --- AGRUPAMENTO POR STATUS (VISUALIZAR PEDIDOS SEPARADOS) ---

            // 1. Pedidos Prontos (Verde)
            val readyItems = existingOrders.filter { it.status == OrderStatus.READY }.flatMap { it.items }
            if (readyItems.isNotEmpty()) {
                item { StatusHeader("Pronto para Entrega", Color(0xFF4CAF50), Icons.Default.CheckCircle) }
                items(readyItems) { ExistingOrderItemCard(it) }
            }

            // 2. Pedidos Preparando (Laranja)
            val preparingItems = existingOrders.filter { it.status == OrderStatus.PREPARING }.flatMap { it.items }
            if (preparingItems.isNotEmpty()) {
                item { StatusHeader("Na Cozinha / Preparando", Color(0xFFFF9800), Icons.Default.Schedule) }
                items(preparingItems) { ExistingOrderItemCard(it) }
            }

            // 3. Pedidos Entregues (Cinza)
            val deliveredItems = existingOrders.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.NEEDS_CLEANING }.flatMap { it.items }
            if (deliveredItems.isNotEmpty()) {
                item { StatusHeader("Entregues / Consumindo", Color.Gray, Icons.Default.DoneAll) }
                items(deliveredItems) { ExistingOrderItemCard(it) }
            }

            // 4. Novos Itens (Carrinho)
            if (cartItems.isNotEmpty()) {
                item { HorizontalDivider(); Spacer(Modifier.height(8.dp)); Text("Novos Itens (Rascunho)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                items(cartItems) { item ->
                    OrderItemCard(item, { qtd -> if (qtd <= 0) orderViewModel.removeItem(item) else orderViewModel.upsertItem(item.copy(quantity = qtd), item) }, { onEditItem(item) })
                }
            }

            item {
                TextButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AddCircleOutline, null, tint = Color(0xFF4CAF50)); Spacer(Modifier.width(8.dp)); Text("Adicionar Item", color = Color(0xFF4CAF50))
                }
            }

            item { SummarySectionCard("Local:", if (destinationType == "Local" && orderViewModel.tableSelection.isNotEmpty()) "Mesa ${orderViewModel.tableSelection.joinToString()}" else destinationType ?: "Selecione", { showDestinationDialog = true }) }
            item { SummarySectionCard("Pagamento:", if (totalPaid > 0) "R$ ${"%.2f".format(totalPaid)}" else "Pendente", { showPaymentDialog = true }) }
        }
    }
}

// --- COMPONENTES VISUAIS ---

@Composable
fun StatusHeader(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

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
    alreadyPaidAmount: Double,
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

    val remainingAmount = (totalOrderPrice.toBigDecimal() - totalPaidGlobally.toBigDecimal()).max(BigDecimal.ZERO).toDouble()
    val currentAmountInput = (amountValue.toLongOrNull() ?: 0L) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagamento") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total:"); Text("R$ ${"%.2f".format(totalOrderPrice)}", fontWeight = FontWeight.Bold) }
                        if(alreadyPaidAmount > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Já Pago:"); Text("R$ ${"%.2f".format(alreadyPaidAmount)}") }
                        Divider(Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Falta Pagar:", fontWeight = FontWeight.Bold); Text("R$ ${"%.2f".format(remainingAmount)}", fontWeight = FontWeight.Bold, color = if(remainingAmount > 0.01) Color.Red else Color(0xFF4CAF50)) }
                    }
                }

                if (alreadyPaidAmount < totalOrderPrice - 0.01) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = paymentFormat == "Integral", onClick = { paymentFormat = "Integral"; onClearPayments() }, label = { Text("Integral") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = paymentFormat == "Dividir Conta", onClick = { paymentFormat = "Dividir Conta" }, label = { Text("Dividir") }, modifier = Modifier.weight(1f))
                    }
                }

                if (paymentFormat == "Integral" && remainingAmount > 0.01) {
                    Text("Pagamento restante completo: R$ ${"%.2f".format(remainingAmount)}", fontWeight = FontWeight.Bold)
                    PaymentMethodSelector(selectedPaymentMethod) { selectedPaymentMethod = it }
                } else if (paymentFormat == "Dividir Conta") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = amountValue, onValueChange = { str -> if (str.all { it.isDigit() }) amountValue = str },
                            label = { Text("Valor") }, visualTransformation = CurrencyVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { if (currentAmountInput > 0) { onAddPayment(SplitPayment(currentAmountInput, selectedPaymentMethod)); amountValue = "" } }, enabled = currentAmountInput > 0) { Icon(Icons.Outlined.Add, "Add") }
                    }
                    PaymentMethodSelector(selectedPaymentMethod) { selectedPaymentMethod = it }
                    if (currentPayments.isNotEmpty()) {
                        Text("Pagamentos adicionados:", style = MaterialTheme.typography.labelLarge)
                        Column { currentPayments.forEach { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(it.method); Text("R$ ${"%.2f".format(it.amount)}") }; Divider() } }
                    }
                }
            }
        },
        confirmButton = {
            if (paymentFormat == "Integral" && remainingAmount > 0.01) {
                Button(onClick = { onConfirm(listOf(SplitPayment(remainingAmount, selectedPaymentMethod))) }, modifier = Modifier.fillMaxWidth()) { Text("Confirmar Pagamento Total") }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Concluir / Voltar") }
            }
        },
        dismissButton = { if (paymentFormat == "Integral") TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PaymentMethodSelector(selected: String, onSelect: (String) -> Unit) {
    val methods = listOf("Dinheiro", "Crédito", "Débito", "Pix")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        methods.forEach { method -> FilterChip(selected = selected == method, onClick = { onSelect(method) }, label = { Text(method) }) }
    }
}

@Composable
fun CancellationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cancelar?") }, text = { Text("Perder alterações?") }, confirmButton = { Button(onClick = { onConfirm(); onDismiss() }) { Text("Sim") } })
}