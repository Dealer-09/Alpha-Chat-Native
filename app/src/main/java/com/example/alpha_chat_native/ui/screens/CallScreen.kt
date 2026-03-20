package com.example.alpha_chat_native.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import timber.log.Timber

data class Contact(val name: String, val phoneNumber: String)

@Composable
fun CallScreen() {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val context = LocalContext.current
    
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            contacts = fetchContacts(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
            contacts = fetchContacts(context)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Permission needed to show contacts", color = textColor)
                Button(onClick = { permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) }) {
                    Text("Grant Permission")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn {
                    items(contacts) { contact ->
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                    context.startActivity(intent)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = if(isDark) Color.LightGray else Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = contact.name, color = textColor, fontWeight = FontWeight.Bold)
                                Text(text = contact.phoneNumber, color = textColor.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Green)
                            }
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

private fun fetchContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    try {
        // Only fetch the two columns we need — avoids loading all contact fields into memory
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                contacts.add(Contact(name, number))
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch contacts")
    }
    return contacts
}
