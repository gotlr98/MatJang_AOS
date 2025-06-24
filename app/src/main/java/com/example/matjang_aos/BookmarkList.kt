package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class BookmarkList : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val bookmarkList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_list)

        listView = findViewById(R.id.bookmark_list_view)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bookmarkList)
        listView.adapter = adapter

        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", "") ?: ""
        val db = Firebase.firestore
        db.collection("users").document("${email}&Kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { result ->
                for (group in result) {
                    group.reference.collection("places").get()
                        .addOnSuccessListener { placeDocs ->
                            for (doc in placeDocs) {
                                val name = doc.id
                                val x = doc.getDouble("x") ?: 0.0
                                val y = doc.getDouble("y") ?: 0.0
                                bookmarkList.add(name)
                                adapter.notifyDataSetChanged()
                                listView.setOnItemClickListener { _, _, position, _ ->
                                    val selectedName = bookmarkList[position]
                                    val intent = Intent(this, MainMapActivity::class.java)
                                    intent.putExtra("bookmark_place_name", selectedName)
                                    intent.putExtra("bookmark_x", x)
                                    intent.putExtra("bookmark_y", y)
                                    startActivity(intent)
                                }
                            }
                        }
                }
            }
    }
}