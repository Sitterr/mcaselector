package net.querz.mcaselector.realshading;

import javafx.util.Pair;
import net.querz.mcaselector.config.ConfigProvider;

import java.util.HashMap;

public class ShadeConstants {
    public static int SHADEMOODYNESS = (int)(0.7 * -100);
    public static double ADEG = 90, BDEG = 20;

    public static ShadeConstants GLOBAL = recalcGLOBAL();
    public static final int MAXrXrZ = calcMAXrXrZ(5);



    public ShadeDir dir;
    public double Adeg, Bdeg;
    public double A, B;
    public double cosA, sinA;
    public double cotgB;
    public double cosAcotgB, sinAcotgB;
    public int xp, zp;
    public int rX, rZ;
    public HashMap<Pair<Byte, Byte>, Byte> path;



    public ShadeConstants(double A_deg, double B_deg){
        this.Adeg = A_deg;
        this.Bdeg = B_deg;
        A = degToRad(Adeg);
        B = degToRad(Bdeg);
        cosA = round(Math.cos(A));
        sinA = round(Math.sin(A));
        cotgB = round(1 / Math.tan(B));
        cosAcotgB = cosA * cotgB;
        sinAcotgB = sinA * cotgB;

        xp = nceil(cosA);
        zp = -nceil(sinA);

        rX = (byte)Math.ceil(Math.abs(cosAcotgB * (ConfigProvider.WORLD.DEFAULT_RENDER_HEIGHT + 64)) / 512 + 1);
        rZ = (byte)Math.ceil(Math.abs(sinAcotgB * (ConfigProvider.WORLD.DEFAULT_RENDER_HEIGHT + 64)) / 512 + 1);


        if(sinA >= 0){
            if(cosA <= 0) dir = ShadeDir.upleft;
            else throw new RuntimeException("not supported degree range");
        } else {
            if(cosA > 0) throw new RuntimeException("not supported degree range");
            else throw new RuntimeException("not supported degree range");
        }

        path = calcPath();
    }


    private HashMap<Pair<Byte, Byte>, Byte> calcPath(){
        HashMap<Pair<Byte, Byte>, Byte> map = new HashMap<>();
        boolean[] edge = new boolean[rX * rZ];
        int curr = 0;





        return map;
    }

    private boolean bren(boolean[] shades, int SHADEX, int x1, int z1, int x2, int z2){
        int deltax = Math.abs(x2 - x1);
        int deltaz = Math.abs(z2 - z1);
        int error = 0;
        int br = 0;
        int zz = z1;

        boolean min = true;

        for(int xx = x1; xx <= x2; xx++) {
            int indx = zz * SHADEX + xx;

            //if(shades[(int)Math.ceil(zz) * SHADEX + (int)Math.floor(xx)] == false){
            //	return true;
            //}
            //if(shades[(int)Math.floor(zz) * SHADEX + (int)Math.ceil(xx)] == false){
            //	return true;
            //}
            error = error + deltaz;
            if(2 * error >= deltax) {
                zz = zz + 1;
                error -= deltax;
            }
        }

        return min;
    }


    public static ShadeConstants recalcGLOBAL(){
        GLOBAL = new ShadeConstants(ADEG, BDEG);
        return GLOBAL;
    }


    private static double degToRad(double angle){
        return angle / 180 * Math.PI;
    }

    private static double round(double value){ return Math.round(value * 10000.0) / 10000.0;}

    private static int nceil(double a){
        boolean neg = a < 0;
        a = (int)Math.ceil(Math.abs(a));
        if(neg) a = -a;
        return (int)a;
    }

    private static int calcMAXrXrZ(double minB){
        var max = new ShadeConstants(135.1, minB);
        return max.rX * max.rZ;
    }
}

enum ShadeDir{
    upleft, upright
}
