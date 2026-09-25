package br.oliveira.grokboard.ui

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.oliveira.grokboard.Prefs
import br.oliveira.grokboard.R
import br.oliveira.grokboard.persona.Personas

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val api = findViewById<EditText>(R.id.api_key)
        val model = findViewById<EditText>(R.id.model)
        val spinner = findViewById<Spinner>(R.id.persona)

        api.setText(prefs.getString(Prefs.API_KEY, ""))
        model.setText(prefs.getString(Prefs.MODEL, Prefs.DEFAULT_MODEL))

        val names = Personas.ALL.map { it.name }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val current = Personas.byId(prefs.getString(Prefs.PERSONA_ID, null))
        spinner.setSelection(Personas.ALL.indexOfFirst { it.id == current.id }.coerceAtLeast(0))

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.edit()
                .putString(Prefs.API_KEY, api.text.toString().trim())
                .putString(Prefs.MODEL, model.text.toString().trim().ifBlank { Prefs.DEFAULT_MODEL })
                .putString(Prefs.PERSONA_ID, Personas.ALL[spinner.selectedItemPosition].id)
                .apply()
            Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
