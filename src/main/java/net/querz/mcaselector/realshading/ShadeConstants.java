package net.querz.mcaselector.realshading;

import javafx.util.Pair;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;

import java.util.HashMap;
import java.util.Queue;

public class ShadeConstants {
    public static int SHADEMOODYNESS = (int)(0.7 * -100);
    public static double ADEG = 124, BDEG = 20;

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
    public HashMap<Byte, Boolean> single;



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
            else { dir = ShadeDir.upright; throw new RuntimeException("not implemented degree range"); }
        } else {
            throw new RuntimeException("not supported degree range");
        }

        calcPath();
    }


    private void calcPath(){
        path = new HashMap<>();
        single = new HashMap<>();
        boolean[] bremArr = new boolean[rX * rZ];

        float a = 0.8f;
        brem(bremArr, 0.5, 0.5, 0.5 + rX - 1, 0.5 + rZ - 1);
        brem(bremArr, a, 0, a + rX - 1, rZ - 1);
        brem(bremArr, 0, a, rX - 1, a + rZ - 1);


        int[] arr = new int[rX * rZ];
        for(int i=0;i<arr.length;i++) arr[i] = -1;
        arr[0] = 0;
        path.put(new Pair<>((byte)0, (byte)0), (byte)(0));
        single.put((byte)0, true);
        int curr = 0;
        while(true){
            int br = 0;

            for(int i=0;i<rX*rZ;i++){
                if(arr[i] == curr){
                    if((i + 1) % rX != 0) {
                        int xx = (i + 1) % rX, zz = (i + 1) / rX;
                        xx = xx * xp;
                        zz = zz * zp;
                        if (arr[i + 1] == -1 && bremArr[i + 1]) {
                            arr[i + 1] = curr + 1;
                            path.put(new Pair<>((byte)xx, (byte)zz), (byte)(curr + 1));
                            br++;
                        }
                    }
                    if((i + rX) / rX < rZ) {
                        int xx = (i + rX) % rX, zz = (i + rX) / rX;
                        xx = xx * xp;
                        zz = zz * zp;
                        if (arr[i + rX] == -1 && bremArr[i + rX]) {
                            arr[i + rX] = curr + 1;
                            path.put(new Pair<>((byte)xx, (byte)zz), (byte)(curr + 1));
                            br++;
                        }
                    }
                }
            }

            if(br == 0) break;
            else {
                single.put((byte)(curr + 1), br == 1);
            }

            curr++;
        }
    }

    void brem(boolean[] bremAppr, double x0, double y0, double x1, double y1)
    {
        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);

        int x = (int)Math.floor(x0);
        int y = (int)Math.floor(y0);

        int n = 1;
        int x_inc, y_inc;
        double error;

        if (dx == 0)
        {
            x_inc = 0;
            error = Double.POSITIVE_INFINITY;
        }
        else if (x1 > x0)
        {
            x_inc = 1;
            n += (int)Math.floor(x1) - x;
            error = (Math.floor(x0) + 1 - x0) * dy;
        }
        else
        {
            x_inc = -1;
            n += x - (int)Math.floor(x1);
            error = (x0 - Math.floor(x0)) * dy;
        }

        if (dy == 0)
        {
            y_inc = 0;
            error -= Double.POSITIVE_INFINITY;
        }
        else if (y1 > y0)
        {
            y_inc = 1;
            n += (int)Math.floor(y1) - y;
            error -= (Math.floor(y0) + 1 - y0) * dx;
        }
        else
        {
            y_inc = -1;
            n += y - (int)Math.floor(y1);
            error -= (y0 - Math.floor(y0)) * dx;
        }

        for (; n > 0; --n)
        {
            bremAppr[y * rX + x] = true;
            //visit(x, y);

            if (error > 0)
            {
                y += y_inc;
                error -= dx;
            }
            else
            {
                x += x_inc;
                error += dy;
            }
        }
    }


    public static ShadeConstants recalcGLOBAL(){
        GLOBAL = new ShadeConstants(ShadeConstants.ADEG, ShadeConstants.BDEG);
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
