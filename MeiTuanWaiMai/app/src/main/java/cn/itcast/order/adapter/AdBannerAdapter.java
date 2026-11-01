package cn.itcast.order.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.order.utils.ShopDataUtils;

/**
 * 广告栏的数据适配器（对应教材 12.4.6）
 * 每个页面就是一张 ImageView，图片来自 res/drawable-nodpi
 */
public class AdBannerAdapter extends PagerAdapter {

    private final Context context;
    private List<String> bannerNames = new ArrayList<>();

    public AdBannerAdapter(Context context) {
        this.context = context;
    }

    /** 设置广告图片数据 */
    public void setData(List<String> bannerNames) {
        this.bannerNames = bannerNames == null ? new ArrayList<String>() : bannerNames;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return bannerNames.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int resId = ShopDataUtils.getDrawableId(context, bannerNames.get(position));
        if (resId != 0) {
            imageView.setImageResource(resId);
        }
        container.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
