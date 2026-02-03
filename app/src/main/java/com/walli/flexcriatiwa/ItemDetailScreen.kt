package com.walli.flexcriatiwa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    product: MenuItem,
    productIngredients: List<String>,
    productOptionals: List<OptionalItem>,
    availableOptionals: List<OptionalItem>,
    onNavigateBack: () -> Unit,
    orderViewModel: OrderViewModel,
    onNavigateToOrder: () -> Unit
) {
    val itemBeingEdited = orderViewModel.itemToEdit
    val isEditMode = itemBeingEdited != null

    var removedIngredients by remember {
        mutableStateOf(itemBeingEdited?.removedIngredients ?: emptyList())
    }

    var additionalQuantities by remember { mutableStateOf(itemBeingEdited?.additionalIngredients ?: mapOf()) }
    var meatDoneness by remember { mutableStateOf(itemBeingEdited?.meatDoneness ?: "Ao Ponto") }
    var observations by remember { mutableStateOf(itemBeingEdited?.observations ?: "") }
    var quantity by remember { mutableStateOf(itemBeingEdited?.quantity ?: 1) }

    DisposableEffect(Unit) {
        onDispose { orderViewModel.clearEdit() }
    }

    // Cálculo de preço
    val totalAdditionalPrice = additionalQuantities.entries.fold(0.0) { acc, entry ->
        val optional = availableOptionals.find { it.name == entry.key }
        val price = optional?.price ?: 0.0
        acc + (price * entry.value.toDouble())
    }
    val totalPrice = (product.price + totalAdditionalPrice) * quantity.toDouble()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Item", fontWeight = FontWeight.Bold) },
                navigationIcon = { },
                actions = {
                    TextButton(onClick = onNavigateBack) { Text("Cancelar") }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier.fillMaxHeight().alignByBaseline(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        QuantitySelector(
                            quantity = quantity,
                            onIncrease = { quantity++ },
                            onDecrease = { if (quantity > 1) quantity-- },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val ingredientesRealmenteAdicionados = additionalQuantities.filter { (_, qtd) -> qtd > 0 }

                            val updatedItem = OrderItem(
                                menuItem = product,
                                quantity = quantity,
                                removedIngredients = removedIngredients,
                                additionalIngredients = ingredientesRealmenteAdicionados,
                                meatDoneness = meatDoneness,
                                observations = observations.takeIf { it.isNotBlank() },
                                singleItemTotalPrice = (product.price + totalAdditionalPrice)
                            )

                            orderViewModel.upsertItem(updatedItem, originalItem = itemBeingEdited)
                            onNavigateToOrder()
                        },
                        enabled = quantity > 0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                            .fillMaxHeight()
                            .alignByBaseline(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditMode) "Atualizar" else "Adicionar",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "R$ ${"%.2f".format(totalPrice)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- MUDANÇA AQUI: Título com Código Sequencial ---
                item {
                    val titleText = if (product.code > 0) {
                        "#%03d - %s".format(product.code, product.name) // Ex: #001 - X-Bacon
                    } else {
                        product.name // Caso seja produto legado sem código
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Remover Ingredientes
                if (productIngredients.isNotEmpty()) {
                    item {
                        DetailSection(title = "Remover Ingredientes") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                productIngredients.forEach { ingredient ->
                                    val isSelected = removedIngredients.contains(ingredient)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            removedIngredients = if (isSelected) {
                                                removedIngredients - ingredient
                                            } else {
                                                removedIngredients + ingredient
                                            }
                                        },
                                        label = { Text(ingredient) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Remove, "Removido", Modifier.size(FilterChipDefaults.IconSize)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Adicionar Ingredientes
                if (productOptionals.isNotEmpty()) {
                    item {
                        DetailSection(title = "Adicionar Ingredientes") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                productOptionals.forEach { optional ->
                                    val currentQuantity = additionalQuantities[optional.name] ?: 0
                                    AdditionalIngredientRow(
                                        ingredient = optional,
                                        quantity = currentQuantity,
                                        onQuantityChange = { newQuantity ->
                                            additionalQuantities = additionalQuantities + (optional.name to newQuantity)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Ponto da Carne
                item {
                    DetailSection(title = "Ponto da Carne") {
                        val donenessOptions = listOf("Mal Passada", "Ao Ponto", "Bem Passada")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            donenessOptions.forEach { option ->
                                val isSelected = meatDoneness == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { meatDoneness = option },
                                    label = { Text(option) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Filled.Done, "Selecionado", Modifier.size(FilterChipDefaults.IconSize)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Observações
                item {
                    DetailSection(title = "Observações") {
                        OutlinedTextField(
                            value = observations,
                            onValueChange = { if (it.length <= 250) observations = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ex: Tirar cebola, maionese à parte...") },
                            minLines = 4,
                            supportingText = {
                                Text(
                                    text = "${observations.length} / 250",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// Componentes Auxiliares
@Composable
fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        content()
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onDecrease) { Icon(Icons.Default.Remove, "Diminuir") }
        Text(text = "%02d".format(quantity), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onIncrease) { Icon(Icons.Default.Add, "Aumentar") }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdditionalIngredientRow(
    ingredient: OptionalItem,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "+ R$ ${"%.2f".format(ingredient.price)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        AnimatedContent(
            targetState = quantity > 0,
            label = "AddRemoveAnimation",
            transitionSpec = { fadeIn() + scaleIn(initialScale = 0.8f) togetherWith fadeOut() + scaleOut(targetScale = 0.8f) }
        ) { isQuantityGreaterThanZero ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = if (!isQuantityGreaterThanZero) Modifier.clickable { onQuantityChange(1) } else Modifier
            ) {
                if (isQuantityGreaterThanZero) {
                    CompactQuantitySelector(
                        quantity = quantity,
                        onIncrease = { onQuantityChange(quantity + 1) },
                        onDecrease = { onQuantityChange(quantity - 1) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, "Adicionar")
                    }
                }
            }
        }
    }
}

// Renomeei para evitar conflito com a outra tela se necessário, mas mantive a lógica igual
@Composable
fun CompactQuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        val iconButtonModifier = Modifier.size(32.dp)
        IconButton(onClick = onDecrease, modifier = iconButtonModifier) { Icon(Icons.Default.Remove, "Diminuir") }
        Text(text = "%02d".format(quantity), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onIncrease, modifier = iconButtonModifier) { Icon(Icons.Default.Add, "Aumentar") }
    }
}