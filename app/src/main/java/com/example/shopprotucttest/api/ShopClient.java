package com.example.shopprotucttest.api;

import android.util.Log;

import com.example.shopprotucttest.MyApplication;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ShopClient {

    private static final String TAG = ShopClient.class.getSimpleName();
    private static final String BASE_URL = "http://shop.trabaho.ru/api/";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_PRAGMA = "Pragma";

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .cache(cache())
                .addInterceptor(httpLoggingInterceptor())
                .addNetworkInterceptor(networkInterceptor())
                .addInterceptor(offlineInterceptor())
                .build();
    }

    public static Retrofit retrofitBuilder() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(buildClient())
                .addConverterFactory(MoshiConverterFactory.create())
                .build();
    }

    private static final long cacheSize = 5 * 1024 * 1024; // 5 MB

    private static Cache cache() {
        return new Cache(new File(MyApplication.getInstance().getCacheDir(), "someIdentifier"), cacheSize);
    }

    private static Interceptor offlineInterceptor() {
        return new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Chain chain) throws IOException {
                Log.d(TAG, "offline interceptor: called:");
                Request request = chain.request();

                if (!MyApplication.hasNetwork()) {
                    CacheControl cacheControl = new CacheControl.Builder()
                            .maxStale(7, TimeUnit.DAYS)
                            .build();

                    request = request.newBuilder()
                            .removeHeader(HEADER_PRAGMA)
                            .removeHeader(HEADER_CACHE_CONTROL)
                            .cacheControl(cacheControl)
                            .build();
                }
                return chain.proceed(request);
            }
        };
    }

    private static Interceptor networkInterceptor() {
        return new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Chain chain) throws IOException {
                Log.d(TAG, "network interceptor: called");

                Response response = chain.proceed(chain.request());

                CacheControl cacheControl = new CacheControl.Builder()
                        .maxAge(5, TimeUnit.SECONDS)
                        .build();

                return response.newBuilder()
                        .removeHeader(HEADER_PRAGMA)
                        .removeHeader(HEADER_CACHE_CONTROL)
                        .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                        .build();
            }
        };
    }

    private static HttpLoggingInterceptor httpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(@NotNull String message) {
                        Log.d(TAG, "log: http log: " + message);
                    }
                });
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return httpLoggingInterceptor;
    }


}
