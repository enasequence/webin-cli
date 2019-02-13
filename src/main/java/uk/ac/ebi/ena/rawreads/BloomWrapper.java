/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

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

public class 
BloomWrapper 
{
    //Bloom bloom;
    private final BloomFilter<String> bloom;
    private final double edup = 0.01;
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
            if( suspected.size() >= collect_max )
            {
                //throw new RuntimeException( String.format( "Current BLOOM filter capacity %d cannot accomodate more than %d positive/false positive records. Please increase BLOOM filter capacity", expected_reads, collect_max ) );
            } else
                suspected.add( read_name );
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
        return result.entrySet().stream().filter( e-> counts.get(e.getKey()) > 1 ).collect( Collectors.toMap(e -> e.getKey(), e -> e.getValue() ) );
    }
    
   
    public boolean 
    contains( String read_name )
    {
        return bloom.mightContain( read_name );
    }
}
