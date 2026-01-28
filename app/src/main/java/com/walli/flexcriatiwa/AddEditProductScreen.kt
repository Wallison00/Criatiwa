package com.walli.flexcriatiwa // Use o seu package

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.filled.Close // Para o ícone 'X'
import androidx.compose.foundation.text.KeyboardActions // Para a ação do teclado

import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

// --- IMPORTANTE: ADICIONE ESTA LINHA SE AINDA NÃO TIVER ---
// Se você já tem a classe CurrencyVisualTransformation em outro arquivo, não precisa copiar.
// Se não, copie-a para o final deste arquivo ou para um arquivo separado.
// import com.walli.flexcriatiwa.CurrencyVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    // 2. VERIFICA SE ESTAMOS EDITANDO OU ADICIONANDO
    val productBeingEdited = managementViewModel.productToEdit
    val isEditing = productBeingEdited != null


    // 3. INICIALIZA OS ESTADOS COM OS DADOS DO PRODUTO (se estiver editando)
    var productName by remember { mutableStateOf(productBeingEdited?.name ?: "") }
    var productPrice by remember { mutableStateOf(
        if (isEditing) (productBeingEdited!!.price * 100).toLong().toString() else ""
    ) }
    // --- CORREÇÃO AQUI ---
    var selectedCategory by remember { mutableStateOf(
        productBeingEdited?.category ?: managementViewModel.categories.firstOrNull() ?: ""
    ) }
    var selectedIngredients by remember { mutableStateOf(productBeingEdited?.ingredients ?: emptySet()) }
    var selectedOptionals by remember { mutableStateOf(productBeingEdited?.optionals ?: emptySet()) }
    // ---------------------

    var isActive by remember { mutableStateOf(productBeingEdited?.isActive ?: true) }

    // EFEITO PARA LIMPAR O ESTADO QUANDO A TELA É FECHADA
    DisposableEffect(Unit) {
        onDispose {
            managementViewModel.clearEditState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Produto" else "Adicionar Produto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        // --- 5. LÓGICA DE SALVAR ---
                        val priceDouble = (productPrice.toLongOrNull() ?: 0L) / 100.0
                        val newOrUpdatedProduct = ManagedProduct(
                            id = productBeingEdited?.id ?: System.currentTimeMillis().toString(),
                            name = productName,
                            price = priceDouble,
                            imageUrl = "", // TODO
                            isActive = isActive,
                            // --- CORREÇÃO AQUI: INCLUIR OS DADOS NO OBJETO SALVO ---
                            category = selectedCategory,
                            ingredients = selectedIngredients,
                            optionals = selectedOptionals
                            // ----------------------------------------------------
                        )

                        // CHAMA A FUNÇÃO DO VIEWMODEL
                        managementViewModel.upsertProduct(newOrUpdatedProduct)

                        // NAVEGA DE VOLTA
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvar Produto", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. SEÇÃO DE ADICIONAR FOTO ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { /* TODO: Lógica para abrir galeria */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Adicionar Foto",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Adicionar Foto", color = Color.Gray)
                    }
                }
            }

            // --- 2. CAMPO NOME DO PRODUTO ---
            item {
                FormSection(title = "Nome do Produto") {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        placeholder = { Text("Digite o nome do produto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // --- ADICIONE ESTE NOVO ITEM PARA O STATUS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Produto Ativo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }

            // --- 3. CAMPO PREÇO ---
            item {
                FormSection(title = "Preço") {
                    OutlinedTextField(
                        value = productPrice,
                        onValueChange = {
                            productPrice = it.filter { char -> char.isDigit() }
                        },
                        placeholder = { Text("R$ 0,00") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CurrencyVisualTransformation() // Garanta que esta classe existe no seu projeto
                    )
                }
            }

            // --- 4. SELEÇÃO DE CATEGORIA ---
            item {
                FormSection(title = "Categoria do Produto") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        managementViewModel.categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }

            // --- 5. SELEÇÃO DE INGREDIENTES ---
            item {
                FormSection(title = "Ingredientes") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        managementViewModel.ingredients.forEach { ingredient ->
                            val isSelected = selectedIngredients.contains(ingredient)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedIngredients = if (isSelected) selectedIngredients - ingredient else selectedIngredients + ingredient
                                },
                                label = { Text(ingredient) }
                            )
                        }
                    }
                }
            }

            // --- 6. SELEÇÃO DE OPCIONAIS ---
            item {
                FormSection(title = "Opcionais Disponíveis") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        managementViewModel.optionals.forEach { optional ->
                            val isSelected = selectedOptionals.contains(optional)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedOptionals = if (isSelected) selectedOptionals - optional else selectedOptionals + optional
                                },
                                label = { Text("${optional.name} (+ R$ ${"%.2f".format(optional.price)})") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Componente auxiliar para criar as seções com título
@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}