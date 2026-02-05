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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ExitToApp
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
                RootNavigation()
            }
        }
    }
}

@Composable
fun RootNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState = authViewModel.authState

    when (authState) {
        is AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is AuthState.LoggedOut -> {
            var showRegister by remember { mutableStateOf(false) }
            var showQRScanner by remember { mutableStateOf(false) }

            if (showQRScanner) {
                QRScannerScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { showQRScanner = false }
                )
            } else if (showRegister) {
                RegisterCompanyScreen(
                    authViewModel = authViewModel,
                    onRegistered = { },
                    onNavigateToJoin = { }
                )
            } else {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { showRegister = true },
                    onNavigateToQRCode = { showQRScanner = true }
                )
            }
        }
        is AuthState.NeedsCompanyRegistration -> {
            var isJoining by remember { mutableStateOf(false) }
            if (isJoining) {
                JoinCompanyScreen(authViewModel = authViewModel, onNavigateToCreate = { isJoining = false })
            } else {
                RegisterCompanyScreen(authViewModel = authViewModel, onRegistered = { }, onNavigateToJoin = { isJoining = true })
            }
        }
        is AuthState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Erro: ${authState.message}", color = Color.Red, modifier = Modifier.padding(16.dp))
                Button(onClick = { authViewModel.checkAuthStatus() }) { Text("Tentar Novamente") }
                TextButton(onClick = { authViewModel.signOut() }) { Text("Sair") }
            }
        }
        is AuthState.SuperAdmin -> SuperAdminScreen(authViewModel, onSignOut = { authViewModel.signOut() })
        is AuthState.LoggedIn -> AuthorizedApp(authState.companyId, authViewModel, authState.isOfflineMode)
    }
}

@Composable
fun AuthorizedApp(companyId: String, authViewModel: AuthViewModel, isOffline: Boolean) {
    val navController = rememberNavController()
    val orderViewModel: OrderViewModel = viewModel()
    val managementViewModel: ManagementViewModel = viewModel()
    val kitchenViewModel: KitchenViewModel = viewModel()

    LaunchedEffect(companyId) {
        managementViewModel.updateCompanyContext(companyId)
        kitchenViewModel.updateCompanyContext(companyId)
    }

    NavHost(navController = navController, startDestination = "main_layout") {
        composable("main_layout") {
            MainAppLayout(managementViewModel, orderViewModel, kitchenViewModel, authViewModel, navController, isOffline)
        }
        composable("product_management") {
            ProductManagementScreen(managementViewModel, { navController.popBackStack() }, { managementViewModel.clearEditState(); navController.navigate("add_edit_product") }, { managementViewModel.loadProductForEdit(it); navController.navigate("add_edit_product") })
        }
        composable("add_edit_product") { AddEditProductScreen(managementViewModel, { navController.popBackStack() }) }
        composable("manage_categories") { CategoryManagementScreen(managementViewModel, { navController.popBackStack() }) }

        composable("detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val product = managementViewModel.products.find { it.id == itemId }
            if (product != null) {
                ItemDetailScreen(
                    product = MenuItem(product.id, product.code, product.name, product.price, product.imageUrl),
                    productIngredients = product.ingredients.toList(),
                    productOptionals = product.optionals.toList(),
                    availableOptionals = product.optionals.toList(),
                    orderViewModel = orderViewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToOrder = { navController.popBackStack() }
                )
            }
        }

        composable("order_summary/{tableNumber}?") { backStackEntry ->
            val tableNumberStr = backStackEntry.arguments?.getString("tableNumber")
            val tableNumber = if (tableNumberStr == "null") null else tableNumberStr?.toIntOrNull()
            val existingItems = if (tableNumber != null) kitchenViewModel.ordersByTable.value[tableNumber]?.flatMap { it.items } ?: emptyList() else emptyList()

            LaunchedEffect(tableNumber) {
                orderViewModel.clearAll()
                val order = kitchenViewModel.ordersByTable.value[tableNumber]?.firstOrNull()
                if (order != null) orderViewModel.updateDestination(order.destinationType ?: "Local", order.tableSelection, order.clientName ?: "")
                else if (tableNumber != null) orderViewModel.updateDestination("Local", setOf(tableNumber), "")
            }

            OrderScreen(orderViewModel, kitchenViewModel, existingItems, { orderViewModel.clearAll(); navController.popBackStack() }, { navController.navigate("main_layout") }, { orderViewModel.loadItemForEdit(it); navController.navigate("detail/${it.menuItem.id}") },
                {
                    if (tableNumber != null) kitchenViewModel.addItemsToTableOrder(tableNumber, orderViewModel.currentCartItems)
                    else kitchenViewModel.submitNewOrder(orderViewModel.currentCartItems, orderViewModel.destinationType, orderViewModel.tableSelection, orderViewModel.clientName, orderViewModel.payments)
                    orderViewModel.clearAll(); navController.navigate("main_layout") { popUpTo("main_layout") { inclusive = true } }
                }, { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    managementViewModel: ManagementViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    isOffline: Boolean
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val readyOrders by kitchenViewModel.readyOrders.collectAsState(initial = emptyList())

    val items = listOf("Cardápio" to Icons.Filled.RestaurantMenu, "Cozinha" to Icons.Default.SoupKitchen, "Balcão" to Icons.Filled.Storefront, "Mesa" to Icons.Filled.TableRestaurant, "Gestão" to Icons.Outlined.Settings)
    val isAdminMode = authViewModel.isUserSuperAdmin

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                if (isAdminMode) {
                    Surface(color = Color.Red.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("MODO ESPIÃO", color = Color.Red, fontWeight = FontWeight.Bold)
                                Text("Acesso Admin", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (isOffline) {
                    Surface(color = Color(0xFFFFA000), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiOff, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("MODO OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Dados locais em uso", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                } else {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, null, Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text("FlexCriatiwa", fontWeight = FontWeight.Bold); Text("SaaS Mode", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Divider()
                items.forEachIndexed { index, (title, icon) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, null) },
                        label = { BadgedBox(badge = { if (title == "Balcão" && readyOrders.isNotEmpty()) Badge() }) { Text(title) } },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index; scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = {
                        if (isAdminMode) Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Red)
                        else Icon(Icons.Outlined.ExitToApp, null)
                    },
                    label = {
                        if (isAdminMode) Text("Voltar ao Painel", color = Color.Red, fontWeight = FontWeight.Bold)
                        else Text("Sair")
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            if (isAdminMode) authViewModel.exitCompanyMode() else authViewModel.signOut()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        when (selectedIndex) {
            0 -> MainScreen(managementViewModel, !orderViewModel.isOrderEmpty, { navController.navigate("detail/$it") }, { navController.navigate("order_summary/null") }, { scope.launch { drawerState.open() } })
            1 -> KitchenScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
            2 -> CounterScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
            3 -> TableScreen(kitchenViewModel, { scope.launch { drawerState.open() } }, { navController.navigate("order_summary/$it") })
            // --- A CORREÇÃO ESTÁ AQUI NA LINHA ABAIXO ---
            4 -> ManagementHubScreen(
                managementViewModel, // Passando a instância correta!
                { scope.launch { drawerState.open() } },
                { navController.navigate("product_management") },
                { navController.navigate("manage_categories") },
                {},
                {}
            )
        }
    }
}

// ... Restante do arquivo (MainScreen, MenuItemCard, etc.) permanece igual ...
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
        val all = managementViewModel.productsByCategory
        if (searchText.isBlank()) all else all.mapNotNull { cat ->
            val items = cat.items.filter { it.name.contains(searchText, ignoreCase = true) }
            if (items.isNotEmpty()) cat.copy(items = items) else null
        }
    }

    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val activeCategoryIndex = remember(firstVisibleItemIndex, categories) {
        if (categories.isEmpty()) -1 else 0.coerceAtLeast(categories.indices.find { true } ?: 0)
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Cardápio", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Menu") } }) },
        floatingActionButton = { if (isOrderInProgress) ExtendedFloatingActionButton(onClick = onNavigateToOrder, icon = { Icon(Icons.Filled.ShoppingBag, null) }, text = { Text("Ver Pedido") }) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            SearchBar(searchText, { searchText = it }, { searchText = "" })
            if (categories.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum item encontrado.") }
            else {
                LazyRow(state = categoryChipListState, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(categories) { index, category -> CategoryChip(text = category.name, isSelected = index == activeCategoryIndex, onClick = { scope.launch { if(index == 0) listState.animateScrollToItem(0) } }) }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
                    categories.forEach { category ->
                        stickyHeader { CategoryHeader(category.name) }
                        item {
                            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 2000.dp)) {
                                items(category.items) { MenuItemCard(item = it, onClick = { onNavigateToItemDetail(it.id) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

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

                if (item.code > 0) {
                    Text(
                        text = "#%03d".format(item.code),
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