package com.walli.flexcriatiwa

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

        val authViewModel: AuthViewModel by viewModels()
        val orderViewModel: OrderViewModel by viewModels()
        val kitchenViewModel: KitchenViewModel by viewModels()
        val managementViewModel: ManagementViewModel by viewModels()

        setContent {
            FlexCriatiwaTheme {
                RootNavigation(authViewModel, orderViewModel, kitchenViewModel, managementViewModel)
            }
        }
    }
}

@Composable
fun RootNavigation(
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    managementViewModel: ManagementViewModel
) {
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

        is AuthState.PendingApproval -> {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(24.dp))
                Text("Aguardando Aprovação", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Solicite ao seu gestor para liberar seu acesso.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { authViewModel.signOut() }) { Text("Cancelar / Sair") }
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

        is AuthState.LoggedIn -> AuthorizedApp(authState.companyId, authViewModel, orderViewModel, kitchenViewModel, managementViewModel, authState.isOfflineMode)
    }
}

@Composable
fun AuthorizedApp(
    companyId: String,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    managementViewModel: ManagementViewModel,
    isOffline: Boolean
) {
    val navController = rememberNavController()
    val userRole = authViewModel.currentUserProfile?.role ?: "employee"
    val isOwner = userRole == "owner"

    LaunchedEffect(companyId) {
        managementViewModel.updateCompanyContext(companyId)
        kitchenViewModel.updateCompanyContext(companyId)
    }

    if (isOwner && managementViewModel.pendingUsers.isNotEmpty()) {
        val userToApprove = managementViewModel.pendingUsers.first()
        var selectedRole by remember { mutableStateOf("waiter") }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Novo Acesso Solicitado") },
            text = {
                Column {
                    Text("O funcionário '${userToApprove.name}' solicitou acesso.")
                    Spacer(Modifier.height(16.dp))
                    Text("Defina a função:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selectedRole == "waiter", { selectedRole = "waiter" }); Text("Garçom") }
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selectedRole == "kitchen", { selectedRole = "kitchen" }); Text("Cozinha") }
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selectedRole == "counter", { selectedRole = "counter" }); Text("Balcão") }
                }
            },
            confirmButton = { Button(onClick = { authViewModel.approveUser(userToApprove.uid, selectedRole) }) { Text("Aprovar") } },
            dismissButton = { TextButton(onClick = { }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Ignorar") } }
        )
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

        composable("employee_management") {
            EmployeeManagementScreen(managementViewModel, onNavigateBack = { navController.popBackStack() })
        }

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

    val userProfile = authViewModel.currentUserProfile
    val userRole = userProfile?.role ?: "employee"
    val isAdminMode = authViewModel.isUserSuperAdmin

    val allItems = listOf(
        Triple("Cardápio", Icons.Filled.RestaurantMenu, "Cardápio"),
        Triple("Cozinha", Icons.Default.SoupKitchen, "Cozinha"),
        Triple("Balcão", Icons.Filled.Storefront, "Balcão"),
        Triple("Mesa", Icons.Filled.TableRestaurant, "Mesa"),
        Triple("Gestão", Icons.Outlined.Settings, "Gestão")
    )

    val visibleItems = remember(userRole) {
        when (userRole) {
            "owner" -> allItems
            "waiter" -> allItems.filter { it.first in listOf("Cardápio", "Mesa") }
            "kitchen" -> allItems.filter { it.first == "Cozinha" }
            "counter" -> allItems.filter { it.first in listOf("Balcão", "Cardápio") }
            else -> emptyList()
        }
    }

    if (selectedIndex >= visibleItems.size && visibleItems.isNotEmpty()) selectedIndex = 0

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))

                // --- CABEÇALHO DO MENU ---
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
                } else if (userProfile != null) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userProfile.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    userProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val cargo = when(userProfile.role) {
                                    "owner" -> "Gerente / Dono"
                                    "waiter" -> "Garçom"
                                    "kitchen" -> "Cozinha"
                                    "counter" -> "Balcão"
                                    else -> "Pendente"
                                }
                                Text(
                                    cargo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Divider()
                }
                // -------------------------

                visibleItems.forEachIndexed { index, (title, icon, _) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, null) },
                        label = { Text(title) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index; scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(icon = { Icon(Icons.Outlined.ExitToApp, null) }, label = { Text("Sair") }, selected = false, onClick = { scope.launch { drawerState.close(); if(isAdminMode) authViewModel.exitCompanyMode() else authViewModel.signOut() } }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        val currentItem = visibleItems.getOrNull(selectedIndex)

        if (currentItem != null) {
            when (currentItem.third) {
                "Cardápio" -> MainScreen(managementViewModel, !orderViewModel.isOrderEmpty, { navController.navigate("detail/$it") }, { navController.navigate("order_summary/null") }, { scope.launch { drawerState.open() } })
                "Cozinha" -> KitchenScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                "Balcão" -> CounterScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                "Mesa" -> TableScreen(kitchenViewModel, { scope.launch { drawerState.open() } }, { navController.navigate("order_summary/$it") })
                "Gestão" -> ManagementHubScreen(
                    managementViewModel,
                    { scope.launch { drawerState.open() } },
                    { navController.navigate("product_management") },
                    { navController.navigate("manage_categories") },
                    { navController.navigate("employee_management") }
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Selecione uma opção.") }
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
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 2000.dp)
                            ) {
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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

                // --- MUDANÇA 1: CÓDIGO NO TOPO ESQUERDO ---
                if (item.code > 0) {
                    Text(
                        text = "#%03d".format(item.code),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart) // Mudado para TopStart
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- MUDANÇA 2: PREÇO NO FIM DIREITO DA IMAGEM ---
                Text(
                    text = "R$ %.2f".format(item.price),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Posicionado aqui
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape) // Com fundo para destaque
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // --- MUDANÇA 3: APENAS O NOME ABAIXO ---
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        // O preço foi removido daqui
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