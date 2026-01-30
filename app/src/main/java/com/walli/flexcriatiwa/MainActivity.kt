package com.walli.flexcriatiwa

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.walli.flexcriatiwa.ui.theme.FlexCriatiwaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlexCriatiwaTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val orderViewModel: OrderViewModel = viewModel()
    val managementViewModel: ManagementViewModel = viewModel()
    val kitchenViewModel: KitchenViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main_layout") {

        composable("main_layout") {
            MainAppLayout(
                managementViewModel = managementViewModel,
                orderViewModel = orderViewModel,
                kitchenViewModel = kitchenViewModel,
                navController = navController
            )
        }

        composable("product_management") {
            ProductManagementScreen(
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddProduct = {
                    managementViewModel.clearEditState()
                    navController.navigate("add_edit_product")
                },
                onEditProduct = { productToEdit ->
                    managementViewModel.loadProductForEdit(productToEdit)
                    navController.navigate("add_edit_product")
                }
            )
        }

        composable("add_edit_product") {
            AddEditProductScreen(
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() }
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
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val managedProduct = remember(itemId) {
                managementViewModel.products.find { it.id == itemId }
            }

            if (managedProduct != null) {
                val menuItem = MenuItem(
                    id = managedProduct.id,
                    code = managedProduct.code, // Passa o código também
                    name = managedProduct.name,
                    price = managedProduct.price,
                    imageUrl = managedProduct.imageUrl
                )

                ItemDetailScreen(
                    product = menuItem,
                    productIngredients = managedProduct.ingredients.toList(),
                    productOptionals = managedProduct.optionals.toList(),
                    availableOptionals = managementViewModel.optionals,
                    orderViewModel = orderViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOrder = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable("order_summary/{tableNumber}?") { backStackEntry ->
            val tableNumberStr = backStackEntry.arguments?.getString("tableNumber")
            val tableNumber = if (tableNumberStr == "null") null else tableNumberStr?.toIntOrNull()

            val existingItemsOnTable = if (tableNumber != null) {
                kitchenViewModel.ordersByTable.value[tableNumber]?.flatMap { it.items } ?: emptyList()
            } else {
                emptyList()
            }

            OrderScreen(
                orderViewModel = orderViewModel,
                kitchenViewModel = kitchenViewModel,
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
                    val destination = orderViewModel.destinationType ?: "Viagem"
                    val tables = orderViewModel.tableSelection
                    val client = orderViewModel.clientName ?: "Cliente Balcão"

                    if (tableNumber != null) {
                        kitchenViewModel.addItemsToTableOrder(
                            tableNumber = tableNumber,
                            newItems = orderViewModel.currentCartItems
                        )
                    } else {
                        kitchenViewModel.submitNewOrder(
                            items = orderViewModel.currentCartItems,
                            destinationType = destination,
                            tableSelection = tables,
                            clientName = client,
                            payments = orderViewModel.payments
                        )
                    }

                    orderViewModel.clearAll()
                    navController.navigate("main_layout") {
                        popUpTo("main_layout") { inclusive = true }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    managementViewModel: ManagementViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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
                        label = {
                            BadgedBox(
                                badge = {
                                    if (itemTitle == "Balcão" && readyOrders.isNotEmpty()) {
                                        Badge()
                                    }
                                }
                            ) {
                                Text(itemTitle)
                            }
                        },
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
        when (selectedScreenIndex) {
            0 -> MainScreen(
                managementViewModel = managementViewModel,
                isOrderInProgress = !orderViewModel.isOrderEmpty,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToItemDetail = { itemId -> navController.navigate("detail/$itemId") },
                onNavigateToOrder = { navController.navigate("order_summary/null") }
            )
            1 -> KitchenScreen(kitchenViewModel = kitchenViewModel)
            2 -> CounterScreen(kitchenViewModel = kitchenViewModel)
            3 -> TableScreen(
                kitchenViewModel = kitchenViewModel,
                onTableClick = { tableNum -> navController.navigate("order_summary/$tableNum") }
            )
            4 -> ManagementHubScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToProducts = { navController.navigate("product_management") },
                onNavigateToCategories = { navController.navigate("manage_categories") },
                onNavigateToIngredients = { navController.navigate("manage_ingredients") },
                onNavigateToOptionals = { navController.navigate("manage_optionals") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    managementViewModel: ManagementViewModel,
    isOrderInProgress: Boolean,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToOrder: () -> Unit,
    onOpenDrawer: () -> Unit
) {
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
        if (categories.isEmpty()) -1 else {
            0.coerceAtLeast(categories.indices.find { true } ?: 0)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cardápio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                }
            )
        },
        floatingActionButton = {
            if (isOrderInProgress) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToOrder,
                    icon = { Icon(Icons.Filled.ShoppingBag, "Sacola") },
                    text = { Text("Ver Pedido") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SearchBar(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onClearClick = { searchText = "" }
            )

            if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum item encontrado.")
                }
            } else {
                LazyRow(
                    state = categoryChipListState,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(categories) { index, category ->
                        CategoryChip(
                            text = category.name,
                            isSelected = index == activeCategoryIndex,
                            onClick = { scope.launch { if(index == 0) listState.animateScrollToItem(0) } }
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    categories.forEach { category ->
                        stickyHeader { CategoryHeader(category.name) }
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.heightIn(max = 2000.dp)
                            ) {
                                items(category.items) { menuItem ->
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

// --- COMPONENTES VISUAIS ---

@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    val decodedBitmap = remember(item.imageUrl) {
        try {
            if (item.imageUrl.startsWith("data:image")) {
                val base64String = item.imageUrl.substringAfter(",")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ?.asImageBitmap()
            } else null
        } catch (e: Exception) { null }
    }

    Column(
        modifier = Modifier.width(160.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = Color.White)
                    }
                }

                // --- AQUI ESTÁ O CHIP DE NUMERAÇÃO RESTAURADO ---
                if (item.code > 0) {
                    Text(
                        text = "#%03d".format(item.code), // Exibe #001
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("R$ %.2f".format(item.price), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun CategoryHeader(categoryName: String) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SearchBar(searchText: String, onSearchChange: (String) -> Unit, onClearClick: () -> Unit) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("Buscar produtos...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = onClearClick) { Icon(Icons.Default.Close, null) }
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}