package be.iminds.iot.dianne.api.coordinator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Job {

	public final UUID id;
	public final String name;
	public final String type;
	public final String nn;
	public final String dataset;
	public final Map<String, String> config;
	
	public Job(UUID id, String name, String type, String nn, String d, Map<String, String> c){
		this.id = id;
		this.name = name;
		this.type = type;
		this.nn = nn;
		this.dataset = d;
		this.config = c;
	}
	
	public long submitted;
	public long started;
	public long stopped;
	public List<UUID> targets;
	
}
