package com.progmov.escopeta_acelerometro


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Escopeta {

    private val CAPACIDAD_TUBO = 8;

    var cartuchosCargados by mutableStateOf(8) // Por defecto no cargada
    var flagBombeo by mutableStateOf(false)
    var cartuchoEnRecamara by mutableStateOf(true)

    val totalCartuchos: Int
        get() = cartuchosCargados + if (cartuchoEnRecamara) 1 else 0

    fun recarga(): Boolean {
        /*Si no hay cartuchos, obligatoriamente se va a tener */
        if(cartuchosCargados < CAPACIDAD_TUBO){

            if(cartuchosCargados == 0){
                flagBombeo = true
            }

            cartuchosCargados++

            return true
        }

        return false
    }

    fun dispara(): Boolean {
        if ((cartuchoEnRecamara) && !flagBombeo){
            flagBombeo = true
            cartuchoEnRecamara = false
            return true
        }
        return false
    }

    fun bombeo(){

        /*Cuando se bombea se saca el cartucho que hay en la recámara
        * Si hay cartuchos cargados, se mete uno en la recámara tras bombear
        * Esta función es la única que puede bajar la flag de bombeo, misma
        * que permite disparar, o no */

        cartuchoEnRecamara = false

        if(cartuchosCargados > 0){
            cartuchoEnRecamara = true
            cartuchosCargados --
        }

        flagBombeo = false
    }
}