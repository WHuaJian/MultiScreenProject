package com.whj.multiscreenproject.screenop;

import android.annotation.TargetApi;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/29 上午11:43
 */

public class ImageReaderAvailableObservable extends Observable<ImageReader> {

    public static ImageReaderAvailableObservable of(ImageReader imageReader) {
        return new ImageReaderAvailableObservable(imageReader, null);

    }

    public static ImageReaderAvailableObservable of(ImageReader imageReader, Handler handler) {
        return new ImageReaderAvailableObservable(imageReader, handler);
    }

    private final ImageReader imageReader;
    private final Handler handler;

    private ImageReaderAvailableObservable(ImageReader imageReader, Handler handler) {
        this.imageReader = imageReader;
        this.handler = handler;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void subscribeActual(Observer<? super ImageReader> observer) {
        Listener listener = new Listener(observer, imageReader);
        observer.onSubscribe(listener);
        imageReader.setOnImageAvailableListener(listener, handler);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    static class Listener implements Disposable, ImageReader.OnImageAvailableListener {
        private final AtomicBoolean unsubscribed = new AtomicBoolean();
        private final ImageReader mImageReader;
        private final Observer<? super ImageReader> observer;

        Listener(Observer<? super ImageReader> observer, ImageReader imageReader) {
            this.mImageReader = imageReader;
            this.observer = observer;
        }


        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!isDisposed()) {
                observer.onNext(reader);
//                observer.onComplete();
            }
        }

        @Override
        public void dispose() {
            if (unsubscribed.compareAndSet(false, true)) {
                mImageReader.setOnImageAvailableListener(null, null);
            }
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed.get();
        }
    }
}
