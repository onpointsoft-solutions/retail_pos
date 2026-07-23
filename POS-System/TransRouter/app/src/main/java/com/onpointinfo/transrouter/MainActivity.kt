package com.onpointinfo.transrouter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var transactionList: LinearLayout
    private lateinit var transactionStatus: TextView
    private lateinit var posHostField: EditText
    
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { 
        refreshTransactions() 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transactionList = findViewById(R.id.transactionList)
        transactionStatus = findViewById(R.id.transactionStatus)
        posHostField = findViewById(R.id.posHostField)
        
        val grantSmsButton = findViewById<Button>(R.id.grantSmsButton)
        val savePosButton = findViewById<Button>(R.id.savePosButton)

        posHostField.setText(getSharedPreferences("transrouter", MODE_PRIVATE).getString("pos_host", ""))

        grantSmsButton.setOnClickListener { 
            requestSmsPermission()
            requestIgnoreBatteryOptimizations()
        }
        savePosButton.setOnClickListener {
            val host = posHostField.text.toString().trim()
            val address = try { InetAddress.getByName(host) } catch (_: Exception) { null }
            if (address == null || !address.isSiteLocalAddress) {
                posHostField.error = "Enter the POS private LAN IP shown in Victorious Shop POS"
                return@setOnClickListener
            }
            getSharedPreferences("transrouter", MODE_PRIVATE).edit().putString("pos_host", host).apply()
            Toast.makeText(this, "Configuration saved: $host", Toast.LENGTH_SHORT).show()
        }

        refreshTransactions()
    }

    override fun onResume() {
        super.onResume()
        refreshTransactions()
    }

    private fun requestSmsPermission() {
        val receiveGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        
        if (receiveGranted && readGranted) {
            Toast.makeText(this, "SMS permissions already granted", Toast.LENGTH_SHORT).show()
        } else {
            permissionRequest.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback for devices that don't support the direct intent
                Toast.makeText(this, "Please disable battery optimization for TransRouter manually", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshTransactions() {
        if (!::transactionList.isInitialized) return
        
        transactionList.removeAllViews()
        val transactions = TransactionStore(this).recent()
        
        if (transactions.isEmpty()) {
            transactionStatus.text = getString(R.string.no_transactions)
            transactionStatus.visibility = View.VISIBLE
        } else {
            transactionStatus.visibility = View.GONE
            val inflater = LayoutInflater.from(this)
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            
            transactions.forEach { transaction ->
                val itemView = inflater.inflate(R.layout.item_transaction, transactionList, false)
                
                itemView.findViewById<TextView>(R.id.transactionCode).text = transaction.code
                itemView.findViewById<TextView>(R.id.transactionAmount).text = "KES ${transaction.amount}"
                itemView.findViewById<TextView>(R.id.customerName).text = transaction.customerName
                itemView.findViewById<TextView>(R.id.transactionTime).text = dateFormat.format(Date(transaction.receivedAt))
                
                transactionList.addView(itemView)
            }
        }
    }
}
