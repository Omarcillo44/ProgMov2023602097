fun main() {

    var opcion:Int //Iterador del menú
    val scanner = Scanner(System.`in`) //Scanner para el menú

    do  {
        //Menú de opciones
        println("""
        Bienvenido, seleccione una opción
        1. Suma de 3 numeros
        2. Captura de nombre
        3. Estadísticas - Fecha de Nacimiento
        0. Salir
        """.trimIndent())

        opcion = scanner.nextInt() //Se lee la opcion

        when (opcion) {
            1 -> {
                suma3Numeros()
            }
            2 -> {
                capturaDeNombre()
            }
            3 -> {
                estadisticasDesdeNacimiento()
            }
            0 -> {
                println("Adiós")
            } else -> {
                println("Selecciona una opción válida")
            }
        }
    } while(opcion != 0) //Termina el programa
}

fun suma3Numeros() {

    val scanner = Scanner(System.`in`)
    try {
        val numero1 = scanner.nextDouble()
        val numero2 = scanner.nextDouble()
        val numero3 = scanner.nextDouble()

        val suma = numero1 + numero2 + numero3
        println("La suma es: $suma")

    } catch (e: InputMismatchException) {
        println("Ingresa números válidos")
        suma3Numeros()
    }
}

fun capturaDeNombre() {
    println("Ingresa tu nombre")
    val scanner = Scanner(System.`in`)
    val nombre = scanner.nextLine()
    println("Tu nombre: $nombre")
}

fun estadisticasDesdeNacimiento() {
    val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    val scanner = Scanner(System.`in`)

    println("""
        Ingresa tu fecha de Nacimiento con el formato Día-Mes-Año
        Por ejemplo: 24-10-2004
    """.trimIndent())

    try {
        val entrada: String = scanner.next()
        val fecha = LocalDateTime.parse("$entrada 00:00:00", formatoFecha)

        val mesesTranscurridos = ChronoUnit.MONTHS.between(fecha, LocalDateTime.now())
        val semanasTranscurridas = ChronoUnit.WEEKS.between(fecha, LocalDateTime.now())
        val diasTranscurridos = ChronoUnit.DAYS.between(fecha, LocalDateTime.now())
        val horasTranscurridas = ChronoUnit.HOURS.between(fecha, LocalDateTime.now())
        val minutosTranscurridos = ChronoUnit.MINUTES.between(fecha, LocalDateTime.now())
        val segundosTranscurridos = ChronoUnit.SECONDS.between(fecha, LocalDateTime.now())

        println(
            """
        Desde la fecha de nacimiento $fecha han pasado:
        $mesesTranscurridos ${if (mesesTranscurridos.toInt() == 1) "mes" else "meses"}
        $semanasTranscurridas ${if (semanasTranscurridas.toInt() == 1) "semana" else "semanas"}
        $diasTranscurridos ${if (mesesTranscurridos.toInt() == 1) "día" else "días"}
        $horasTranscurridas horas
        $minutosTranscurridos minutos
        $segundosTranscurridos segundos
        
            """.trimIndent()
        )
    } catch (e: DateTimeParseException) {
        println("Ingresa una fecha válida")
        estadisticasDesdeNacimiento()
    }
}