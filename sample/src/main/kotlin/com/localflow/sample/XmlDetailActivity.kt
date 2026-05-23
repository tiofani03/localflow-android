package com.localflow.sample

import android.os.Bundle
import android.widget.Button
import com.localflow.sdk.ui.xml.LocalflowActivity

class XmlDetailActivity : LocalflowActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}
