import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.AnyMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;


class Model{
	private double wx, wy, wz, theta, scale, tx, ty, tz, smooth;
	private String filename;
	private Vector3D Wv, Tv;
	private RealMatrix tranformationMatrix, verticiesMatrix = new Array2DRowRealMatrix(4,1);
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private ArrayList<RealVector> verticies = new ArrayList<RealVector>();
	private RealVector[] transformedVerticies;
	private int size = 1, count = 0;
	private String materialFileName;
	private Material material;
	private int linenum = 1;
	
	public Model(double inwx, double inwy, double inwz, double intheta, double inscale, double intx, double inty, double intz, double smooth, String objFile){
		this.Wv = new Vector3D(new double[] {inwx, inwy, inwz});
		this.theta = intheta;
		this.scale = inscale;
		this.Tv = new Vector3D(new double[] {intx, inty, intz});
		this.smooth = smooth;
		this.filename = objFile;
		createTranformationMatrix();
		createTriangles();
		transformVerticies();
	}

	private void createTranformationMatrix(){
		if(this.Wv.getX() + this.Wv.getY() + this.Wv.getZ() == 0){
			this.Wv = new Vector3D(1,0,0);
		}
		Vector3D Mv = new Vector3D(new double[] {Math.abs(this.Wv.getX()), Math.abs(this.Wv.getY()), Math.abs(this.Wv.getZ())});
		double min = Double.MAX_VALUE;
		int index = 0;
		double[] minMv = Mv.toArray();
		for(int i = 0; i < minMv.length; i++){
			if(minMv[i] <= min){
				min = minMv[i];
				index = i;
			}
		}
		int minIndex = 0;
		for(int i = 0; i < minMv.length; i++){
			if(minMv[i] == minMv[index]){
				minIndex = i;
			}
		}
		minMv[minIndex] = 1.0;
		Mv = new Vector3D(minMv);
		Vector3D Uv = this.Wv.crossProduct(Mv).normalize();
		Vector3D Vv = this.Wv.crossProduct(Uv);
		RealMatrix rotationMatrix = new Array2DRowRealMatrix(3, 3);
		rotationMatrix.setRow(0, Uv.toArray());
		rotationMatrix.setRow(1, Vv.toArray());
		rotationMatrix.setRow(2, this.Wv.toArray());
		double cosTheta = Math.cos(Math.toRadians(this.theta));
		double sinTheta = Math.sin(Math.toRadians(this.theta));
		RealMatrix rotationMatrixZ = MatrixUtils.createRealIdentityMatrix(3);
		rotationMatrixZ.setEntry(0, 0, cosTheta);
		rotationMatrixZ.setEntry(0, 1, -sinTheta);
		rotationMatrixZ.setEntry(1, 0, sinTheta);
		rotationMatrixZ.setEntry(1, 1, cosTheta);
		RealMatrix rotationMatrixT = rotationMatrix.transpose().multiply(rotationMatrixZ).multiply(rotationMatrix);
		RealVector one = rotationMatrixT.getRowVector(0);
		RealVector two = rotationMatrixT.getRowVector(1);
		RealVector three = rotationMatrixT.getRowVector(2);
		one = one.append(0);
		two = two.append(0);
		three = three.append(0);
		RealVector four = new ArrayRealVector(new double[] {0, 0, 0, 1});
		rotationMatrixT = new Array2DRowRealMatrix(4, 4);
		rotationMatrixT.setRowVector(0, one);
		rotationMatrixT.setRowVector(1, two);
		rotationMatrixT.setRowVector(2, three);
		rotationMatrixT.setRowVector(3, four);
		RealMatrix scaleMatrix = MatrixUtils.createRealIdentityMatrix(4);
		scaleMatrix = scaleMatrix.scalarMultiply(this.scale);
		scaleMatrix.setEntry(3, 3, 1.0);
		RealMatrix translationMatrix = MatrixUtils.createRealIdentityMatrix(4);
		double[] temp = this.Tv.toArray();
		RealVector translation = new ArrayRealVector(temp);
		translation = translation.append(1.0);
		translationMatrix.setColumnVector(3, translation);
		tranformationMatrix = translationMatrix.multiply(scaleMatrix).multiply(rotationMatrixT);
	}
	
	private void createTriangles(){
		String obj = readFile(filename);
		String[] instructions = obj.split("\n");
		for(int i = 0; i < instructions.length; i++){
			if(instructions[i].isEmpty()){continue;}
			if(instructions[i].charAt(0) == '#'){continue;}
			if(instructions[i].charAt(0) == 's'){continue;}
			if(instructions[i].contains("v") && !instructions[i].contains("vn") && !instructions[i].contains("vt")){
				String[] temp = instructions[i].split("[ ]");
				RealVector vert = new ArrayRealVector(new double[] {Double.parseDouble(temp[1]), Double.parseDouble(temp[2]), Double.parseDouble(temp[3]), 0});
				verticies.add(vert);
				if(count != 0){
					size += 1;
				}
				count++;
				
			}
			if(instructions[i].contains("f")){
				String[] temp = instructions[i].split("[ ]");
				String[] faceone = temp[1].split("[//]");
				String[] facetwo = temp[2].split("[//]");
				String[] facethree = temp[3].split("[//]");
				triangles.add(new Triangle(Integer.parseInt(faceone[0]), Integer.parseInt(facetwo[0]), Integer.parseInt(facethree[0]), material));
			}
			if(instructions[i].contains("mtllib")){
				String[] temp = instructions[i].split("[ ]");
				materialFileName = temp[1];
			}
			if(instructions[i].contains("usemtl")){
				String[] temp = instructions[i].split("[ ]");
				String mat = readFile(materialFileName);
				material = parseMaterialFile(mat, temp[1]);
			}
		}
	}
	
	private String readFile(String file){
        String fileAsString = "";
        try{
            InputStream is = new FileInputStream(file);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();
            while(line != null){
                sb.append(line).append("\n");
                line = buf.readLine();
            }
            fileAsString = sb.toString();
        }
        catch(Exception e){
            System.out.println(e);
        }
        return fileAsString;
    }
    
    private Material parseMaterialFile(String matFile, String mat){
		String[] materialFile = matFile.split("[\n]");
		double kaR = 0, kaG = 0, kaB = 0, kdR = 0, kdG = 0, kdB = 0, ksR = 0, ksG = 0, ksB = 0, krR = 0, krG = 0, krB = 0, Ns = 0, Ni = 0, d = 0, illum = 0, keR = 0, keG = 0, keB = 0, trR = 0, trG = 0, trB = 0;
		Material material;
		for(int i = 0; i < materialFile.length; i++){
			if(materialFile[i].isEmpty()){continue;}
			if(materialFile[i].charAt(0) == '#'){continue;}
			if(materialFile[i].contains("newmtl") && materialFile[i].contains(mat)){
				int index = i + 1;
				while(index < materialFile.length && !materialFile[index].contains("newmtl")){
					String[] values = materialFile[index].split("[ ]");
					if(values[0].isEmpty()){index++; continue;}
					if(values[0].contains("Ns")){Ns = Double.parseDouble(values[1]);}
					if(values[0].contains("Ka")){
						kaR = Double.parseDouble(values[1]);
						kaG = Double.parseDouble(values[2]);
						kaB = Double.parseDouble(values[3]);
					}
					if(values[0].contains("Kd") && values[0].charAt(0) != 'd'){
						kdR = Double.parseDouble(values[1]);
						kdG = Double.parseDouble(values[2]);
						kdB = Double.parseDouble(values[3]);
					}
					if(values[0].contains("Ks")){
						ksR = Double.parseDouble(values[1]);
						ksG = Double.parseDouble(values[2]);
						ksB = Double.parseDouble(values[3]);
					}
					if(values[0].contains("Ke")){
						keR = Double.parseDouble(values[1]);
						keG = Double.parseDouble(values[2]);
						keB = Double.parseDouble(values[3]);
					}
					if(values[0].contains("Ni")){Ni = Double.parseDouble(values[1]);}
					if(values[0].contains("d")){d = Double.parseDouble(values[1]);}
					if(values[0].contains("illum")){illum = Double.parseDouble(values[1]);}
					if(values[0].contains("Kr")){
						krR = Double.parseDouble(values[1]);
						krG = Double.parseDouble(values[2]);
						krB = Double.parseDouble(values[3]);
					}
					if(values[0].contains("Tr")){
						trR = Double.parseDouble(values[1]);
						trG = Double.parseDouble(values[2]);
						trB = Double.parseDouble(values[3]);
					}
					index++;
				}
			}
			else{continue;}
		}
		if(illum == 3 || illum == 6 || illum == 7){
			krR = ksR;
			krG = ksG;
			krB = ksB;
		}
		if(illum == 3 && trR + trG + trB != 0){
			trR = 0;
			trG = 0;
			trB = 0;
		}
		if(illum == 6 || illum == 7 && trR + trG + trB == 0){
			trR = 1;
			trG = 1;
			trB = 1;
		}
		material = new Material(kaR, kaG, kaB, kdR, kdG, kdB, ksR, ksG, ksB, krR, krG, krB);
		material.setMaterialKe(keR, keG, keB);
		material.setMaterialAlpha(Ns);
		material.setMaterialNi(Ni);
		material.setMaterialD(d);
		material.setMaterialtr(trR, trG, trB);
		return material;
    }
    
    private void transformVerticies(){
		verticiesMatrix = MatrixUtils.createRealMatrix(size, 4);
		for(int i = 0; i < verticies.size(); i++){
			verticiesMatrix.setRowVector(i, verticies.get(i));
		}
		RealMatrix transformedVerticiesMatrix = tranformationMatrix.multiply(verticiesMatrix.transpose()).transpose();
		transformedVerticies = new RealVector[transformedVerticiesMatrix.getRowDimension()];
		for(int i = 0; i < transformedVerticiesMatrix.getRowDimension(); i++){
			RealVector getTranVert = transformedVerticiesMatrix.getRowVector(i);
			RealVector tranVert = new ArrayRealVector(new double[] {getTranVert.getEntry(0) + this.Tv.getX(), getTranVert.getEntry(1) + this.Tv.getY(), getTranVert.getEntry(2) + this.Tv.getZ()});
			transformedVerticies[i] = tranVert;
		}
		for(int i = 0; i < triangles.size(); i++){
			triangles.get(i).setTriangleFirstVertex(transformedVerticies[triangles.get(i).getTriangleFirstVertexIndex()]);
			triangles.get(i).setTriangleSecondVertex(transformedVerticies[triangles.get(i).getTriangleSecondVertexIndex()]);
			triangles.get(i).setTriangleThirdVertex(transformedVerticies[triangles.get(i).getTriangleThirdVertexIndex()]);
			triangles.get(i).computeTriangleSurfaceNormal();
		}
		for(int i = 0; i < triangles.size(); i++){
			for(int j = 0; j < triangles.get(i).getTriangleFaces().length; j++){
				Vector3D sumA = triangles.get(i).getTriangleSurfaceNormal();
				Vector3D sumB = triangles.get(i).getTriangleSurfaceNormal();
				Vector3D sumC = triangles.get(i).getTriangleSurfaceNormal();
				for(int k = 0; k < triangles.size(); k++){
					if(triangles.get(i).equals(triangles.get(k))){continue;}
					if(triangles.get(k).getTriangleIndexOne() == triangles.get(i).getTriangleFaces()[j] || triangles.get(k).getTriangleIndexTwo() == triangles.get(i).getTriangleFaces()[j] || triangles.get(k).getTriangleIndexThree() == triangles.get(i).getTriangleFaces()[j] && !triangles.get(i).equals(triangles.get(k))){
						double smoothFactor = triangles.get(i).getTriangleSurfaceNormal().dotProduct(triangles.get(k).getTriangleSurfaceNormal());
						if(smoothFactor > 1){smoothFactor = 1;}
						if(smoothFactor < -1){smoothFactor = -1;}
						if(Math.toDegrees(Math.acos(smoothFactor)) < this.smooth){
							if(j == 0){
								sumA = sumA.add(triangles.get(k).getTriangleSurfaceNormal());
							}
							if(j == 1){
								sumB = sumB.add(triangles.get(k).getTriangleSurfaceNormal());
							}
							if(j == 2){
								sumC = sumC.add(triangles.get(k).getTriangleSurfaceNormal());
							}
						}
					}
				}
				if(j == 0){
					if(sumA.getNorm() != 0){
						sumA = sumA.normalize();
					}
					triangles.get(i).setTriangleVertexSumA(sumA);
				}
				if(j == 1){
					if(sumB.getNorm() != 0){
						sumB = sumB.normalize();
					}
					triangles.get(i).setTriangleVertexSumB(sumB);
				}
				if(j == 2){
					if(sumC.getNorm() != 0){
						sumC = sumC.normalize();
					}
					triangles.get(i).setTriangleVertexSumC(sumC);
				}
			}
		}
    }
	
	public String getObjFileName(){return filename;}
	
	public ArrayList<Triangle> getTrianglesList(){return triangles;}
}
