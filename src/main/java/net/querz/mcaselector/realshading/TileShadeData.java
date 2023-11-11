package net.querz.mcaselector.realshading;

public class TileShadeData {
    private static final int max = ShadeConstants.MAX.maxdist + 2;
    private LevelTileShadeData[] levels = new LevelTileShadeData[max];
    private byte continuity = 0;

    public TileShadeData(){ }

    public int memory() {
        int mem = 0;
        for(var lvl : levels){
            if(lvl != null) mem += lvl.memory();
        }
        return mem;
    }

    private LevelTileShadeData getLevel(byte dist){
        dist++;
        if(levels[dist] == null){
            levels[dist] = new LevelTileShadeData();
        }
        return levels[dist];
    }
    public byte getContinuity(){
        return continuity;
    }

    public void set(byte dist, boolean[] shades, byte oldContinuity, int part) {
        if(oldContinuity != continuity) {
            System.out.println(" -> failed");
            return;
        }
        var level = getLevel(dist);
        if(continuity > dist) {
            //getLevel((byte)(dist)).reset();
            for(int i = 0; i <= dist; i++){
                getLevel((byte)(dist)).reset();
            }
            continuity = dist;
        }

        level.addPlain(shades);

        if(continuity == dist) {
            if (ShadeConstants.GLOBAL.single[dist]) {
                level.part[0] = true;
                level.part[1] = true;
            } else {
                if(level.part[part]) System.out.println("BBBBBBBBBBBBBBBBBBBBBBB");
                level.part[part] = true;
            }

            if(level.ready()) {
                //getLevel((byte)(dist)).reset();
                getLevel((byte)(dist + 1)).reset();
                getLevel((byte)(dist - 1)).reset();
                continuity++;
            }
        }

        System.out.println(" -> saved");
    }
    public boolean[] get(byte dist) {
        if(dist == -1) return null;
        return getLevel(dist).getPlain();
    }

    public boolean isCurrent(byte dist){
        if(dist == 0) return true;
        return dist == continuity;
    }

    public boolean isAlreadyDone(byte dist, int part) {
        if(continuity > dist) return true;
        else if(continuity == dist){
            return getLevel(dist).part[part];
        } else return false;
    }
}

class LevelTileShadeData{
    private static final boolean[] EMPTYFALSE = new boolean[512 * 512];
    private static final boolean[] EMPTYTRUE = new boolean[512 * 512];
    static {
        for(int i = 0; i < EMPTYFALSE.length; i++){
            EMPTYFALSE[i] = false;
            EMPTYTRUE[i] = true;
        }
    }

    public boolean[] shadePlain;
    public boolean[] part;
    public boolean nullVariant;


    public LevelTileShadeData(){
        reset();
    }

    public int memory(){
        int memory = 0;

        memory += 2;
        if(shadePlain != null) memory += shadePlain.length;
        else memory += 1;

        return memory;
    }

    public void reset() {
        shadePlain = null;
        part = new boolean[2];
        nullVariant = false;
    }

    public boolean[] getPlain(){
        if(shadePlain == null){
            if(!nullVariant) return null;
            else return EMPTYTRUE;
        } else return shadePlain;
    }
    public void addPlain(boolean[] newShadePlain) {
        if(shadePlain == null) {
            if(!nullVariant) shadePlain = EMPTYFALSE;
            else return;
        }

        boolean onlyTrue = true;
        for(int i = 0; i < shadePlain.length; i++){
            newShadePlain[i] = newShadePlain[i] || shadePlain[i];
            if(!newShadePlain[i]) onlyTrue = false;
        }

        shadePlain = newShadePlain;
        if(onlyTrue){
            shadePlain = null;
            nullVariant = true;
        }
    }

    public boolean ready(){
        return part[0] && part[1];
    }
}