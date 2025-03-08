class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Constructor de la clase padre
        setContent { //Se llama a la funcion UIPrincipal para mostrar la interfaz
            setContent{ UIPrincipal()}
        }
    }
}




@Composable
fun UIPrincipal() {
    val contexto = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }

    // Aplicamos padding a la columna principal (actúa como margen)
    Column(
        modifier = Modifier
            .padding(16.dp) // margen exterior alrededor de toda la columna
    ) {

        Text(
            text = "Nombre: ",
            modifier = Modifier.padding(bottom = 8.dp) // espacio debajo del texto
        )

        OutlinedTextField(
            singleLine = true,
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Introduce tu nombre") },
            modifier = Modifier
                .padding(bottom = 16.dp) // espacio debajo del campo
        )

        Button(
            onClick = {
                Toast.makeText(contexto, "Hola $nombre", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(top = 8.dp) // espacio encima del botón
        ) {
            Text("Saludar!")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Previsualizacion() {
    UIPrincipal()
}