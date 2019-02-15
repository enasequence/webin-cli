package uk.ac.ebi.ena.service;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class 
AbstractService
{
    static {
        try {
            serviceMessages = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource("/ServiceMessages.properties"));
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private final static String webinRestUriTest = "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
    private final static String webinRestUriProd = "https://www.ebi.ac.uk/ena/submit/drop-box/";
    private final static Properties serviceMessages;
    private final String userName;
    private final String password;
    private final boolean test;

    final String getServiceMessage(String messageKey) {
        return serviceMessages.getProperty(messageKey);
    }

    final String getWebinRestUri(String uri, boolean test) {
        return (test) ?
                webinRestUriTest + uri :
                webinRestUriProd + uri;
    }

    
    public abstract static class
    AbstractBuilder<T>
    {
        protected String userName;
        protected String password; 
        protected boolean test;
        
        
        public AbstractBuilder<T>
        setUserName( String userName )
        {
            this.userName = userName;
            return this;
        }
        
        
        public AbstractBuilder<T>
        setPassword( String password )
        {
            this.password = password;
            return this;
        }

        
        public AbstractBuilder<T>
        setCredentials( String userName, String password )
        {
            setUserName( userName );
            setPassword( password );
            return this;
        }
        
        
        public AbstractBuilder<T>
        setTest( boolean test )
        {
            this.test = test;
            return this;
        }
        

        public abstract T build();
    }

    
    
    public String
    getUserName()
    {
        return this.userName;
    }

    
    protected 
    AbstractService( AbstractBuilder<?> builder )
    {
        this.userName = builder.userName;
        this.password = builder.password;
        this.test     = builder.test;
    }
    
    
    public String
    getPassword()
    {
        return this.password;
    }

    
    
    public boolean
    getTest()
    {
        return this.test;
    }
}
