package uk.ac.ebi.ena.rawreads;

import java.util.Iterator;

public abstract class
DelegateIterator<T1, T2> implements Iterator<T2>
{
    private Iterator<T1> iterator;
    
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
