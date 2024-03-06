/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ShellExec {
    private static final Logger log = LoggerFactory.getLogger(ShellExec.class);

    private final String command;

    private final Map<String, String> vars;

    private static class StreamConsumer extends Thread {
        private final Reader ireader;

        private final StringBuilder consumedInput = new StringBuilder(8192);
        
        public StreamConsumer( InputStream istream ) {
            this.ireader = new InputStreamReader( istream, StandardCharsets.UTF_8 );

            setName( getClass().getSimpleName() );
        }

        public String getConsumedInput() {
            return consumedInput.toString();
        }

        @Override
        public void run() {
            try {
                int ch = 0;
                while( ( ch = ireader.read() ) != -1 ) {
                    if( ch > 0 ) {
                        consumedInput.append((char)ch);
                    }
                }
            } catch( IOException e ) { }
        }
    }

    public static class Result {
        private final int exitCode;

        private final String stdout;
        private final String stderr;

        public Result(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }

    public ShellExec( String command, Map<String, String> vars ) {
        this.command = command;
        this.vars = vars;
    }

    public Map<String, String> getVars() {
        return vars;
    }

    public String getCommand() {
        return command;
    }
    
    public Result exec() throws IOException, InterruptedException {
        return exec( getCommand(), getVars() );
    }
    
    private Result exec(String command, Map<String, String> vars) throws IOException, InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool( 2 );

        try {
            ((ThreadPoolExecutor) es).prestartAllCoreThreads();

            log.debug( "Invoking: {}", command );
            
            ProcessBuilder pb = System.getProperty( "os.name" ).toLowerCase().contains( "win" )
                ? new ProcessBuilder( "cmd", "/c", command )
                : new ProcessBuilder( "sh", "-c",  command );
            pb.environment().putAll( vars );
            pb.directory( null );
            
            Process proc = pb.start();

            StreamConsumer inputStreamConsumer = new StreamConsumer( proc.getInputStream() );
            StreamConsumer errorStreamConsumer = new StreamConsumer( proc.getErrorStream() );
            
            es.submit( inputStreamConsumer );
            es.submit( errorStreamConsumer );

            int exitCode = proc.waitFor();

            return new Result(exitCode, inputStreamConsumer.getConsumedInput(), errorStreamConsumer.getConsumedInput());
        } finally {
            es.shutdown();
            es.awaitTermination( 30, TimeUnit.SECONDS );
        }
    }
}
