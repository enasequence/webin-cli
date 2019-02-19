package uk.ac.ebi.ena.service;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class 
LatestReleaseService extends AbstractService
{
    private static final Logger log = LoggerFactory.getLogger( LatestReleaseService.class );
    
    public static class 
    GitHubReleaseInfo
    {
        public String tag_name;
        public String name;
        public Boolean draft;
        public Boolean prerelease;
        public Timestamp created_at;
        public Timestamp published_at;
        public List<GitHubReleaseAsset> assets;
        public String body;
    }


    public static class 
    GitHubReleaseAsset
    {
        public String name;
        public String state;
        public Long   size;
        public Long   download_count;
        public Timestamp created_at; 
        public Timestamp updated_at;
        public String browser_download_url;
    }
    

    public static class 
    Builder extends AbstractBuilder<LatestReleaseService>
    {
        @Override public LatestReleaseService
        build()
        {
            return new LatestReleaseService( this );
        }
    }
        
    
    protected
    LatestReleaseService( Builder builder )
    {
        super( builder );
    }
    
    
    public static class 
    NotFoundErrorHandler implements ResponseErrorHandler 
    {
        String error_msg;
        
        public 
        NotFoundErrorHandler( String systemError )
        {
            this.error_msg = systemError;
        }

        
        @Override public boolean 
        hasError( ClientHttpResponse httpResponse ) throws IOException 
        {
            return ( httpResponse.getStatusCode().series() == CLIENT_ERROR
                     || httpResponse.getStatusCode().series() == SERVER_ERROR );
        }

        
        @Override public void 
        handleError( ClientHttpResponse httpResponse ) throws IOException
        {
            switch( httpResponse.getStatusCode() )
            {
                case UNAUTHORIZED:
                case FORBIDDEN:
                    throw WebinCliException.createUserError( WebinCli.AUTHENTICATION_ERROR );
                case NOT_FOUND:
                default:
                    throw WebinCliException.createSystemError( error_msg );
            }
        }
    }
    
    
    public GitHubReleaseInfo 
    getLatestInfo() 
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler( new NotFoundErrorHandler( "Unable to fetch latest version" ) );

        ResponseEntity<GitHubReleaseInfo> response = restTemplate.exchange( "https://api.github.com/repos/enasequence/webin-cli/releases/latest",
                                                                            HttpMethod.GET,
                                                                            new HttpEntity<>( (new HttpHeaderBuilder() ).build() ),
                                                                            GitHubReleaseInfo.class );
        return response.getBody();
    }
}
