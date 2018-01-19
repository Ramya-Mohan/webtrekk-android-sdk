/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Webtrekk GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Created by Arsen Vartbaronov on 30.05.16.
 */

package com.webtrekk.SDKTest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.webtrekk.SDKTest.SimpleHTTPServer.DisposableManager;
import com.webtrekk.webtrekksdk.Utils.WebtrekkLogging;
import com.webtrekk.webtrekksdk.WebtrekkRecmmtn;
import com.webtrekk.webtrekksdk.Webtrekk;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RecommendationActivity extends Activity {

    static class RecommendationAdapter extends ArrayAdapter<WebtrekkRecmmtn.RecommendationProduct>
{
    public RecommendationAdapter(Context context, int resource, List<WebtrekkRecmmtn.RecommendationProduct> list) {
        super(context, resource, list);
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {

        View view;
        WebtrekkRecmmtn.RecommendationProduct product = getItem(i);

        if (convertView != null)
            view = convertView;
        else {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater
                    .inflate(R.layout.recomendation_item, null);
        }

        String text = "Title:" + product.getTitle()+"; id ="+product.getId() + "\n";

        for (String key: product.getValues().keySet())
        {
            text += "Value name:"+key + " value:" + product.getValue(key) + " type:"+product.getValueType(key);
        }

        ((TextView)view.findViewById(R.id.recomendation_text)).setText(text);

        return view;
    }
}

    static public final String RECOMMENDATION_NAME = "RECOMMENDATION_NAME";
    static public final String RECOMMENDATION_PRODUCT_ID = "RECOMMENDATION_PRODUCT_ID";
    static public final String RECOMMENDATION_PRODUCT_CAT = "RECOMMENDATION_PRODUCT_CAT";
    private RecommendationAdapter mAdapter;
    private WebtrekkRecmmtn.QueryRecommendationResult mLastResult;
    public Observer<List<WebtrekkRecmmtn.RecommendationProduct>> mObserver;
    ListView listView;
    private boolean mRequestFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recommendation);
        listView = (ListView)findViewById(R.id.recomendation_list);
        final String recommendationName = getIntent().getExtras().getString(RECOMMENDATION_NAME);
        final String productID = getIntent().getExtras().getString(RECOMMENDATION_PRODUCT_ID);
        final String productCat = getIntent().getExtras().getString(RECOMMENDATION_PRODUCT_CAT);

        Webtrekk webtrekk = Webtrekk.getInstance();
        WebtrekkRecmmtn recommendations = webtrekk.getRecommendations();

        recommendations.setProductId(productID);
        recommendations.configureRecommendation(recommendationName);

        WebtrekkRecmmtn.MyObservable myObservable =  recommendations.getObservable();
        createObserver();

        Observable<List<WebtrekkRecmmtn.RecommendationProduct>> mRecmObservable = myObservable.requestRecommendationData();
        if(mRecmObservable != null) {
            mRecmObservable.observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(mObserver);
        }
        else
            WebtrekkLogging.log(" Error in creating Observer Object");
      /*  recommendations.queryRecommendation(new WebtrekkRecommendations.RecommendationCallback() {
            @Override
            public void onReceiveRecommendations(List<WebtrekkRecommendations.RecommendationProduct> products, WebtrekkRecommendations.QueryRecommendationResult result) {
                mLastResult = result;
                if (result == WebtrekkRecommendations.QueryRecommendationResult.RECEIVED_OK)
                {
                    mAdapter = new RecommendationAdapter(RecommendationActivity.this, R.layout.recomendation_item, products);
                    listView.setAdapter(mAdapter);
                    listView.invalidate();
                }

                mUsedUIThread = Looper.getMainLooper().getThread() == Thread.currentThread();
                mRequestFinished = true;
            }
        }, recommendationName)
        .setProductId(productID).call(); */
        //.setProductCat(productCat)

    }

    public WebtrekkRecmmtn.QueryRecommendationResult getLastResult()
    {
        return mLastResult;
    }

    public int getRecommendationCount()
    {
        return mAdapter.getCount();
    }
    public boolean isRequestFinished() {
        return mRequestFinished;
    }

    private void createObserver()
    {
        mObserver = new Observer<List<WebtrekkRecmmtn.RecommendationProduct>>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
                WebtrekkLogging.log(" "+e.getMessage());
            }

            @Override
            public void onNext(List<WebtrekkRecmmtn.RecommendationProduct> products) {
                mAdapter = new RecommendationAdapter(RecommendationActivity.this, R.layout.recomendation_item, products);
                listView.setAdapter(mAdapter);
                listView.invalidate();
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableManager.add(d);
            }
        };
    }

}
