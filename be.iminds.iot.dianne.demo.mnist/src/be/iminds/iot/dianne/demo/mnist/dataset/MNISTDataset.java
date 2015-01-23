package be.iminds.iot.dianne.demo.mnist.dataset;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import be.iminds.iot.dianne.nn.train.Dataset;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class MNISTDataset implements Dataset{

	public static enum Set {TRAIN, TEST};

	private final TensorFactory factory;
	
	private Tensor data;
	private int inputSize;
	private int outputSize;
	private int noSamples;
	
	// lazy read - only read when index required
	private int read = 0;
	private InputStream imageInput;
	private InputStream labelInput;
	
	private String dir = "";
	private String images;
	private String labels;
	
	
	public MNISTDataset(TensorFactory factory, String dir, Set set, boolean readOnInit){
		this(factory, dir, set);
		
		if(readOnInit){
			read(noSamples);
		}
	}
	
	public MNISTDataset(TensorFactory factory, String dir, Set set) {
		this.factory = factory;
		this.dir = dir;
		if(set == Set.TRAIN){
			images = "train-images.idx3-ubyte";
			labels = "train-labels.idx1-ubyte";
		} else if(set == Set.TEST){
			images = "t10k-images.idx3-ubyte";
			labels = "t10k-labels.idx1-ubyte";			
		}
		init();
	}
	
	public void init(){
		try {
			imageInput = new FileInputStream(dir+images);

			int magic = readInt(imageInput);
			assert magic == 2051;
			int noImages = readInt(imageInput);
			int noRows = readInt(imageInput);
			int noColumns = readInt(imageInput);
			
			labelInput = new FileInputStream(dir+labels);
			magic = readInt(labelInput);
			assert magic == 2049;
			int noLabels = readInt(labelInput);

			System.out.println("Reading MNIST dataset");
			System.out.println("#Images: "+noImages+" #Rows: "+noRows+" #Columns: "+noColumns+" #Labels: "+noLabels);

			assert noLabels == noImages;
			
			noSamples = noImages;
			inputSize = noRows*noColumns;
			outputSize = 10;
			
			int sampleSize = inputSize+outputSize;

			data = factory.createTensor(noSamples, sampleSize);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@Override
	public int size() {
		return noSamples;
	}

	@Override
	public int inputSize() {
		return inputSize;
	}

	@Override
	public int outputSize() {
		return outputSize;
	}

	@Override
	public Tensor getInputSample(int index) {
		read(index);
		return data.narrow(index, 1, 0, inputSize);
	}

	@Override
	public Tensor getInputBatch(int startIndex, int size) {
		read(startIndex+size);
		return data.narrow(startIndex, size, 0, inputSize);
	}

	@Override
	public Tensor getOutputSample(int index) {
		read(index);
		return data.narrow(index, 1, inputSize, 10);
	}

	@Override
	public Tensor getOutputBatch(int startIndex, int size) {
		read(startIndex+size);
		return data.narrow(startIndex, size, inputSize, 10);
	}

	private int readInt(InputStream is) throws IOException{
		byte[] b = new byte[4];
		is.read(b, 0, 4);
		int i = ((0xFF & b[0]) << 24) | ((0xFF & b[1]) << 16) |
	            ((0xFF & b[2]) << 8) | (0xFF & b[3]);
		return i;
	}
	
	private int readUByte(InputStream is) throws IOException{
		byte[] b = new byte[1];
		is.read(b, 0, 1);
		int i = (0xFF & b[0]);
		return i;
	}
	
	private void read(int index) {
		if(read <= index){
			try {
				for(;read<=index && read<noSamples;read++){
					for(int j=0;j<inputSize;j++){
						data.set((float)readUByte(imageInput)/255f, read,j);
					}
					int output = readUByte(labelInput);
					data.set(1.0f, read, inputSize+output);
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}
