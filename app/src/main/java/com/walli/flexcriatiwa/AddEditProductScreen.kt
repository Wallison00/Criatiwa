package com.walli.flexcriatiwa

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    managementViewModel: ManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val productBeingEdited = managementViewModel.productToEdit
    val isEditing = productBeingEdited != null

    var productName by remember { mutableStateOf(productBeingEdited?.name ?: "") }
    var productPrice by remember { mutableStateOf(if (isEditing) (productBeingEdited!!.price * 100).toLong().toString() else "") }
    var selectedCategory by remember { mutableStateOf(productBeingEdited?.category ?: managementViewModel.categories.firstOrNull() ?: "") }
    var selectedIngredients by remember { mutableStateOf(productBeingEdited?.ingredients ?: emptySet()) }
    var selectedOptionals by remember { mutableStateOf(productBeingEdited?.optionals ?: emptySet()) }
    var isActive by remember { mutableStateOf(productBeingEdited?.isActive ?: true) }

    var newSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val existingImageUrl = productBeingEdited?.imageUrl ?: ""

    // Decodificador manual para mostrar a imagem antiga
    val existingBitmap = remember(existingImageUrl) {
        try {
            if (existingImageUrl.startsWith("data:image")) {
                val base64String = existingImageUrl.substringAfter(",")
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } else null
        } catch (e: Exception) { null }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) newSelectedImageUri = uri }
    )

    DisposableEffect(Unit) {
        onDispose { managementViewModel.clearEditState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Produto" else "Adicionar Produto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val priceDouble = (productPrice.toLongOrNull() ?: 0L) / 100.0
                        val productToSave = ManagedProduct(
                            id = productBeingEdited?.id ?: "",
                            name = productName,
                            price = priceDouble,
                            imageUrl = existingImageUrl,
                            isActive = isActive,
                            category = selectedCategory,
                            ingredients = selectedIngredients,
                            optionals = selectedOptionals
                        )

                        managementViewModel.saveProductWithImage(
                            context = context,
                            product = productToSave,
                            newImageUri = newSelectedImageUri,
                            onSuccess = { onNavigateBack() }
                        )
                    },
                    enabled = !managementViewModel.isUploading && productName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (managementViewModel.isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processando...")
                    } else {
                        Text("Salvar Produto", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (newSelectedImageUri != null) {
                            // Imagem nova da galeria
                            AsyncImage(model = newSelectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (existingBitmap != null) {
                            // Imagem decodificada do banco
                            Image(bitmap = existingBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, "Add Foto", Modifier.size(48.dp), tint = Color.Gray)
                                Text("Tocar para adicionar foto", color = Color.Gray)
                            }
                        }
                    }
                }
            }

            item { FormSection("Nome do Produto") { OutlinedTextField(value = productName, onValueChange = { productName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ex: X-Bacon") }, singleLine = true) } }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Produto Ativo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }

            item {
                FormSection("Preço") {
                    OutlinedTextField(
                        value = productPrice,
                        onValueChange = { productPrice = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("R$ 0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CurrencyVisualTransformation()
                    )
                }
            }

            item {
                FormSection("Categoria") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        managementViewModel.categories.forEach { category ->
                            FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                        }
                    }
                }
            }

            item {
                FormSection("Ingredientes (Padrão)") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        managementViewModel.ingredients.forEach { ingredient ->
                            val isSelected = selectedIngredients.contains(ingredient)
                            FilterChip(selected = isSelected, onClick = { selectedIngredients = if (isSelected) selectedIngredients - ingredient else selectedIngredients + ingredient }, label = { Text(ingredient) })
                        }
                    }
                }
            }

            item {
                FormSection("Opcionais Permitidos") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        managementViewModel.optionals.forEach { optional ->
                            val isSelected = selectedOptionals.contains(optional)
                            FilterChip(selected = isSelected, onClick = { selectedOptionals = if (isSelected) selectedOptionals - optional else selectedOptionals + optional }, label = { Text(optional.name) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}