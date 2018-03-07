package uk.ac.ebi.ena.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import uk.ac.ebi.embl.agp.reader.AGPFileReader;
import uk.ac.ebi.embl.agp.reader.AGPLineReader;
import uk.ac.ebi.embl.api.entry.genomeassembly.ChromosomeEntry;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.entry.qualifier.QualifierFactory;
import uk.ac.ebi.embl.api.validation.SequenceEntryUtils;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.EntryReader;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestObj;

public class GenomeAssemblyFileUtils
{
	public static FlatFileReader<?> getFileReader(FileFormat format,File file,BufferedReader reader) throws IOException
	{
		 if(!EntryReader.getBlockCounter().isEmpty())
             EntryReader.getBlockCounter().clear();
          if(!EntryReader.getSkipTagCounter().isEmpty())
              EntryReader.getSkipTagCounter().clear();
		FlatFileReader<?> flatfileReader=null;
		switch(format)
	 	{
	 	case FASTA:
	 		flatfileReader = new FastaFileReader(new FastaLineReader(reader));
	 		break;
	 	case AGP:
	 		flatfileReader = new AGPFileReader(new AGPLineReader(reader));
            break;
	 	case FLATFILE:
	 		EmblEntryReader emblReader = new EmblEntryReader(reader,EmblEntryReader.Format.EMBL_FORMAT,file.getName());
			emblReader.setCheckBlockCounts(true);
			flatfileReader = emblReader;
            break;
	 	case INFO:
	 		flatfileReader= new AssemblyInfoReader(file);
	 		break;
	 	case CHROMOSOME_LIST:
	 		flatfileReader= new ChromosomeListFileReader(file);
            break;
	 	case UNLOCALISED_LIST:
	 		flatfileReader = new UnlocalisedListFileReader(file);
	 		break;
	 	default :
	 		break;
	 	}
	 	return flatfileReader;
	}
	
	public static List<Qualifier> getChromosomeQualifier(ChromosomeEntry entry,boolean isVirus)
	{
		String chromosomeType = entry.getChromosomeType();
		String chromosomeLocation = entry.getChromosomeLocation();
		String chromosomeName = entry.getChromosomeName();
		List<Qualifier> chromosomeQualifiers = new ArrayList<Qualifier>();
		
		if (chromosomeLocation != null && !chromosomeLocation.isEmpty()&& !isVirus&&!chromosomeLocation.equalsIgnoreCase("Phage"))
		{
			String organelleValue =  SequenceEntryUtils.getOrganelleValue(chromosomeLocation);
			if (organelleValue != null)
			{									
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.ORGANELLE_QUALIFIER_NAME, SequenceEntryUtils.getOrganelleValue(chromosomeLocation)));
			}
		}	
		else if (chromosomeName != null && !chromosomeName.isEmpty())
		{
			if (Qualifier.PLASMID_QUALIFIER_NAME.equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.PLASMID_QUALIFIER_NAME, chromosomeName));
			}
			else if (Qualifier.CHROMOSOME_QUALIFIER_NAME.equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.CHROMOSOME_QUALIFIER_NAME, chromosomeName));
			}
			else if("segmented".equals(chromosomeType)||"multipartite".equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.SEGMENT_QUALIFIER_NAME, chromosomeName));

			}
			else if("monopartite".equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.NOTE_QUALIFIER_NAME, chromosomeType));
			}
		}
		return chromosomeQualifiers;
	}
	
	public static boolean writeValidationResult(ValidationPlanResult planResult,ValidationResult parseResult, Writer writer,String file) throws IOException
	{		
		boolean valid=writeValidationPlanResult(planResult, writer, file)&&writeValidationResult(parseResult, writer, file);
		writer.flush();
		return valid;
	}
	
	public static boolean writeValidationPlanResult(ValidationPlanResult planResult, Writer writer,String file) throws IOException
	{
		if(planResult==null)
			return true;
			for (ValidationResult result : planResult.getResults())
				result.writeMessages(writer,Severity.ERROR,file);
			return planResult.isValid();
		
	}
	
	public static boolean writeValidationResult(ValidationResult validationResult, Writer writer,String file) throws IOException
	{
		if(validationResult==null)
			return true;
		validationResult.writeMessages(writer,Severity.ERROR,file);
		return validationResult.isValid();
		
	}
	
	public static List<ManifestObj> readManifest(File file) throws FileNotFoundException, IOException
	{
		ManifestFileReader reader= new ManifestFileReader();
		reader.read(file.toString());
		return reader.getManifestFileObjects();
	}

	public static String getFile(String file) {
		String fileName = null;
		URL url = GenomeAssemblyFileUtils.class.getClassLoader().getResource("uk/ac/ebi/ena/assembly/"+file);
		if (url != null) {
			fileName = url.getPath().replaceAll("%20", " ");
		}
		return fileName;
	}
}
