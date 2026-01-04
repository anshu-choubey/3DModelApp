package com.arkdev.a3dmodelapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arkdev.a3dmodelapp.R
import com.arkdev.a3dmodelapp.adapter.ModelAdapter
import com.arkdev.a3dmodelapp.auth.AuthManager
import com.arkdev.a3dmodelapp.databinding.ActivityUserBinding
import com.arkdev.a3dmodelapp.repo.ModelRepository

class UserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserBinding
    private lateinit var repository: ModelRepository
    private lateinit var adapter: ModelAdapter
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        repository = ModelRepository(this)
        authManager = AuthManager(this)

        setupRecyclerView()
        loadModels()
    }
    private fun setupRecyclerView() {
        adapter = ModelAdapter(
            isAdmin = false,
            onViewClick = { model ->
                val intent = Intent(this, ModelViewerActivity::class.java)
                intent.putExtra("MODEL_ID", model.id)
                startActivity(intent)
            },
            onDeleteClick = null
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadModels() {
        val models = repository.getAllModels()
        adapter.submitList(models)

        // Update stats
        binding.tvModelCount.text = models.size.toString()
        // Show/hide empty state
        binding.emptyState.visibility = if (models.isEmpty()) View.VISIBLE else View.GONE
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
                authManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}