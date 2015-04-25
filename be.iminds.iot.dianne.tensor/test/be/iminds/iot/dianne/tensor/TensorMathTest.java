package be.iminds.iot.dianne.tensor;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import be.iminds.iot.dianne.tensor.impl.java.JavaTensorFactory;
import be.iminds.iot.dianne.tensor.impl.nd4j.ND4JTensorFactory;

@RunWith(Parameterized.class)
public class TensorMathTest<T extends Tensor<T>> {

	private TensorFactory<T> factory;
	private TensorMath<T> math;

	public TensorMathTest(TensorFactory<T> f, String name) {
		this.factory = f;
	}

	@Parameters(name="{1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { 
				{ new JavaTensorFactory(), "Java Tensor" },
				{ new ND4JTensorFactory(), "ND4J Tensor" } 
		});
	}
	
    @Before
    public void setUp() {
        math = factory.getTensorMath();
    }

	@Test
	public void testAdd1() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		
		T r = math.add(null, t1, 3);
		
		T exp = factory.createTensor(2,2);
		exp.fill(5);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testAdd2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = math.add(null, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(5);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testAdd3() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = math.add(null, t1, 2, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(8);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testAdd4() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.add(r, t1, 2, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(8);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testSub1() {
		T t1 = factory.createTensor(2,2);
		t1.fill(3);
		
		T r = math.sub(null, t1, 1);
		
		T exp = factory.createTensor(2,2);
		exp.fill(2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testSub2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(3);
		T t2 = factory.createTensor(4);
		t2.fill(1);
		
		T r = math.sub(null, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(2);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testSub3() {
		T t1 = factory.createTensor(2,2);
		t1.fill(3);
		T t2 = factory.createTensor(4);
		t2.fill(1);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.sub(r, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMul() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		
		T r = math.mul(null, t1, 2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(4);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testMul2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.mul(r, t1, 2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(4);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testCMul() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = math.cmul(null, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(6);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testCMul2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.cmul(r, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(6);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testDiv() {
		T t1 = factory.createTensor(2,2);
		t1.fill(6);
		
		T r = math.div(null, t1, 2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(3);
		
		Assert.assertEquals(exp, r);
	}

	@Test
	public void testDiv2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(6);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.div(r, t1, 2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(3);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testCDiv() {
		T t1 = factory.createTensor(2,2);
		t1.fill(6);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = math.cdiv(null, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testCDiv2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(6);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.cdiv(r, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testDot() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(4);
		t2.fill(3);
		
		float dot = math.dot(t1, t2);
		
		Assert.assertEquals(24f, dot, 0.01);
	}
	
	@Test
	public void testMv1() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(2);
		t2.fill(3);
		
		T r = math.mv(null, t1, t2);
		
		T exp = factory.createTensor(2);
		exp.fill(12);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMv2() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(2);
		t2.fill(3);
		
		T r = factory.createTensor(2);
		r.fill(1.0f);
		math.mv(r, t1, t2);
		
		T exp = factory.createTensor(2);
		exp.fill(12);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMm1() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(2,2);
		t2.fill(3);
		
		T r = math.mm(null, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(12);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMm2() {
		T t1 = factory.createTensor(3,2);
		int k = 1; 
		for(int i=0;i<3;i++){
			for(int j=0;j<2;j++){
				t1.set(k, i, j);
				k++;
			}
		}
		
		T t2 = factory.createTensor(2,3);
		k = 1; 
		for(int i=0;i<2;i++){
			for(int j=0;j<3;j++){
				t2.set(k, i, j);
				k++;
			}
		}

		T r = math.mm(null, t1, t2);
		
		T exp = factory.createTensor(3,3);
		exp.set(9, 0, 0);
		exp.set(12, 0, 1);
		exp.set(15, 0, 2);
		exp.set(19, 1, 0);
		exp.set(26, 1, 1);
		exp.set(33, 1, 2);
		exp.set(29, 2, 0);
		exp.set(40, 2, 1);
		exp.set(51, 2, 2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMm3() {
		T t1 = factory.createTensor(2,2);
		t1.fill(2);
		T t2 = factory.createTensor(2,2);
		t2.fill(3);
		
		T r = factory.createTensor(2,2);
		r.fill(1.0f);
		math.mm(r, t1, t2);
		
		T exp = factory.createTensor(2,2);
		exp.fill(12);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testSum() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		
		Assert.assertEquals(8.0, math.sum(t1), 0.1f);
	}
	
	@Test
	public void testMax() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		t1.set(0, 0);
		t1.set(5, 3);
		
		Assert.assertEquals(5.0, math.max(t1), 0.1f);
	}
	
	@Test
	public void testMin() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		t1.set(0, 0);
		t1.set(5, 3);
		
		Assert.assertEquals(0.0, math.min(t1), 0.1f);
	}
	
	@Test
	public void testMean() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		t1.set(0, 0);
		t1.set(5, 3);
		
		Assert.assertEquals(2.25, math.mean(t1), 0.1f);
	}
	
	@Test
	public void testArgMax() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		t1.set(0, 0);
		t1.set(5, 3);
		
		Assert.assertEquals(3, math.argmax(t1));
	}
	
	@Test
	public void testArgMin() {
		T t1 = factory.createTensor(4);
		t1.fill(2);
		t1.set(0, 0);
		t1.set(5, 3);
		Assert.assertEquals(0, math.argmin(t1));
	}
	
	@Test
	public void testConvolution(){
		T t1 = factory.createTensor(5,5);
		T t2 = factory.createTensor(3,3);
		T exp = factory.createTensor(3,3);
		
		t1.fill(1.0f);
		t1.set(2.0f, 0, 0);
		t1.set(2.0f, 4, 4);
		t2.fill(2.0f);
		exp.fill(18.0f);
		exp.set(20.0f, 0, 0);
		exp.set(20.0f, 2, 2);
		
		Assert.assertEquals(exp, math.convolution2D(null, t1, t2, 1, 1, 0, false));
	}
	
	@Test
	public void testConvolution2(){
		T t1 = factory.createTensor(5,5);
		T t2 = factory.createTensor(3,3);
		T exp = factory.createTensor(3,3);
		
		t1.fill(1.0f);
		t1.set(2.0f, 0, 0);
		t1.set(2.0f, 4, 4);
		t2.fill(2.0f);
		exp.fill(18.0f);
		exp.set(20.0f, 0, 0);
		exp.set(20.0f, 2, 2);
		
		T r = factory.createTensor(3,3);
		r.fill(1.0f);
		math.convolution2D(r, t1, t2, 1, 1, 0, false);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testConvolutionFull(){
		T t1 = factory.createTensor(5,5);
		T t2 = factory.createTensor(3,3);
		t1.fill(1.0f);
		t1.set(2.0f, 0, 0);
		t1.set(2.0f, 4, 4);
		t2.fill(2.0f);
		
		float[] data = new float[]{
			4.0f, 6.0f,  8.0f,  6.0f,  6.0f,  4.0f,  2.0f,
			6.0f, 10.0f, 14.0f, 12.0f, 12.0f, 8.0f,  4.0f,
			8.0f, 14.0f, 20.0f, 18.0f, 18.0f, 12.0f, 6.0f,
			6.0f, 12.0f, 18.0f, 18.0f, 18.0f, 12.0f, 6.0f,
			6.0f, 12.0f, 18.0f, 18.0f, 20.0f, 14.0f, 8.0f,
			4.0f, 8.0f,  12.0f, 12.0f, 14.0f, 10.0f, 6.0f,
			2.0f, 4.0f,  6.0f,  6.0f,  8.0f,  6.0f,  4.0f
		};
		T exp = factory.createTensor(data, 7,7);
		
		Assert.assertEquals(exp, math.convolution2D(null, t1, t2, 1, 1, 1, false));
	}
	
	@Test
	public void testConvolutionSame(){
		T t1 = factory.createTensor(5,5);
		T t2 = factory.createTensor(3,3);
		t1.fill(1.0f);
		t1.set(2.0f, 0, 0);
		t1.set(2.0f, 4, 4);
		t2.fill(2.0f);
		
		float[] data = new float[]{
				10.0f, 14.0f, 12.0f, 12.0f, 8.0f,
				14.0f, 20.0f, 18.0f, 18.0f, 12.0f,
				12.0f, 18.0f, 18.0f, 18.0f, 12.0f,
				12.0f, 18.0f, 18.0f, 20.0f, 14.0f,
				8.0f,  12.0f, 12.0f, 14.0f, 10.0f,
		};
		T exp = factory.createTensor(data, 5, 5);
	
		Assert.assertEquals(exp, math.convolution2D(null, t1, t2, 1, 1, 2, false));
	}
	
	@Test
	public void testMaxpool(){
		T t1 = factory.createTensor(4,6);
		t1.fill(0.0f);
		t1.set(1.0f, 0, 0);
		t1.set(1.0f, 0, 3);
		t1.set(2.0f, 2, 0);
		t1.set(4.0f, 0, 5);
		
		T exp = factory.createTensor(2,3);
		exp.set(1.0f, 0, 0);
		exp.set(1.0f, 0, 1);
		exp.set(2.0f, 1, 0);
		exp.set(0.0f, 1, 1);
		exp.set(4.0f, 0, 2);
		
		Assert.assertEquals(exp, math.maxpool2D(null, t1, 2, 2, 2, 2));
	}

	@Test
	public void testMaxpool2(){
		T t1 = factory.createTensor(4,6);
		t1.fill(0.0f);
		t1.set(1.0f, 0, 0);
		t1.set(1.0f, 0, 3);
		t1.set(2.0f, 2, 0);
		t1.set(4.0f, 0, 5);
		
		T exp = factory.createTensor(2,3);
		exp.set(1.0f, 0, 0);
		exp.set(1.0f, 0, 1);
		exp.set(2.0f, 1, 0);
		exp.set(0.0f, 1, 1);
		exp.set(4.0f, 0, 2);
		
		T r = factory.createTensor(2,3);
		r.fill(1.0f);
		math.maxpool2D(r, t1, 2, 2, 2, 2);
		
		Assert.assertEquals(exp, r);
	}
	
	@Test
	public void testMaxpoolStride(){
		T t1 = factory.createTensor(4,6);
		t1.fill(0.0f);
		t1.set(1.0f, 0, 0);
		t1.set(1.0f, 0, 3);
		t1.set(2.0f, 2, 0);
		t1.set(4.0f, 0, 5);
		
		T exp = factory.createTensor(3,5);
		exp.set(1.0f, 0, 0);
		exp.set(1.0f, 0, 2);
		exp.set(1.0f, 0, 3);
		exp.set(4.0f, 0, 4);
		exp.set(2.0f, 1, 0);
		exp.set(2.0f, 2, 0);

		Assert.assertEquals(exp, math.maxpool2D(null, t1, 2, 2, 1, 1));
	}
	
	
	@Test
	public void testDMaxpool(){
		T t1 = factory.createTensor(4,6);
		t1.fill(0.0f);
		t1.set(1.0f, 0, 0);
		t1.set(1.0f, 0, 3);
		t1.set(2.0f, 2, 0);
		t1.set(4.0f, 0, 5);
		
		T grad = factory.createTensor(2,3);
		grad.set(1.0f, 0, 0);
		grad.set(1.0f, 0, 1);
		grad.set(1.0f, 1, 0);
		grad.set(0.0f, 1, 1);
		grad.set(1.0f, 0, 2);
		
		T exp = factory.createTensor(4,6);
		exp.fill(0.0f);
		exp.set(1.0f, 0, 0);
		exp.set(1.0f, 0, 3);
		exp.set(1.0f, 2, 0);
		exp.set(1.0f, 0, 5);
	
		Assert.assertEquals(exp, math.dmaxpool2D(null, grad, t1, 2, 2, 2 ,2));
	}
}
