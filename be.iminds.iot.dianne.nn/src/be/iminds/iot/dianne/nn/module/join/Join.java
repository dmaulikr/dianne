package be.iminds.iot.dianne.nn.module.join;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import be.iminds.iot.dianne.api.nn.module.AbstractModule;
import be.iminds.iot.dianne.api.nn.module.Module;
import be.iminds.iot.dianne.api.nn.module.Module.Mode;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public abstract class Join extends AbstractModule {

	protected Map<UUID, Tensor> inputs = new HashMap<UUID, Tensor>();
	protected Map<UUID, Tensor> gradInputs = new HashMap<UUID, Tensor>();
	
	// this will make sure that one will wait until all prev have given input before forwarding 
	// during training
	protected Map<UUID, AtomicBoolean> prevLock = new HashMap<UUID, AtomicBoolean>();
	
	// might need the order of id
	protected UUID[] prevIds;
	
	public Join(TensorFactory factory) {
		super(factory);
	}
	
	public Join(TensorFactory factory, UUID id) {
		super(factory, id);
	}
	
	protected void callPrevious(){
		// call all previous
		for(int i=0; i< next.length;i++){
			UUID id = prevIds[i];
			Module m = prev[i];
			
			executor.execute(new BackwardRunnable(m, gradInputs.get(id), tags));
		}
	}
	
	@Override
	public void forward(final UUID moduleId, final Tensor input, final String... tags) {
		synchronized(nextBusy){
			if(nextBusy.get()){
				// next is busy, either block or skip
				if(mode.contains(Mode.SKIP)){
					System.out.println("Module "+id+" skipped input");
					return;
				} else {
					// default mode BLOCKING
					try {
						nextBusy.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		this.inputs.put(moduleId, input);
		this.tags = tags;
		
		// when in wait-for-all mode, wait for input from each prev
		if(mode.contains(Mode.WAIT_FOR_ALL) && prev!=null && prev.length>1){
			synchronized(prevLock){
				prevLock.get(moduleId).set(true);
				for(AtomicBoolean b : prevLock.values()){
					if(!b.get()){
						return;
					}
				}
				for(AtomicBoolean b : prevLock.values()){
					b.set(false);
				}
			}
		} 
		
		forward();
		
		if(next!=null)
			callNext();
	
		if(fwdListeners.size()>0)
			notifyForwardListeners();
	}
	
	@Override
	public void setPrevious(final Module... prev) {
		if(prev==null){
			this.prev = null;
			this.prevIds = null;
		} else {
			this.prev = prev;
			this.prevIds = new UUID[prev.length];
			for(int i=0;i<prev.length;i++){
				// make sure that UUIDs are in keys
				// TODO better fix for this?
				UUID id = prev[i].getId();
				prevIds[i] = id;
				inputs.put(id, null);
				gradInputs.put(id, null);
				prevLock.put(id, new AtomicBoolean(false));
			}
		}
	}
}
