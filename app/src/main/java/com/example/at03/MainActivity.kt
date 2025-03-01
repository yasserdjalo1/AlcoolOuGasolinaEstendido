package com.example.at03

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import com.example.at03.ui.theme.AT03Theme

// Definição da classe FuelStation
data class FuelStation(
    val name: String,
    val alcoholPrice: Float,
    val gasolinePrice: Float,
    val location: String,
    val date: String
) : Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AT03Theme {
                FuelCalculatorApp(context = this)
            }
        }
    }
}

@Composable
fun FuelCalculatorApp(context: Context) {
    var showStationList by remember { mutableStateOf(false) }
    var showCalculationScreen by remember { mutableStateOf(false) }
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showEditStationDialog by remember { mutableStateOf(false) }
    var selectedStationIndex by remember { mutableStateOf(-1) }
    var stations by remember { mutableStateOf(loadFuelStations(context)) }

    // Atualiza a lista de postos sempre que houver mudanças
    LaunchedEffect(showAddStationDialog, showEditStationDialog) {
        stations = loadFuelStations(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showStationList) {
            FuelStationList(
                stations = stations,
                onStationSelected = { index ->
                    selectedStationIndex = index
                    showEditStationDialog = true
                },
                onAddStation = { showAddStationDialog = true },
                onBack = { showStationList = false }
            )
        } else if (showCalculationScreen) {
            AlcoholGasolineCalculationScreen(
                onBack = { showCalculationScreen = false }
            )
        } else {
            // Tela principal do app
            Image(
                painter = painterResource(id = R.drawable.fuel2_image), // Substitua pelo ID da sua imagem
                contentDescription = "Fuel Image",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { showStationList = true }) {
                Text(text = context.getString(R.string.see_gas_stations))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showCalculationScreen = true }) {
                Text(text = context.getString(R.string.calculate_alc_gas))
            }
        }
    }

    // Diálogo para adicionar um novo posto
    if (showAddStationDialog) {
        AddEditStationDialog(
            onDismiss = { showAddStationDialog = false },
            onSave = { station ->
                addFuelStation(context, station)
                showAddStationDialog = false
            }
        )
    }

    // Diálogo para editar um posto existente
    if (showEditStationDialog && selectedStationIndex != -1) {
        AddEditStationDialog(
            station = stations[selectedStationIndex],
            onDismiss = { showEditStationDialog = false },
            onSave = { updatedStation ->
                editFuelStation(context, selectedStationIndex, updatedStation)
                showEditStationDialog = false
            },
            onDelete = {
                deleteFuelStation(context, selectedStationIndex)
                showEditStationDialog = false
            }
        )
    }
}

@Composable
fun FuelStationList(
    stations: List<FuelStation>,
    onStationSelected: (Int) -> Unit,
    onAddStation: () -> Unit,
    onBack: () -> Unit
) {
    Column {
        val context = LocalContext.current

        Button(onClick = onBack) {
            Text(text = context.getString(R.string.go_back))
        }
        LazyColumn {
            items(stations.size) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onStationSelected(index) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stations[index].name, fontSize = 18.sp)
                        Text(text = "${context.getString(R.string.alcohol)}: R$ ${stations[index].alcoholPrice}", fontSize = 14.sp)
                        Text(text = "${context.getString(R.string.gasoline)}: R$ ${stations[index].gasolinePrice}", fontSize = 14.sp)
                        Text(text = "${context.getString(R.string.location)}: ${stations[index].location}", fontSize = 14.sp)
                        Text(text = "${context.getString(R.string.date)}: ${stations[index].date}", fontSize = 14.sp)
                    }
                }
            }
            item {
                Button(onClick = onAddStation) {
                    Text(text = context.getString(R.string.add_station))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlcoholGasolineCalculationScreen(onBack: () -> Unit) {
    var alcoholPrice by remember { mutableStateOf("") }
    var gasolinePrice by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(70) }
    var resultMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Álcool (preço por litro)", fontSize = 16.sp)
        OutlinedTextField(
            value = alcoholPrice,
            onValueChange = { alcoholPrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Gasolina (preço por litro)", fontSize = 16.sp)
        OutlinedTextField(
            value = gasolinePrice,
            onValueChange = { gasolinePrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(text = "Escolha a porcentagem", fontSize = 16.sp)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf("70%", "75%")
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = {
                            selectedPercentage = if (label == "70%") 70 else 75
                        },
                        selected = selectedPercentage == if (label == "70%") 70 else 75,
                        label = { Text(label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val alcohol = alcoholPrice.toFloatOrNull()
            val gasoline = gasolinePrice.toFloatOrNull()

            resultMessage = if (alcohol == null || gasoline == null) {
                "Por favor, preencha todos os campos."
            } else {
                val threshold = gasoline * (selectedPercentage / 100.0)
                if (alcohol <= threshold) {
                    "Vale mais a pena usar Álcool!"
                } else {
                    "Vale mais a pena usar Gasolina!"
                }
            }
        }) {
            Text("Calcular")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = resultMessage, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Voltar")
        }
    }
}

@Composable
fun AddEditStationDialog(
    station: FuelStation? = null,
    onDismiss: () -> Unit,
    onSave: (FuelStation) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(station?.name ?: "") }
    var alcoholPrice by remember { mutableStateOf(station?.alcoholPrice?.toString() ?: "") }
    var gasolinePrice by remember { mutableStateOf(station?.gasolinePrice?.toString() ?: "") }
    var location by remember { mutableStateOf(station?.location ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (station == null) "Adicionar Posto" else "Editar Posto") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Posto") }
                )
                OutlinedTextField(
                    value = alcoholPrice,
                    onValueChange = { alcoholPrice = it },
                    label = { Text("Preço do Álcool") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = gasolinePrice,
                    onValueChange = { gasolinePrice = it },
                    label = { Text("Preço da Gasolina") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localização") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newStation = FuelStation(
                    name = name,
                    alcoholPrice = alcoholPrice.toFloatOrNull() ?: 0f,
                    gasolinePrice = gasolinePrice.toFloatOrNull() ?: 0f,
                    location = location,
                    date = SimpleDateFormat("dd/MM/yyyy").format(Date())
                )
                onSave(newStation)
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            if (onDelete != null) {
                Button(onClick = onDelete) {
                    Text("Excluir")
                }
            }
        }
    )
}

// Funções para manipular SharedPreferences
fun saveFuelStations(context: Context, stations: List<FuelStation>) {
    val sharedPreferences = context.getSharedPreferences("FuelAppPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(stations)
    editor.putString("fuelStations", json)
    editor.apply()
}

fun loadFuelStations(context: Context): List<FuelStation> {
    val sharedPreferences = context.getSharedPreferences("FuelAppPrefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("fuelStations", "[]")
    val type = object : TypeToken<List<FuelStation>>() {}.type
    return Gson().fromJson(json, type)
}

fun addFuelStation(context: Context, station: FuelStation) {
    val stations = loadFuelStations(context).toMutableList()
    stations.add(station)
    saveFuelStations(context, stations)
}

fun editFuelStation(context: Context, index: Int, updatedStation: FuelStation) {
    val stations = loadFuelStations(context).toMutableList()
    if (index in stations.indices) {
        stations[index] = updatedStation
        saveFuelStations(context, stations)
    }
}

fun deleteFuelStation(context: Context, index: Int) {
    val stations = loadFuelStations(context).toMutableList()
    if (index in stations.indices) {
        stations.removeAt(index)
        saveFuelStations(context, stations)
    }
}