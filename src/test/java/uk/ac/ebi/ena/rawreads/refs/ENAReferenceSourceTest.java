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
        
        SAMSequenceRecord record = new SAMSequenceRecord( "noname", -1 );
        record.setMd5( "1b22b98cdeb4a9304cb5d48026a85128" );
        byte[] ref = rs.getReferenceBases( record, true );

        SAMSequenceRecord record1 = new SAMSequenceRecord( "noname", -1 );
        record1.setMd5( "0740173db9ffd264d728f32784845cd7" );
        byte[] ref1 = rs.getReferenceBases( record1, true );
        
        long stored_length = Files.walk( cache ).filter( e -> Files.isRegularFile( e ) ).collect( Collectors.summarizingLong( e -> e.toFile().length() ) ).getSum();
        Assert.assertEquals( ref.length + ref1.length, stored_length );
    }

}
