package com.alexsheiko.coffeenearby

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.location.places.Place

class PlacesAdapter : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {

    // Provide a reference to the views for each data item
    class ViewHolder(val nameTextView: TextView) : RecyclerView.ViewHolder(nameTextView)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): PlacesAdapter.ViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
        return ViewHolder(view)
    }

    private val dataset: MutableList<Place> = ArrayList()

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from the dataset at this position
        // - replace the contents of the view with that element
        holder.nameTextView.text = dataset[position].name
    }

    // Return the size of the dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

    fun add(place: Place) {
        dataset.add(place)

        notifyItemInserted(dataset.size)
    }
}
