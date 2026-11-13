package com.example.controliot

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object EnlaceHttp {
    private val clave = "clave-compartida-demo".toByteArray()
    private var contador = System.currentTimeMillis()

    private fun firmar(msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(clave, "HmacSHA256"))
        return mac.doFinal(msg.toByteArray()).joinToString("") { "%02x".format(it) }
    }


    suspend fun enviarComando(base: String, accion: String): String =
        withContext(Dispatchers.IO) {
            contador++
            val sig = firmar("CMD;$accion;$contador")
            val url = URL("${base}cmd?accion=$accion&n=$contador&sig=$sig")
            val con = url.openConnection() as HttpURLConnection

            val cuerpo = (if (con.responseCode < 400) con.inputStream else con.errorStream)
                .bufferedReader().readText()

            Log.i("HZB", "cmd $accion -> ${con.responseCode} $cuerpo")
            cuerpo
        }


    suspend fun enviarComandoAdulterado(base: String, accion: String): String =
        withContext(Dispatchers.IO) {
            contador++
            val sigFalsa = "0000000000000000000000000000000000000000000000000000000000000000"
            val url = URL("${base}cmd?accion=$accion&n=$contador&sig=$sigFalsa")
            val con = url.openConnection() as HttpURLConnection

            val cuerpo = (if (con.responseCode < 400) con.inputStream else con.errorStream)
                .bufferedReader().readText()

            Log.i("HZB", "cmd $accion (ADULTERADO) -> ${con.responseCode} $cuerpo")
            cuerpo
        }

    suspend fun obtenerLectura(base: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("${base}lectura")
            val con = url.openConnection() as HttpURLConnection
            val cuerpo = con.inputStream.bufferedReader().readText()
            Log.i("HZB", "lectura -> ${con.responseCode} $cuerpo")
            cuerpo
        }
}