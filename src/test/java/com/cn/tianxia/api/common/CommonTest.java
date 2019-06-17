package com.cn.tianxia.api.common;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class CommonTest {

    public static void main(String[] args) {
        // 获取本机所有ip地址(包括保留地址，ipv4,ipv6 如果安装了虚拟机会更多其他的地址)
        // try {
        // InetAddress ads = null;
        // Enumeration<NetworkInterface> adds = NetworkInterface.getNetworkInterfaces();
        // while(adds.hasMoreElements()) {
        // Enumeration<InetAddress> inetAds = adds.nextElement().getInetAddresses();
        // while(inetAds.hasMoreElements()) {
        // ads = inetAds.nextElement();
        // System.out.println(ads.getHostAddress());
        // }
        // }
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // 获取可用ipv6地址
        try {
            System.out.println(getLocalIPv6Address());
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getLocalIPv6Address() throws SocketException {
        InetAddress inetAddress = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        outer: while (networkInterfaces.hasMoreElements()) {
            Enumeration<InetAddress> inetAds = networkInterfaces.nextElement().getInetAddresses();
            while (inetAds.hasMoreElements()) {
                inetAddress = inetAds.nextElement();
                // 检查此地址是否是IPv6地址以及是否是保留地址
                if (inetAddress instanceof Inet6Address && !isReservedAddr(inetAddress)) {
                    break outer;

                }
            }
        }
        String ipAddr = inetAddress.getHostAddress();
        // 过滤网卡
        int index = ipAddr.indexOf('%');
        if (index > 0) {
            ipAddr = ipAddr.substring(0, index);
        }
        return ipAddr;
    }

    private static boolean isReservedAddr(InetAddress inetAddr) {
        if (inetAddr.isAnyLocalAddress() || inetAddr.isLinkLocalAddress() || inetAddr.isLoopbackAddress()) {
            return true;
        }
        return false;
    }
}
