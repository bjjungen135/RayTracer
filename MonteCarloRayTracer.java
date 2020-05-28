import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.Arrays;

class MonteCarloRayTracer implements Runnable{
	private Ray ray;
	private int depth;
	private ArrayList<Light> lights;
	private ArrayList<RayObject> collisionObjects;
	private Camera camera;
	private Vector3D color = new Vector3D(0, 0, 0);
	private int numSamples = 0, maxSamples = 0;
	private CountDownLatch latch;

	public MonteCarloRayTracer(Ray ray, int depth, ArrayList<Light> lights, ArrayList<RayObject> collisionObjects, Camera camera, int samples, CountDownLatch latch){
		this.ray = ray;
		this.depth = depth;
		this.lights = lights;
		this.collisionObjects = collisionObjects;
		this.camera = camera;
		this.maxSamples = samples;
		this.latch = latch;
	}
	
	public void run(){
		try{
			while(numSamples < (maxSamples + 1)){
				numSamples++;
				this.color = this.color.add(rayTrace(this.ray, 0, new Vector3D(1, 1, 1)));
			}
			latch.countDown();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Intersect rayFind(Ray ray){
		Boolean[] hit = new Boolean[this.collisionObjects.size()];
        Double[] distance = new Double[this.collisionObjects.size()];
        Intersect[] intersects = new Intersect[this.collisionObjects.size()];
        for(int i = 0; i < this.collisionObjects.size(); i++){
			Intersect intersect = this.collisionObjects.get(i).rayTest(ray);
			if(intersect != null){
				hit[i] = intersect.getIntersectBool();
				distance[i] = intersect.getIntersectT();
				intersects[i] = intersect;
			}
        }
        Integer index = getSmallestDistance(hit, distance);
        if(index != null){
				intersects[index].setIntersectCollisionObject(this.collisionObjects.get(index));
				return intersects[index];
		}
        return null;
    }
    
    public Integer getSmallestDistance(Boolean[] hit, Double[] distance){
        double min = Double.MAX_VALUE;
        Integer index =  null;
        for(int i = 0; i < distance.length; i++){
            if(distance[i] != null && min > distance[i] && hit[i] && distance[i] > 0.0001){
                min = distance[i];
                index = i;
            }
        }
        return index;
    }
	
    public Vector3D rayTrace(Ray ray, int depth, Vector3D currentAlbedo){
		Intersect temp = rayFind(ray);
		if(temp != null){
			Material material = temp.getIntersectMaterial();
			int materialTest = (int)material.getMaterialNi();
			Ray scatter = null;
			Vector3D albedo = null;
			Vector3D emitted = new Vector3D(0, 0, 0);
			switch(materialTest){
				case 0:
					emitted = material.getMaterialka();
					break;
				case 1: 
					scatter = scatterLambertian(ray, temp);
					albedo = material.getMaterialka();
					break;
				case 2:
					scatter = scatterMirror(ray, temp);
					albedo = material.getMaterialka();
					break;
				case 3:
					scatter = scatterRefraction(ray, temp);
					albedo = material.getMaterialka();
				default:
					break;
			}
			if(scatter != null && depth < this.depth){
				currentAlbedo = pairwiseProduct(albedo, currentAlbedo);
				double max = Math.max(currentAlbedo.getX(), Math.max(currentAlbedo.getY(), currentAlbedo.getZ()));
				if(max < Math.random()){
					return new Vector3D(0, 0, 0);
				}
				currentAlbedo = elementDivision(currentAlbedo, max);
				return pairwiseProduct(currentAlbedo, emitted).add(rayTrace(scatter, (depth + 1), currentAlbedo));
			}
			else{
				return pairwiseProduct(currentAlbedo, emitted);
			}
		}
		else{
			return pairwiseProduct(currentAlbedo, new Vector3D(0, 0, 0));
		}
    }
    
    public Vector3D randomRayDirection(){
		double max = 1, min = -1;
		while(true){
			double x = (Math.random() * ((max - min) + 1)) + min;
			double y = (Math.random() * ((max - min) + 1)) + min;
			double z = (Math.random() * ((max - min) + 1)) + min;
			Vector3D randomDirection = new Vector3D(x, y, z);
			if(randomDirection.dotProduct(randomDirection) < 1){
				return randomDirection;
			}
		}
    }
    
    public Ray scatterLambertian(Ray ray, Intersect intersect){
		Vector3D direction = intersect.getIntersectSurfaceNormal().add(randomRayDirection()).normalize();
		return new Ray(intersect.getIntersectPt(), direction);
    }
    
    public Ray scatterMirror(Ray ray, Intersect intersect){
		Vector3D normal = intersect.getIntersectSurfaceNormal();
		Vector3D direction = ray.getDv();
		Vector3D reflected = normal.scalarMultiply(normal.dotProduct(direction)).scalarMultiply(2).subtract(direction).normalize();
		return new Ray(intersect.getIntersectPt(), reflected);
    }
    
    public Ray scatterRefraction(Ray ray, Intersect intersect){
		return intersect.getIntersectCollisionObject().refractionExit(ray.getDv().negate(), intersect.getIntersectPt(), 2, this.camera.getCameraEtaOutside(), null);
    }
    
    
    private Vector3D pairwiseProduct(Vector3D Vone, Vector3D Vtwo){
        return new Vector3D((Vone.getX() * Vtwo.getX()), (Vone.getY() * Vtwo.getY()), (Vone.getZ() * Vtwo.getZ()));
    }
    
    private Vector3D elementDivision(Vector3D Vone, double in){
        return new Vector3D((Vone.getX() / in), (Vone.getY() / in), (Vone.getZ() / in));
    }
	
	public Vector3D getColor(){return this.color;}
}
