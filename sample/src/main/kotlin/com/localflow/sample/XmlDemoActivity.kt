package com.localflow.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.localflow.sdk.Localflow
import com.localflow.sdk.ui.xml.LocalflowActivity
import kotlinx.coroutines.launch

class XmlDemoActivity : LocalflowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateMetadataText()

        // Bind language selection buttons
        findViewById<Button>(R.id.btn_lang_en).setOnClickListener {
            Localflow.setLanguage("en")
            Toast.makeText(this, "Language switched to English", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_lang_id).setOnClickListener {
            Localflow.setLanguage("id")
            Toast.makeText(this, "Language switched to Bahasa Indonesia", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_lang_ja).setOnClickListener {
            Localflow.setLanguage("ja")
            Toast.makeText(this, "Language switched to Japanese", Toast.LENGTH_SHORT).show()
        }

        // Bind manual sync button
        findViewById<Button>(R.id.btn_sync).setOnClickListener {
            lifecycleScope.launch {
                Toast.makeText(this@XmlDemoActivity, "Syncing in background...", Toast.LENGTH_SHORT).show()
                Localflow.forceSync()
                updateMetadataText()
                Toast.makeText(this@XmlDemoActivity, "Sync completed!", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to XML Detail
        findViewById<Button>(R.id.btn_go_detail).setOnClickListener {
            val intent = Intent(this, XmlDetailActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Jetpack Compose demo
        findViewById<Button>(R.id.btn_go_compose).setOnClickListener {
            val intent = Intent(this, ComposeDemoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateMetadataText()
    }

    private fun updateMetadataText() {
        val version = Localflow.getVersion() ?: -1
        val checksum = Localflow.getChecksum()?.take(8) ?: "N/A"
        val activeLang = Localflow.getCurrentLanguage()

        findViewById<TextView>(R.id.tv_version_info).text = 
            "Active: $activeLang | Version: v$version | Checksum: $checksum"
    }
}
