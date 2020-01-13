package com.whj.multiscreenproject.screenop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/29 上午11:43
 */

public class MediaProjectionHelper {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Intent getCaptureIntent(MediaProjectionManager systemService) {
        return systemService == null ? null : systemService.createScreenCaptureIntent();
    }

    public static MediaProjectionManager getMediaProjectionManager(Context context) {
        return (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("ConstantConditions")
    public static Observable<MediaProjection> requestCapture(final FragmentActivity activity) {
        final MediaProjectionManager mediaProjectionManager = getMediaProjectionManager(activity);
        if (mediaProjectionManager == null) {
            return Observable.just(null);
        } else {
            return Observable
                    .just(getCaptureIntent(mediaProjectionManager))
                    .filter(new Predicate<Intent>() {
                        @Override
                        public boolean test(Intent intent) throws Exception {
                            return intent != null;
                        }
                    })
                    .flatMap(new Function<Intent, ObservableSource<ResultEvent>>() {
                        @Override
                        public ObservableSource<ResultEvent> apply(Intent intent) throws Exception {
                            return ActivityResultRequest.rxQuest(activity, intent);
                        }
                    })
                    .filter(new Predicate<ResultEvent>() {
                        @Override
                        public boolean test(ResultEvent it) throws Exception {
                            return it.resultCode == Activity.RESULT_OK && it.data != null;
                        }
                    })
                    .map(new Function<ResultEvent, MediaProjection>() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public MediaProjection apply(ResultEvent resultEvent) throws Exception {
                            return mediaProjectionManager.getMediaProjection(resultEvent.resultCode, resultEvent.data);
                        }
                    });
        }
    }
}
