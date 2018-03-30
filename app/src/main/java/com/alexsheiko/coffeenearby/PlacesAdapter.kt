package com.alexsheiko.coffeenearby

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.places.Place
import kotlinx.android.synthetic.main.item_place.view.*
import org.jetbrains.anko.startActivity

class PlacesAdapter(val context: Context) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {

    private val dataset: MutableList<Place> = ArrayList()

    // Provide a reference to the views for each data item
    class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        val nameTextView = rootView.nameTextView
        val addressTextView = rootView.addressTextView
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): PlacesAdapter.ViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from the dataset at this position
        // - replace the contents of the view with that element
        holder.nameTextView.text = dataset[position].name
        holder.addressTextView.text = dataset[position].address
        holder.itemView.setOnClickListener {
            val place = dataset[position]
            context.startActivity<DetailActivity>(
                    "name" to place.name,
                    "lat" to place.latLng.latitude,
                    "lng" to place.latLng.longitude)
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

    fun addAll(placeList: List<Place>) {
        dataset.addAll(placeList)

        notifyItemRangeInserted(0, dataset.size)
    }
}
