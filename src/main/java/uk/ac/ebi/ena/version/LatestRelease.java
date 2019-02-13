package uk.ac.ebi.ena.version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;


public class 
LatestRelease 
{
    private static final Logger log = Logger.getLogger( LatestRelease.class );
    
    static class 
    GitHubReleaseInfo
    {
        String tag_name;
        String name;
        Boolean draft;
        Boolean prerelease;
        Timestamp created_at;
        Timestamp published_at;
        List<GitHubReleaseAsset> assets;
        String body;
    }


    static class 
    GitHubReleaseAsset
    {
        String name;
        String state;
        Long   size;
        Long   download_count;
        Timestamp created_at; 
        Timestamp updated_at;
        String browser_download_url;
    }
    

    private final ScriptEngine engine;
    
    
    public
    LatestRelease()
    {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName( "javascript" );
    }
    
    
    private String
    fetchData( URL url ) throws IOException
    {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        StringBuilder sb = new StringBuilder();
        try( BufferedReader br = new BufferedReader( new InputStreamReader( con.getInputStream() ) ) )
        {
            String input;
            while( ( input = br.readLine() ) != null )
                sb.append( input );
        }
        return sb.toString();
    }
    
    
    private Timestamp
    parseTimestamp( Map<String, Object> map, String field_name )
    {
        try
        {
            return Timestamp.from( Instant.parse( (String)map.get( field_name ) ) );
        } catch( IllegalArgumentException iae )
        {
            log.info( iae );
        }
        return null;
    }
    
    
    private Long
    parseLong( Map<String, Object> map, String field_name )
    {
        try
        {
            return ( (Number)map.get( field_name ) ).longValue();
        } catch( NullPointerException npe )
        {
            log.info( npe );
        }
        return null;
    }

    
    private Boolean
    parseBoolean( Map<String, Object> map, String field_name )
    {
        try
        {
            return (Boolean) map.get(field_name);
        } catch( NullPointerException npe )
        {
            log.info( npe );
        }
        return null;
    }
    
    
    private List<GitHubReleaseAsset>
    parseAssets( List<Map<String, Object>> assets )
    {
        //[url, id, node_id, name, label, uploader, content_type, state, size, download_count, created_at, updated_at, browser_download_url]
        List<GitHubReleaseAsset> result_list = new ArrayList<>();
        for( Map<String, Object> asset : assets )
        {
            GitHubReleaseAsset result = new GitHubReleaseAsset();
            result.name = String.valueOf( asset.get( "name" ) );
            result.state = String.valueOf( asset.get( "state" ) );
            result.browser_download_url = String.valueOf( asset.get( "browser_download_url" ) );
            result.size = parseLong( asset, "size" );
            result.download_count = parseLong( asset, "download_count" );
            result.created_at = parseTimestamp( asset, "created_at" );
            result.updated_at = parseTimestamp( asset, "updated_at" );

            result_list.add( result );
        }
        return result_list;
    }
    
    
    @SuppressWarnings( "unchecked" )
    private GitHubReleaseInfo
    parseGitHubJson(String json) throws ScriptException
    {
        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = this.engine.eval( script );
        Map<String, Object> content = (Map<String, Object>) result;
        
//        [url, assets_url, upload_url, html_url, id, node_id, tag_name, target_commitish, name, draft, author, prerelease, created_at, published_at, assets, tarball_url, zipball_url, body]
        GitHubReleaseInfo info = new GitHubReleaseInfo();
        
        info.tag_name = (String)content.get( "tag_name" );
        info.name = (String)content.get( "name" );
        info.body = (String)content.get( "body" );
        info.draft = parseBoolean( content, "draft" );
        info.prerelease = parseBoolean( content, "prerelease" );
        info.created_at = parseTimestamp( content, "created_at" );
        info.published_at = parseTimestamp( content, "published_at" );
        List<Map<String,Object>> assets = (List<Map<String, Object>>)content.get( "assets" );
        info.assets = parseAssets( assets );
        return info;
    }


    
    public GitHubReleaseInfo
    getLatestInfo() throws IOException, ScriptException {
        String s = fetchData( new URL( "https://api.github.com/repos/enasequence/webin-cli/releases/latest" ) );
        return parseGitHubJson( s );
    }
}
