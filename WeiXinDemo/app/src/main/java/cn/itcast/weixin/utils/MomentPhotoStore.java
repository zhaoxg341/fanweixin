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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 朋友圈配图的存与取。
 *
 * 和头像（AvatarStore）的区别：
 * 头像是正方形小图，动态配图是长方形大图，不能裁成方的；
 * 这里按「最长边 1080 + 质量 88」压缩，既够清晰，又不会让九宫格列表卡顿。
 *
 * 读取时走 LruCache + inSampleSize=2，ListView 滑动不会反复解码原图。
 */
public class MomentPhotoStore {

    private static final String DIR = "moments";
    private static final int MAX_EDGE = 1080;
    private static final int CACHE_SIZE = 8 * 1024 * 1024;

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(CACHE_SIZE) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private MomentPhotoStore() {
    }

    /** 是不是本机导入的动态配图 */
    public static boolean isLocalFile(String path) {
        return path != null && path.trim().startsWith("/");
    }

    /**
     * 从相册导入一张配图。
     *
     * @return 保存后的绝对路径；失败返回 null
     */
    public static String importFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        InputStream in = null;
        Bitmap origin = null;
        Bitmap scaled = null;
        try {
            // 第一步只读尺寸，算出采样率，避免几千万像素的图直接进内存
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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE);
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            origin = BitmapFactory.decodeStream(in, null, options);
            if (origin == null) {
                return null;
            }

            // 竖拍照片靠 EXIF 标记转正，否则发到朋友圈是躺着的
            int degrees = readOrientation(context, uri);
            if (degrees != 0) {
                Bitmap rotated = rotate(origin, degrees);
                if (rotated != origin) {
                    origin.recycle();
                    origin = rotated;
                }
            }

            scaled = scaleToMaxEdge(origin, MAX_EDGE);

            File dir = new File(context.getFilesDir(), DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String fileName = "moment_" + System.currentTimeMillis()
                    + "_" + (int) (Math.random() * 1000) + ".jpg";
            File file = new File(dir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            try {
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
                out.flush();
            } finally {
                closeQuietly(out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeQuietly(in);
            if (scaled != null && !scaled.isRecycled()) {
                scaled.recycle();
            }
            if (origin != null && origin != scaled && !origin.isRecycled()) {
                origin.recycle();
            }
        }
    }

    /** 读取配图，带内存缓存 */
    public static Bitmap load(String path) {
        if (path == null) {
            return null;
        }
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

    /**
     * 看大图时用：不做采样、按原尺寸解码。
     *
     * 配图存进私有目录时最长边已经压到 1080，所以这里直接解也不会 OOM；
     * 放大到两三倍时，比列表里那份采样一半的图清楚得多。
     * 单独解码、不进 LruCache，免得把列表的缓存挤掉。
     */
    public static Bitmap loadFull(String path) {
        if (path == null) {
            return null;
        }
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 删动态时顺手把图片文件也删掉，避免私有目录越攒越大 */
    public static void deleteQuietly(String path) {
        if (!isLocalFile(path)) {
            return;
        }
        String key = path.trim();
        CACHE.remove(key);
        File file = new File(key);
        if (file.exists()) {
            file.delete();
        }
    }

    private static int sampleSize(int width, int height, int target) {
        int sample = 1;
        while (Math.max(width, height) / (sample * 2) >= target) {
            sample *= 2;
        }
        return sample;
    }

    /** 按最长边等比缩放，先垫白底防止透明 PNG 压成 JPEG 后发黑 */
    private static Bitmap scaleToMaxEdge(Bitmap src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= maxEdge) {
            return src;
        }
        float ratio = maxEdge / (float) longEdge;
        int targetW = Math.max(1, Math.round(w * ratio));
        int targetH = Math.max(1, Math.round(h * ratio));

        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(src, new Rect(0, 0, w, h),
                new Rect(0, 0, targetW, targetH), paint);
        return out;
    }

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

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
