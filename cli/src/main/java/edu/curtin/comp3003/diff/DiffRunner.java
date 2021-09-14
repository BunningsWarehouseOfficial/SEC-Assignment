package edu.curtin.comp3003.diff;

import java.io.*;

/** 
 * A simple command-line Diff application. It expects two filenames to be provided as command-kine
 * arguments. It will then launch the Diff algorithm to compare the files, and output the results
 * using coloured text to indicate the differences.
 */
public class DiffRunner
{
    // ANSI colour codes. Printing these will cause the terminal to change colours.
    private static final String NORMAL_COLOUR = "\033[0m";       // Default background and text
    private static final String FIRST_HIGHLIGHT = "\033[41;1m";  // Red background, bold text
    private static final String SECOND_HIGHLIGHT = "\033[42;1m"; // Green background, bold text

    public static void main(String[] args)
    {
        if(args.length == 2)
        {
            try
            {
                String prevFormat = "";
                DiffResult diffResult = new Diff().fileDiff(new File(args[0]), new File(args[1]));
                for(DiffResult.Entry entry : diffResult)
                {
                    char ch = entry.getChar();
    
                    String format;
                    if(entry.isCommon() || ch == '\n' || ch == '\r') // Don't highlight newlines
                    {
                        format = NORMAL_COLOUR;
                    }
                    else if(entry.isFirstText())
                    {
                        format = FIRST_HIGHLIGHT;
                    }
                    else //if(entry.isSecondText())
                    {
                        format = SECOND_HIGHLIGHT;
                    }
                    
                    if(!prevFormat.equals(format))
                    {
                        // Only output the colour code if different from the previous one.
                        prevFormat = format;
                        System.out.print(format);
                    }
                    System.out.print(ch);
                }
                System.out.print(NORMAL_COLOUR);
            }
            catch(IOException e)
            {
                System.out.println("Can't read file(s): " + e.getMessage());
            }
        }
        else
        {
            System.out.println("Please specify two text files to compare.");
        }
    }
}
