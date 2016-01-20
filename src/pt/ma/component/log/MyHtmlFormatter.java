package pt.ma.component.log;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Formatador das linhas dos logs em HTML
 * 
 */
public class MyHtmlFormatter extends Formatter {
    
    @Override
    public String format(LogRecord rec) {
        StringBuffer buf = new StringBuffer(1000);
        //Bold
        if(rec.getLevel().intValue() >= Level.WARNING.intValue()) {
            buf.append("<b>");
            buf.append(rec.getLevel());
            buf.append("</b>");
        } else {
            buf.append(rec.getLevel());
        }
        
        buf.append(" ").append(rec.getMillis()).append(" ").append(formatMessage(rec)).append("\n");
        return buf.toString();  
    }
    
    /**
     * Este metodo eh chamado sempre antes do handler que usa este formatador eh
     * criado
     */
    @Override
    public String getHead(Handler hand) {
        return "<HTML><HEAD>" + Utilities.dameDataHora() + "</HEAD><BODY><PRE><br>";
    }
    
     /**
     * Este metodo eh chamado sempre apos o handler que usa este formatador eh
     * fechado
     */
    @Override
    public String getTail(Handler hand) {
        return "</PRE></BODY>"+ Utilities.dameDataHora() +"</HTML><br>";
    }
   
}
