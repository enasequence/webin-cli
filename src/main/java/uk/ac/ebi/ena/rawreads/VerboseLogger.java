package uk.ac.ebi.ena.rawreads;

import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public interface 
VerboseLogger 
{
    boolean verbose = true;
    
    default public void
    flushConsole()
    {
        if( verbose )
            WebinCliReporter.flushConsole();
    }
    
    
    default public void
    printfToConsole( String msg, Object... arg1 )
    {
        if( verbose )
            WebinCliReporter.printfToConsole( msg, arg1 );
    }
    

    default public void
    printlnToConsole( String msg )
    {
        if( verbose )
            WebinCliReporter.printlnToConsole( msg );
    }

    
    default public void
    printlnToConsole()
    {
        if( verbose )
            WebinCliReporter.printlnToConsole();
    }

    
    default void 
    printProcessedReadNumber( long count )
    {
        printfToConsole( "\rProcessed %16d read(s)", count );
        flushConsole();
    }

}
