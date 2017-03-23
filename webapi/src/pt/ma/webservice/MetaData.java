package pt.ma.webservice;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

@ApplicationPath("app")
public class MetaData extends Application {
	
	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> resources = new HashSet<Class<?>>();
		// add the resources
		resources.add(MetaRequest.class);
		// Add additional features such as support for Multipart.
        resources.add(MultiPartFeature.class);
		return resources;
		
	}

}
