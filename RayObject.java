import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.*;

abstract class RayObject{
    
    abstract public Intersect rayTest(Ray ray);
    
    abstract public Material getMaterial();
    
    abstract public Ray refractionExit(Vector3D W, Vector3D pt, double etaIn, double etaOut, ArrayList<Model> models);
    
    abstract public Vector3D refractionRay(Vector3D W, Vector3D pt, Vector3D N, double etaOne, double etaTwo);
    
    //have checkCollision
    //get normal
    //get material
}
