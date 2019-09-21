/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ManifestCVList
{
    private final Properties cvMap = new Properties();
    private final ArrayList<String> cvList = new ArrayList();

    private static InputStream getResourceAsStream( File resource ) {
        return ManifestCVList.class.getClassLoader().getResourceAsStream(
                resource.getPath().replaceAll( "\\\\+", "/" ));
    }

    public ManifestCVList( File resource )
    {
        try (InputStream in = getResourceAsStream( resource )) {
            this.cvMap.load(in);
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }

        try (InputStream in = getResourceAsStream( resource )) {
                new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))
                        .lines()
                        .forEach(line -> cvList.add(line.split("\\s*=\\s*")[0]
                                .replaceAll("\\\\", "")));
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public ManifestCVList( String ... values )
    {
        for (String value: values) {
            cvMap.setProperty(value, value);
            cvList.add(value);
        }
    }


    private static String
    normalizeString( Object s )
    {
        return String.valueOf( s ).toLowerCase().replaceAll( "[ _-]+", "" );
    }


    public boolean
    contains( String key )
    {
        return cvMap.keySet().stream().anyMatch(e -> normalizeString( e ).equals( normalizeString( key ) ) );
    }

    public String
    getKey( String key )
    {
        return cvMap.entrySet().stream()
                .map( e -> new AbstractMap.SimpleEntry<>( normalizeString( e.getKey() ), String.valueOf( e.getKey() ) ) )
                .filter( e -> e.getKey().equals( normalizeString( key ) ) ).findFirst()
                .orElse( new AbstractMap.SimpleEntry<>( null, null ) ).getValue();
    }

    public String
    getValue( String key )
    {
        return cvMap.entrySet().stream()
                .map( e -> new AbstractMap.SimpleEntry<>( normalizeString( e.getKey() ), String.valueOf( e.getValue() ) ) )
                .filter( e -> e.getKey().equals( normalizeString( key ) ) ).findFirst()
                .orElse( new AbstractMap.SimpleEntry<>( null, null ) ).getValue();
    }

    public List<String>
    keyList()
    {
        return cvList.stream().collect( Collectors.toList() );
    }

    public String
    toString()
    {
        return String.valueOf( cvMap.entrySet() );
    }
}
