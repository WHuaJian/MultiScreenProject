package com.whj.multiscreenproject.screenop;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.apkfuns.logutils.Logger;
import com.whj.multiscreenproject.AppManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/29 下午12:02
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RxScreenShot {

    private String TAG = "RxScreenShot";

    private Handler mCallBackHandler = new CallBackHandler();
    private MediaCallBack mMediaCallBack = new MediaCallBack();
    private MediaProjection mediaProjection;
    private SurfaceFactory mSurfaceFactory;
    private ImageReader mImageReader;

    public boolean isRecording; //是否正在演示

    public int width = 480;
    public int height = 720;
    public int dpi = 1;

    private RxScreenShot(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public static RxScreenShot of(MediaProjection mediaProjection) {
        return new RxScreenShot(mediaProjection);
    }


    private static RxScreenShot instance;

    public static RxScreenShot getInstance() {
        synchronized (RxScreenShot.class) {
            if (instance == null) {
                synchronized (RxScreenShot.class) {
                    instance = new RxScreenShot();
                }
            }
        }
        return instance;
    }

    public RxScreenShot() {
    }

    public RxScreenShot setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        return instance;
    }

    public RxScreenShot createImageReader() {
        //注意这里使用RGB565报错提示，只能使用RGBA_8888
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        mSurfaceFactory = new ImageReaderSurface(mImageReader);
        createProject();
        return this;
    }

    private void createProject() {
        mediaProjection.registerCallback(mMediaCallBack, mCallBackHandler);
        mediaProjection.createVirtualDisplay(TAG + "-display", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurfaceFactory.getInputSurface(), null, null);
    }

    /**
     * description: 获取屏幕宽高和密度
     *
     * @param
     * @author weihuajian
     * create at 2018/5/29 下午7:18
     */
    private RxScreenShot initWidthHeight(FragmentActivity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        dpi = metrics.densityDpi;
        return this;
    }

    /**
     * description: 生成bitmap
     *
     * @author weihuajian
     * create at 2018/5/29 下午6:55
     */
    public Observable<Object> startCapture() {
        isRecording = true;
        return ImageReaderAvailableObservable.of(mImageReader)
                .observeOn(Schedulers.io())
                .map(new Function<ImageReader, Object>() {
                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Object apply(ImageReader imageReader) throws Exception {
                        String mImageName = System.currentTimeMillis() + ".png";
                        Log.e(TAG, "image name is : " + mImageName);
                        Bitmap bitmap = null;
                        Image image = imageReader.acquireLatestImage();
                        if (image != null) {
                            int width = image.getWidth();
                            int height = image.getHeight();
                            final Image.Plane[] planes = image.getPlanes();
                            final ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * width;
                            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                            image.close();
                        }

                        return bitmap == null ? new Object() : bitmap;
                    }
                });
    }


    /**
     * 开始录屏
     *
     * @param activity
     * @return
     */
    public static Observable<byte[]> shoot(final FragmentActivity activity) {
        return MediaProjectionHelper
                .requestCapture(activity)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<MediaProjection, RxScreenShot>() {
                    @Override
                    public RxScreenShot apply(MediaProjection mediaProjection) throws Exception {
                        return RxScreenShot.getInstance() //创建MediaProjection必须在主线程
                                .setMediaProjection(mediaProjection)
                                .initWidthHeight(activity)
                                .createImageReader();
                    }
                })
                //后面所有都是子线程包括udp广播
                .flatMap(new Function<RxScreenShot, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(RxScreenShot rxScreenShot) throws Exception {
                        return rxScreenShot.startCapture();
                    }
                }).map(new Function<Object, byte[]>() {
                    @Override
                    public byte[] apply(Object object) throws Exception {
                        //将bitmap转换成byte[]
                        byte[] byteArray = new byte[0];
                        if (object instanceof Bitmap) {
                            ByteArrayOutputStream byteArrayOutputStream = null;
                            try {
                                Bitmap bitmap = (Bitmap) object;
                                int compressQuality = 60;
                                byteArrayOutputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, byteArrayOutputStream);
                                while (byteArrayOutputStream.toByteArray().length > 64 * 1024 && compressQuality > 5) {
                                    try {
                                        byteArrayOutputStream.flush();
                                        byteArrayOutputStream.reset();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    compressQuality -= 5;
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, byteArrayOutputStream);
                                }

                                byteArray = byteArrayOutputStream.toByteArray();
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            } finally {
                                if (byteArrayOutputStream != null) {
                                    byteArrayOutputStream.close();
                                }
                            }
                        }
                        return byteArray;
                    }
                });
    }


    /**
     * 将bitmap压缩
     *
     */
//    private static Bitmap zoomBitmap(Bitmap orgBitmap) {
//        int pcWidth = ZxingModelUtils.getInstance().getPcWidth();
//        int pcHeight = ZxingModelUtils.getInstance().getPcHeight();
//
//        float width = orgBitmap.getWidth();
//        float height = orgBitmap.getHeight();
//
//        if (null == orgBitmap) {
//            return null;
//        }
//        if (orgBitmap.isRecycled()) {
//            return null;
//        }
//        if (pcWidth <= 0 || pcHeight <= 0) {
//            return null;
//        }
//
//        //如果高度和宽度不超过pc的宽高就不压缩
//        if (width <= pcWidth && height <= pcHeight) {
//            return orgBitmap;
//        }
//
//        Matrix matrix = new Matrix();
//        float scaleWidth = ((float) pcWidth) / width;
//        float scaleHeight = ((float) pcHeight) / height;
//        matrix.postScale(scaleWidth, scaleHeight);
//        Bitmap bitmap = Bitmap.createBitmap(orgBitmap, 0, 0, (int) width, (int) height, matrix, true);
//        return bitmap;
//    }

    class MediaCallBack extends MediaProjection.Callback {
        @Override
        public void onStop() {
            super.onStop();
        }
    }

    static class CallBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    public interface SurfaceFactory {
        Surface getInputSurface();
    }

    class ImageReaderSurface implements SurfaceFactory {

        private ImageReader imageReader;

        public ImageReaderSurface(ImageReader imageReader) {
            this.imageReader = imageReader;
        }

        @Override
        public Surface getInputSurface() {
            return imageReader.getSurface();
        }
    }

    /**
     * 停止录屏
     */
    public void stopCapture() {
        isRecording = false;
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection.unregisterCallback(mMediaCallBack);
            mCallBackHandler.removeCallbacksAndMessages(null);
            instance = null;
        }
    }


    /**
     * 开始录屏并发送数据
     */
    public static void startScreenRecord(final Context context) {
        RxScreenShot.shoot((FragmentActivity) AppManager.getAppManager().currentActivity())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<byte[]>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        Log.i("send  ","byteSize" + bytes.length);
                        UdpByteSendHelper.getInstance(context).sendByte(bytes);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        Log.i("send  ","byteSize  " + "complete");
                    }
                });
    }

    /**
     * 停止录屏
     */
    public static void stopScreenRecord(Context context) {
        RxScreenShot.getInstance().stopCapture(); //停止录屏
        UdpByteSendHelper.getInstance(context).close();
    }
}
