package com.example.controliot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var estadoVentiladorActual = "OFF"
    private var temperaturaActual = 28.5f
    private var baseUrl = "http://192.168.4.11/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Si Hertzbench pasa la URL por extras, la tomamos
        intent.getStringExtra("board_url")?.let {
            if (it.isNotEmpty()) baseUrl = it
        }

        dbHelper = DatabaseHelper(this)

        val tvTemperatura = findViewById<TextView>(R.id.tvTemperatura)
        val tvEstadoVentilador = findViewById<TextView>(R.id.tvEstadoVentilador)
        val btnActualizarTemp = findViewById<Button>(R.id.btnActualizarTemp)
        val btnEncender = findViewById<Button>(R.id.btnEncender)
        val btnApagar = findViewById<Button>(R.id.btnApagar)
        val btnProbarAtaque = findViewById<Button>(R.id.btnProbarAtaque)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        actualizarInterfaz(tvTemperatura, tvEstadoVentilador)

        // Consultar temperatura desde /lectura
        btnActualizarTemp.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val jsonStr = EnlaceHttp.obtenerLectura(baseUrl)
                    val json = JSONObject(jsonStr)
                    temperaturaActual = json.getDouble("temperatura").toFloat()
                    estadoVentiladorActual = json.getString("estado")
                    actualizarInterfaz(tvTemperatura, tvEstadoVentilador)
                    Toast.makeText(this@DashboardActivity, "Sensor actualizado", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DashboardActivity, "Sin conexión con ESP32", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Enviar comando ON firmado
        btnEncender.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = EnlaceHttp.enviarComando(baseUrl, "ON")
                    estadoVentiladorActual = "ON"
                    actualizarInterfaz(tvTemperatura, tvEstadoVentilador)
                    Toast.makeText(this@DashboardActivity, "ESP32: $resp", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DashboardActivity, "Error de red al enviar ON", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Enviar comando OFF firmado
        btnApagar.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = EnlaceHttp.enviarComando(baseUrl, "OFF")
                    estadoVentiladorActual = "OFF"
                    actualizarInterfaz(tvTemperatura, tvEstadoVentilador)
                    Toast.makeText(this@DashboardActivity, "ESP32: $resp", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DashboardActivity, "Error de red al enviar OFF", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Probar comando con firma adulterada (Debe recibir 401)
        btnProbarAtaque.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = EnlaceHttp.enviarComandoAdulterado(baseUrl, "ON")
                    Toast.makeText(this@DashboardActivity, "Respuesta rechazo: $resp", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DashboardActivity, "Error de red en prueba", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Guardar respaldo local en SQLite
        btnRegistrar.setOnClickListener {
            val exito = dbHelper.insertLog(temperaturaActual, estadoVentiladorActual)
            if (exito) {
                Toast.makeText(this, "Lectura guardada en SQLite", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al guardar registro", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun actualizarInterfaz(tvTemp: TextView, tvEstado: TextView) {
        tvTemp.text = "$temperaturaActual °C"
        tvEstado.text = "Ventilador: $estadoVentiladorActual"
    }
}