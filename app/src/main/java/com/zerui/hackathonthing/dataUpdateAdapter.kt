package com.zerui.hackathonthing

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.one_row.view.*

class DataUpdateAdapter(private val casesData : ArrayList<List<String>>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {
    fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T { // Adapter for onClickListener
        itemView.setOnClickListener {
            event.invoke(adapterPosition, itemViewType)
        }
        return this
    }

    // Gets the number of rows
    override fun getItemCount(): Int {
        return casesData.size
    }

    private fun getHighResURL(dp: Int, URL: String): String {
        val desiredImgWidth = (dp * Resources.getSystem().displayMetrics.density).toInt()
            .toString() + "px"

        return URL.replace(
            "23px", desiredImgWidth
        ).replace(
            "15px", desiredImgWidth
        ).replace(
            "20px", desiredImgWidth
        ).replace(
            "21px", desiredImgWidth
        ).replace(
            "22px", desiredImgWidth
        ).replace(
            "14px", desiredImgWidth
        ).replace(
            "18px", desiredImgWidth
        ).replace(
            "19px", desiredImgWidth
        )
        // Wikipedia flags comes in many garbage resolutions...
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.one_row, parent, false))
            .listen { row, _ ->
                val dataList = casesData[row]
                val highResURL = getHighResURL(232, dataList[3])

                val intent = Intent(context, DetailsView::class.java)
                intent.putExtra("cases", dataList[0])
                intent.putExtra("deaths", dataList[1])
                intent.putExtra("recoveries", dataList[2])
                intent.putExtra("flagURL", highResURL)
                intent.putExtra("learnMoreURL", dataList[4])
                intent.putExtra("country", dataList[5])
                intent.putExtra("thumbnailURL", dataList[3])
                context.startActivity(intent)
            }
    }

    // Binds each row of data in the ArrayList to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val cases = casesData[position][0]
        val country = casesData[position][5]
        holder.headerText.text = country
        if (cases == "No data") {
            holder.totalCases.text = cases
        }
        else {
            holder.totalCases.text = "$cases Cases"
        }
        val highResURL = getHighResURL(116, casesData[position][3])

        val radius = PreferenceManager.getDefaultSharedPreferences(context).getString("radius", "14")!!.toInt()

        Picasso.get().load(casesData[position][3]) // Thumbnail URL for faster loading
            .placeholder(R.drawable.ic_round_refresh_30)
            // .transform(RoundedTransformation(20, 0)) Slows down rendering way too much
            .into(holder.imgView, object : Callback {
                override fun onSuccess() {
                    Picasso.get()
                        .load(highResURL) // image url goes here
                        .placeholder(holder.imgView.drawable)
                        .transform(RoundedTransformation(radius, 0))
                        .into(holder.imgView)
                }

                override fun onError(e: Exception?) {
                    // Do nothing here
                }
            })
    }
}

class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val headerText: TextView = view.country
    val totalCases: TextView = view.totalCases
    val imgView: ImageView = view.countryImg
}