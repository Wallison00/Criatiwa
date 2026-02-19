package com.walli.flexcriatiwa

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
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

        composable("management_hub") {
            ManagementHubScreen(
                managementViewModel,
                { },
                { navController.navigate("product_management") },
                { navController.navigate("manage_categories") },
                { navController.navigate("employee_management") }
            )
        }

        composable("company_qr_code") {
            CompanyQRCodeScreen(
                companyId = companyId,
                companyName = authViewModel.currentUserProfile?.companyName ?: "Empresa",
                onNavigateBack = { navController.popBackStack() }
            )
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
            val tableNumberArg = if (tableNumberStr == "null") null else tableNumberStr?.toIntOrNull()

            val activeTableId = tableNumberArg ?: orderViewModel.tableSelection.firstOrNull()
            val ordersByTable by kitchenViewModel.ordersByTable.collectAsState()

            val existingOrders = remember(ordersByTable, activeTableId) {
                if (activeTableId != null) {
                    ordersByTable[activeTableId] ?: emptyList()
                } else {
                    emptyList()
                }
            }

            LaunchedEffect(tableNumberArg) {
                if (tableNumberArg != null) {
                    orderViewModel.clearAll()
                    val currentOrders = kitchenViewModel.ordersByTable.value
                    val order = currentOrders[tableNumberArg]?.firstOrNull()

                    if (order != null) {
                        orderViewModel.updateDestination(order.destinationType ?: "Local", order.tableSelection, order.clientName ?: "")
                    } else {
                        orderViewModel.updateDestination("Local", setOf(tableNumberArg), "")
                    }
                }
            }

            OrderScreen(orderViewModel, kitchenViewModel, existingOrders, { orderViewModel.clearAll(); navController.popBackStack() }, { navController.navigate("main_layout") }, { orderViewModel.loadItemForEdit(it); navController.navigate("detail/${it.menuItem.id}") },
                {
                    if (activeTableId != null) kitchenViewModel.addItemsToTableOrder(activeTableId, orderViewModel.currentCartItems)
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

    val userProfile = authViewModel.currentUserProfile
    val userRole = userProfile?.role ?: "employee"
    val isAdminMode = authViewModel.isUserSuperAdmin
    val hasOrder = !orderViewModel.isOrderEmpty

    var currentScreen by remember { mutableStateOf("Cardápio") }

    val bottomNavItems = remember(userRole, hasOrder) {
        val items = mutableListOf<Triple<String, ImageVector, String>>()

        if (userRole in listOf("owner", "waiter", "counter")) {
            items.add(Triple("Cardápio", Icons.Filled.RestaurantMenu, "Cardápio"))
        }
        if (userRole in listOf("owner", "kitchen")) {
            items.add(Triple("Cozinha", Icons.Default.SoupKitchen, "Cozinha"))
        }

        if (hasOrder && userRole in listOf("owner", "waiter", "counter")) {
            items.add(Triple("Pedido", Icons.Filled.ShoppingBag, "Pedido"))
        }

        if (userRole in listOf("owner", "counter")) {
            items.add(Triple("Balcão", Icons.Filled.Storefront, "Balcão"))
        }
        if (userRole in listOf("owner", "waiter")) {
            items.add(Triple("Mesa", Icons.Filled.TableRestaurant, "Mesa"))
        }

        items
    }

    LaunchedEffect(userRole) {
        if (userRole == "kitchen") currentScreen = "Cozinha"
    }

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
                            Column { Text("MODO ESPIÃO", color = Color.Red, fontWeight = FontWeight.Bold); Text("Acesso Admin", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                } else if (userProfile != null) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(text = userProfile.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(userProfile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val cargo = when(userProfile.role) { "owner"->"Gerente"; "waiter"->"Garçom"; "kitchen"->"Cozinha"; "counter"->"Balcão"; else->"Pendente" }
                                Text(cargo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                    Divider()
                }

                if (userRole == "owner" || isAdminMode) {
                    Text("Gestão", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.QrCode2, null) },
                        label = { Text("QR Code da Loja") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close(); navController.navigate("company_qr_code") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ShoppingBag, null) },
                        label = { Text("Produtos") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close(); navController.navigate("product_management") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("Categorias") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close(); navController.navigate("manage_categories") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Equipe") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close(); navController.navigate("employee_management") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Divider(Modifier.padding(vertical = 8.dp))
                }

                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.ExitToApp, null) },
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); if(isAdminMode) authViewModel.exitCompanyMode() else authViewModel.signOut() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { (title, icon, routeKey) ->
                        val isSelected = currentScreen == routeKey
                        val isOrder = routeKey == "Pedido"

                        NavigationBarItem(
                            icon = {
                                if (isOrder) {
                                    BadgedBox(badge = { Badge { Text("!") } }) {
                                        Icon(icon, null, tint = if(isSelected) MaterialTheme.colorScheme.primary else Color.Red)
                                    }
                                } else {
                                    Icon(icon, null)
                                }
                            },
                            label = { Text(title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (isOrder) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            onClick = {
                                if (isOrder) {
                                    navController.navigate("order_summary/null")
                                } else {
                                    currentScreen = routeKey
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    "Cardápio" -> MainScreen(managementViewModel, !orderViewModel.isOrderEmpty, { navController.navigate("detail/$it") }, { navController.navigate("order_summary/null") }, { scope.launch { drawerState.open() } })
                    "Cozinha" -> KitchenScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                    "Balcão" -> CounterScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                    "Mesa" -> TableScreen(kitchenViewModel, { scope.launch { drawerState.open() } }, { navController.navigate("order_summary/$it") })
                }
            }
        }
    }
}

// --- TELA DE QR CODE CORRIGIDA: Exibe o CÓDIGO DE ACESSO correto ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyQRCodeScreen(
    companyId: String,
    companyName: String,
    onNavigateBack: () -> Unit
) {
    var shareCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(companyId) {
        Firebase.firestore.collection("companies").document(companyId).get()
            .addOnSuccessListener {
                shareCode = it.getString("shareCode") ?: ""
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val qrBitmap = remember(shareCode) {
        if (shareCode.isNotEmpty()) QRCodeUtils.generateQRCode(shareCode) else null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Acesso da Loja", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Peça para o autônomo escanear ou digitar este código:",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code da Empresa",
                    modifier = Modifier.size(250.dp).background(Color.White).padding(16.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text("Código de Acesso:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = shareCode, // Exibe o código correto (6 letras)
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
            } else {
                Box(Modifier.size(300.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                    Text("Erro ao carregar código")
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(companyName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ... (Restante do arquivo permanece igual) ...
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
    var isSearchExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val categoryChipListState = rememberLazyListState()

    val categories = remember(managementViewModel.products, searchText) {
        val all = managementViewModel.productsByCategory
        if (searchText.isBlank()) all else all.mapNotNull { cat ->
            val items = cat.items.filter {
                val formattedCode = "#%03d".format(it.code)
                it.name.contains(searchText, ignoreCase = true) ||
                        formattedCode.contains(searchText, ignoreCase = true)
            }
            if (items.isNotEmpty()) cat.copy(items = items) else null
        }
    }

    val activeCategoryIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Cardápio", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Menu") }
                },
                actions = {
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchExpanded) "Fechar Busca" else "Buscar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {

            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar produtos...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, "Limpar")
                            }
                        }
                    }
                )
            }

            if (categories.isNotEmpty()) {
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
                            onClick = {
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum item encontrado.")
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            ) {
                categories.forEach { category ->
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

                if (item.code > 0) {
                    Text(
                        text = "#%03d".format(item.code),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "R$ %.2f".format(item.price),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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