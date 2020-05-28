import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

class Material{
    private Vector3D ka, kd, ks, kr, ke, tr;
    private double alpha = 0;
    private double Ni = 1, d = 1;

    public Material(double kaR, double kaG, double kaB, double kdR, double kdG, double kdB, double ksR, double ksG, double ksB, double krR, double krG, double krB){
        this.ka = new Vector3D(new double[] {kaR, kaG, kaB});
        this.kd = new Vector3D(new double[] {kdR, kdG, kdB});
        this.ks = new Vector3D(new double[] {ksR, ksG, ksB});
        this.kr = new Vector3D(new double[] {krR, krG, krB});
    }
    
    public void setMaterialAlpha(double inalpha){this.alpha = inalpha;}
    
    public void setMaterialNi(double inNi){this.Ni = inNi;}
    
    public void setMaterialD(double inD){this.d = inD;}
    
    public void setMaterialKe(double keR, double keG, double keB){this.ke = new Vector3D(keR, keG, keB);}
    
    public void setMaterialtr(double trR, double trG, double trB){this.tr = new Vector3D(trR, trG, trB);}
    
    public Vector3D getMaterialka(){return this.ka;}
    
    public Vector3D getMaterialkd(){return this.kd;}
    
    public Vector3D getMaterialks(){return this.ks;}
    
    public Vector3D getMaterialkr(){return this.kr;}
    
    public Vector3D getMaterialtr(){return this.tr;}
    
    public double getMaterialAlpha(){return this.alpha;}
    
    public Vector3D getMaterialke(){return this.ke;}
    
    public double getMaterialNi(){return this.Ni;}
    
    public double getMaterialD(){return this.d;}
    
    public void printMaterial(){
		System.out.println("Material: " + this + " - ka = " + this.ka + " kd = " + this.kd + " ks = " + this.ks + " kr = " + kr + " alpha = " + this.alpha);
    }
}
