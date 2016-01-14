package pt.ma.log;

import java.io.Closeable;


public final class IOHelper {

	public static void close(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception ex) {
			//ignore error
		}
	
	}
}
