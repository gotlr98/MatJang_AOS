package com.example.matjang_aos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.matjang_aos.databinding.ActivityBookmarkListBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class BookmarkList : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarkListBinding
    private lateinit var adapter: ArrayAdapter<String>
    private val bookmarkList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListView()
        fetchBookmarks()
    }

    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bookmarkList)
        binding.bookmarkListView.adapter = adapter

        binding.bookmarkListView.setOnItemClickListener { _, _, position, _ ->
            val selectedName = bookmarkList[position]
            val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", "") ?: ""
            val db = Firebase.firestore

            db.collection("users").document("${email}&Kakao")
                .collection("bookmark")
                .get()
                .addOnSuccessListener { groups ->
                    for (group in groups) {
                        group.reference.get()
                            .addOnSuccessListener { groupDoc ->
                                val placeMap = groupDoc.data ?: return@addOnSuccessListener
                                val placeData = placeMap[selectedName] as? Map<*, *> ?: return@addOnSuccessListener
                                val x = (placeData["x"] as? Number)?.toDouble() ?: return@addOnSuccessListener
                                val y = (placeData["y"] as? Number)?.toDouble() ?: return@addOnSuccessListener

                                val intent = Intent(this, MainMapActivity::class.java)
                                intent.putExtra("bookmark_place_name", selectedName)
                                intent.putExtra("bookmark_x", x)
                                intent.putExtra("bookmark_y", y)
                                startActivity(intent)
                                return@addOnSuccessListener
                            }
                    }
                }
        }
    }

    private fun fetchBookmarks() {
        val email = getSharedPreferences("signIn", Context.MODE_PRIVATE).getString("email", "") ?: ""
        val db = Firebase.firestore

        db.collection("users").document("${email}&Kakao")
            .collection("bookmark")
            .get()
            .addOnSuccessListener { result ->
                for (group in result) {
                    group.reference.get().addOnSuccessListener { groupDoc ->
                        val places = groupDoc.data?.keys ?: emptySet()
                        bookmarkList.addAll(places.map { it.toString() })
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }
}
