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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class FuelStation(
    val name: String,
    val alcoholPrice: Float,
    val gasolinePrice: Float,
    val location: String,
    val date: String,
    val latitude: Double,
    val longitude: Double
) : Serializable

class MainActivity : ComponentActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        setContent {
            AT03Theme {
                FuelCalculatorApp(context = this)
            }
        }
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun getLastLocation(onLocationReceived: (latitude: Double, longitude: Double) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocationReceived(location.latitude, location.longitude)
                    }
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
    val context = LocalContext.current

    Column {
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

                        // Verifica se a latitude e longitude são diferentes de zero
                        if (stations[index].latitude != 0.0 && stations[index].longitude != 0.0) {
                            Button(onClick = {
                                showLocationOnMap(context, stations[index].latitude, stations[index].longitude)
                            }) {
                                Text(text = context.getString(R.string.view_map_text))
                            }
                        }
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
    val context = LocalContext.current
    var selectedPercentage by remember { mutableStateOf(loadSelectedPercentage(context)) }
    var resultMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = context.getString(R.string.alcohol_price), fontSize = 16.sp)
        OutlinedTextField(
            value = alcoholPrice,
            onValueChange = { alcoholPrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = context.getString(R.string.gasoline_price), fontSize = 16.sp)
        OutlinedTextField(
            value = gasolinePrice,
            onValueChange = { gasolinePrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(text = context.getString(R.string.choose_percentage), fontSize = 16.sp)
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
                            saveSelectedPercentage(context, selectedPercentage)
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
                context.getString(R.string.fill_all_fields_text)
            } else {
                val threshold = gasoline * (selectedPercentage / 100.0)
                if (alcohol <= threshold) {
                    context.getString(R.string.alcohol_better)
                } else {
                    context.getString(R.string.gasoline_better)
                }
            }
        }) {
            Text(text = context.getString(R.string.calculate))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = resultMessage, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text(text = context.getString(R.string.go_back))
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
    var latitude by remember { mutableStateOf(station?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(station?.longitude?.toString() ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (station == null) context.getString(R.string.add_station) else context.getString(R.string.edit_station) ) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = context.getString(R.string.name_station)) }
                )
                OutlinedTextField(
                    value = alcoholPrice,
                    onValueChange = { alcoholPrice = it },
                    label = { Text(text = context.getString(R.string.alcohol_price_text)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = gasolinePrice,
                    onValueChange = { gasolinePrice = it },
                    label = { Text(text = context.getString(R.string.gasoline_price_text)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(text = context.getString(R.string.location)) }
                )
                // Botão para capturar a localização
                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { location ->
                                if (location != null) {
                                    latitude = location.latitude.toString()
                                    longitude = location.longitude.toString()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.location_unable), Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, context.getString(R.string.location_denied), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(context.getString(R.string.location_capture))
                }
                // Exibir latitude e longitude capturadas
                Text("${context.getString(R.string.location_capture)}: $latitude")
                Text("${context.getString(R.string.location_capture)}: $longitude")
            }
        },
        confirmButton = {
            Button(onClick = {
                val newStation = FuelStation(
                    name = name,
                    alcoholPrice = alcoholPrice.toFloatOrNull() ?: 0f,
                    gasolinePrice = gasolinePrice.toFloatOrNull() ?: 0f,
                    location = location,
                    date = SimpleDateFormat("dd/MM/yyyy").format(Date()),
                    latitude = latitude.toDoubleOrNull() ?: 0.0, // Salvar latitude
                    longitude = longitude.toDoubleOrNull() ?: 0.0 // Salvar longitude
                )
                onSave(newStation)
            }) {
                Text(context.getString(R.string.save))
            }
        },
        dismissButton = {
            if (onDelete != null) {
                Button(onClick = onDelete) {
                    Text(context.getString(R.string.delete))
                }
            }
        }
    )
}

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

fun saveSelectedPercentage(context: Context, percentage: Int) {
    val sharedPreferences = context.getSharedPreferences("FuelAppPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putInt("selectedPercentage", percentage)
    editor.apply()
}

fun loadSelectedPercentage(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("FuelAppPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getInt("selectedPercentage", 70) // 70 é o valor padrão
}

fun showLocationOnMap(context: Context, latitude: Double, longitude: Double) {
    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    context.startActivity(mapIntent)
}