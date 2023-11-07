package net.querz.mcaselector.realshading;

import javafx.util.Pair;
import net.querz.mcaselector.tile.Tile;

import java.util.HashMap;

public class Shade {
    private static int SIZE = Tile.SIZE * Tile.SIZE;
    private static final HashMap<Long, Pair<Pair<Short, Short>, Boolean[]>> pool = new HashMap<>();

    private static final HashMap<Long, PairArr> pairpool = new HashMap<>(), pairpoolLoading = new HashMap<>();
    private static final HashMap<Long, Boolean> paircompletepool = new HashMap<>();

    public static double getMBSize(){
        return ((double)pool.size() * (SIZE + 16) + pairpool.size() * (ShadeConstants.MAXrXrZ * 2)) / (8 * 1048576);
    }


    public static Pair<Pair<Short, Short>, Boolean[]> _get(int scale, long region, short distanceX, short distanceZ){
        var a = pool.get(region);
        if(a == null){
            SIZE = SIZE / (scale * scale);
            a = new Pair<>(new Pair<>((short)-1, (short)-1), new Boolean[SIZE]);
            pool.put(region, a);
        } else if(a.getKey().getKey() >= distanceX && a.getKey().getValue() >= distanceZ){
            SIZE = SIZE / (scale * scale);
            a = new Pair<>(new Pair<>((short)-1, (short)-1), new Boolean[SIZE]);
            pool.put(region, a);
        } else if(a.getValue().length != SIZE){

        }

        return a;
    }

    public static Pair<Short, Short> getDist(int scale, long region){
        var a = pool.get(region);
        if(a == null){
            return new Pair<>((short)-1, (short)-1);
        } else {
            return a.getKey();
        }
    }

    public static void get(int scale, long region, short distanceX, short distanceZ, boolean[] shading, int offsetX, int offsetZ, int sizeX, int sizeZ){
        var _a = _get(scale, region, distanceX, distanceZ);
        var a = _a.getValue();

        synchronized (a) {
            int sx = Math.min(Tile.SIZE, sizeX - offsetX), sz = Math.min(Tile.SIZE, sizeZ - offsetZ);
            for (int xx = offsetX; xx < offsetX + sx; xx++) {
                for (int zz = offsetZ; zz < offsetZ + sz; zz++) {
                    int ai = (zz - offsetZ) * Tile.SIZE + (xx - offsetX), si = zz * sizeX + xx;
                    boolean aai = false;
                    if(a[ai] != null) aai = a[ai];
                    shading[si] = aai;
                }
            }
        }
    }

    public static void add(int scale, long region, short distanceX, short distanceZ, boolean[] shading, int offsetX, int offsetZ, int sizeX, int sizeZ){
        var _a = _get(scale, region, distanceX, distanceZ);
        var a = _a.getValue();

        synchronized (a) {
            boolean changed = false;
            int sx = Math.min(Tile.SIZE, sizeX - offsetX), sz = Math.min(Tile.SIZE, sizeZ - offsetZ);
            for (int xx = offsetX; xx < offsetX + sx; xx++) {
                for (int zz = offsetZ; zz < offsetZ + sz; zz++) {
                    int ai = (zz - offsetZ) * Tile.SIZE + (xx - offsetX), si = zz * sizeX + xx;
                    boolean aai = false;
                    if(a[ai] != null) aai = a[ai];

                    if(aai || shading[si] != aai){
                        changed = true;
                        a[ai] = aai || shading[si];
                    }
                }
            }
            if(changed){
                pool.put(region, new Pair<>(new Pair<>(distanceX, distanceZ), a));
            }
        }
    }

    public static void override(long region, boolean[] shading, int offsetX, int offsetZ, int sizeX, int sizeZ){ }

    public static void delete(long region){
        pool.remove(region);
    }

    public static void deleteAll(){
        pool.clear();
        pairpool.clear();
    }




    public static final PairArr b = new PairArr(true);
    public static PairArr getPairs(long region, boolean readonly){
        var a = pairpool.get(region);
        if(a == null){
            boolean complete = paircompletepool.getOrDefault(region, false);

            if(!readonly){
                pairpool.put(region, new PairArr(complete));
                return getPairs(region, readonly);
            } else{
                if(complete) return PairArr.empty;
                else return new PairArr(false);
            }
        }
        return a;
    }

    public static boolean isEmpty(long region){
        if(!pairpool.containsKey(region)) {
            return paircompletepool.getOrDefault(region, false);
        }
        var a = getPairs(region, true);
        var res = a.isEmpty();
        if(res[1]){
            paircompletepool.put(region, !res[2]);
            pairpool.remove(region);
        }

        return res[0];
    }

    public static void addFullPair(long region, int rx, int rz){
        var a = getPairs(region, false);
        a.setFull(rz * ShadeConstants.GLOBAL.rX + rx, true);
    }
    public static void addPair(long region, int rx, int rz){
        var a = getPairs(region, false);
        a.Increment(rz * ShadeConstants.GLOBAL.rX + rx);
    }

    public static void saveToLoading(long region){
        boolean[] arr = new boolean[ShadeConstants.MAXrXrZ];
        var a = getPairs(region, true);
        {
            for(int i=0;i<arr.length;i++){
                arr[i] = a.isFull(i);
            }
        }
        pairpoolLoading.put(region, new PairArr(arr, null));
    }

    public static boolean shouldRedraw(long region){
        return pairpoolLoading.containsKey(region);
    }

    public static boolean[] getFromLoadingAndDelete(long region){
        var a = pairpoolLoading.get(region);
        pairpoolLoading.remove(region);
        return a.exist;
    }

}