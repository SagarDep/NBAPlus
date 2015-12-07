package com.me.silencedut.nbaplus.RxMethod;

import android.util.Log;

import com.me.silencedut.greendao.GreenNews;
import com.me.silencedut.greendao.GreenNewsDao;
import com.me.silencedut.nbaplus.app.AppService;
import com.me.silencedut.nbaplus.data.Constant;
import com.me.silencedut.nbaplus.event.NewsEvent;
import com.me.silencedut.nbaplus.model.News;

import java.util.List;

import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.Query;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by SilenceDut on 2015/12/2.
 */
public class RxNews {
    public static Subscription updateNews(final String newsType) {
        Subscription subscription = AppService.getNbaPlus().updateNews(newsType)
                .subscribeOn(Schedulers.io())
                .doOnNext(new Action1<News>() {
                    @Override
                    public void call(News news) {
                       cacheNews(news,newsType);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<News>() {
                    @Override
                    public void onCompleted() {

                    }
                    @Override
                    public void onError(Throwable e) {
                        Log.d("subscriptiononError", "onError" + e.toString());
                    }

                    @Override
                    public void onNext(News news) {
                        AppService.getBus().post(new NewsEvent(news, NewsEvent.GETNEWSWAY.UPDATE));
                    }
                });
        return subscription;
    }

    public static Subscription initNews(String type) {
        Subscription subscription = Observable.create(new Observable.OnSubscribe<News>() {
            @Override
            public void call(Subscriber<? super News> subscriber) {
                List<GreenNews> greenNewses = getCacheNews();
                if (greenNewses != null && greenNewses.size() > 0) {
                    News news = AppService.getGson().fromJson(greenNewses.get(0).getNewslist(), News.class);
                    subscriber.onNext(news);
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(new Action1<News>() {
                    @Override
                    public void call(News news) {
                        AppService.getBus().post(new NewsEvent(news, NewsEvent.GETNEWSWAY.INIT));
                    }
                });

        return subscription;
    }
    private static void cacheNews(News news, String newsType) {
        GreenNewsDao greenNewsDao = AppService.getDBHelper().getDaoSession().getGreenNewsDao();
        DeleteQuery deleteQuery = greenNewsDao.queryBuilder()
                .where(GreenNewsDao.Properties.Type.eq(newsType))
                .buildDelete();
        deleteQuery.executeDeleteWithoutDetachingEntities();
        String newsList = AppService.getGson().toJson(news);
        GreenNews greenNews = new GreenNews(null,newsList,newsType);
        greenNewsDao.insert(greenNews);
    }

    private static List<GreenNews> getCacheNews() {
        GreenNewsDao greenNewsDao = AppService.getDBHelper().getDaoSession().getGreenNewsDao();
        Query query = greenNewsDao.queryBuilder()
                .where(GreenNewsDao.Properties.Type.eq(Constant.NEWSTYPE.NEWS.getNewsType()))
                .build();
        // 查询结果以 List 返回
        List<GreenNews> greenNewses = query.list();
        return greenNewses;
    }



}
