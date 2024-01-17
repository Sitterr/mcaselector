package net.querz.mcaselector.realshading;

import javafx.util.Pair;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tile.Tile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Shade {
    private static final HashMap<Long, TileShadeData> pool = new HashMap<>();

    public static double getMBSize(){
        double memory = 0;
        for(var entr : pool.entrySet()){
            memory += entr.getValue().memory();
        }
        return (memory) / (8 * 1048576);
    }


    public static TileShadeData _get(long region){
        var a = pool.get(region);
        if(a == null){
            a = new TileShadeData();
            pool.put(region, a);
        }

        return a;
    }

    public static byte get(Point2i nachRegion, boolean[] shading, int _ix, int _iz){
        byte ix = (byte)(_ix * ShadeConstants.GLOBAL.xp), iz = (byte)(_iz * ShadeConstants.GLOBAL.zp);
        var region = new Point2i(nachRegion.getX() + ix, nachRegion.getZ() + iz).asLong();
        byte distance = ShadeConstants.GLOBAL.getPath(new Pair<>(ix, iz));
        if(distance == -1) return -1;
        int offsetX = ShadeConstants.GLOBAL.nflowX(_ix, 0, ShadeConstants.GLOBAL.rX) * 512;
        int offsetZ = ShadeConstants.GLOBAL.nflowZ(_iz, 0, ShadeConstants.GLOBAL.rZ) * 512;

        var a = _get(region);
        var arr = a.get((byte)(distance - 1));
        System.out.println("read: " + new Point2i(nachRegion.getX() + ix, nachRegion.getZ() + iz) + " | level: " + (distance - 1));

        if(arr != null) {
            synchronized (a) {
                int sx = Math.min(Tile.SIZE, (ShadeConstants.GLOBAL.rX * 512) - offsetX), sz = Math.min(Tile.SIZE, (ShadeConstants.GLOBAL.rZ * 512) - offsetZ);
                for (int xx = offsetX; xx < offsetX + sx; xx++) {
                    for (int zz = offsetZ; zz < offsetZ + sz; zz++) {
                        int ai = (zz - offsetZ) * Tile.SIZE + (xx - offsetX), si = zz * (ShadeConstants.GLOBAL.rX * 512) + xx;
                        shading[si] = arr[ai];
                    }
                }
            }
        }

        return a.getContinuity();
    }

    public static void add(Point2i nachRegion, boolean[] shading, byte continuity, int _ix, int _iz){
        byte ix = (byte)(_ix * ShadeConstants.GLOBAL.xp), iz = (byte)(_iz * ShadeConstants.GLOBAL.zp);
        var region = new Point2i(nachRegion.getX() + ix, nachRegion.getZ() + iz).asLong();
        byte distance = ShadeConstants.GLOBAL.getPath(new Pair<>(ix, iz));
        if(distance == -1) return;
        int offsetX = ShadeConstants.GLOBAL.nflowX(_ix, 0, ShadeConstants.GLOBAL.rX) * 512;
        int offsetZ = ShadeConstants.GLOBAL.nflowZ(_iz, 0, ShadeConstants.GLOBAL.rZ) * 512;

        var a = _get(region);

        synchronized (a) {
            boolean[] arr = new boolean[512 * 512];
            int sx = Math.min(Tile.SIZE, (ShadeConstants.GLOBAL.rX * 512) - offsetX), sz = Math.min(Tile.SIZE, (ShadeConstants.GLOBAL.rZ * 512) - offsetZ);
            for (int xx = offsetX; xx < offsetX + sx; xx++) {
                for (int zz = offsetZ; zz < offsetZ + sz; zz++) {
                    int ai = (zz - offsetZ) * Tile.SIZE + (xx - offsetX), si = zz * (ShadeConstants.GLOBAL.rX * 512) + xx;
                    arr[ai] = shading[si];
                }
            }
            System.out.print("saving: " + new Point2i(nachRegion.getX() + ix, nachRegion.getZ() + iz) + " | " + distance);
            a.set(distance, arr, continuity, ShadeConstants.GLOBAL.part.get(new Pair<>(ix, iz)) ? 1 : 0);
        }
    }

    public static void delete(long region){
        pool.remove(region);
    }

    public static void deleteAll(){
        pool.clear();
    }
}