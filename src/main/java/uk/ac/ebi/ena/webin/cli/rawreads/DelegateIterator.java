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

package uk.ac.ebi.ena.webin.cli.rawreads;

import java.util.Iterator;

public abstract class
DelegateIterator<T1, T2> implements Iterator<T2>
{
    private final Iterator<T1> iterator;
    
    public abstract T2 convert( T1 obj );
    
    public
    DelegateIterator( Iterator<T1> iterator )
    {
      this.iterator = iterator;
    }
    
    @Override public boolean 
    hasNext()
    {
        return iterator.hasNext();
    }

    
    @Override public T2
    next()
    {
        return convert( iterator.next() );
    }
    
}
