package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author strind
 * @date 2024/9/16 10:14
 * @description 哈希一致性 路由策略
 */
public class ExecutorRouteConsistentHash extends ExecutorRouter {
    //哈希环上存储的地址的容量限制
    private static final int VIRTUAL_NODE_NUM = 100;

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = hashJob(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }

    public String hashJob(int jobId, List<String> addressList) {
        TreeMap<Long, String> addressRing = new TreeMap<>();
        for (String address: addressList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                //计算执行器地址的hash值
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                //把地址hash值和地址放到TreeMap中
                addressRing.put(addressHash, address);
            }
        }
        //计算定时任务id的hahs值
        long jobHash = hash(String.valueOf(jobId));
        //TreeMap的tailMap方法在这里很重要，这个方法会让内部键值对的键跟jobHash做比较
        //比jobHash的值大的键，对应的键值对都会返回给用户
        //这里得到的lastRing就相当于圆环上所有比定时任务hash值大的hash值了
        SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);
        //如果不为空
        if (!lastRing.isEmpty()) {
            //取第一个就行，最接近定时任务hash值就行
            return lastRing.get(lastRing.firstKey());
        }
        //如果为空，就从addressRing中获取第一个执行器地址即可
        return addressRing.firstEntry().getValue();
    }

    // 计算哈希值
    private static long hash(String key){
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        }
        md5.update(keyBytes);
        byte[] digest = md5.digest();
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
            | ((long) (digest[2] & 0xFF) << 16)
            | ((long) (digest[1] & 0xFF) << 8)
            | (digest[0] & 0xFF);
        return hashCode & 0xffffffffL;
    }
}
