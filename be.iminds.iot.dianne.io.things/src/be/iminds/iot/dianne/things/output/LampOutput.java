package be.iminds.iot.dianne.things.output;

import java.awt.Color;
import java.util.UUID;

import be.iminds.iot.dianne.api.nn.module.ForwardListener;
import be.iminds.iot.dianne.api.nn.module.ModuleException;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;
import be.iminds.iot.things.api.lamp.Lamp;

public class LampOutput implements ForwardListener {

	private final Lamp lamp;
	private final TensorFactory factory;
	
	private int index = -1;
	
	public LampOutput(TensorFactory factory, Lamp l){
		this.lamp = l;
		this.factory = factory;
	}
	
	public LampOutput(TensorFactory factory, Lamp l, int index){
		this(factory, l);
		this.index = index;
	}
	
	@Override
	public void onForward(final UUID moduleId, final Tensor output, final String... tags) {
		if(output.size(0)==1){
			// on/off of lamp
			float v = output.get(0);
			if(v < 0.5f){
				// turn off
				lamp.off();
			}  else {
				lamp.on();
			}
		} else if(output.size(0)==2){
			// if argmax == 0 : n, if argmax == 1 : off
			int i = factory.getTensorMath().argmax(output);
			if(i==0){
				lamp.on();
			} else {
				lamp.off();
			}
		} else if(output.size(0)==3){
			// interpret as r,g,b colors
			lamp.setColor(new Color(output.get(0),output.get(1),output.get(2)));
			lamp.on();
		} else if(index >= 0 && index < output.size(0)){
			// set green if index is detected, red otherwise
			float val = output.get(index);
			Color c = new Color(1-val, val, 0);
			lamp.setColor(c);
			lamp.on();
		} else {
			// generate color hues based on size and argmax
			float stride = 1.0f/output.size(0);
			int i = factory.getTensorMath().argmax(output);
			Color c = getColor(i*stride);
			lamp.setColor(c);
			lamp.on();
		}
	}

	@Override
	public void onError(UUID moduleId, ModuleException e, String... tags) {
		// TODO set lamp color on error?
	}
	
	
	// generate rgb color from just hue (0..1) (set saturation and value to max)
	private static Color getColor(float hue){
		float value = 1.0f;
		float saturation = 1.0f;
		float c = value*saturation;
		
		float h = hue*6;   // when hue 0..360 then h = hue/60 thus ....
		float x = c*(1 - Math.abs(h % 2 -1));
		
		float r = 0;
		float g = 0;
		float b = 0;
		
		if(h < 1.0) {
			r = c;
			g = x;
		} else if(h < 2.0) {
			r = x;
			g = c;
		} else if(h < 3.0) {
			g = c;
			b = x;
		} else if(h < 4.0) {
			g = x;
			b = c;
		} else if(h < 5.0) {
			r = x;
			b = c;
		} else if(h <= 6.0) {
			r = c;
			b = x;
		}
	
		float min = value - c;
		 
		r += min;
		g += min;
		b += min;
		
		Color rgb = new Color(r, g, b);
		return rgb;
	}

}
