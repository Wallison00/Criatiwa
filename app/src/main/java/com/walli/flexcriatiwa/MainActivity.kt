package com.walli.flexcriatiwa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableRestaurant

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar // <-- ADICIONE ESTA LINHA
import androidx.compose.material3.TopAppBarDefaults // <-- ADICIONE ESTA LINHA
import androidx.compose.material3.OutlinedTextField // <-- ADICIONE ESTE
import androidx.compose.material3.OutlinedTextFieldDefaults // <-- ADICIONE ESTE
import androidx.compose.foundation.text.BasicTextField // Para o campo de texto customizado
import androidx.compose.material.icons.filled.Close // Para o ícone 'X' de limpar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.walli.flexcriatiwa.ui.theme.FlexCriatiwaTheme
import androidx.compose.foundation.layout.width // <-- ADICIONE ESTA LINHA
import androidx.compose.foundation.layout.size // <-- ADICIONE ESTA LINHA
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp // <-- ADICIONE ESTA LINHA para o tamanho da fonte
import androidx.compose.material3.LocalTextStyle

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.material.icons.filled.ShoppingBag // Ícone da sacola
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton // FAB com texto
// No arquivo MainActivity.kt

import androidx.compose.material.icons.outlined.Settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Surface

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf

import androidx.navigation.NavController
import androidx.compose.material3.CenterAlignedTopAppBar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlexCriatiwaTheme { // Ou o nome do seu tema
                // --- AQUI ESTÁ A CORREÇÃO ---
                // Simplesmente chamamos a sua função de navegação principal.
                // Todo o resto (NavHost, dados de exemplo) será removido daqui.
                AppNavigation()
            }
        }
    }
}

// Dados de exemplo para o app funcionar (pode mover para MenuData.kt depois)
val sampleMenuItem1 = MenuItem(id = "1", name = "X-Salada Monstro", price = 25.50, imageUrl = "")
val sampleMenuItem2 = MenuItem(id = "2", name = "Suco de Laranja", price = 8.20, imageUrl = "")

val sampleOrder = listOf(
    OrderItem(
        menuItem = sampleMenuItem1,
        quantity = 1,
        removedIngredients = setOf("Queijo"),
        additionalIngredients = mapOf("Ovo" to 1),
        meatDoneness = "Ao Ponto",
        observations = null,
        singleItemTotalPrice = 25.50 + 2.0
    )
)

// ADICIONE ESTA NOVA FUNÇÃO COMPLETA NO SEU ARQUIVO MainActivity.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    managementViewModel: ManagementViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    navController: NavController // Precisamos dele para a navegação interna
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Este estado agora controla qual tela principal é mostrada
    var selectedScreenIndex by remember { mutableIntStateOf(0) }

    val readyOrders by kitchenViewModel.readyOrders.collectAsState(initial = emptyList())

    val drawerItems = listOf(
        "Cardápio" to Icons.Filled.RestaurantMenu,
        "Cozinha" to Icons.Default.SoupKitchen,
        "Balcão" to Icons.Filled.Storefront,
        "Mesa" to Icons.Filled.TableRestaurant,
        "Gestão" to Icons.Outlined.Settings
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                drawerItems.forEachIndexed { index, (itemTitle, itemIcon) ->
                    NavigationDrawerItem(
                        icon = { Icon(itemIcon, contentDescription = itemTitle) },

                        // --- SUBSTITUA O 'label' ANTIGO POR ESTE BLOCO ---
                        label = {
                            // Usamos BadgedBox para conter o texto e a notificação
                            BadgedBox(
                                badge = {
                                    // Mostra o Badge SOMENTE se o item for "Balcão"
                                    // E se a lista de pedidos prontos NÃO estiver vazia.
                                    if (itemTitle == "Balcão" && readyOrders.isNotEmpty()) {
                                        Badge() // O Badge padrão é uma bolinha vermelha
                                    }
                                }
                            ) {
                                Text(itemTitle) // O texto do item de menu
                            }
                        },
                        // ----------------------------------------------------

                        selected = selectedScreenIndex == index,
                        onClick = {
                            selectedScreenIndex = index
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        // Com base no item de menu clicado, decidimos qual tela mostrar
        when (selectedScreenIndex) {
            0 -> MainScreen(
                managementViewModel = managementViewModel,
                isOrderInProgress = !orderViewModel.isOrderEmpty,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToItemDetail = { itemId -> navController.navigate("detail/$itemId") },
                onNavigateToOrder = { navController.navigate("order_summary/") }
            )
            // --- NOVO CASE PARA A COZINHA ---
            1 -> KitchenScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                kitchenViewModel = kitchenViewModel
            )
            2 -> CounterScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                kitchenViewModel = kitchenViewModel // Passa o mesmo ViewModel!
            )
            3 -> TableScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                kitchenViewModel = kitchenViewModel, // Passa o mesmo ViewModel!
                navController = navController
            )
            4 -> ManagementHubScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToProducts = { navController.navigate("product_management") },
                onNavigateToCategories = { navController.navigate("manage_categories") },
                onNavigateToIngredients = { navController.navigate("manage_ingredients") },
                onNavigateToOptionals = { navController.navigate("manage_optionals") }
            )
            else -> {
                // AGORA USAMOS A TELA PADRÃO QUE JÁ TEM O MENU
                PlaceholderScreen(
                    screenName = drawerItems[selectedScreenIndex].first,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}


// --- NOSSO GRAFO DE NAVEGAÇÃO COMPLETO ---
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val orderViewModel: OrderViewModel = viewModel()
    val managementViewModel: ManagementViewModel = viewModel()
    val kitchenViewModel: KitchenViewModel = viewModel()

    // O startDestination agora aponta para o nosso novo layout
    NavHost(navController = navController, startDestination = "main_layout") {
        composable("main_layout") {
            MainAppLayout(
                managementViewModel = managementViewModel,
                orderViewModel = orderViewModel,
                kitchenViewModel = kitchenViewModel,
                navController = navController
            )
        }

        // --- ADICIONE ESTA NOVA ROTA PARA A LISTA DE PRODUTOS ---
        composable("product_management") {
            ProductManagementScreen(
                managementViewModel = managementViewModel, // <-- PASSE O VIEWMODEL
                onNavigateBack = { navController.popBackStack() },
                onAddProduct = {
                    // Limpa o estado de edição antes de ir para a tela de adicionar
                    managementViewModel.clearEditState()
                    navController.navigate("add_edit_product")
                },
                // --- NOVA LÓGICA DE EDIÇÃO ---
                onEditProduct = { productToEdit ->
                    // 1. Carrega o produto no ViewModel
                    managementViewModel.loadProductForEdit(productToEdit)
                    // 2. Navega para a tela de edição
                    navController.navigate("add_edit_product")
                }
            )
        }

        composable("manage_categories") {
            CategoryManagementScreen(
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("manage_ingredients") {
            IngredientManagementScreen(
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("manage_optionals") {
            OptionalManagementScreen(
                managementViewModel = managementViewModel, // <-- ADICIONE ESTA LINHA
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- ADICIONE ESTE NOVO BLOCO DE ROTA ---
        composable("add_edit_product") {
            AddEditProductScreen(
                managementViewModel = managementViewModel, // <-- PASSE O VIEWMODEL
                onNavigateBack = { navController.popBackStack() }
                // O botão de salvar agora está dentro da tela, então onSaveProduct não é mais necessário aqui
            )
        }

        // Rota para a tela de detalhes
        // --- ROTA DE DETALHES CORRIGIDA ---
        composable("detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")

            val managedProduct = remember(itemId) {
                managementViewModel.products.find { it.id == itemId }
            }

            if (managedProduct != null) {
                // 1. Converte o produto de gestão para um produto de pedido
                val menuItem = MenuItem(
                    id = managedProduct.id,
                    name = managedProduct.name,
                    price = managedProduct.price,
                    imageUrl = managedProduct.imageUrl
                )

                // 2. Chama a tela passando TUDO que ela precisa
                ItemDetailScreen(
                    product = menuItem,
                    productIngredients = managedProduct.ingredients.toList(), // <-- CORREÇÃO: .toList()
                    productOptionals = managedProduct.optionals.toList(),     // <-- CORREÇÃO: .toList()
                    availableOptionals = managementViewModel.optionals,
                    orderViewModel = orderViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOrder = { navController.popBackStack() }
                )
            } else {
                // Se o produto não for encontrado (caso raro), apenas volta
                navController.popBackStack()
            }
        }

        composable("order_summary/{tableNumber}?") { backStackEntry ->
            val tableNumber = backStackEntry.arguments?.getString("tableNumber")?.toIntOrNull()

            // Encontra os itens que JÁ ESTÃO na mesa, se houver.
            val existingItemsOnTable = if (tableNumber != null) {
                kitchenViewModel.ordersByTable.value[tableNumber]?.flatMap { it.items } ?: emptyList()
            } else {
                emptyList()
            }

            // Garante que o carrinho de NOVOS itens esteja limpo ao entrar na tela.
            // Se for um pedido novo, ele já estará limpo. Se for uma mesa, isso evita
            // que itens de um pedido anterior "vazem".
            if (tableNumber != null) {
                LaunchedEffect(Unit) {
                    orderViewModel.clearAll()
                }
            }

            OrderScreen(
                orderViewModel = orderViewModel,
                kitchenViewModel = kitchenViewModel, // <-- ADICIONE ESTA LINHA
                existingItems = existingItemsOnTable,
                onCancelOrder = {
                    orderViewModel.clearAll()
                    navController.popBackStack()
                },
                onAddItem = {
                    navController.navigate("main_layout")
                },
                onEditItem = { itemToEdit ->
                    orderViewModel.loadItemForEdit(itemToEdit)
                    navController.navigate("detail/${itemToEdit.menuItem.id}")
                },
                onSendToKitchen = {
                    if (tableNumber != null) {
                        kitchenViewModel.addItemsToTableOrder(
                            tableNumber = tableNumber,
                            newItems = orderViewModel.currentCartItems
                        )
                    } else {
                        kitchenViewModel.submitNewOrder(
                            items = orderViewModel.currentCartItems,
                            destinationType = orderViewModel.destinationType,
                            tableSelection = orderViewModel.tableSelection,
                            clientName = orderViewModel.clientName,
                            payments = orderViewModel.payments
                        )
                    }
                    orderViewModel.clearAll()
                    navController.navigate("main_layout") { popUpTo("main_layout") { inclusive = true } }
                }
            )
        }


    }
}



// SUBSTITUA SUA FUNÇÃO INTEIRA PELA VERSÃO ABAIXO

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    managementViewModel: ManagementViewModel,
    isOrderInProgress: Boolean,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToOrder: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    // A lógica de estados (searchText, listState, etc.) continua aqui
    var searchText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val categoryChipListState = rememberLazyListState()

    val categories = remember(managementViewModel.products, searchText) {
        val allCategories = managementViewModel.productsByCategory
        if (searchText.isBlank()) {
            allCategories
        } else {
            val query = searchText.lowercase().trim()
            allCategories.mapNotNull { category ->
                val filteredItems = category.items.filter { it.name.lowercase().contains(query) }
                if (filteredItems.isNotEmpty()) category.copy(items = filteredItems) else null
            }
        }
    }

    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val activeCategoryIndex = remember(firstVisibleItemIndex, categories) {
        categories.indexOfFirst { category ->
            val categoryStartIndex = categories.indexOf(category) * 2
            firstVisibleItemIndex >= categoryStartIndex && firstVisibleItemIndex < categoryStartIndex + 2
        }.let { if (it == -1 && firstVisibleItemIndex > 0 && categories.isNotEmpty()) categories.lastIndex else it }
    }


    LaunchedEffect(activeCategoryIndex) {
        if (activeCategoryIndex != -1) {
            scope.launch {
                categoryChipListState.animateScrollToItem(activeCategoryIndex)
            }
        }
    }

    // O SCAFFOLD AGORA É O COMPONENTE RAIZ. O DRAWER FOI REMOVIDO.
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // O título "Cardápio" agora é exibido permanentemente
                    Text("Cardápio", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    // O ícone de menu continua o mesmo
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                },
                actions = {
                    // O ícone de lupa agora é apenas visual, não tem ação
                    // Podemos até removê-lo se quisermos, mas vamos mantê-lo por enquanto
                    // para indicar que a barra abaixo é de busca.
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Busca", // Apenas descritivo
                        modifier = Modifier.padding(end = 8.dp), // Um padding para não colar na borda
                        tint = Color.Transparent // Deixa o ícone invisível, mas ocupando espaço
                    )
                }
            )
        },
        floatingActionButton = {
            if (isOrderInProgress) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToOrder,
                    icon = { Icon(Icons.Filled.ShoppingBag, "Ícone de Sacola de Compras") },
                    text = { Text(text = "Ver Pedido") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SearchBar(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onClearClick = { searchText = "" }
            )
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (searchText.isBlank()) "Nenhum produto cadastrado." else "Nenhum resultado encontrado.")
                }
            } else {
                LazyRow(
                    state = categoryChipListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(categories) { index, category ->
                        val isSelected = index == activeCategoryIndex
                        CategoryChip(
                            text = category.name,
                            isSelected = isSelected,
                            onClick = {
                                scope.launch {
                                    val itemIndex = index * 2
                                    listState.animateScrollToItem(itemIndex)
                                }
                            }
                        )
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    categories.forEach { category ->
                        stickyHeader {
                            CategoryHeader(category.name)
                        }
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.heightIn(max = 10000.dp)
                            ) {
                                items(category.items, key = { it.id }) { menuItem ->
                                    MenuItemCard(
                                        item = menuItem,
                                        onClick = { onNavigateToItemDetail(menuItem.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- ADICIONE ESTA NOVA FUNÇÃO ---
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Define as cores com base na seleção
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val indicatorColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // O texto da categoria
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        // O indicador colorido abaixo do texto
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(40.dp)
                .background(indicatorColor, shape = CircleShape)
        )
    }
}


// --- AJUSTE A SUA FUNÇÃO CategoryHeader ---
@Composable
fun CategoryHeader(categoryName: String) {
    Text(
        text = categoryName,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .fillMaxWidth()
            // Adicione esta linha para dar um fundo opaco que corresponde ao fundo da tela
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp, horizontal = 4.dp)
    )
}



@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreenPreview() {
    FlexCriatiwaTheme {
        MainScreen(
            managementViewModel = viewModel(),
            isOrderInProgress = true,
            onNavigateToItemDetail = {},
            onNavigateToOrder = {},
            onOpenDrawer = {}
        )
    }
}

// Componente que renderiza um único card de item
@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    // --- MUDANÇA 1: O COMPONENTE RAIZ AGORA É UMA COLUMN ---
    // A Column inteira é clicável.
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        // Espaçamento vertical entre a imagem e o texto
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- MUDANÇA 2: A IMAGEM DENTRO DE UM CARD PARA TER AS BORDAS ARREDONDADAS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                // Proporção quadrada para a imagem
                .aspectRatio(1f),
            shape = RoundedCornerShape(16.dp), // Arredondamento maior, como na referência
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // A imagem ocupa todo o espaço do Card
                Image(
                    painter = rememberAsyncImagePainter(item.imageUrl),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // O ID do item continua no canto inferior direito
                Text(
                    text = item.id,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }

        // --- MUDANÇA 3: O TEXTO FICA FORA DO CARD, ABAIXO DA IMAGEM ---
        Column {
            // O nome do item (Title)
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge, // Fonte maior para o título
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            // O preço do item (Updated today)
            Text(
                text = "R$ ${"%.2f".format(item.price)}",
                style = MaterialTheme.typography.bodyMedium, // Fonte um pouco menor
                color = Color.Gray // Cor cinza para o subtítulo
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    screenName: String,
    onOpenDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Tela '$screenName' em desenvolvimento.")
        }
    }
}

// Adicione esta nova função no final do seu arquivo MainActivity.kt

@Composable
private fun SearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = CircleShape
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Ícone de Busca",
            modifier = Modifier.padding(start = 16.dp)
        )
        BasicTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                    if (searchText.isEmpty()) {
                        Text(
                            text = "Pesquise por nome ou código",
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        )
        if (searchText.isNotEmpty()) {
            IconButton(onClick = onClearClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpar pesquisa"
                )
            }
        }
    }
}
