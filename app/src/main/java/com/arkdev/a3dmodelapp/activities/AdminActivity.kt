package com.arkdev.a3dmodelapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arkdev.a3dmodelapp.activities.LoginActivity
import com.arkdev.a3dmodelapp.activities.ModelViewerActivity
import com.arkdev.a3dmodelapp.R
import com.arkdev.a3dmodelapp.adapter.ModelAdapter
import com.arkdev.a3dmodelapp.auth.AuthManager
import com.arkdev.a3dmodelapp.databinding.ActivityAdminBinding
import com.arkdev.a3dmodelapp.repo.ModelRepository

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var repository: ModelRepository
    private lateinit var adapter: ModelAdapter

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { showAddDialog(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        repository = ModelRepository(this)

        setupRecyclerView()
        loadModels()

        binding.fabAdd.setOnClickListener {
            pickFile.launch("*/*")
        }
    }
    private fun setupRecyclerView() {
        adapter = ModelAdapter(
            isAdmin = true,
            onViewClick = { model ->
                startActivity(Intent(this, ModelViewerActivity::class.java).apply {
                    putExtra("MODEL_ID", model.id)
                })
            },
            onDeleteClick = { model ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Model")
                    .setMessage("Delete ${model.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        repository.deleteModel(model)
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                        loadModels()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadModels() {
        val models = repository.getAllModels()
        adapter.submitList(models)
        binding.tvModelCount.text = models.size.toString()
        binding.emptyState.visibility = if (models.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog(uri: Uri) {
        val view = layoutInflater.inflate(R.layout.dialog_add_model, null)
        val etName = view.findViewById<EditText>(R.id.etModelName)
        val etDesc = view.findViewById<EditText>(R.id.etModelDescription)

        AlertDialog.Builder(this)
            .setTitle("Add 3D Model")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString()
                val desc = etDesc.text.toString()

                if (name.isNotEmpty()) {
                    val result = repository.addModel(name, desc, uri)
                    if (result > 0) {
                        Toast.makeText(this, "Model added", Toast.LENGTH_SHORT).show()
                        loadModels()
                    } else {
                        Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadModels()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                AuthManager(this).logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}