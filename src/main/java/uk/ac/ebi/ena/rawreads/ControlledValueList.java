package uk.ac.ebi.ena.rawreads;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


public enum ControlledValueList
{
    Instrument( "uk/ac/ebi/ena/rawreads/instrument.properties", 38 ),
    Platform( "uk/ac/ebi/ena/rawreads/platform.properties", 5 ),
    Selection( "uk/ac/ebi/ena/rawreads/selection.properties", 31 ),
    Source( "uk/ac/ebi/ena/rawreads/source.properties", 9 ),
    Strategy( "uk/ac/ebi/ena/rawreads/strategy.properties", 36 );

    String     resource_path;
    int        record_cnt;
    Properties p = new Properties();


    ControlledValueList( String resource_path, int record_cnt )
    {
        this.resource_path = resource_path;
        this.record_cnt = record_cnt;
        try
        {
            this.p.load( ControlledValueList.class.getClassLoader().getResourceAsStream( resource_path ) );
            if( record_cnt != p.size() )
                throw new RuntimeException( "Expected list size mismatch" );
        } catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


    static String
    normalizeString( Object s )
    {
        return String.valueOf( s ).toLowerCase().replaceAll( "[ _-]+", "" );
    }


    public boolean
    contains( String key )
    {
        return p.keySet().stream().anyMatch( e -> normalizeString( e ).equals( normalizeString( key ) ) );
    }


    public String 
    getKey( String key )
    {
        return p.entrySet().stream()
                .map( e -> new SimpleEntry<String, String>( normalizeString( e.getKey() ), String.valueOf( e.getKey() ) ) )
                .filter( e -> e.getKey().equals( normalizeString( key ) ) ).findFirst()
                .orElse( new SimpleEntry<>( null, null ) ).getValue();
    }


    public String 
    getValue( String key )
    {
        return p.entrySet().stream()
                .map( e -> new SimpleEntry<String, String>( normalizeString( e.getKey() ), String.valueOf( e.getValue() ) ) )
                .filter( e -> e.getKey().equals( normalizeString( key ) ) ).findFirst()
                .orElse( new SimpleEntry<>( null, null ) ).getValue();
    }


    public List<String> 
    keyList()
    {
        return p.keySet().stream().map( String::valueOf ).collect( Collectors.toList() );
    }


    public String 
    toString()
    {
        return String.valueOf( p.entrySet() );
    }
}
