import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.AnyMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.LUDecomposition;
import java.util.*;

class Intersect{
	private Boolean intersect = false;
	private double t;
	private Vector3D surfaceNormal, intersectPt, unSmoothedSurfaceNormal;
	private Material material;
	private RayObject collisionObject;
	
	public Intersect(Boolean intersect, double tval, Vector3D surfaceN, Vector3D intersectpt, Material mat){
		this.intersect = intersect;
		this.t = tval;
		this.surfaceNormal = surfaceN;
		this.unSmoothedSurfaceNormal = unSmoothedSurfaceNormal;
		this.intersectPt = intersectpt;
		this.material = mat;
	}
	
	public void setIntersectCollisionObject(RayObject collisionObject){this.collisionObject = collisionObject;}
	
	public Boolean getIntersectBool(){return this.intersect;}
	
	public double getIntersectT(){return this.t;}
	
	public Vector3D getIntersectPt(){return this.intersectPt;}
	
	public Material getIntersectMaterial(){return this.material;}
	
	public Vector3D getIntersectSurfaceNormal(){return this.surfaceNormal;}
	
	public RayObject getIntersectCollisionObject(){return this.collisionObject;}
}
