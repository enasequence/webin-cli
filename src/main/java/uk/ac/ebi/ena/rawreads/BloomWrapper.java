package uk.ac.ebi.ena.rawreads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import oracle.net.aso.d;

public class 
BloomWrapper 
{
    //Bloom bloom;
    private BloomFilter<String> bloom;
    private final double edup = 0.01;
    private final long expected_reads; 
    private final AtomicLong adds_no = new AtomicLong();
    private final AtomicLong susp_no = new AtomicLong();
    private final int collect_max; 
    private final Set<String> suspected;
    
    
    public 
    BloomWrapper( long expected_reads )
    {
        this( expected_reads, 100_000 );
    }
    
    
     
    BloomWrapper( long expected_reads, int collect_max )
    {
        this.expected_reads = expected_reads;
        this.collect_max = collect_max;
        this.bloom = BloomFilter.create( Funnels.unencodedCharsFunnel(), expected_reads, edup );
        suspected = new HashSet<>( this.collect_max );
    }


    public void
    add( String read_name )
    {
        adds_no.incrementAndGet();
        
        if( bloom.mightContain( read_name ) )
        {
            susp_no.incrementAndGet();
            suspected.add( read_name );
            if( suspected.size() > collect_max )
                throw new RuntimeException( String.format( "Current BLOOM filter capacity %d cannot accomodate more than %d positive/false positive records. Please increase BLOOM filter capacity", expected_reads, collect_max ) );
        } else
        {
            bloom.put( read_name );
        }
    }
    
    
    public long
    getAddsNumber()
    {
        return adds_no.get();
    }
    
    
    public boolean
    hasPossibleDuplicates()
    {
        return getPossibleDuplicateCount() > 0;
    }
    
    
    public Long
    getPossibleDuplicateCount()
    {
        return susp_no.get();
    }

    
    public Set<String>
    getSuspected()
    {
        return suspected;
    }
    
    
    public Map<String, Set<String>>
    findAllduplications( String[] read_names, int limit )
    {
        Map<String, Integer>      counts = new HashMap<>( limit );
        Map<String, Set<String>>  result = new HashMap<>( limit );
        
        if( !hasPossibleDuplicates() )
            return result;
        
        AtomicInteger index = new AtomicInteger();
        for( String read_name : read_names )
        {
            index.incrementAndGet();
            if( contains( read_name ) )
            {
                counts.put( read_name, counts.getOrDefault( read_name, 0 ) + 1 );
                Set<String> dlist = result.getOrDefault( read_name, new LinkedHashSet<>() ); //findDuplicateLocations( read_name );
                dlist.add( String.format( "%s", index.get() ) );
                result.put( read_name, dlist );
            }
            //TODO
            if( result.size() >= limit )
                break;
        }
        //return result;
        return result.entrySet().stream().filter( e-> counts.get( e.getKey() ).intValue() > 1 ).collect( Collectors.toMap( e -> e.getKey(), e -> e.getValue() ) );
    }
    
   
    public boolean 
    contains( String read_name )
    {
        return bloom.mightContain( read_name );
    }
}
