import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

class Ray{
    private Vector3D Lv, Dv;
    private Boolean refraction = false;
    
    public Ray(Vector3D L, Vector3D D){
        this.Lv = L;
        this.Dv = D;
        this.Dv = Dv.normalize();
    }
    
    public void setRayBool(Boolean refraction){this.refraction = refraction;}
    
    public Vector3D getLv(){return this.Lv;}
    
    public Vector3D getDv(){return this.Dv;}
    
    public Boolean getRayBool(){return this.refraction;}
}
