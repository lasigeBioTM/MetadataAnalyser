package pt.ma.parse;

/**
 * 
 * 
 *
 */
public class MetaClass {

	/**
	 * 
	 */
	private String classID;
	
	/**
	 * 
	 */
	private String className;
	
	/**
	 * 
	 */
	private double specValue;
	
	/**
	 * 
	 */
	private double covValue;

	/**
	 * 
	 * @param classID
	 * @param className
	 */
	public MetaClass(
			String classID, 
			String className) {
		super();
		
		//
		this.classID = classID;
		this.className = className;
		
		//
		this.specValue = -1f;
		this.covValue = -1f;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isEvaluated() {
		boolean result = false;
		if (specValue >= 0 && covValue >= 0) {
			result = true;
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	public double getSpecValue() {
		return specValue;
	}

	/**
	 * 
	 * @param specValue
	 */
	public void setSpecValue(double specValue) {
		this.specValue = specValue;
	}

	/**
	 * 
	 * @return
	 */
	public double getCovValue() {
		return covValue;
	}

	/**
	 * 
	 * @param covValue
	 */
	public void setCovValue(double covValue) {
		this.covValue = covValue;
	}

	/**
	 * 
	 * @return
	 */
	public String getClassID() {
		return classID;
	}

	/**
	 * 
	 * @return
	 */
	public String getClassName() {
		return className;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classID == null) ? 0 : classID.hashCode());
		result = prime * result + ((className == null) ? 0 : className.hashCode());
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
		MetaClass other = (MetaClass) obj;
		if (classID == null) {
			if (other.classID != null)
				return false;
		} else if (!classID.equals(other.classID))
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}
}
