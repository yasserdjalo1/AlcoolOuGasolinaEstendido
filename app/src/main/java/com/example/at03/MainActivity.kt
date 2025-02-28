package com.example.at03

    import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.at03.ui.theme.AT03Theme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AT03Theme {
                FuelCalculatorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelCalculatorApp() {
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
        // Imagem
        Image(
            painter = painterResource(id = R.drawable.fuel2_image), // Substitua pelo ID da sua imagem
            contentDescription = "Fuel Image",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para álcool
        Text(text = "Álcool (preço por litro)", fontSize = 16.sp)
        OutlinedTextField(
            value = alcoholPrice,
            onValueChange = { alcoholPrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para gasolina
        Text(text = "Gasolina (preço por litro)", fontSize = 16.sp)
        OutlinedTextField(
            value = gasolinePrice,
            onValueChange = { gasolinePrice = it },
            label = { Text("R$ 0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            //modifier = Modifier.fillMaxWidth(),
            //horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Escolha a porcentagem", fontSize = 16.sp)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Opções do botão segmentado
                val options = listOf("70%", "75%")
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { selectedPercentage = if (label == "70%") 70 else 75 },
                        selected = selectedPercentage == if (label == "70%") 70 else 75,
                        label = { Text(label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão Calcular
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

        // Mensagem de resultado
        Text(text = resultMessage, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    }
}
