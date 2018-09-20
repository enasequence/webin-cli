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

package uk.ac.ebi.ena.rawreads.refs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import htsjdk.samtools.SAMSequenceRecord;

public class 
ENAReferenceSourceTest 
{
    @Test public void
    test() throws IOException
    {
        Path cache = Files.createTempDirectory( "tmp-ref-cache" );
        Path refdir = cache.resolve( "%2s/%2s/%s" );
        ENAReferenceSource rs = new ENAReferenceSource( refdir.toString().replaceAll( "\\\\+", "/" ) );
     
        long fetched_length = Stream.of( "fd9ca7dfdde04b03cbdb695877193918", 
                                         "03e7a85b8170cdbe9c069bd4ccd30b9a",
                                         "75cf94b19ae9b525eb9f3b4cb7f2c609",
                                         "0bdd9cff24d3747dd10598123096f572" )
                                    .collect( Collectors.summarizingLong( md5 -> { return getRef( rs, md5 ).length; } ) )
                                    .getSum(); 

        long stored_length = Files.walk( cache )
                                  .filter( e -> Files.isRegularFile( e ) )
                                  .collect( Collectors.summarizingLong( e -> e.toFile().length() ) )
                                  .getSum();
        
        Assert.assertEquals( fetched_length, stored_length );
    }

    
    private byte[] 
    getRef( ENAReferenceSource rs, String md5 )
    {
        SAMSequenceRecord record = new SAMSequenceRecord( "noname", -1 );
        record.setMd5( md5 );
        return rs.getReferenceBases( record, true );
    }
}
