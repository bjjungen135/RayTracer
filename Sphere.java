import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.*;


class Sphere extends RayObject{
	private Vector3D center;
    private double radius;
    private Material mat;
    private Vector3D pt;
    private Double t;
    private Sphere sph;
    private Boolean intersect;
    
    public Sphere(double inx, double iny, double inz, double inradius, double kaR, double kaG, double kaB, double kdR, double kdG, double kdB, double ksR, double ksG, double ksB, double krR, double krG, double krB){
        this.center = new Vector3D(new double[] {inx, iny, inz});
        this.radius = inradius;
        this.mat = new Material(kaR,  kaG,  kaB,  kdR,  kdG,  kdB,  ksR,  ksG,  ksB,  krR,  krG,  krB);
    }
    
    public Intersect rayTest(Ray ray){
		double t = 0;
		Boolean intersect = null;
		Vector3D pt = null;
        Vector3D Cv = this.center;
        Vector3D Lv = ray.getLv();
        Vector3D Uv = ray.getDv();
        Vector3D Tv = Cv.subtract(Lv);
        double v = Tv.dotProduct(Uv);
        double csq = Tv.dotProduct(Tv);
        double disc = Math.pow(this.radius, 2) - (csq - Math.pow(v, 2));
        if(disc > 0){
            double d = Math.sqrt(disc);
            t = v - d;
            pt = Lv.add(Uv.scalarMultiply(t));
            intersect = true;
            Vector3D surfaceNormal = pt.subtract(this.center).normalize();
            return new Intersect(intersect, t, surfaceNormal, pt, this.mat);
        }
        return null;
    }
    
    public Ray refractionExit(Vector3D W, Vector3D pt, double etaIn, double etaOut, ArrayList<Model> models){
		Vector3D testOne = refractionRay(W, pt, (pt.subtract(this.center)).normalize(), etaOut, etaIn);
		if(testOne.getX() + testOne.getY() + testOne.getZ() == 0.0){
			return null;
		}
		else{
			Vector3D exit = testOne.scalarMultiply((this.center.subtract(pt).dotProduct(testOne))).scalarMultiply(2).add(pt);
			Vector3D Nin = (this.center.subtract(exit)).normalize();
			Vector3D testTwo = refractionRay(testOne.negate(), exit, Nin, etaIn, etaOut);
			Ray reflectionRay = new Ray(exit, testTwo);
			return reflectionRay;
		}
    }
    
    public Vector3D refractionRay(Vector3D W, Vector3D pt, Vector3D N, double etaOne, double etaTwo){
		Vector3D T;
		double etar = (etaOne / etaTwo);
		double a = -etar;
		double wn = W.dotProduct(N);
		double radsq = Math.pow(etar, 2) * (Math.pow(wn, 2) - 1) + 1;
		if(radsq < 0){
			T = new Vector3D(0,0,0);
		}
		else{
			double b = (etar * wn) - Math.sqrt(radsq);
			T = N.scalarMultiply(b).add(W.scalarMultiply(a));
		}
		return T;
    }
    
    private static Vector3D pairwiseProduct(Vector3D Vone, Vector3D Vtwo){
        return new Vector3D(new double[] {Vone.getX() * Vtwo.getX(), Vone.getY() * Vtwo.getY(), Vone.getZ() * Vtwo.getZ()});
    }
    
    public Vector3D getSphereCenter(){return center;}
    
    public double getSphereRadius(){return this.radius;}
    
    public Material getSphereMaterial(){return this.mat;}
    
    public Material getMaterial(){return this.mat;}
}
