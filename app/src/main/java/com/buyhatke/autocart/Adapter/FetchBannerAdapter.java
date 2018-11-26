package com.buyhatke.autocart.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.buyhatke.autocart.AutoCart;
import com.buyhatke.autocart.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;

/**
 * Created by abhishek on 26/12/17.
 */
public class FetchBannerAdapter extends PagerAdapter {
    private Context context;
    private JSONArray fetchArray;

    public FetchBannerAdapter(Context context, JSONArray fetchArray) {
        this.context = context;
        this.fetchArray = fetchArray;
    }


    @Override
    public int getCount() {
        return fetchArray.length();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {

        ImageView imgBanner;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.banner_item, container,
                false);

        imgBanner = (ImageView) itemView.findViewById(R.id.imgBannerHome);
        try {

            final String imgUrl;
            String title;
            final String link;
            final int isProduct;
            imgUrl = fetchArray.getJSONObject(position).getString(
                    "image");
            link = fetchArray.getJSONObject(position)
                    .getString("url");
            title = fetchArray.getJSONObject(position)
                    .getString("offer");

            Log.d("Banner URL", link);
            isProduct = fetchArray.getJSONObject(position)
                    .getInt("isProd");

            if (title.equals("none")) {
//                                    //do not delete useful for debugging
                imgBanner.setVisibility(View.GONE);
            } else {
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(
                                R.drawable.banner_loader)
                        .into(imgBanner);
                imgBanner.setVisibility(View.VISIBLE);
                imgBanner.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (isProduct == 1) {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(link));
                            context.startActivity(intent);
                            AutoCart.sendUpdateToServer("Banner Clicked", link);
                        }

                    }
                });
            }
            ((ViewPager) container).addView(itemView);
        } catch (Exception e) {

        }

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((RelativeLayout) object);
    }
}
