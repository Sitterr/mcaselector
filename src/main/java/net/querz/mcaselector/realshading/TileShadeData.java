package net.querz.mcaselector.realshading;

public class TileShadeData {
    private static final boolean[] EMPTYSHADE = new boolean[512 * 512];
    private static final int max = ShadeConstants.MAX.maxdist + 2;
    private LevelTileShadeData[] levels = new LevelTileShadeData[max];
    private byte continuity = 0;

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

    public TileShadeData(){ }

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
                level.part[part] = true;
            }

            if(level.ready()){
                //getLevel((byte)(dist)).reset();
                getLevel((byte)(dist + 1)).reset();
                continuity++;
            }
        }

        System.out.println(" -> saved");
    }
    public boolean[] get(byte dist) {
        if(dist == -1) return null;
        var shades = getLevel(dist).shadePlain;
        return shades;
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
    public boolean[] shadePlain;
    public boolean[] part;


    public LevelTileShadeData(){
        reset();
    }
    public void reset() {
        shadePlain = null;
        part = new boolean[2];
    }

    public void addPlain(boolean[] newShadePlain) {
        if(shadePlain == null) shadePlain = new boolean[512 * 512];
        for(int i=0;i<shadePlain.length;i++){
            shadePlain[i] = shadePlain[i] || newShadePlain[i];
        }
    }

    public boolean ready(){
        return part[0] && part[1];
    }
}