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

package uk.ac.ebi.ena.submit;

import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.SequenceAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.rawreads.RawReadsWebinCli;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;

public enum
ContextE
{
 sequence( "Sequence assembly: %s",
           "SEQUENCE_FLATFILE",
           SequenceAssemblyWebinCli.class ),

 transcriptome( "Transcriptome assembly: %s",
                "TRANSCRIPTOME_ASSEMBLY",
                TranscriptomeAssemblyWebinCli.class ),

 genome( "Genome assembly: %s",
         "SEQUENCE_ASSEMBLY",
         GenomeAssemblyWebinCli.class ),
 
 reads( "Raw reads: %s",
        "RUN",
        RawReadsWebinCli.class );

    private String                            title;
    private String                            type;
    private Class<? extends AbstractWebinCli> klass;


    private ContextE( String title, 
                      String type,
                      Class<? extends AbstractWebinCli> klass )
    {
        this.title = title;
        this.type = type;
        this.klass = klass;
    }


    public Class<? extends AbstractWebinCli>
    getValidatorClass()
    {
        return this.klass;
    }


    public String
    getTitle( String name )
    {
        return String.format( this.title, name );
    }


    public String
    getType()
    {
        return type;
    }


    public static ContextE
    getContext( String context )
    {
        try
        {
            return ContextE.valueOf( context );
        
        } catch( Exception e )
        {
            return null;
        }
    }
}
