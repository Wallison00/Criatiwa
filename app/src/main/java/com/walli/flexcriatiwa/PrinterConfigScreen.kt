package com.walli.flexcriatiwa

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterConfigScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val offlineManager = remember { OfflineSessionManager(context) }

    var registeredPrinters by remember { mutableStateOf(offlineManager.getRegisteredPrinters()) }
    var printerStatuses by remember { mutableStateOf<Map<String, Boolean?>>(emptyMap()) } // mac -> true(online), false(offline), null(verificando)
    var nextUpdateSeconds by remember { mutableIntStateOf(10) }

    // Form inputs
    var selectedMac by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var printerAlias by remember { mutableStateOf("") }

    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var expandedPrinterMenu by remember { mutableStateOf(false) }
    var hasBluetoothPermission by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = permissions.values.all { it }
        if (hasBluetoothPermission) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            @SuppressLint("MissingPermission")
            pairedDevices = bluetoothManager.adapter?.bondedDevices?.toList() ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        hasBluetoothPermission = bluetoothPermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }
        if (hasBluetoothPermission) {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                @SuppressLint("MissingPermission")
                pairedDevices = bluetoothManager.adapter?.bondedDevices?.toList() ?: emptyList()
            } catch (_: SecurityException) {
                hasBluetoothPermission = false
            }
        }
    }

    // Ping Constants and Coroutine
    LaunchedEffect(registeredPrinters, hasBluetoothPermission) {
        if (!hasBluetoothPermission || registeredPrinters.isEmpty()) return@LaunchedEffect
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        // Loop infinito pingando as impressoras da lista
        while(true) {
            nextUpdateSeconds = 0 // Indica que está verificando
            val newStatuses = mutableMapOf<String, Boolean?>()
            withContext(Dispatchers.IO) {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Genérico Serial
                for (printer in registeredPrinters) {
                    try {
                        val device = adapter?.getRemoteDevice(printer.mac)
                        @SuppressLint("MissingPermission")
                        val socket = device?.createRfcommSocketToServiceRecord(uuid)
                        socket?.connect()
                        socket?.close()
                        newStatuses[printer.mac] = true
                    } catch (_: Exception) {
                        newStatuses[printer.mac] = false
                    }
                }
            }
            printerStatuses = newStatuses
            
            // Countdown visual de 10 segundos
            for (i in 10 downTo 1) {
                nextUpdateSeconds = i
                delay(1000)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cadastro de Impressoras", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Nova Impressora",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (!hasBluetoothPermission) {
                            Button(
                                onClick = { permissionLauncher.launch(bluetoothPermissions) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Conceder Permissões de Bluetooth")
                            }
                        } else {
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expandedPrinterMenu,
                                onExpandedChange = { expandedPrinterMenu = !expandedPrinterMenu }
                            ) {
                                val displayValue = selectedName.ifEmpty { "Selecione a Impressora Pareada" }
                                OutlinedTextField(
                                    value = displayValue,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Dispositivo Pareado no Tablet") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrinterMenu) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPrinterMenu,
                                    onDismissRequest = { expandedPrinterMenu = false }
                                ) {
                                    pairedDevices.forEach { device ->
                                        @SuppressLint("MissingPermission")
                                        DropdownMenuItem(
                                            text = { Text("${device.name ?: "Desconhecido"} (${device.address})") },
                                            onClick = {
                                                selectedMac = device.address
                                                selectedName = device.name ?: "Desconhecido"
                                                expandedPrinterMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = printerAlias,
                                onValueChange = { printerAlias = it },
                                label = { Text("Apelido / Nome Interno") },
                                placeholder = { Text("Ex: Impressora da Cozinha, Balcão...") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (selectedMac != null && printerAlias.isNotBlank()) {
                                        val newPrinter = RegisteredPrinter(
                                            mac = selectedMac!!,
                                            name = selectedName,
                                            alias = printerAlias
                                        )
                                        offlineManager.addRegisteredPrinter(newPrinter)
                                        registeredPrinters = offlineManager.getRegisteredPrinters() // RECARREGA LISTA
                                        
                                        // Padrão Fallback se app não tiver múltipla implementada em outras telas
                                        if (offlineManager.getPrinterMacAddress() == null) {
                                            offlineManager.setPrinterMacAddress(newPrinter.mac)
                                            offlineManager.setPrinterAlias(newPrinter.alias)
                                        }

                                        selectedMac = null
                                        selectedName = ""
                                        printerAlias = ""
                                        scope.launch { snackbarHostState.showSnackbar("Impressora Registrada com sucesso!") }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Selecione um aparelho e digite um apelido.") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Salvar Cadastro")
                            }
                        }
                    }
                }
            }

            if (registeredPrinters.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Impressoras Cadastradas",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (nextUpdateSeconds > 0) {
                            Text("Atualiza em ${nextUpdateSeconds}s", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                        } else {
                            Text("Atualizando...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            items(registeredPrinters) { printer ->
                val status = printerStatuses[printer.mac] 
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = "Impressora", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(printer.alias, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${printer.name} • ${printer.mac}", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                            }
                            IconButton(
                                onClick = {
                                    offlineManager.removeRegisteredPrinter(printer.mac)
                                    registeredPrinters = offlineManager.getRegisteredPrinters()
                                    // Fallback limpa o primeiro do banco único antigo
                                    if (offlineManager.getPrinterMacAddress() == printer.mac) {
                                        val nextDefault = registeredPrinters.firstOrNull()
                                        offlineManager.setPrinterMacAddress(nextDefault?.mac)
                                        offlineManager.setPrinterAlias(nextDefault?.alias)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Status Sync: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            
                            when (status) {
                                null -> {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pingando Bluetooth...", color = androidx.compose.ui.graphics.Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                true -> {
                                    Box(modifier = Modifier.size(10.dp).background(androidx.compose.ui.graphics.Color(0xFF4CAF50), CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Online (Pronta para uso)", color = androidx.compose.ui.graphics.Color(0xFF4CAF50), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                                false -> {
                                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Offline (Inacessível / Desligada)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        val impressora = EscPosPrinter()
                                        val itens = listOf(EscPosPrinter.Item(1, "Impressão Teste", observacoes = emptyList()))
                                        val pedido = EscPosPrinter.Pedido("000", "TESTE DE PING", "APP", "01/01/2026 12:00", itens)
                                        val bytes = impressora.gerarBufferBytes("--- TESTE -> ${printer.alias.uppercase()} ---", pedido)
                                        EscPosPrinter.imprimirBuffer(context, printer.mac, bytes)
                                    }
                                    if (success) snackbarHostState.showSnackbar("Documento enviado para '${printer.alias}'!")
                                    else snackbarHostState.showSnackbar("Falha ao abrir Socket na impressora '${printer.alias}'.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text("Imprimir Folha Teste")
                        }
                    }
                }
            }
        }
    }
}
