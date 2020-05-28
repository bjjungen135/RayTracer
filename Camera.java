import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

class Camera{
    private static Vector3D Ev, Lv, Up, Wv, Uv, Vv;
    private static double distance = 0, leftbound = 0, rightbound = 0, upbound = 0, downbound = 0, ambientR = 0, ambientG = 0, ambientB = 0;
    private static int resuno = 0, resdos = 0;
    private static double etaOutside = 1;
    
    public Camera(){
    }
    
    public static void setOrigin(double ex, double ey, double ez){
        Ev = new Vector3D(new double[] {ex, ey, ez});
    }
    
    public static void setLook(double lx, double ly, double lz){
        Lv = new Vector3D(new double[] {lx, ly, lz});
    }
    
    public static void setUp(double upx, double upy, double upz){
        Up = new Vector3D(new double[] {upx, upy, upz});
    }
    
    public static void setDistance(double d){
        distance = d;
    }
    
    public double getDistance(){return distance;}
    
    public static void setBounds(double l, double r, double d, double u){
        leftbound = l;
        rightbound = r;
        downbound = d;
        upbound = u;
    }
    
    public double getLeftBound(){return leftbound;}
    
    public double getRightBound(){return rightbound;}
    
    public double getDownBound(){return downbound;}
    
    public double getUpBound(){return upbound;}
    
    public void setResolution(int resone, int restwo){
        if(resone != restwo){
            System.out.println("Resolutions do not match and cannot produce the image");
            System.exit(0);
        }
        resuno = resone;
        resdos = restwo;
    }
    
    public static int getWidth(){return resuno;}
    
    public static int getHeight(){return resdos;}
    
    public static void setAmbient(double R, double G, double B){
        ambientR = R;
        ambientG = G;
        ambientB = B;
        
    }
    
    public Vector3D getAmbient(){return new Vector3D(new double[] {ambientR, ambientG, ambientB});}
    
    public static void buildCamera(){
        Wv = Ev.subtract(Lv);
        Wv = Wv.normalize();
        Uv = Uv.crossProduct(Up, Wv);
        Uv = Uv.normalize();
        Vv = Vv.crossProduct(Wv, Uv);
    }
    
    public Vector3D getEv(){return Ev;}
    
    public Vector3D getWv(){return Wv;}
    
    public Vector3D getUv(){return Uv;}
    
    public Vector3D getVv(){return Vv;}
    
    public double getCameraEtaOutside(){return this.etaOutside;}
    
    public void print(){
    System.out.println("Camera: Origin = " + Ev);
    System.out.println("Look point: Look = " + Lv);
    System.out.println("Up Vector: Vector = " + Up);
    System.out.println("Distance: d = " + distance);
    System.out.println("Bounds: left = " + leftbound + " right = " + rightbound + " bottom = " + downbound + " top = " + upbound);
    System.out.println("Resolution: res = " + resuno + " x " + resdos);
    System.out.println("Ambient: R = " + ambientR + " G = " + ambientG + " B = " + ambientB);
    }
}
