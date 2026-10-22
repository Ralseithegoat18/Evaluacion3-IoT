package com.example.controliot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dbHelper = DatabaseHelper(this)

        val tvTemperatura = findViewById<TextView>(R.id.tvTemperatura)
        val tvEstadoVentilador = findViewById<TextView>(R.id.tvEstadoVentilador)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val tempSimulada = 28.5f
        val estadoSimulado = "ACTIVADO"

        tvTemperatura.text = "$tempSimulada °C"
        tvEstadoVentilador.text = "Estado de ventilador: $estadoSimulado"

        btnRegistrar.setOnClickListener {
            val exito = dbHelper.insertLog(tempSimulada, estadoSimulado)
            if (exito) {
                Toast.makeText(this, "Lectura guardada en la BD SQLite", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al guardar registro", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}