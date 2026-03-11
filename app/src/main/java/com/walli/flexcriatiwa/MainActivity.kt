package com.walli.flexcriatiwa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.outlined.Notifications // Ícone de notificação
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

        createNotificationChannels() // Canais criados (Cozinha e Balcão)

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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Canal 1: Cozinha
            val channelKitchen = NotificationChannel("kitchen_orders", "Pedidos Cozinha", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Novos pedidos para preparo"
            }
            notificationManager.createNotificationChannel(channelKitchen)

            // Canal 2: Balcão/Garçom
            val channelCounter = NotificationChannel("counter_ready", "Pedidos Prontos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pedidos prontos para entrega"
            }
            notificationManager.createNotificationChannel(channelCounter)
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

        is AuthState.LoggedIn -> AuthorizedApp(authState.companyId, authViewModel, orderViewModel, kitchenViewModel, managementViewModel)
    }
}

@Composable
fun AuthorizedApp(
    companyId: String,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    kitchenViewModel: KitchenViewModel,
    managementViewModel: ManagementViewModel
) {
    val navController = rememberNavController()
    val userRole = authViewModel.currentUserProfile?.role ?: "employee"
    val isOwner = userRole == "owner"

    LaunchedEffect(companyId) {
        managementViewModel.updateCompanyContext(companyId)
        managementViewModel.loadPaymentConfig(companyId) // Adicione esta linha aqui!
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
            MainAppLayout(managementViewModel, orderViewModel, kitchenViewModel, authViewModel, navController)
        }

        composable("management_hub") {
            ManagementHubScreen(
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProducts = { navController.navigate("product_management") },
                onNavigateToCategories = { navController.navigate("manage_categories") },
                onNavigateToEmployees = { navController.navigate("employee_management") },
                onNavigateToPaymentConfig = { navController.navigate("payment_config") }
            )
        }

        // --- ROTA DE CONFIGURAÇÕES (Nova) ---
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // --- ROTA DA IMPRESSORA (Isolada) ---
        composable("printer_config") {
            PrinterConfigScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("company_qr_code") {
            CompanyQRCodeScreen(
                companyId = companyId,
                companyName = authViewModel.currentUserProfile?.companyName ?: "Empresa",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTableLayout = { navController.navigate("table_layout_config") }
            )
        }

        composable("table_layout_config") {
            TableLayoutConfigScreen(
                companyId = companyId,
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
                // Intersect product's saved data with CURRENT CURRENT category configuration
                val categoryConfig = managementViewModel.categoryConfigs.find { it.name.equals(product.category, ignoreCase = true) }
                
                val validIngredients = product.ingredients.intersect(categoryConfig?.defaultIngredients?.toSet() ?: emptySet()).toList()
                
                val validOptionals = product.optionals
                    .filter { pOpt -> categoryConfig?.availableOptionals?.any { cOpt -> cOpt.name == pOpt.name } == true }
                    .map { pOpt ->
                        // Grab the LATEST price from category config
                        val cOpt = categoryConfig?.availableOptionals?.find { it.name == pOpt.name }
                        OptionalItem(pOpt.name, cOpt?.price ?: pOpt.price)
                    }
                    .toList()
                    
                val currentCategoryOptionals = categoryConfig?.availableOptionals ?: emptyList()

                ItemDetailScreen(
                    product = MenuItem(product.id, product.code, product.name, product.price, product.imageUrl),
                    productIngredients = validIngredients,
                    productOptionals = validOptionals,
                    availableOptionals = currentCategoryOptionals,
                    orderViewModel = orderViewModel, 
                    onNavigateBack = { navController.popBackStack() }, 
                    onNavigateToOrder = { navController.popBackStack() }
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

            OrderScreen(
                orderViewModel = orderViewModel,
                kitchenViewModel = kitchenViewModel,
                managementViewModel = managementViewModel, // <--- ADICIONE ESTA LINHA
                existingOrders = existingOrders,
                onCancelOrder = { orderViewModel.clearAll(); navController.popBackStack() },
                onAddItem = { navController.navigate("main_layout") },
                onEditItem = { orderViewModel.loadItemForEdit(it); navController.navigate("detail/${it.menuItem.id}") },
                onSendToKitchen = { generatedTimestamp ->
                    if (activeTableId != null) kitchenViewModel.addItemsToTableOrder(
                        tableNumber = activeTableId, 
                        newItems = orderViewModel.currentCartItems, 
                        timestamp = generatedTimestamp,
                        destinationType = orderViewModel.destinationType ?: "Local",
                        clientName = orderViewModel.clientName?.takeIf { it.isNotBlank() } ?: "Mesa $activeTableId"
                    )
                    else kitchenViewModel.submitNewOrder(orderViewModel.currentCartItems, orderViewModel.destinationType, orderViewModel.tableSelection, orderViewModel.clientName, orderViewModel.payments, generatedTimestamp)
                    orderViewModel.clearAll()
                    navController.navigate("main_layout") { popUpTo("main_layout") { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Dentro do NavHost em AuthorizedApp
        composable("payment_config") {
            PaymentConfigScreen(
                companyId = companyId,
                managementViewModel = managementViewModel,
                onNavigateBack = { navController.popBackStack() }
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
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val userProfile = authViewModel.currentUserProfile
    val userRole = userProfile?.role ?: "employee"
    val isAdminMode = authViewModel.isUserSuperAdmin

    // --- LÓGICA DE NOTIFICAÇÕES INTELIGENTES ---
    val context = LocalContext.current
    val offlineManager = remember { OfflineSessionManager(context) }

    val kitchenOrders by kitchenViewModel.kitchenOrders.collectAsState()
    val readyOrders by kitchenViewModel.readyOrders.collectAsState()

    val pendingKitchenCount = kitchenOrders.size
    val pendingCounterCount = readyOrders.size

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 1. Monitora Novos Pedidos (Cozinha)
    var previousKitchenCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(pendingKitchenCount) {
        if (pendingKitchenCount > previousKitchenCount) {
            // VERIFICA SE O USUÁRIO ATIVOU ESTA OPÇÃO
            if (offlineManager.getNotifyKitchen() && hasNotificationPermission) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val builder = NotificationCompat.Builder(context, "kitchen_orders")
                            .setSmallIcon(android.R.drawable.ic_dialog_alert)
                            .setContentTitle("Cozinha: Novo Pedido!")
                            .setContentText("Existem $pendingKitchenCount pedidos aguardando.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)

                        with(NotificationManagerCompat.from(context)) { notify(1001, builder.build()) }
                    } catch (_: Exception) { }
                }
            }
        }
        previousKitchenCount = pendingKitchenCount
    }

    // 2. Monitora Pedidos Prontos (Balcão/Garçom)
    var previousCounterCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(pendingCounterCount) {
        if (pendingCounterCount > previousCounterCount) {
            // VERIFICA SE O USUÁRIO ATIVOU ESTA OPÇÃO
            if (offlineManager.getNotifyCounter() && hasNotificationPermission) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val builder = NotificationCompat.Builder(context, "counter_ready")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("Balcão: Pedido Pronto!")
                            .setContentText("$pendingCounterCount pedidos prontos para entrega.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)

                        with(NotificationManagerCompat.from(context)) { notify(1002, builder.build()) }
                    } catch (_: Exception) { }
                }
            }
        }
        previousCounterCount = pendingCounterCount
    }

    var currentScreen by remember { mutableStateOf("Cardápio") }

    val bottomNavItems = remember(userRole) {
        val items = mutableListOf<Triple<String, ImageVector, String>>()

        if (userRole in listOf("owner", "waiter", "counter")) {
            items.add(Triple("Cardápio", Icons.Filled.RestaurantMenu, "Cardápio"))
        }
        if (userRole in listOf("owner", "kitchen")) {
            items.add(Triple("Cozinha", Icons.Default.SoupKitchen, "Cozinha"))
        }

        if (userRole in listOf("owner", "waiter", "counter")) {
            items.add(Triple("Pedidos", Icons.AutoMirrored.Filled.List, "Pedidos"))
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
                    HorizontalDivider()
                }

                // --- ITEM DE CONFIGURAÇÃO (Para TODOS os usuários) ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Notifications, null) },
                    label = { Text("Config. de Notificações") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); navController.navigate("settings") } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // --- NOVO ITEM: CONFIGURAR MAQUININHA (Apenas para Gestores/Admin) ---
                if (userRole == "owner" || isAdminMode) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CreditCard, null) },
                        label = { Text("Configurar Maquininha") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate("payment_config")
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                if (userRole == "owner" || isAdminMode) {
                    Text("Gestão", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.QrCode2, null) },
                        label = { Text("Configurações da Loja") },
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
                        icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
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
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Print, null) },
                        label = { Text("Configuração de Impressora") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close(); navController.navigate("printer_config") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }

                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, null) },
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
                        val badgeCount = when(routeKey) {
                            "Cozinha" -> pendingKitchenCount
                            "Balcão" -> pendingCounterCount
                            else -> 0
                        }
                        val showCountBadge = badgeCount > 0
                        NavigationBarItem(
                            icon = {
                                if (showCountBadge) {
                                    BadgedBox(badge = {
                                        Badge { Text(badgeCount.toString()) }
                                    }) {
                                        Icon(icon, null, tint = if(isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                                    }
                                } else {
                                    Icon(icon, null)
                                }
                            },
                            label = { Text(title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            onClick = {
                                currentScreen = routeKey
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    "Cardápio" -> MainScreen(managementViewModel, orderViewModel, { navController.navigate("detail/$it") }, { navController.navigate("order_summary/null") }, { scope.launch { drawerState.open() } })
                    "Cozinha" -> KitchenScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                    "Pedidos" -> OrdersListScreen(kitchenViewModel, { scope.launch { drawerState.open() } }, { navController.navigate("order_summary/$it") })
                    "Balcão" -> CounterScreen(kitchenViewModel) { scope.launch { drawerState.open() } }
                    "Mesa" -> TableScreen(kitchenViewModel, { scope.launch { drawerState.open() } }, { navController.navigate("order_summary/$it") })
                }
            }
        }
    }
}

// ... (Restante do arquivo permanece igual) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyQRCodeScreen(
    companyId: String,
    companyName: String,
    onNavigateBack: () -> Unit,
    onNavigateToTableLayout: () -> Unit
) {
    var shareCode by remember { mutableStateOf("") }
    var tableCountText by remember { mutableStateOf("20") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(companyId) {
        Firebase.firestore.collection("companies").document(companyId).get()
            .addOnSuccessListener {
                shareCode = it.getString("shareCode") ?: ""
                val count = it.getLong("tableCount")?.toInt() ?: 20
                tableCountText = count.toString()
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val qrBitmap = remember(shareCode) {
        if (shareCode.isNotEmpty()) QRCodeUtils.generateQRCode(shareCode) else null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurações da Loja", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // verticalArrangement removed for better layout with top and bottom sections
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(companyName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Acesso de Funcionários",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Peça para o autônomo escanear ou digitar este código:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

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
                    text = shareCode,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
            } else {
                Box(Modifier.size(200.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                    Text("Erro ao carregar código")
                }
            }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "Quantidade de Mesas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Configure quantas mesas exibir na tela de atendimento de Salão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tableCountText,
                            onValueChange = { tableCountText = it.filter { char -> char.isDigit() } },
                            label = { Text("Número Total de Mesas") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val count = tableCountText.toIntOrNull() ?: return@Button
                                if (count in 1..200) {
                                    scope.launch {
                                        try {
                                            Firebase.firestore.collection("companies").document(companyId)
                                                .update("tableCount", count)
                                                .addOnSuccessListener {
                                                    scope.launch { snackbarHostState.showSnackbar("Mesas atualizadas com sucesso!") }
                                                }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Erro ao atualizar camas.")
                                        }
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Quantidade inválida (1-200)") }
                                }
                            },
                        ) {
                            Text("Salvar")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToTableLayout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configurar Layout do Salão")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    managementViewModel: ManagementViewModel,
    orderViewModel: OrderViewModel,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToOrder: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
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
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (!orderViewModel.isOrderEmpty && !showCartSheet) {
                ExtendedFloatingActionButton(
                    onClick = { showCartSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(0.9f)
                ) {
                    val itemCount = orderViewModel.currentCartItems.sumOf { it.quantity }
                    val totalPrice = orderViewModel.currentCartItems.sumOf { (it.singleItemTotalPrice * it.quantity) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$itemCount Itens • R$ %.2f".format(totalPrice), fontWeight = FontWeight.Bold)
                        }
                        Text("Ver Pedido", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Seu Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(orderViewModel.currentCartItems) { cartItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("${cartItem.quantity}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(cartItem.menuItem.name, fontWeight = FontWeight.Bold)
                                        if (cartItem.additionalIngredients.isNotEmpty()) {
                                            Text(cartItem.additionalIngredients.map { "${it.value}x ${it.key}" }.joinToString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                                Text("R$ %.2f".format(cartItem.singleItemTotalPrice * cartItem.quantity), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()
                        }
                    }
                    
                    val totalPrice = orderViewModel.currentCartItems.sumOf { (it.singleItemTotalPrice * it.quantity) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total do Pedido", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("R$ %.2f".format(totalPrice), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCartSheet = false },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Adicionar Itens", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                showCartSheet = false
                                onNavigateToOrder()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Finalizar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
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

            if (managementViewModel.isLoadingProducts) {
                // SKELETON SCREEN -> Carregando os Produtos
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(4) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(32.dp)
                                .shimmerEffect(shape = RoundedCornerShape(16.dp))
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 2000.dp)
                        ) {
                            items(10) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.85f)
                                        .shimmerEffect(shape = RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            } else if (categories.isNotEmpty()) {
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
        } catch (_: Exception) { null }
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

// ==========================================
// MODIFIER DO EFEITO CHINAMMER / SKELETON 
// ==========================================
fun Modifier.shimmerEffect(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = ""
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        ),
        shape = shape
    ).onGloballyPositioned {
        size = it.size
    }
}