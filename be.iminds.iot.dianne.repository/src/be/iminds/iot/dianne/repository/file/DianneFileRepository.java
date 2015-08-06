package be.iminds.iot.dianne.repository.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.repository.DianneRepository;
import be.iminds.iot.dianne.nn.util.DianneJSONConverter;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

import com.google.gson.JsonParser;

@Component(immediate=true)
public class DianneFileRepository implements DianneRepository {

	private String dir = "nn";
	
	private final JsonParser parser = new JsonParser();
	
	private TensorFactory factory;
	
	@Activate
	public void activate(BundleContext context){
		String s = context.getProperty("be.iminds.iot.dianne.storage");
		if(s!=null){
			dir = s;
		}
		File d = new File(dir+"/weights/");
		d.mkdirs();
	}

	@Override
	public List<String> avialableNeuralNetworks() {
		List<String> networks = new ArrayList<String>();
		File d = new File(dir);
		for(File f : d.listFiles()){
			if(f.isDirectory()){
				String name = f.getName();
				if(!name.equals("weights")){
					networks.add(f.getName());
				}
			}
		}
		return networks;
	}

	@Override
	public NeuralNetworkDTO loadNeuralNetwork(String network) throws IOException {
		String nn = new String(Files.readAllBytes(Paths.get(dir+"/"+network+"/modules.txt")));
		return DianneJSONConverter.parseJSON(nn);
	}
	
	@Override
	public void storeNeuralNetwork(NeuralNetworkDTO nn){
		File d = new File(dir+"/"+nn.name);
		d.mkdirs();
		
		File n = new File(dir+"/"+nn.name+"/modules.txt");
		
		String output = DianneJSONConverter.toJsonString(nn, true);
		
		PrintWriter p = null;
		try {
			p = new PrintWriter(n);
			p.write(output);
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			if(p!=null){
				p.close();
			}
		}
	}

	@Override
	public String loadLayout(String network) throws IOException {
		String layout = new String(Files.readAllBytes(Paths.get(dir+"/"+network+"/layout.txt")));
		return layout;
	}
	
	@Override
	public void storeLayout(String network, String layout){
		File l = new File(dir+"/"+network+"/layout.txt");
		PrintWriter p = null;
		try {
			p = new PrintWriter(l);
			p.write(layout);
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			if(p!=null){
				p.close();
			}
		}		
	}
	
	@Override
	public Tensor loadParameters(UUID id, String[] tag) throws IOException {
		File d = new File(dir);
		File f = null;
		for(String l : d.list()){
			f = new File(dir+"/"+l+"/"+parametersId(id, tag));
			if(f.exists()){
				break;
			} else {
				f = null;
			}
		}
		if(f!=null){
			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
			int length = is.readInt();
			float[] data = new float[length];
			for(int i=0;i<length;i++){
				data[i] = is.readFloat();
			}
			is.close();
			return factory.createTensor(data, new int[]{length});
		}
		throw new FileNotFoundException();
	}

	@Override
	public void storeParameters(Tensor parameters, UUID id, String[] tag) {
		File f = new File(dir+"/weights/"+parametersId(id, tag));
		DataOutputStream os = null;
		try {
			os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			float[] data = parameters.get();
			os.writeInt(data.length);
			for(int i=0;i<data.length;i++){
				os.writeFloat(data[i]);
			}
			os.flush();
			os.close();
		} catch(IOException e){
			e.printStackTrace();
		} finally {
			if(os!=null){
				try {
					os.close();
				} catch (IOException e) {}
			}
		}
	}

	
	private String parametersId(UUID id, String[] tag){
		String pid = id.toString();
		if(tag!=null && tag.length>0){
			for(String t : tag){
				pid+="-"+t;
			}
		}
		return pid;
	}
	
	@Reference
	public void setTensorFactory(TensorFactory factory){
		this.factory = factory;
	}
}
