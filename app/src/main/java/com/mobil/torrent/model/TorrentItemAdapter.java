package com.mobil.torrent.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobil.torrent.R;
import com.mobil.torrent.TorrentListActivity;

import java.util.ArrayList;

public class TorrentItemAdapter
        extends RecyclerView.Adapter<TorrentItemAdapter.ViewHolder>
        implements Filterable {
    // Member variables.
    private ArrayList<TorrentItem> mTorrentData;
    private ArrayList<TorrentItem> mTorrentDataAll;
    private Context mContext;
    private int lastPosition = -1;

    public TorrentItemAdapter(Context context, ArrayList<TorrentItem> itemsData) {
        this.mTorrentData = itemsData;
        this.mTorrentDataAll = itemsData;
        this.mContext = context;
    }

    @Override
    public TorrentItemAdapter.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(TorrentItemAdapter.ViewHolder holder, int position) {
        // Get current sport.
        TorrentItem currentItem = mTorrentData.get(position);

        // Populate the textviews with data.
        holder.bindTo(currentItem);


        if(holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mTorrentData.size();
    }


    /**
     * RecycleView filter
     * **/
    @Override
    public Filter getFilter() {
        return torrentFilter;
    }

    private Filter torrentFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<TorrentItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0) {
                results.count = mTorrentDataAll.size();
                results.values = mTorrentDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for(TorrentItem item : mTorrentDataAll) {
                    if(item.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }

                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mTorrentData = (ArrayList)filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        // Member Variables for the TextViews
        private TextView mTitleText;
        private TextView mInfoText;
        private TextView mSizeText;
        private TextView mDownloadsText;
        private ImageView mItemImage;
        private RatingBar mRatingBar;

        ViewHolder(View itemView) {
            super(itemView);

            // Initialize the views.
            mTitleText = itemView.findViewById(R.id.itemTitle);
            mInfoText = itemView.findViewById(R.id.subTitle);
            mItemImage = itemView.findViewById(R.id.itemImage);
            mRatingBar = itemView.findViewById(R.id.ratingBar);
            mSizeText = itemView.findViewById(R.id.size);
            mDownloadsText = itemView.findViewById(R.id.downloads);
        }

        void bindTo(TorrentItem currentItem){
            mTitleText.setText(currentItem.getName());
            mInfoText.setText(currentItem.getInfo());
            mSizeText.setText(currentItem.getSize());
            mDownloadsText.setText(String.valueOf(currentItem.getDownloadCount() + " letöltés"));
            mRatingBar.setRating(currentItem.getRatedInfo());

            // Load the images into the ImageView using the Glide library.
            Glide.with(mContext).load(currentItem.getImageResource()).into(mItemImage);

            itemView.findViewById(R.id.add_to_cart).setOnClickListener(view -> ((TorrentListActivity)mContext).updateAlertIcon(currentItem));
            itemView.findViewById(R.id.delete).setOnClickListener(view -> ((TorrentListActivity)mContext).deleteItem(currentItem));
        }
    }
}
