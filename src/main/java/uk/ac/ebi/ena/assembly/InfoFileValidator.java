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

package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.FileType;
import uk.ac.ebi.embl.api.validation.FlatFileOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.api.validation.ValidationScope;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.GenomeAssemblyValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.ena.submit.ContextE;

public class 
InfoFileValidator 
{
	private AssemblyInfoEntry assemblyInfoEntry = null;
	ValidationResult validationResult = new ValidationResult();
	
	
	public AssemblyInfoEntry
	getAssemblyEntry()
	{
	    return this.assemblyInfoEntry;
	}
	
	
    AssemblyInfoEntry 
    parseAssemblyEntry( File assemblyInfoFile, ValidationResult parseResult ) throws IOException
    {
        if( assemblyInfoFile == null )
            return null;
        
        AssemblyInfoReader reader = new AssemblyInfoReader( assemblyInfoFile );
        parseResult.append( reader.read() );
        if( reader.isEntry() )
            return ( AssemblyInfoEntry ) reader.getEntry();
        
        return null;
    }


    boolean
    read( File assembly_info, Map<Integer, Integer> line_number_list ) throws IOException
    {
        assemblyInfoEntry = parseAssemblyEntry( assembly_info, validationResult );
        if( null != line_number_list && !line_number_list.isEmpty() )
            translateLineNumbers( validationResult, line_number_list );
        
        return validationResult.isValid();
    }
   
    
    public ValidationResult
    getValidationResult()
    {
        return validationResult;
    }
    
    
    public boolean 
    validate( ContextE context ) throws IOException, ValidationEngineException 
    {
        if( assemblyInfoEntry != null )
        {
            EmblEntryValidationPlanProperty property = new EmblEntryValidationPlanProperty();
            property.isRemote.set( true );
            property.fileType.set( FileType.ASSEMBLYINFO );
            
            if( ContextE.transcriptome.equals( context ) )
                property.validationScope.set( ValidationScope.ASSEMBLY_TRANSCRIPTOME );

            ValidationPlan validationPlan = getValidationPlan( assemblyInfoEntry, property );
            ValidationPlanResult vpr = validationPlan.execute( assemblyInfoEntry );
            validationResult.append( vpr.getMessages() );
             
        }
        
        return validationResult.isValid(); 
    }
    
    
	private void 
	translateLineNumbers( ValidationResult assemblyInfoParseResult, Map<Integer, Integer> line_number_list )
    {
	    for( ValidationMessage<Origin> m : assemblyInfoParseResult.getMessages() )
	    {
	        List<Origin> tset = new ArrayList<>( m.getOrigins().size() );
	        for( Origin o :  m.getOrigins() )
	        {
	            if( o instanceof FlatFileOrigin )
	            {
	                FlatFileOrigin ffo = ( (FlatFileOrigin) o);
	                FlatFileOrigin nf = new FlatFileOrigin( ffo.getFileId(), line_number_list.get( ffo.getFirstLineNumber() ), line_number_list.get( ffo.getLastLineNumber() ) ); 
	                tset.add( nf );
	            } else
	            {
	                tset.add( o );
	            }
	        }
	        
	        m.getOrigins().clear();
	        m.getOrigins().addAll( tset );
	    }
    }


	public ValidationPlan getValidationPlan(Object entry,EmblEntryValidationPlanProperty property)
	{
		ValidationPlan validationPlan = new GenomeAssemblyValidationPlan(property);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_FIXER_BUNDLE);
		return validationPlan;
	}
}
