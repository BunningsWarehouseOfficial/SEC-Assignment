package edu.curtin.comp3003.diff

import spock.lang.*

/** 
 * A simple set of unit tests for the Diff class, using the 'Spock' unit test framework.
 * This is a Groovy file, but Spock itself plays with the Groovy syntax in interesting ways.
 *
 * There is an error on the last line, to help illustrate what a test failure looks like in Gradle.
 */
class DiffTest extends Specification
{
    def getCommonString(DiffResult result)
    {
        return result.findResults{ it.isCommon() ? it.char : null }.join()
    }
        
    def 'LCS logical combinations'(String text1, String text2, String lcs)
    {
        when:
            Diff diffObj = new Diff()
            
        then:
            lcs == getCommonString(diffObj.diff(text1, text2))
            
        where:
            text1     | text2      || lcs
            ""        | ""         || ""
            ""        | "xyz"      || ""
            "xyz"     | ""         || ""
            "xyz"     | "xyz"      || "xyz"
            "xyz"     | "abxyzcd"  || "xyz"
            "abxyzcd" | "xyz"      || "xyz"
            "xyz"     | "xabycdz"  || "xyz"
            "xabycdz" | "xyz"      || "xyz"
            "abcdxyz" | "xyzabcd"  || "abcd"
            //"abcdxyz" | "xyzabcd"  || "abcd_OUT_OF_CHEESE_ERROR"
    }
}
