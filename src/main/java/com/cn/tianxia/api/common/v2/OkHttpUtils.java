package com.cn.tianxia.api.common.v2;


import com.google.gson.Gson;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Consts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jacky
 * @version 1.0.0
 * @ClassName OkHttpClient
 * @Description httpUtils
 * @Date 2019年5月6日 下午17:08:53
 */
@Component
public class OkHttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(OkHttpUtils.class);
    static final String USER_AGENT = "User-Agent";
    static final String APPLICATION = "application/x-www-form-urlencoded";
    static final String CHARSET = "charset";
    static final String UTF_C = "UTF-8";
    static final String CONNECTION_C = "Connection";
    static final String CLOSE_C = "close";


    private final Gson gson = new Gson();
    static class Gist { Map<String, GistFile> files;}
    static class GistFile { String content; }

    final static okhttp3.OkHttpClient client = OkHttpClient.CLIENT.getClientInstance();
    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    static  final  Headers.Builder builder = new Headers.Builder();
    static {
        builder.add("User-Agent", "application/x-www-form-urlencoded")
                .add("charset", "UTF-8")
                .add("Connection", "close");
    }


    public String sendMapPost(String url, Map<String, String> map) throws Exception {
        return sendMapPost(url, map, builder.build());
    }

    public String sendJsonPost(String url, String requestParams) throws Exception {
        return sendJsonPost(url, requestParams, builder.build());
    }

    public String sendGet(String url) throws Exception {
        return sendGet(url, builder.build());
    }


    public String sendGet(String url, Headers headers) throws Exception {
        if (StringUtils.isBlank(url)) throw new Exception("GET 请求URL不能为空！");

        Request request = new Request.Builder()
                .headers(headers)
                .get()
                .url(url)
                .build();
        String returnJson = null;
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response.code());

            if (response.body() != null)
                    returnJson = IOUtils.toString(response.body().byteStream(), Consts.UTF_8);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            if(e.getMessage().equals("Read timed out")) return  returnJson;  throw  e;
        } catch (Exception e) {
            logger.error("发送HTTP GET请求异常！", e);
            throw e;
        }
        return returnJson;

    }


    public Map<String, String> sendGetByGson(String url) throws Exception {
        Request request = new Request.Builder()
                .addHeader(USER_AGENT, APPLICATION)
                .addHeader(CHARSET, UTF_C)
                .url(url)
                .build();
        Map<String, String> map = new HashMap<>();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response.code());

            Gist gist;
            if (response.body() != null) {
                gist = gson.fromJson(response.body().charStream(), Gist.class);
            } else {
                return null;
            }
            for (Map.Entry<String, GistFile> entry : gist.files.entrySet()) {
                map.put(entry.getKey(), entry.getValue().content);
            }

        }catch (IOException e){
            logger.error(e.getMessage(),e);
            throw  e;
        } catch (Exception e) {
            logger.error("发送GET 请求异常！", e);
            throw e;
        }

        return map;

    }

    public String sendJsonPost(String url, String requestParam, Headers headers) throws Exception {
        if (StringUtils.isBlank(requestParam) || StringUtils.isBlank(url))
            throw new Exception("POST 请求参数或请求URL不能为空！");
        RequestBody requestBody = RequestBody.create(JSON, requestParam);
        return sendPost(url, requestBody, headers);

    }

    public String sendMapPost(String url, Map<String,String> requestMap, Headers headers) throws Exception {
        if (StringUtils.isEmpty(url) || requestMap.isEmpty())
            throw new Exception("POST 请求URL或请求参数不能为空！");
        RequestBody requestBody = RequestBody.create(JSON, requestMap.toString());
        return sendPost(url, requestBody, headers);
    }


    public String sendPost(String url, RequestBody requestBody, Headers headers) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .headers(headers)
                .build();
        String returnJson = null;
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response.code());

            if (response.body() != null) {
                 InputStream inputStream = response.body().byteStream();
                 returnJson = IOUtils.toString(inputStream, Consts.UTF_8);
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("发送HTTPPOST请求错误！", e);
            throw e;
        }
        return returnJson;

    }
}
