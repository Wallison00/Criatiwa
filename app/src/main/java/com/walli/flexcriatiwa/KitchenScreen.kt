package com.walli.flexcriatiwa

// Imports de Layout e Componentes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

// Imports de Ícones
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu

// Imports do Runtime do Compose (para 'collectAsState', 'getValue', etc.)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// Imports de UI (Cor, Fonte, etc.)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

// Em KitchenScreen.kt
// SUBSTITUA A FUNÇÃO INTEIRA POR ESTA VERSÃO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(
    onOpenDrawer: () -> Unit,
    kitchenViewModel: KitchenViewModel
) {
    // --- CORREÇÃO: UMA ÚNICA DECLARAÇÃO ---
    // Coleta a lista de pedidos que devem aparecer na cozinha (status PREPARING)
    val kitchenOrders by kitchenViewModel.kitchenOrders.collectAsState(initial = emptyList())
    // Coleta o timer para o cronômetro
    val currentTime by kitchenViewModel.timerFlow.collectAsState(initial = System.currentTimeMillis())
    // ----------------------------------------

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cozinha", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        // --- CORREÇÃO: USA A VARIÁVEL CORRETA 'kitchenOrders' ---
        if (kitchenOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aguardando novos pedidos...")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- CORREÇÃO: USA A VARIÁVEL CORRETA 'kitchenOrders' ---
                items(kitchenOrders, key = { it.id }) { order ->
                    KitchenOrderCard(
                        order = order,
                        currentTime = currentTime,
                        onUpdateStatus = { newStatus ->
                            kitchenViewModel.updateOrderStatus(order.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}


// Substitua a sua KitchenOrderCard por esta versão

@Composable
fun KitchenOrderCard(
    order: KitchenOrder,
    currentTime: Long,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    // 1. CALCULA O TEMPO DECORRIDO
    val elapsedSeconds = (currentTime - order.timestamp) / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = "%02d:%02d".format(minutes, seconds)

    // 2. DEFINE A COR COM BASE NO TEMPO
    val cardHeaderColor = when {
        minutes >= 5 -> Color(0xFFD32F2F) // Vermelho (Urgente)
        minutes >= 2 -> Color(0xFFFFA000) // Laranja (Atenção)
        else -> Color(0xFF388E3C)         // Verde (Normal)
    }

    // 3. ESTRUTURA DO CARD
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), // Bordas menos arredondadas
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)) // Fundo escuro
    ) {
        Column {
            // --- CABEÇALHO COLORIDO ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardHeaderColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Define o título principal (Mesa ou Viagem)
                val titleText = buildAnnotatedString {
                    val mainTitle = when (order.destinationType) {
                        "Local" -> "Mesa ${order.tableSelection.joinToString()}"
                        else -> "Viagem" // Para "Viagem" ou nulo
                    }
                    // Adiciona o título principal em negrito
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(mainTitle)
                    }
                    // Se houver um nome de cliente, adiciona entre parênteses
                    if (!order.clientName.isNullOrBlank()) {
                        append(" (${order.clientName})")
                    }
                }
                Text(
                    text = titleText,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    timeString,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )            }

            // --- CORPO DO CARD ---
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Lista de itens
                order.items.forEach { item ->
                    KitchenOrderItem(item = item)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão de Ação
                Button(
                    onClick = { onUpdateStatus(OrderStatus.READY) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Pedido Pronto")
                }
            }
        }
    }
}

// Substitua sua KitchenOrderItem por esta versão

// Em KitchenScreen.kt (ou onde o componente estiver)
// SUBSTITUA A FUNÇÃO ANTIGA POR ESTA VERSÃO CORRIGIDA

@Composable
private fun KitchenOrderItem(item: OrderItem) {
    Column(
        modifier = Modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Aumenta um pouco o espaçamento
    ) {
        // Linha principal: "1 x X-Burger Especial" (sem mudanças)
        Text(
            text = "${item.quantity} x ${item.menuItem.name}",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold // Deixa o nome do item principal em negrito
        )

        // Detalhes (com, sem, obs) indentados
        Column(modifier = Modifier.padding(start = 16.dp)) {

            // --- SEÇÃO DE INGREDIENTES REMOVIDOS ---
            item.removedIngredients.takeIf { it.isNotEmpty() }?.let {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFFFFA000))) { // Laranja
                            append("Sem: ")
                        }
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append(it.joinToString())
                        }
                    }
                )
            }

            // --- SEÇÃO DE INGREDIENTES ADICIONAIS (CORRIGIDA) ---
            item.additionalIngredients.takeIf { it.isNotEmpty() }?.let { ingredients ->
                // 1. Mapeia o mapa de ingredientes para uma lista de strings formatadas
                val additionalText = ingredients.map { (name, quantity) ->
                    if (quantity > 1) "$name (x$quantity)" else name
                }.joinToString()

                // 2. Usa buildAnnotatedString para colorir o prefixo
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF66BB6A))) { // Verde
                            append("Com: ")
                        }
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append(additionalText)
                        }
                    }
                )
            }
            // ----------------------------------------------------

            // --- PONTO DA CARNE E OBSERVAÇÕES (sem mudanças) ---
            item.meatDoneness?.let {
                Text("Ponto: $it", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            item.observations?.let {
                Text("Obs: $it", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}



