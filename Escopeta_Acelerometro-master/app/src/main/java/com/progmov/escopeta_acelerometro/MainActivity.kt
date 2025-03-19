package com.progmov.escopeta_acelerometro

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null

    private var escopeta = Escopeta()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Constructor de la clase padre
        setContent { //Se llama a la funcion UIPrincipal para mostrar la interfaz
            setContent { UIPrincipal() }
        }

        // Inicializar SensorManager y obtener el acelerómetro
        sensorManager = getSystemService(SensorManager::class.java)
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI) // Para movimientos más lentos
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    private val medicionesBombeo = mutableListOf<Triple<Float, Float, Float>>()
    private val medicionesDisparo = mutableListOf<Triple<Float, Float, Float>>()
    private val medicionesRecarga = mutableListOf<Triple<Float, Float, Float>>()

    private var enPeriodoEnfriamiento = false
    private var ultimoTiempoNotificacion: Long = 0
    private val DELAY_ENFRIAMIENTO = 500_000_000L // 0.5 segundos de enfriamiento

    var flagPosicionDisparo = false


    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val tiempoActual = event.timestamp

            if (checaSiEstaEnDelay(tiempoActual)) return@let

            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            bombeo(x, y, z, tiempoActual)
            disparo(x, y, z, tiempoActual)
            recarga(x, y, z, tiempoActual)
        }
    }

    private fun bombeo(x: Float, y: Float, z: Float, tiempoActual: Long) {
        val UMBRAL_MAX_Y = 20f
        val UMBRAL_MAX_X = 8f
        val UMBRAL_MAX_Z = 5f
        val CANTIDAD_MEDICIONES = 5

        if (y >= UMBRAL_MAX_Y && z < UMBRAL_MAX_X) {
            medicionesBombeo.add(Triple(x, y, z))
            if (medicionesBombeo.size >= CANTIDAD_MEDICIONES) {
                MediaPlayer.create(this, R.raw.bombeo).start()
                escopeta.bombeo()
                iniciarEnfriamiento(tiempoActual)
                medicionesBombeo.clear()
            }
        } else {
            medicionesBombeo.clear()
        }
    }

    private fun disparo(x: Float, y: Float, z: Float, tiempoActual: Long) {
        val UMBRAL_Y = 4f
        val UMBRAL_Z = 4f
        val UMBRAL_INICIAL_X_MIN = 7f
        val UMBRAL_INICIAL_X_MAX = 12f
        val CANTIDAD_MEDICIONES_POSICION = 20
        val CANTIDAD_MEDICIONES_MOVIMIENTO = 8

        // Primera fase: detectar posición de disparo
        if (!flagPosicionDisparo) {
            // Verificar si el dispositivo está en posición horizontal
            if (z < UMBRAL_Z && x in UMBRAL_INICIAL_X_MIN..UMBRAL_INICIAL_X_MAX && y < UMBRAL_Y) {
                medicionesDisparo.add(Triple(x, y, z))

                // Limitar tamaño de la lista para evitar crecimiento excesivo
                if (medicionesDisparo.size > CANTIDAD_MEDICIONES_POSICION) {
                    medicionesDisparo.removeAt(0)
                }

                if (medicionesDisparo.size >= CANTIDAD_MEDICIONES_POSICION) {
                    println("Posición de disparo detectada")
                    flagPosicionDisparo = true
                    medicionesDisparo.clear() // Limpiamos para la siguiente fase
                }
            } else {
                medicionesDisparo.clear()
            }
        } else {

            // Segunda fase: detectar el movimiento de disparo (hacia arriba y luego hacia abajo)
            medicionesDisparo.add(Triple(x, y, z))

            // Limitar tamaño de la lista
            if (medicionesDisparo.size > CANTIDAD_MEDICIONES_MOVIMIENTO) {
                medicionesDisparo.removeAt(0)
            }

            // Si tenemos suficientes mediciones para analizar el movimiento
            if (medicionesDisparo.size >= CANTIDAD_MEDICIONES_MOVIMIENTO) {
                // Dividir en dos partes: primera mitad y segunda mitad
                val primerasMediciones = medicionesDisparo.subList(0, medicionesDisparo.size / 2)
                val ultimasMediciones = medicionesDisparo.subList(medicionesDisparo.size / 2, medicionesDisparo.size)

                // Calcular aceleración promedio en X para ambas partes
                val promedioXPrimera = primerasMediciones.map { it.first }.average().toFloat()
                val promedioXUltima = ultimasMediciones.map { it.first }.average().toFloat()

                // Verificar si hubo un movimiento hacia arriba (X disminuye) y luego hacia abajo (X aumenta)
                val diferencia = promedioXUltima - promedioXPrimera

                if (diferencia > 5f) {  // Movimiento significativo hacia abajo después de estar arriba
                    println("Movimiento de disparo detectado: $diferencia")

                    if (escopeta.dispara()) {
                        MediaPlayer.create(this, R.raw.disparo).start()
                    } else { //Sin munición, o con cartucho usado en la recámara
                        MediaPlayer.create(this, R.raw.sin_municion).start()
                    }

                    // Activamos período de enfriamiento
                    iniciarEnfriamiento(tiempoActual)

                    // Limpiamos listas y reseteamos estado
                    medicionesDisparo.clear()
                    flagPosicionDisparo = false
                }

                // Si han pasado demasiadas mediciones sin detectar el movimiento, reiniciar
                if (medicionesDisparo.size >= CANTIDAD_MEDICIONES_MOVIMIENTO * 2) {
                    flagPosicionDisparo = false
                    medicionesDisparo.clear()
                }
            }
        }
    }

    fun recarga(x: Float, y: Float, z: Float, tiempoActual: Long) {
        medicionesRecarga.add(Triple(x, y, z))
        val ultimosOcho = medicionesRecarga.takeLast(8)
        var enRangoPriX = false
        var enRangoPriZ = false
        var aumentoCorrX = false
        var aumentoCorrZ = false
        var yEnRango = true

        for ((x, y, z) in ultimosOcho) {
            if (y !in (-4.0..4.0)) {
                yEnRango = false
            }
            if (!enRangoPriX && (x in 8.0..12.0)) {
                enRangoPriX = true
            } else if (enRangoPriX && (x in -2.5..2.5)) {
                aumentoCorrX = true
            }
            if (!enRangoPriZ && (z in -3.0..3.0)) {
                enRangoPriZ = true
            } else if (enRangoPriZ && (z in -3.0..20.0)) {
                aumentoCorrZ = true
            }
        }

        if (aumentoCorrZ && aumentoCorrX && yEnRango) {
            if(escopeta.recarga()){
                MediaPlayer.create(this, R.raw.recarga).start()
            } else {
                MediaPlayer.create(this, R.raw.sin_municion).start()
            }

            iniciarEnfriamiento(tiempoActual)

            medicionesRecarga.clear()
        }
    }

    private fun checaSiEstaEnDelay(tiempoActual: Long): Boolean {
        if (enPeriodoEnfriamiento) {
            if ((tiempoActual - ultimoTiempoNotificacion) >= DELAY_ENFRIAMIENTO) {
                enPeriodoEnfriamiento = false
                limpiarListas()
            }
            return true
        }
        return false
    }

    private fun iniciarEnfriamiento(tiempoActual: Long) {
        enPeriodoEnfriamiento = true
        ultimoTiempoNotificacion = tiempoActual
    }

    private fun limpiarListas() {
        medicionesBombeo.clear()
        medicionesDisparo.clear()
        medicionesRecarga.clear()
    }




    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    @Composable //Indica que el metodo es agregable
    fun UIPrincipal() {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            var imageShell by remember { mutableStateOf(R.drawable.cartucho8) }
            var imageshotgun by remember { mutableStateOf(R.drawable.escopeta_cartucho) }

            Image(painterResource(id = imageshotgun), null)
            Image(painterResource(id = imageShell),null)

            when(escopeta.cartuchosCargados){
                8 -> imageShell = R.drawable.cartucho8
                7 -> imageShell = R.drawable.cartucho7
                6 -> imageShell = R.drawable.cartucho6
                5 -> imageShell = R.drawable.cartucho5
                4 -> imageShell = R.drawable.cartucho4
                3 -> imageShell = R.drawable.cartucho3
                2 -> imageShell = R.drawable.cartucho2
                1 -> imageShell = R.drawable.cartucho1
                0 -> imageShell = R.drawable.cartucho0
            }

            if(escopeta.cartuchoEnRecamara)
                imageshotgun = R.drawable.escopeta_cartucho
            else
                imageshotgun = R.drawable.escopeta

            //println("Cartuchos cargados: ${escopeta.cartuchosCargados}")
            //println("Cartucho en recámara: ${if (escopeta.cartuchoEnRecamara) "Sí" else "No"}")
            //println("Total cartuchos: ${escopeta.totalCartuchos}")
            //println(if (escopeta.flagBombeo) { "Bombea!" } else ("Listo"))
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Previsualizacion() {
        UIPrincipal()
    }


}





