package com.example.matjang_aos

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewAdapter(
    private val reviews: List<Review>,
    private val onReportClick: (Review) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.review_card_with_report, parent, false) as ViewGroup
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.view.findViewById<TextView>(R.id.place_name).text = "📍 ${review.placeName}"
        holder.view.findViewById<TextView>(R.id.rate_text).text = "⭐ 평점: ${review.rate}"
        holder.view.findViewById<TextView>(R.id.comment_text).text = "💬 ${review.comment}"

        holder.view.findViewById<TextView>(R.id.report_button).setOnClickListener {
            onReportClick(review)
        }
    }

    override fun getItemCount(): Int = reviews.size
}
