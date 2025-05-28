package com.example.matjang_aos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userEmail: TextView = view.findViewById(R.id.userEmail)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val comment: TextView = view.findViewById(R.id.comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.userEmail.text = review.user_email
        holder.ratingBar.rating = review.rate.toFloat()
        holder.comment.text = review.comment
    }

    override fun getItemCount(): Int = reviews.size
}
