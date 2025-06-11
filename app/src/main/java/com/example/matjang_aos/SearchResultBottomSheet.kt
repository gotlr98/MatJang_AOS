package com.example.matjang_aos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SearchResultBottomSheetFragment(
    private val results: List<Matjip>,
    private val onPlaceSelected: (Matjip) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_search_result, container, false)
        val listView = view.findViewById<ListView>(R.id.result_list_view)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, results.map { it.placeName })
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            onPlaceSelected(results[position])
            dismiss()
        }

        return view
    }
}
