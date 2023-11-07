package net.querz.mcaselector.realshading;

public class PairArr {
    public boolean[] exist, full;
    public PairArr(boolean complete) {
        this.exist = createEmpty();
        this.full = createEmpty();

        if(!complete) {
            setFull((ShadeConstants.GLOBAL.rZ - 1) * ShadeConstants.GLOBAL.rX + (ShadeConstants.GLOBAL.rX - 1), true);
        }
    }

    public PairArr(boolean[] exist, boolean[] full) {
        this.exist = exist;
        this.full = full;
    }

    public void setFull(int i, boolean val) {
        exist[i] = val;
        full[i] = val;
    }
    public void Increment(int i) {
        if(!exist[i]){
            exist[i] = true;
        } else {
            full[i] = true;
        }
    }
    public boolean isFull(int i) {
        return exist[i] && full[i];
    }

    public boolean[] isEmpty() {
        boolean completeEmpty = true, empty = true, xz = false;
        for (int i = 0; i < ShadeConstants.MAXrXrZ; i++) {
            if(i != (ShadeConstants.GLOBAL.rZ - 1) * ShadeConstants.GLOBAL.rX + (ShadeConstants.GLOBAL.rX - 1)) {
                if (exist[i] || full[i]) completeEmpty = false;
                if (exist[i] && full[i]) {
                    empty = false;
                    break;
                }
            } else {
                if (exist[i] && full[i]) {
                    empty = false;
                    xz = true;
                    break;
                } else if(exist[i] || full[i]) completeEmpty = false;
            }
        }
        return new boolean[] { empty, completeEmpty, xz };
    }

    public static PairArr empty = new PairArr(createEmpty(), createEmpty());

    private static boolean[] createEmpty() {
        return new boolean[ShadeConstants.MAXrXrZ];
    }
}
