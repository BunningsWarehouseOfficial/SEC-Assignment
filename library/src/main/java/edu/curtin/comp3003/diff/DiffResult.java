package edu.curtin.comp3003.diff;

import java.util.*;

/**
 * Represents the final Diff result -- a record of which parts of the two strings/files match, and
 * which do not.
 */
public class DiffResult implements Iterable<DiffResult.Entry>
{
    /**
     * Represents a single character in the sequence, which may be either a character common to
     * both strings, or specific to one or the other.
     */
    public static class Entry
    {
        private final char c;
        private final byte which;
        
        public Entry(char c, boolean first, boolean second)
        {
            this.c = c;
            this.which = (byte)(( first ? 0x1 : 0 ) | (second ? 0x2 : 0));
        }
        
        public char getChar() { return c; }
        
        public boolean isFirstText() { return (which & 0x1) == 0x1; }
        
        public boolean isSecondText() { return (which & 0x2) == 0x2; }
        
        public boolean isCommon() { return (which & 0x3) == 0x3; }
    }
    
    private List<Entry> entries = new ArrayList<>();
    
    public DiffResult()
    {
    }
    
    /**
     * Used by Diff to build up the result from the end to the start.
     */
    public void prepend(char c, boolean first, boolean second)
    {
        entries.add(0, new Entry(c, first, second));
    }
    
    /**
     * Used to retrieve the result character by character; e.g., using a for-each loop.
     */
    @Override
    public Iterator<Entry> iterator()
    {
        return entries.iterator();
    }
}
