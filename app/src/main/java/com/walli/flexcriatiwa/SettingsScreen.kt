package com.walli.flexcriatiwa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val offlineManager = remember { OfflineSessionManager(context) }

    var notifyKitchen by remember { mutableStateOf(offlineManager.getNotifyKitchen()) }
    var notifyCounter by remember { mutableStateOf(offlineManager.getNotifyCounter()) }

    var printerMac by remember { mutableStateOf(offlineManager.getPrinterMacAddress()) }
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
                pairedDevices = bluetoothManager.adapter?.bondedDevices?.toList() ?: emptyList()
            } catch (e: SecurityException) {
                hasBluetoothPermission = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Notificações",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Switch 1: Novos Pedidos (Cozinha)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Novos Pedidos (Cozinha)", fontWeight = FontWeight.Bold)
                            Text(
                                "Receber alerta quando um novo pedido for enviado para preparo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyKitchen,
                            onCheckedChange = {
                                notifyKitchen = it
                                offlineManager.setNotifyKitchen(it)
                            }
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // Switch 2: Pedidos Prontos (Balcão)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pedidos Prontos (Balcão/Garçom)", fontWeight = FontWeight.Bold)
                            Text(
                                "Receber alerta quando um pedido estiver pronto para entrega.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyCounter,
                            onCheckedChange = {
                                notifyCounter = it
                                offlineManager.setNotifyCounter(it)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Impressora Térmica (Bluetooth)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
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
                        ExposedDropdownMenuBox(
                            expanded = expandedPrinterMenu,
                            onExpandedChange = { expandedPrinterMenu = !expandedPrinterMenu }
                        ) {
                            @SuppressLint("MissingPermission")
                            val selectedDeviceName = pairedDevices.find { it.address == printerMac }?.name ?: "Nenhuma impressora selecionada"
                            OutlinedTextField(
                                value = selectedDeviceName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Impressora Padrão") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrinterMenu) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPrinterMenu,
                                onDismissRequest = { expandedPrinterMenu = false }
                            ) {
                                pairedDevices.forEach { device ->
                                    @SuppressLint("MissingPermission")
                                    DropdownMenuItem(
                                        text = { Text("${device.name} (${device.address})") },
                                        onClick = {
                                            printerMac = device.address
                                            offlineManager.setPrinterMacAddress(device.address)
                                            expandedPrinterMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val mac = printerMac
                                if (mac != null) {
                                    scope.launch {
                                        val success = withContext(Dispatchers.IO) {
                                            val impressora = EscPosPrinter()
                                            val itens = listOf(
                                                EscPosPrinter.Item(1, "Impressão Teste", observacoes = listOf("FLEX CRIATIWA"))
                                            )
                                            val pedido = EscPosPrinter.Pedido("000", "MESA: TESTE", "LOCAL", "01/01/2026 12:00", itens)
                                            val bytes = impressora.gerarBufferBytes("--- TESTE DE COMUNICAÇÃO ---", pedido)
                                            EscPosPrinter.imprimirBuffer(context, mac, bytes)
                                        }
                                        if (success) {
                                            snackbarHostState.showSnackbar("Teste enviado com sucesso!")
                                        } else {
                                            snackbarHostState.showSnackbar("Erro: Falha ao enviar para impressora.")
                                        }
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Selecione uma impressora primeiro.") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar Teste de Impressão")
                        }
                    }
                }
            }
        }
    }
}