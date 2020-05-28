import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.*;
import java.util.concurrent.CountDownLatch;

class Raytracer{
    private static ArrayList<Light> lights = new ArrayList<Light>();
    private static Camera camera = new Camera();
    private static int depth = 0;
    private static ArrayList<RayObject> collisionObjects = new ArrayList<RayObject>();
    private static ArrayList<Model> models = new ArrayList<Model>();
    private static Vector3D rgb = new Vector3D(0 , 0, 0);
    private static Boolean montecarlo = false;
    private static int samples = 0;
    private static int threadLimit = 0, coreNumber = 0;
    private static int linenum = 1;
    
    public static void main(String[] args){
        if(args.length > 2 || args.length < 2){
            System.out.println("Not enough arguments. Please give a driver file for the first argument and a filename to write the image too as the second argument");
            System.exit(0);
        }
        String driver = readFile(args[0]);
        String[] instructions = driver.split("\n");
        getInstructions(instructions);
        camera.buildCamera();
        Vector3D[][] picture = new Vector3D[camera.getWidth()][camera.getHeight()];
        final double startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(camera.getWidth() * camera.getHeight());
        String threadLimitString;
        String coreNumberString;
        try{
			Process p = Runtime.getRuntime().exec("ulimit -u");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			threadLimitString = br.readLine();
			p.waitFor();
			p.destroy();
			threadLimit = Integer.parseInt(threadLimitString);
			p = Runtime.getRuntime().exec("nproc");
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			coreNumberString = br.readLine();
			p.waitFor();
			p.destroy();
			coreNumber = Integer.parseInt(coreNumberString);
			threadLimit = threadLimit - (threadLimit/coreNumber - 100);
        }
        catch(Exception e){e.printStackTrace();}
        if(montecarlo){
 			MonteCarloRayTracer[][] monteRaytraceThread = new MonteCarloRayTracer[camera.getWidth()][camera.getHeight()];
 			for(int i = 0; i < camera.getWidth(); i++){
 				for(int j = 0; j < camera.getHeight(); j++){
					//System.out.println(Thread.activeCount());
					while(Thread.activeCount() <= threadLimit){
						if(Thread.activeCount() >= threadLimit){
							System.out.println("About to crash homie");
							System.out.println(Thread.activeCount());
						}
						if(Thread.activeCount() < threadLimit){
							Ray ray = pixelRay(i, j);
							monteRaytraceThread[i][j] = new MonteCarloRayTracer(ray, depth, lights, collisionObjects, camera, samples, latch);
							Thread object = new Thread(monteRaytraceThread[i][j]);
							object.start();
							break;
						}
 					}
 				}
 			}
 			try{
 				latch.await();
 				for(int i = 0; i < camera.getWidth(); i++){
 					for(int j = 0; j < camera.getHeight(); j++){
						Vector3D color = colorPixelMonte(monteRaytraceThread[i][j].getColor());
						if(color.getX() + color.getY() + color.getZ() == 0){
							color = new Vector3D(255,128,0);
							color = colorPixelMonte(monteRaytraceThread[i][j].getColor());
						}
 						picture[j][i] = color;
 					}
 				}
 			}
 			catch(Exception e){e.printStackTrace();}
			}
			
        else{
			RaytracerThreaded[][] raytraceThread = new RaytracerThreaded[camera.getWidth()][camera.getHeight()];
			for(int i = 0; i < camera.getWidth(); i++){
				for(int j = 0; j < camera.getHeight(); j++){
					while(true){
						if(Thread.activeCount() < threadLimit){
							Ray ray = pixelRay(i, j);
							raytraceThread[i][j] = new RaytracerThreaded(ray, depth, lights, collisionObjects, camera, models, latch);
							Thread object = new Thread(raytraceThread[i][j]);
							object.start();
							break;
						}
					}
				}
			}
			try{
				latch.await();
				for(int i = 0; i < camera.getWidth(); i++){
					for(int j = 0; j < camera.getHeight(); j++){
						picture[j][i] = colorPixel(raytraceThread[i][j].getColor());
					}
				}
			}
			catch(Exception e){e.printStackTrace();}
        }
        writePicture(args[1], picture);
        final double endTime = System.currentTimeMillis();
        double executionTime = endTime - startTime;
        int hours = (int) ((executionTime / (1000 * 60 * 60)) % 24);
		int minutes = (int) ((executionTime / (1000 * 60)) % 60);
		int seconds = (int) ((executionTime / 1000) % 60);
        System.out.println("Total execution time: " + hours + " hours " + minutes + " min and " + seconds + " secs");
    }
    
    public static String readFile(String file){
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
    
    public static void getInstructions(String[] instructions){
        int index = 0;
        for(int i = 0; i < instructions.length; i++){
			if(instructions[i].isEmpty()){
				continue;
			}
            if(instructions[i].charAt(0) == '#'){
                continue;
            }
            if(instructions[i].contains("eye")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setOrigin(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]));
            }
            if(instructions[i].contains("look")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setLook(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]));
            }
            if(instructions[i].contains("up")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setUp(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]));
            }
            if(instructions[i].charAt(0) == 'd'){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setDistance(Double.parseDouble(temp[index]));
            }
            if(instructions[i].contains("bounds")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setBounds(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]), Double.parseDouble(temp[index + 3]));
            }
            if(instructions[i].contains("res")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setResolution(Integer.parseInt(temp[index]), Integer.parseInt(temp[ index + 1]));
            }
            if(instructions[i].contains("ambient")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                camera.setAmbient(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]));
            }
            if(instructions[i].contains("light")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                Light templight = new Light(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]), Double.parseDouble(temp[index + 3]), Double.parseDouble(temp[index + 4]), Double.parseDouble(temp[index + 5]), Double.parseDouble(temp[index + 6]));
                lights.add(templight);
            }
            if(instructions[i].contains("sphere") && !instructions[i].contains(".obj")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                Sphere tempsphere = new Sphere(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]), Double.parseDouble(temp[index + 3]), Double.parseDouble(temp[index + 4]), Double.parseDouble(temp[index + 5]), Double.parseDouble(temp[index + 6]), Double.parseDouble(temp[index + 7]), Double.parseDouble(temp[index + 8]), Double.parseDouble(temp[index + 9]), Double.parseDouble(temp[index + 10]), Double.parseDouble(temp[index + 11]), Double.parseDouble(temp[index + 12]), Double.parseDouble(temp[index + 13]), Double.parseDouble(temp[index + 14]), Double.parseDouble(temp[index + 15]));
                tempsphere.getSphereMaterial().setMaterialAlpha(16);
                double ni = Double.parseDouble(temp[index + 16]);
                tempsphere.getSphereMaterial().setMaterialNi(ni);
                if(ni != 0){
					tempsphere.getSphereMaterial().setMaterialtr((1 - tempsphere.getSphereMaterial().getMaterialkr().getX()), (1 - tempsphere.getSphereMaterial().getMaterialkr().getY()), (1 - tempsphere.getSphereMaterial().getMaterialkr().getZ())); 
                }
                else{
					tempsphere.getSphereMaterial().setMaterialtr(0, 0, 0);
                }
                collisionObjects.add(tempsphere);
            }
            if(instructions[i].contains("recursionlevel")){
                String[] temp = instructions[i].split("[ ]");
                index = getIndex(temp);
                depth = Integer.parseInt(temp[index]);
            }
            if(instructions[i].contains("model")){
				String[] temp = instructions[i].split("[ ]");
				index = getIndex(temp);
				Model tempmodel = new Model(Double.parseDouble(temp[index]), Double.parseDouble(temp[index + 1]), Double.parseDouble(temp[index + 2]), Double.parseDouble(temp[index + 3]), Double.parseDouble(temp[index + 4]), Double.parseDouble(temp[index + 5]), Double.parseDouble(temp[index + 6]), Double.parseDouble(temp[index + 7]), Double.parseDouble(temp[index + 8]), temp[index + 9]);
				models.add(tempmodel);
				ArrayList<Triangle> getTriangles = tempmodel.getTrianglesList();
 				for(int j = 0; j < getTriangles.size(); j++){
 					collisionObjects.add(getTriangles.get(j));
 				}
            }
            if(instructions[i].contains("montecarlo")){
				montecarlo = true;
            }
            if(instructions[i].contains("samples")){
				String[] temp = instructions[i].split("[ ]");
				index = getIndex(temp);
				samples = Integer.parseInt(temp[index]);
            }
            else{
                continue;
            }
        }
    }
    
    public static int getIndex(String[] temp){
        for(int i = 0; i < temp.length; i++){
            if(temp[i].equals("light")){continue;}
            if(temp[i].equals("recursionlevel")){continue;}
            if(temp[i].equals("eye")){continue;}
            if(temp[i].equals("look")){continue;}
            if(temp[i].equals("up")){continue;}
            if(temp[i].equals("d")){continue;}
            if(temp[i].equals("bounds")){continue;}
            if(temp[i].equals("res")){continue;}
            if(temp[i].equals("ambient")){continue;}
            if(temp[i].equals("sphere")){continue;}
            if(temp[i].equals("model")){continue;}
            if(temp[i].equals("samples")){continue;}
            if(temp[i].isEmpty()){continue;}
            else{
                return i;
            }
        }
        return 1;
    }
    
    public static void writePicture(String filename, Vector3D[][] picture){
        try{
            FileWriter fw = new FileWriter(filename);
            fw.write("P3\n");
            fw.write(camera.getWidth() + " " + camera.getHeight() + " " + 255 + "\n");
            int pretty = 0;
            for(int i = 0; i < picture.length; i++){
                for(int j = 0; j < picture[i].length; j++){
                    if(pretty == (i % camera.getWidth())){
                        fw.write((int)picture[i][j].getX() + " " + (int)picture[i][j].getY() + " " + (int)picture[i][j].getZ() + " ");
                    }
                    else if(pretty != (i % camera.getWidth())){
                        fw.write("\n" + (int)picture[i][j].getX() + " " + (int)picture[i][j].getY() + " " + (int)picture[i][j].getZ() + " ");
                        pretty++;
                    }
                }
            }
            fw.write("\n");
            fw.close();
        }
        catch(Exception e){System.out.println(e);}
    }
    
    public static Ray pixelRay(double i, double j){
        double px = i/(camera.getWidth() - 1) * (camera.getRightBound() - camera.getLeftBound()) + camera.getLeftBound();
        double py = j/(camera.getHeight() - 1) * (camera.getDownBound() - camera.getUpBound()) + camera.getUpBound();
        Vector3D Lv = camera.getEv().add(camera.getWv().scalarMultiply(camera.getDistance())).add(camera.getUv().scalarMultiply(px)).add(camera.getVv().scalarMultiply(py));
        Vector3D Dv = (Lv.subtract(camera.getEv())).normalize();
        return new Ray(Lv, Dv);
    }
    
    private static Vector3D colorPixel(Vector3D pixel){
        double tempR = pixel.getX() * 255;
        double tempG = pixel.getY() * 255;
        double tempB = pixel.getZ() * 255;
        double R = Math.round(tempR);
        double G = Math.round(tempG);
        double B = Math.round(tempB);
        if(R > 255){
            R = 255;
        }
        if(R < 0){
            R = 0;
        }
        if(G > 255){
            G = 255;
        }
        if(G < 0){
            G = 0;
        }
        if(B > 255){
            B = 255;
        }
        if(B < 0){
            B = 0;
        }
        Vector3D result = new Vector3D(R, G, B);
        return result;
    }
    
    private static Vector3D colorPixelMonte(Vector3D pixel){
		double tempR = 255 * Math.sqrt(pixel.getX()/samples);
        double tempG = 255 * Math.sqrt(pixel.getY()/samples);
        double tempB = 255 * Math.sqrt(pixel.getZ()/samples);
        double R = Math.round(tempR);
        double G = Math.round(tempG);
        double B = Math.round(tempB);
        if(R > 255){
            R = 255;
        }
        if(R < 0){
            R = 0;
        }
        if(G > 255){
            G = 255;
        }
        if(G < 0){
            G = 0;
        }
        if(B > 255){
            B = 255;
        }
        if(B < 0){
            B = 0;
        }
        Vector3D result = new Vector3D(R, G, B);
        return result;
    }
    
    private static Vector3D pairwiseProduct(Vector3D Vone, Vector3D Vtwo){
        return new Vector3D((Vone.getX() * Vtwo.getX()), (Vone.getY() * Vtwo.getY()), (Vone.getZ() * Vtwo.getZ()));
    }
}
