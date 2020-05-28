import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

class Light{
    private double x = 0, y = 0, z = 0, w = 0, r = 0, g = 0, b = 0;
    
    public Light(double lightx, double lighty, double lightz, double lightw, double lightR, double lightG, double lightB){
        x = lightx;
        y = lighty;
        z = lightz;
        w = lightw;
        r = lightR;
        g = lightG;
        b = lightB;
    }
    
    public Vector3D getLightPoint(){return new Vector3D(new double[] {x, y, z});}
    
    public Vector3D getLightRGB(){return new Vector3D(new double[] {r, g, b});}
    
    public void print(){
        System.out.println("Light: x = " + x + " y = " + y + " z = " + z + " w = " + w + " R = " + r + " G = " + g + " B = " + b);
    }
}
