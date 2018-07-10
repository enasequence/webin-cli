package uk.ac.ebi.ena.rawreads;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class 
Bloom 
{
    public interface 
    HashPhunct 
    {
        int calc( String value );
    }


    static class 
    simpleH implements HashPhunct
    {
        final int seed;
        final int hboundary;
        
        public
        simpleH( int seed, int hboundary )
        {
            this.seed = seed;
            this.hboundary = hboundary;
        }
        
        
        @Override public int 
        calc( String value )
        {
            return ( value.chars().reduce( seed, ( result, e ) -> result + ( ( seed + e ) << 2 ) ) & 0x8FFFFFFF ) % hboundary;
        }
    }
    
    static class 
    sha256 implements HashPhunct
    {
        final String salt;
        final long hboundary;
        
        public
        sha256( String salt, int hboundary )
        {
            this.salt = salt;
            this.hboundary = hboundary;
        }
        
        
        @Override public int
        calc( String value )
        {
            try
            {
                MessageDigest md = MessageDigest.getInstance( "SHA-256" );
                md.update( salt.getBytes() );
                byte[] bytes = md.digest( value.getBytes() );
                BigInteger bi = new BigInteger( bytes );
                return bi.mod( new BigInteger( String.valueOf( hboundary ), 10  ) ).intValue();
//                long acc = 0;
//                for( int index = 0; index < bytes.length; index += 4 )
//                {
//                    int ll = ( 0xFF & bytes[ index + 3 ] );
//                    int lh = ( 0xFF & bytes[ index + 2 ] ) << 8;
//                    int hl = ( 0xFF & bytes[ index + 1 ] ) << 16;
//                    int hh = ( 0xFF & bytes[ index ] ) << 24;
//                    acc += ( hh | hl | lh | ll );
//                }
//                return (int) ( Math.abs( acc ) % hboundary );
            } catch( NoSuchAlgorithmException nsae )
            {
                throw new RuntimeException( nsae );
            }
        }
    }

    
    
    private HashPhunct h[];
    private BitSet v;
    private int bitset_size;
       
    
    public
    Bloom( int bits_per_element, int expected, int hash_num )
    {
        this.bitset_size = bits_per_element * expected;
        this.v = new BitSet( bits_per_element * expected );
        List<HashPhunct> hlist = ThreadLocalRandom.current().ints( hash_num, 0, expected ).mapToObj( e -> new sha256( String.valueOf( e ), bitset_size ) ).collect( Collectors.toList() );
        this.h = hlist.toArray( new HashPhunct[ hlist.size() ] );
        System.out.println( "Bitset size: " + bitset_size + ", ph: " + h.length );
    }
    
    
    public 
    Bloom( double false_positive_probability, int expected )
    {
        this( (int)( Math.ceil( -( Math.log( false_positive_probability ) / Math.log( 2 ) ) ) / Math.log( 2 ) ),
               expected,
              (int) Math.ceil( -( Math.log( false_positive_probability ) / Math.log( 2 ) ) ) );
    }

    
    public void
    add( String value )
    {
        for( int i = 0; i < h.length; ++i )
            v.set( h[ i ].calc( value ) );
    }


    public boolean
    contains( String value )
    {
        for( int i = 0; i < h.length; ++i )
        {
            if( !v.get( h[ i ].calc( value ) ) )
                return false;
        }
        
        return true;
    }

}
