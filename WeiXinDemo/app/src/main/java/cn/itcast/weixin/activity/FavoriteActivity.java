package cn.itcast.weixin.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.itcast.weixin.R;
import cn.itcast.weixin.bean.FavoriteBean;
import cn.itcast.weixin.bean.MsgBean;
import cn.itcast.weixin.db.FavoriteDao;
import cn.itcast.weixin.utils.MomentPhotoStore;

/**
 * 收藏页（"我-收藏"）：长按聊天消息收藏的内容都在这里。
 * 列表按收藏时间倒序；点图片收藏看大图；长按删除收藏（不动原聊天记录）。
 */
public class FavoriteActivity extends AppCompatActivity {

    private final List<FavoriteBean> favorites = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        ((TextView) findViewById(R.id.tv_title)).setText(R.string.favorite_title);
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.iv_right).setVisibility(View.GONE);

        tvEmpty = findViewById(R.id.tv_empty);
        ListView listView = findViewById(R.id.list_favorite);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return favorites.size();
            }

            @Override
            public Object getItem(int position) {
                return favorites.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(R.layout.item_favorite, parent, false);
                }
                final FavoriteBean fav = favorites.get(position);

                ImageView thumb = view.findViewById(R.id.iv_fav_thumb);
                TextView tvContent = view.findViewById(R.id.tv_fav_content);
                TextView tvFrom = view.findViewById(R.id.tv_fav_from);

                // 缩略图：图片收藏显示图片，其余按类型显示一个底色块
                if (fav.getType() == MsgBean.TYPE_IMAGE) {
                    Bitmap bitmap = MomentPhotoStore.load(fav.getContent());
                    if (bitmap != null) {
                        thumb.setImageBitmap(bitmap);
                    } else {
                        thumb.setImageResource(R.drawable.ic_album_photo);
                    }
                } else {
                    thumb.setImageResource(iconFor(fav.getType()));
                }

                tvContent.setText(fav.preview());
                String from = fav.isFromMe()
                        ? getString(R.string.favorite_from_me) : fav.getFromName();
                tvFrom.setText(getString(R.string.favorite_from_format,
                        from == null ? "" : from, fav.getTime() == null ? "" : fav.getTime()));

                // 点图片收藏 → 全屏看图
                if (fav.getType() == MsgBean.TYPE_IMAGE) {
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(FavoriteActivity.this,
                                    PhotoViewerActivity.class);
                            intent.putStringArrayListExtra(PhotoViewerActivity.EXTRA_IMAGES,
                                    new ArrayList<>(Collections.singletonList(fav.getContent())));
                            startActivity(intent);
                        }
                    });
                } else {
                    view.setOnClickListener(null);
                }
                return view;
            }
        };
        listView.setAdapter(adapter);

        // 长按删除收藏
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           final int position, long id) {
                new AlertDialog.Builder(FavoriteActivity.this)
                        .setTitle(R.string.favorite_delete_title)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FavoriteDao.deleteById(favorites.get(position).getId());
                                        reload();
                                        Toast.makeText(FavoriteActivity.this,
                                                R.string.favorite_deleted,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        favorites.clear();
        favorites.addAll(FavoriteDao.queryAll());
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 非图片收藏的类型图标 */
    private int iconFor(int type) {
        switch (type) {
            case MsgBean.TYPE_REDPACKET:
                return R.drawable.ic_redpacket;
            case MsgBean.TYPE_VOICE:
                return R.drawable.ic_voice_wave;
            case MsgBean.TYPE_TRANSFER:
                return R.drawable.ic_transfer;
            default:
                return R.drawable.ic_comment_bubble;
        }
    }
}
