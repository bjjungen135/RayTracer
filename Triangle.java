import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.AnyMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.LUDecomposition;
import java.util.*;

class Triangle extends RayObject{
	private int indexOne, indexTwo, indexThree;
	private Material material;
	private RealVector vertexOne, vertexTwo, vertexThree;
	private Vector3D pt, surfaceNormal, A, B, C, sumVertexA, sumVertexB, sumVertexC;

	public Triangle(int inIndexOne, int inIndexTwo, int inIndexThree, Material inMaterial){
		this.indexOne = inIndexOne;
		this.indexTwo = inIndexTwo;
		this.indexThree = inIndexThree;
		this.material = inMaterial;
	}
	
	public Intersect rayTest(Ray ray){
		RealVector Lv = new ArrayRealVector(new double[] {ray.getLv().getX(), ray.getLv().getY(), ray.getLv().getZ()});
		RealVector Dv = new ArrayRealVector(new double[] {ray.getDv().getX(), ray.getDv().getY(), ray.getDv().getZ()});
		RealMatrix YM = new Array2DRowRealMatrix(3,1);
		YM.setColumnVector(0, this.vertexOne.subtract(Lv));
		RealMatrix MM = new Array2DRowRealMatrix(3,3);
		MM.setColumnVector(0, this.vertexOne.subtract(this.vertexTwo));
		MM.setColumnVector(1, this.vertexOne.subtract(this.vertexThree));
		MM.setColumnVector(2, Dv);
		RealMatrix MMsOne = MM.copy();
		RealMatrix MMsTwo = MM.copy();
		RealMatrix MMsThree = MM.copy();
		MMsOne.setColumnVector(0, YM.getColumnVector(0));
		MMsTwo.setColumnVector(1, YM.getColumnVector(0));
		MMsThree.setColumnVector(2, YM.getColumnVector(0));
		double determinantMM = new LUDecomposition(MM).getDeterminant();
		if(determinantMM != 0){
			double determinantMOne = new LUDecomposition(MMsOne).getDeterminant();
			double determinantMTwo = new LUDecomposition(MMsTwo).getDeterminant();
			double determinantMThree = new LUDecomposition(MMsThree).getDeterminant();
			double beta = determinantMOne/determinantMM;
			double gamma = determinantMTwo/determinantMM;
			double t = determinantMThree/determinantMM;
			if(beta >= 0 && gamma >= 0 && (beta + gamma) <= 1 && t > 0.0001){
				Boolean intersect = true;
				Vector3D pt = this.A.add(this.B.subtract(this.A).scalarMultiply(beta)).add(this.C.subtract(this.A).scalarMultiply(gamma));
				Vector3D surfaceNormal = this.sumVertexA.scalarMultiply(1 - beta - gamma).add(this.sumVertexB.scalarMultiply(beta)).add(this.sumVertexC.scalarMultiply(gamma));
				if(surfaceNormal.dotProduct(ray.getDv()) > 0.0){
					surfaceNormal = surfaceNormal.negate();
				}
				return new Intersect(intersect, t, surfaceNormal, pt, this.material);
			}
			else if(determinantMM == 0){
				return null;
			}
		}
		return null;
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
	
	public Ray refractionExit(Vector3D W, Vector3D pt, double etaIn, double etaOut, ArrayList<Model> models){
		Vector3D epsilon = new Vector3D(0.000001, 0.000001, 0.000001);
		Vector3D testOne = refractionRay(W, pt, this.surfaceNormal, etaOut, etaIn);
		if(testOne.getX() + testOne.getY() + testOne.getZ() == 0.0){
			return null;
		}
		Intersect intersect = null;
		for(int i = 0; i < models.size(); i++){
			if(models.get(i).getTrianglesList().contains(this)){
				for(int j = 0; j < models.get(i).getTrianglesList().size(); j++){
					Vector3D inpt = pt.add(epsilon);
					inpt = new Vector3D(pt.getX() * this.surfaceNormal.getX(), pt.getY() * this.surfaceNormal.getY(), pt.getZ() * this.surfaceNormal.getZ());
					intersect = models.get(i).getTrianglesList().get(j).rayTest(new Ray(inpt, testOne));
					if(intersect != null){
						//System.out.println("Found a refraction!!!");
						break;
					}
				}
			}
		}
		if(intersect != null){
			//After finds intersect then call refractionRay to get the exit ray and give that ray back with the intersectPt
			Vector3D testTwo = refractionRay(testOne.negate(), intersect.getIntersectPt(), this.surfaceNormal, etaIn, etaOut);
			return new Ray(intersect.getIntersectPt(), testTwo);
		}
		return null;
	}
	
	public void computeTriangleSurfaceNormal(){
		this.A = new Vector3D(this.vertexOne.getEntry(0), this.vertexOne.getEntry(1), this.vertexOne.getEntry(2));
		this.B = new Vector3D(this.vertexTwo.getEntry(0), this.vertexTwo.getEntry(1), this.vertexTwo.getEntry(2));
		this.C = new Vector3D(this.vertexThree.getEntry(0), this.vertexThree.getEntry(1), this.vertexThree.getEntry(2));
		this.surfaceNormal = ((this.A.subtract(this.B)).crossProduct((this.A.subtract(this.C))));
		if(this.surfaceNormal.getNorm() != 0){
			this.surfaceNormal = this.surfaceNormal.normalize();
		}
	}
	
	public void setTriangleFirstVertex(RealVector vertexA){this.vertexOne = vertexA;}
	
	public void setTriangleSecondVertex(RealVector vertexB){this.vertexTwo = vertexB;}
	
	public void setTriangleThirdVertex(RealVector vertexC){this.vertexThree = vertexC;}
	
	public void setTriangleVertexA(Vector3D vertexA){this.A = vertexA;}
	
	public void setTriangleVertexB(Vector3D vertexB){this.B = vertexB;}
	
	public void setTriangleVertexC(Vector3D vertexC){this.C = vertexC;}
	
	public void setTriangleVertexSumA(Vector3D vertexA){this.sumVertexA = vertexA;}
	
	public void setTriangleVertexSumB(Vector3D vertexB){this.sumVertexB = vertexB;}
	
	public void setTriangleVertexSumC(Vector3D vertexC){this.sumVertexC = vertexC;}
	
	public Vector3D getTriangleFace(){return new Vector3D(new double[] {indexOne, indexTwo, indexThree});}
	
	public int[] getTriangleFaces(){return new int[] {indexOne, indexTwo, indexThree};}
	
	public int getTriangleIndexOne(){return this.indexOne;}
	
	public int getTriangleIndexTwo(){return this.indexTwo;}
	
	public int getTriangleIndexThree(){return this.indexThree;}
	
	public int getTriangleFirstVertexIndex(){return this.indexOne - 1;}
	
	public int getTriangleSecondVertexIndex(){return this.indexTwo - 1;}
	
	public int getTriangleThirdVertexIndex(){return this.indexThree - 1;}
	
	public Vector3D getTriangleSurfaceNormal(){return this.surfaceNormal;}
	
	public Material getMaterial(){return this.material;}
}
