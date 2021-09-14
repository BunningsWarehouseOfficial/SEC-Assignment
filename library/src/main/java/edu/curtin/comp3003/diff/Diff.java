package edu.curtin.comp3003.diff;

import java.io.*;
import java.nio.file.Files;

/**
 * The core Diff algorithm -- an implementation of the Longest Common Subsequence (LCS) algorithm.
 */
public class Diff
{
    private static final byte UP = (byte)0x1;
    private static final byte LEFT = (byte)0x2;

    public DiffResult diff(String s1, String s2)
    {
        char[] c1 = s1.toCharArray();
        char[] c2 = s2.toCharArray();
        
        // The table (to be built below) has two aspects: the 'goodness' is the longest common 
        // subsequence up until a given point (i,j) in each string. It lets us compare different 
        // sub-solutions in the table, so we pick the best one. 'prev' is a record of which 
        // sub-solution each other sub-solution is building on top of. It lets us work out, at the
        // end, what the entire solution is.
        
        int[][] goodness = new int[c1.length + 1][c2.length + 1];
        byte[][] prev = new byte[c1.length + 1][c2.length + 1];
        
        // Build the table of comparisons between all combinations of characters in the two
        // strings. The table rows represent characters in the first string, and columns represent
        // characters in the second.
        
        for(int i = 0; i < c1.length; i++)
        {
            for(int j = 0; j < c2.length; j++)
            {
                if(c1[i] == c2[j])
                {
                    goodness[i + 1][j + 1] = goodness[i][j] + 1;
                    prev[i + 1][j + 1] = UP | LEFT;
                }
                else if(goodness[i][j + 1] >= goodness[i + 1][j])
                {
                    goodness[i + 1][j + 1] = goodness[i][j + 1];
                    prev[i + 1][j + 1] = UP;
                }
                else
                {
                    goodness[i + 1][j + 1] = goodness[i + 1][j];
                    prev[i + 1][j + 1] = LEFT;
                }
            }
        }
        
        // Second, traverse the table from the end point (the bottom-right corner) to find the
        // longest common subsequence. We also find the location within that sequence of all the
        // non-common text belonging to either of the input strings.
        //
        // As we go, we build up a 'DiffResult', which contains the complete sequence.
        
        DiffResult result = new DiffResult();
        int i = c1.length;
        int j = c2.length;
        while(i > 0 && j > 0)
        {
            switch(prev[i][j])
            {
                case UP | LEFT:
                    result.prepend(c1[i - 1], true, true);
                    i--;
                    j--;
                    break;
                    
                case UP:
                    result.prepend(c1[i - 1], true, false);
                    i--;
                    break;
                    
                case LEFT:
                    result.prepend(c2[j - 1], false, true);
                    j--;
                    break;
            }
        }
        
        while(i > 0)
        {
            result.prepend(c1[i - 1], true, false);
            i--;
        }
        
        while(j > 0)
        {
            result.prepend(c2[j - 1], false, true);
            j--;
        }
        
        return result;
    }
    
    /**
     * Convenience method for comparing files. We simply extract the bytes from each file, convert 
     * them to Strings, and run the main Diff algorithm.
     */
    public DiffResult fileDiff(File f1, File f2) throws IOException
    {
        return diff(
            new String(Files.readAllBytes(f1.toPath())),
            new String(Files.readAllBytes(f2.toPath())));
    }
}
