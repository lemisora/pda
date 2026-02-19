package com.pda.Manager.Security;

import java.net.InetAddress;
import java.util.regex.Pattern;

public class NetFilter {
    private static final Pattern TAILSCALE_PATTERN = Pattern.compile("^100\\.(?:6[4-9]|[7-9][0-9]|1[0-1][0-9]|12[0-7])\\.\\d{1,3}\\.\\d{1,3}$");
    
    public static boolean isTailscaleIP(InetAddress address) {
        String ip =  address.getHostAddress();
        return TAILSCALE_PATTERN.matcher(ip).matches();
    }
    
    public static boolean isLocalhost(InetAddress address) {
        return address.isLoopbackAddress();
    }
}