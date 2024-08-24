package com.miniclock.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author strind
 * @date 2024/8/24 7:51
 * @description 网络工具类
 */
public class NetUtil {
    public static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

    public static int findAvailablePort(int defaultPort){
        int temPort = defaultPort;
        while (temPort < 65535){
            if (!isPortUsed(temPort)){
                return temPort;
            }else {
                temPort ++;
            }
        }
        temPort = --defaultPort;
        while (temPort > 0){
            if (!isPortUsed(temPort)){
                return temPort;
            }else {
                temPort --;
            }
        }
        throw new RuntimeException("no available port.");
    }

    private static boolean isPortUsed(int port) {
        boolean used = false;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            used = false;
        } catch (IOException e) {
            logger.info(">>>>>>>>>>> xxl-job, port[{}] is in use.", port);
            used = true;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.info("");
                }
            }
        }
        return used;
    }

}
