package pt.ma.metadata;

import java.io.Serializable;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * 
 *
 */
public class MetaTerm implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3798140470512773063L;

	/**
	 * 
	 */
	private String id;
	
	/**
	 * 
	 */
	private UUID uniqueId;
	
	/**
	 * 
	 */
	private String name;
	 
	/**
	 * 
	 * @param id
	 * @param name
	 */
	public MetaTerm(String id, String name) {
		super();
		//
		this.id = id;
		this.name = name;
		this.uniqueId = UUID.randomUUID();
		
	}
	
	// PUBLIC  METHODS

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetaTerm other = (MetaTerm) obj;
		if (uniqueId == null) {
			if (other.uniqueId != null)
				return false;
		} else if (!uniqueId.equals(other.uniqueId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		/*
		StringBuilder result = new StringBuilder();
		result.append("--[");
		result.append("ID: " + this.id);
		result.append(", UniqueURI: " + this.uniqueId);
		result.append(", Name: " + this.name);		
		result.append("]--");
		return result.toString();
		*/
		
		//
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String result = gson.toJson(this);
		return result;

	}

}
