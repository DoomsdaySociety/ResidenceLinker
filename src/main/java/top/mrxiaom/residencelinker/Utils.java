package top.mrxiaom.residencelinker;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.text.DecimalFormat;

public class Utils {
    public final static DecimalFormat D2 = new DecimalFormat("#.##");

    public static Location toLocation(String world, String loc) {
        Location l = null;
        try {
            String[] s = loc.split(",");
            double x = Double.parseDouble(s[0]);
            double y = Double.parseDouble(s[1]);
            double z = Double.parseDouble(s[2]);
            float yaw = s.length > 3 ? Float.parseFloat(s[3]) : 0;
            float pitch = s.length > 4 ? Float.parseFloat(s[4]) : 0;
            l = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        } catch (Throwable ignored) {
        }
        return l;
    }

    public static String fromLocation(Location loc) {
        return D2.format(loc.getX()) + "," + D2.format(loc.getY()) + "," + D2.format(loc.getZ()) + ","
                + D2.format(loc.getYaw()) + "," + D2.format(loc.getPitch());
    }
}
