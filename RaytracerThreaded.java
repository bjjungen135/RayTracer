import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.*;
import java.util.concurrent.CountDownLatch;

class RaytracerThreaded implements Runnable{
	private Vector3D color = new Vector3D(0, 0, 0), refatt, rgb;
	private Ray ray;
	private int depth;
	private ArrayList<RayObject> collisionObjects;
	private ArrayList<Light> lights;
	private Camera camera;
	private ArrayList<Model> models;
	private CountDownLatch latch;
	
	public RaytracerThreaded(Ray ray, int depth, ArrayList<Light> lights, ArrayList<RayObject> collisionObjects, Camera camera, ArrayList<Model> models, CountDownLatch latch){
		this.ray = ray;
		this.depth = depth;
		this.collisionObjects = collisionObjects;
		this.lights = lights;
		this.camera = camera;
		this.models = models;
		this.latch = latch;
		this.refatt = new Vector3D(1.0, 1.0, 1.0);
		this.rgb = new Vector3D(0 , 0, 0);
	}

	public void run(){
		try{
			this.color = rayTrace(this.ray, this.rgb, this.refatt, this.depth);
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

	public Boolean checkShadow(Ray shadowRay, Light light){
		Boolean[] hit = new Boolean[this.collisionObjects.size()];
        Double[] distance = new Double[this.collisionObjects.size()];
        for(int i = 0; i < this.collisionObjects.size(); i++){
			Intersect intersect = this.collisionObjects.get(i).rayTest(shadowRay);
			if(intersect != null && intersect.getIntersectBool()){
				if(intersect.getIntersectT() > 0.0001 && intersect.getIntersectT() < intersect.getIntersectPt().subtract(light.getLightPoint()).getNorm()){
					return true;
				}
			}
        }
        return false;
	}
    
    public Vector3D rayTrace(Ray ray, Vector3D accum, Vector3D reflectionattenuation, int level){
		Intersect temp = rayFind(ray);
		if(temp != null){
			Vector3D surfaceNormal = temp.getIntersectSurfaceNormal();
			Material material = temp.getIntersectMaterial();
			Vector3D color = pairwiseProduct(this.camera.getAmbient(), material.getMaterialka());
			for(int i = 0; i < this.lights.size(); i++){
				Vector3D toLight = (this.lights.get(i).getLightPoint().subtract(temp.getIntersectPt())).normalize();
				Boolean shadowCheck = checkShadow(new Ray(temp.getIntersectPt(), toLight), this.lights.get(i));
				if(shadowCheck){
					continue;
				}
				double surfaceNormalDotToLight = surfaceNormal.dotProduct(toLight);
				if(surfaceNormalDotToLight > 0.0){
					if(!ray.getRayBool()){
						color = color.add(pairwiseProduct(material.getMaterialkd(), this.lights.get(i).getLightRGB()).scalarMultiply(surfaceNormalDotToLight));
						Vector3D toCamera = (ray.getLv().subtract(temp.getIntersectPt())).normalize();
						Vector3D reflection = (surfaceNormal.scalarMultiply(surfaceNormalDotToLight).scalarMultiply(2).subtract(toLight)).normalize();
						double toCameraDotReflection = toCamera.dotProduct(reflection);
						if(toCameraDotReflection > 0.0){
							color = color.add(pairwiseProduct(material.getMaterialks(), this.lights.get(i).getLightRGB()).scalarMultiply(Math.pow(toCameraDotReflection, material.getMaterialAlpha())));
						}
					}
				}
			}
			accum = accum.add(new Vector3D((reflectionattenuation.getX() * color.getX()), (reflectionattenuation.getY() * color.getY()), (reflectionattenuation.getZ() * color.getZ())));
			if(level > 0){
				Vector3D flec = new Vector3D(0, 0, 0);
				Vector3D Uinverse = ray.getDv().negate();
				Vector3D reflectionRayDirection = (surfaceNormal.scalarMultiply(surfaceNormal.dotProduct(Uinverse)).scalarMultiply(2).subtract(Uinverse)).normalize();
				//if(temp.getIntersectUnSmoothedSurfaceNormal().dotProduct(reflectionRayDirection) >= 0.0001){
					flec = rayTrace(new Ray(temp.getIntersectPt(), reflectionRayDirection), flec, pairwiseProduct(material.getMaterialkr(), reflectionattenuation), (level - 1));
					accum = accum.add(flec);
				//}
			}
			if(level > 0 && ((material.getMaterialtr().getX() + material.getMaterialtr().getY() + material.getMaterialtr().getZ()) > 0.0  && material.getMaterialNi() != 0)){
				Vector3D thru = new Vector3D(0, 0, 0);
				Ray fraR = null;
				if(temp.getIntersectCollisionObject().getClass() == Triangle.class){
					if(ray.getRayBool()){
						Vector3D direction = temp.getIntersectCollisionObject().refractionRay(ray.getDv().negate(), temp.getIntersectPt(), temp.getIntersectSurfaceNormal(), material.getMaterialNi(), this.camera.getCameraEtaOutside());
						if(direction.getX() + direction.getY() + direction.getZ() != 0){
							fraR = new Ray(temp.getIntersectPt(), direction);
							fraR.setRayBool(false);
						}
					}
					else{
						Vector3D direction = temp.getIntersectCollisionObject().refractionRay(ray.getDv().negate(), temp.getIntersectPt(), temp.getIntersectSurfaceNormal(), this.camera.getCameraEtaOutside(), material.getMaterialNi());
						if(direction.getX() + direction.getY() + direction.getZ() != 0){
							fraR = new Ray(temp.getIntersectPt(), direction);
							fraR.setRayBool(true);
						}
					}
				}
				else{
					fraR = temp.getIntersectCollisionObject().refractionExit(ray.getDv().negate(), temp.getIntersectPt(), material.getMaterialNi(), this.camera.getCameraEtaOutside(), this.models);
				}
				if(fraR != null){
					thru = rayTrace(fraR, thru, pairwiseProduct(material.getMaterialtr(),
					reflectionattenuation), (level - 1));
					accum = accum.add(thru);
				}
			}
		}
        return accum;
    }
    
    private Vector3D pairwiseProduct(Vector3D Vone, Vector3D Vtwo){
        return new Vector3D((Vone.getX() * Vtwo.getX()), (Vone.getY() * Vtwo.getY()), (Vone.getZ() * Vtwo.getZ()));
    }
    
    public Vector3D getColor(){return this.color;}
}
