package cn.itcast.weixin.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 头像的「进站」和「出站」。
 *
 * 进站 importFromUri()：
 *   相册给的是一个 content:// 的 Uri，不是文件路径；而且原图可能有几千万像素，
 *   直接 BitmapFactory.decodeStream 很容易 OOM。所以分两步：
 *   1. 先 inJustDecodeBounds 只读宽高，算出采样率；
 *   2. 再按采样率解码，居中裁成正方形，压成 JPEG 存进 App 私有目录 files/avatars/。
 *   最后返回绝对路径，交给 AvatarDao / AppPrefs 保存。
 *
 * 出站 apply()：
 *   拿到的是路径就解码成 Bitmap 塞给 ImageView，是资源名就走原来的 setImageResource。
 *   中间加了一层 LruCache —— ListView 每滑一下都会 getView，没有缓存会反复读盘解码。
 *
 * 这样设计的好处：数据库里只存一个字符串，至于它是 "avatar1" 还是 "/data/.../xxx.jpg"，
 * 所有界面都不用关心，统一调 apply() 就行。
 */
public class AvatarStore {

    /** 私有目录名：/data/data/cn.itcast.weixin/files/avatars/ */
    private static final String DIR = "avatars";
    /** 存盘尺寸：512 像素够所有界面用，再大只是白占空间 */
    private static final int STORE_SIZE = 512;
    /** 内存缓存上限 8MB，一张 256x256 的图约 256KB，够缓存几十张 */
    private static final int CACHE_SIZE = 8 * 1024 * 1024;

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(CACHE_SIZE) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private AvatarStore() {
    }

    /** 判断一个头像值是不是「相册导入的本地文件路径」 */
    public static boolean isLocalFile(String avatar) {
        return avatar != null && avatar.trim().startsWith("/");
    }

    /**
     * 把相册里的图片导入成 App 私有文件。
     *
     * @param key 用来生成文件名的种子（传联系人昵称或我的昵称即可）
     * @return 保存后的绝对路径；失败返回 null
     */
    public static String importFromUri(Context context, Uri uri, String key) {
        if (context == null || uri == null) {
            return null;
        }
        InputStream in = null;
        Bitmap origin = null;
        Bitmap square = null;
        try {
            // 第一步：只读尺寸，不解码像素
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            BitmapFactory.decodeStream(in, null, bounds);
            closeQuietly(in);
            in = null;
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            // 第二步：按采样率解码，例如 4000x3000 的图只解出 1000x750
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, STORE_SIZE);
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            origin = BitmapFactory.decodeStream(in, null, options);
            if (origin == null) {
                return null;
            }

            // 手机竖着拍的照片，像素其实是横的，只带一个「旋转 90 度」的 EXIF 标记。
            // 不处理的话头像会躺着，所以这里读一下标记再转正。
            int degrees = readOrientation(context, uri);
            if (degrees != 0) {
                Bitmap rotated = rotate(origin, degrees);
                if (rotated != origin) {
                    origin.recycle();
                    origin = rotated;
                }
            }

            square = cropCenter(origin, STORE_SIZE);

            File dir = new File(context.getFilesDir(), DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            // 文件名用「昵称 hash + 时间戳」：避免中文名问题，连续换头像也不会互相覆盖
            String fileName = "avatar_" + Math.abs(key == null ? 0 : key.hashCode())
                    + "_" + System.currentTimeMillis() + ".jpg";
            File file = new File(dir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            try {
                square.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
            } finally {
                closeQuietly(out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            // 用户可能在选图过程中把原图删了，或相册返回的 Uri 已经失效
            e.printStackTrace();
            return null;
        } finally {
            closeQuietly(in);
            if (square != null && !square.isRecycled()) {
                square.recycle();
            }
            if (origin != null && origin != square && !origin.isRecycled()) {
                origin.recycle();
            }
        }
    }

    /**
     * 统一显示出口。
     *
     * @param avatar      头像值：本地路径或 drawable 资源名
     * @param fallbackRes 路径加载失败、或 avatar 为空时用的默认图
     */
    public static void apply(ImageView view, String avatar, int fallbackRes) {
        if (view == null) {
            return;
        }
        if (isLocalFile(avatar)) {
            Bitmap bitmap = load(avatar.trim());
            if (bitmap != null) {
                view.setImageBitmap(bitmap);
                return;
            }
        }
        if (fallbackRes != 0) {
            view.setImageResource(fallbackRes);
        }
    }

    /** 从磁盘加载并缓存；显示尺寸远小于存盘尺寸，所以采样率取 2 省一半内存 */
    public static Bitmap load(String path) {
        Bitmap cached = CACHE.get(path);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap != null) {
            CACHE.put(path, bitmap);
        }
        return bitmap;
    }

    /** 换头像时把上一张自己导入的图片删掉，避免私有目录越攒越大 */
    public static void deleteIfLocal(String avatar) {
        if (!isLocalFile(avatar)) {
            return;
        }
        String path = avatar.trim();
        CACHE.remove(path);
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /** 计算 2 的幂采样率，保证解码后短边不小于 target */
    private static int sampleSize(int width, int height, int target) {
        int sample = 1;
        while (Math.min(width, height) / (sample * 2) >= target) {
            sample *= 2;
        }
        return sample;
    }

    /**
     * 读 EXIF 里的旋转角度，返回 0 / 90 / 180 / 270。
     * ExifInterface(InputStream) 是 API 24 才有的，所以低版本直接返回 0（不转）。
     */
    private static int readOrientation(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return 0;
        }
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(in);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (Exception e) {
            // 有些图片格式没有 EXIF，读不到就按原样显示
            e.printStackTrace();
        } finally {
            closeQuietly(in);
        }
        return 0;
    }

    private static Bitmap rotate(Bitmap src, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    /**
     * 居中裁剪成正方形并缩放到 maxSize（小图不放大）。
     * 用 Canvas 画而不是 createBitmap，是为了先垫一层白底：
     * 透明 PNG 直接压成 JPEG 时透明区域会变黑。
     */
    private static Bitmap cropCenter(Bitmap src, int maxSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);
        int left = (w - side) / 2;
        int top = (h - side) / 2;
        int target = Math.min(side, maxSize);

        Bitmap out = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(src, new Rect(left, top, left + side, top + side),
                new Rect(0, 0, target, target), paint);
        return out;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
