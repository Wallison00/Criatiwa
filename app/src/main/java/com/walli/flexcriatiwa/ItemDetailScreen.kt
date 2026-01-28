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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha

import androidx.compose.runtime.DisposableEffect


// --- TELA DE DETALHES DO ITEM ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Adicione ExperimentalLayoutApi
@Composable
fun ItemDetailScreen(
    product: MenuItem, // <-- MUDANÇA: Recebe o produto completo
    productIngredients: List<String>,       // <-- NOVO PARÂMETRO
    productOptionals: List<OptionalItem>,
    availableOptionals: List<OptionalItem>, // <-- MUDANÇA: Recebe a lista de opcionais
    onNavigateBack: () -> Unit,
    orderViewModel: OrderViewModel,
    onNavigateToOrder: () -> Unit
)
{
    val itemBeingEdited = orderViewModel.itemToEdit
    val isEditMode = itemBeingEdited != null

    var removedIngredients by remember { mutableStateOf(itemBeingEdited?.removedIngredients ?: setOf()) }

    // Dentro de ItemDetailScreen, logo após a linha do removedIngredients
    var additionalQuantities by remember { mutableStateOf(itemBeingEdited?.additionalIngredients ?: mapOf()) }

    var meatDoneness by remember { mutableStateOf(itemBeingEdited?.meatDoneness ?: "Ao Ponto") } // <-- ADICIONE ESTA LINHA

    var observations by remember { mutableStateOf(itemBeingEdited?.observations ?: "") }

    var quantity by remember { mutableStateOf(itemBeingEdited?.quantity ?: 1) }

    DisposableEffect(Unit) {
        onDispose {
            orderViewModel.clearEdit()
        }
    }

    if (product == null) {
        Text("Item não encontrado") // Você pode criar uma tela de erro mais elaborada aqui
        return
    }

// --- MOVA OS CÁLCULOS PARA DEPOIS DA VERIFICAÇÃO ---
    // CÓDIGO CORRIGIDO USANDO FOLD
    val totalAdditionalPrice = additionalQuantities.entries.fold(0.0) { acc, entry ->
        // AGORA BUSCA NA LISTA CORRETA DE OPCIONAIS
        val optional = availableOptionals.find { it.name == entry.key }
        val price = optional?.price ?: 0.0
        acc + (price * entry.value.toDouble())
    }
    val totalPrice = (product.price + totalAdditionalPrice) * quantity.toDouble() // <-- Usa product.price

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar( // <-- MUDANÇA AQUI
                title = {
                    Text("Item", fontWeight = FontWeight.Bold) // Título centralizado
                },
                // O navigationIcon fica vazio para não ter nada à esquerda
                navigationIcon = { },
                actions = {
                    // O botão de cancelar continua como uma ação à direita
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancelar")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        // Substitua o bottomBar existente por este:
        bottomBar = {
            // Surface cria o fundo branco com bordas arredondadas para a barra
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp // Adiciona uma sombra para destacar a barra
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
                        modifier = Modifier.fillMaxHeight().alignByBaseline(), // Ocupa a altura máxima
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        QuantitySelector(
                            quantity = quantity,
                            onIncrease = { quantity++ },
                            onDecrease = { if (quantity > 1) quantity-- },
                            // Adicionamos o padding de volta aqui para dar espaço interno
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Botão de Adicionar com Preço
                    Button(
                        // Dentro do Button "Adicionar"
                        onClick = {
                            // Código limpo e final
                            val ingredientesRealmenteAdicionados = additionalQuantities.filter { (_, quantity) ->
                                quantity > 0
                            }

                            val updatedItem = OrderItem(
                                menuItem = product,
                                quantity = quantity,
                                removedIngredients = removedIngredients,
                                additionalIngredients = ingredientesRealmenteAdicionados,
                                meatDoneness = meatDoneness,
                                observations = observations.takeIf { it.isNotBlank() },
                                singleItemTotalPrice = totalPrice / quantity
                            )

                            orderViewModel.upsertItem(updatedItem, originalItem = itemBeingEdited)

                            onNavigateToOrder()
                        },

                        enabled = quantity > 0,

                        // --- PARÂMETRO 2: O MODIFIER ---
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                            .fillMaxHeight()
                            .alignByBaseline(),
                        // --- PARÂMETRO 3: O SHAPE ---
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        // --- PARÂMETRO 4: O CONTENT (O QUE APARECE DENTRO DO BOTÃO) ---
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
        // O Box engloba tanto a lista quanto a barra de rolagem
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // O padding vai para o Box
        ) {
            val listState = rememberLazyListState() // 1. Lembra o estado da lista

            // SUBSTITUA TODA A SUA LAZYCOLUMN POR ESTA VERSÃO COMPLETA

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Nome do Item
                item {
                    Text(
                        text = "${product.id} - ${product.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- CONTEÚDO RESTAURADO ABAIXO ---

                // Seção de Remover Ingredientes
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
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Removido",
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else {
                                            null
                                        },
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

                // Seção de Adicionar Ingredientes
                if (productOptionals.isNotEmpty()) {
                    item {
                        DetailSection(title = "Adicionar Ingredientes") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // --- AQUI ESTÁ A CORREÇÃO ---
                                // Agora, iteramos sobre a lista de opcionais DO PRODUTO
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

                // Seção Ponto da Carne
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
                        }
                    }
                }

                // Seção de Observações
                item {
                    DetailSection(title = "Observações") {
                        OutlinedTextField(
                            value = observations,
                            onValueChange = {
                                if (it.length <= 250) {
                                    observations = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Informe uma breve observação que deve ser aplicada ao pedido.") },
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
            } // Fim da LazyColumn


            val scrollbarAlpha by remember(listState.isScrollInProgress) {
                mutableStateOf(if (listState.isScrollInProgress) 1f else 0f)
            }
            val scrollbarHeight = listState.layoutInfo.viewportSize.height.toFloat()
            val totalContentHeight = listState.layoutInfo.totalItemsCount * 56f // Estimativa
            val scrollbarVisibleHeight = (scrollbarHeight / totalContentHeight) * scrollbarHeight
            val scrollbarOffset = (listState.firstVisibleItemScrollOffset.toFloat() / (totalContentHeight - scrollbarHeight)) * (scrollbarHeight - scrollbarVisibleHeight)


            if (listState.canScrollForward || listState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(4.dp)
                        .fillMaxHeight()
                        .alpha(scrollbarAlpha)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Spacer(
                        modifier = Modifier
                            .height(scrollbarVisibleHeight.dp)
                            .offset(y = scrollbarOffset.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        } // Fim do Box
    } // Fim do Scaffold
} // Fim do ItemDetailScreen

// --- COMPONENTES AUXILIARES PARA A TELA DE DETALHES ---

@Composable
fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp // <-- FORÇANDO UM TAMANHO MAIOR
        )
        content()
    }
}

// SUBSTITUA o seu QuantitySelector por esta versão CORRIGIDA

@Composable
fun QuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically
) {
    Row(
        // REMOVEMOS o .fillMaxWidth() daqui!
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        // Voltamos para o spacedBy, que é mais simples para este caso
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onDecrease) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuir quantidade")
        }

        Text(
            text = "%02d".format(quantity),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onIncrease) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar quantidade")
        }
    }
}



// No arquivo ItemDetailScreen.kt

// No arquivo ItemDetailScreen.kt

@OptIn(ExperimentalAnimationApi::class) // <-- Adicione esta anotação na função
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
        // Coluna para Nome e Preço (continua igual)
        Column {
            Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "+ R$ ${"%.2f".format(ingredient.price)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // --- AQUI ESTÁ A MUDANÇA PRINCIPAL ---
        AnimatedContent(
            // 1. O 'targetState' é o valor que o AnimatedContent observa.
            //    Usamos um booleano: a quantidade é maior que zero?
            targetState = quantity > 0,
            label = "AddRemoveAnimation", // Um rótulo para ajudar na depuração
            // 2. 'transitionSpec' define como a animação acontece.
            transitionSpec = {
                // Animação para o conteúdo que está ENTRANDO na tela
                fadeIn() + scaleIn(initialScale = 0.8f) togetherWith
                        // Animação para o conteúdo que está SAINDO da tela
                        fadeOut() + scaleOut(targetScale = 0.8f)
            }
        ) { isQuantityGreaterThanZero ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                // Adicionamos um modificador clickable SE a quantidade for zero
                modifier = if (!isQuantityGreaterThanZero) {
                    Modifier.clickable { onQuantityChange(1) }
                } else {
                    Modifier
                }
            ) {
                // O conteúdo DENTRO do Card muda
                if (isQuantityGreaterThanZero) {
                    // ESTADO ABERTO: Mostra o seletor completo
                    CompactQuantitySelector(
                        quantity = quantity,
                        onIncrease = { onQuantityChange(quantity + 1) },
                        onDecrease = { onQuantityChange(quantity - 1) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    // ESTADO FECHADO: Mostra apenas o ícone de '+'
                    // Usamos um Box para centralizar o ícone e dar o mesmo padding do seletor
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .size(32.dp), // Mesmo tamanho do IconButton do seletor
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Adicionar ${ingredient.name}"
                        )
                    }
                }
            }
        }
    }
}

// SUBSTITUA o seu CompactQuantitySelector por esta versão ajustada

@Composable
fun CompactQuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // Aumentando um pouco o espaçamento entre os elementos
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        // Aumentando um pouco o tamanho da área de clique e do ícone
        val iconButtonModifier = Modifier.size(32.dp)

        IconButton(onClick = onDecrease, modifier = iconButtonModifier) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuir quantidade")
        }

        Text(
            text = "%02d".format(quantity),
            // Mantendo a fonte um pouco menor que a principal, mas legível
            style = MaterialTheme.typography.bodyLarge, // <-- Voltamos para bodyLarge
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onIncrease, modifier = iconButtonModifier) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar quantidade")
        }
    }
}





