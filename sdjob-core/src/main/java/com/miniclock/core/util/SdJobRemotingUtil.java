package com.miniclock.core.util;

import com.miniclock.core.biz.model.ReturnT;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author strind
 * @date 2024/8/24 9:35
 * @description SdJob用于远程调用的工具类
 */

public class SdJobRemotingUtil {

    private static Logger logger = LoggerFactory.getLogger(SdJobRemotingUtil.class);

    public static final String SD_JOB_ACCESS_TOKEN = "SD-JOB-ACCESS-TOKEN";


    /**
     * 信任该http链接
     */
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();
            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }


    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};


    /**
     * 发送post消息
     */
    public static ReturnT postBody(String url, String accessToken, int timeout, Object requestObj, Class<ReturnT> returnTargClassOfT) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;

        url = "http://" + url;
        HttpPost httpPost = new HttpPost("https://" + url);

        try {
            if (url.startsWith("https")) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts,new java.security.SecureRandom());
                httpClient = HttpClients.custom()
                    .setSSLContext(sslContext).build();
            }else httpClient = HttpClients.createDefault();

            httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setHeader("Accept-Charset", "application/json;charset=UTF-8");
            httpPost.setHeader("Connection", "Keep-Alive");

            if (accessToken != null && !accessToken.trim().isEmpty()) {
                httpPost.setHeader(SD_JOB_ACCESS_TOKEN, accessToken);
            }
            if (requestObj != null){
                String requestBody = GsonTool.toJson(requestObj);
                HttpEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
            }

            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200){
                //设置失败结果
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting fail, StatusCode("+ statusCode +") invalid. for url : " + url);
            }
//            HttpEntity responseEntity = response.getEntity();
//            String resultJson = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
//            return GsonTool.fromJson(resultJson, returnTargClassOfT);
            return ReturnT.SUCCESS;
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
    }

}
