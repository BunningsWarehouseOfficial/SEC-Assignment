package edu.curtin.comp3003.diff;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A simple GUI Diff application. The user enters two filenames (or uses dialog boxes to search for
 * the files), and then presses a 'Compare Files' button, and the comparison results are displayed
 * in the window.
 */
public class GUIDiffRunner
{
    public static void main(String[] args)
    {
        // Start the Swing GUI.
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override 
            public void run()
            {            
                new GUIDiffRunner().buildInterface();
            }
        });
    }
    
    private JFrame window = null;
    private JFileChooser fileChooser = null;
    private JTextField fileName1Field = null;
    private JTextField fileName2Field = null;
    private JEditorPane display = null;
    
    private static final String OPEN_IMAGE = "baseline_folder_open_black_18dp.png";
    private static final String COMPARE_IMAGE = "baseline_compare_arrows_black_18dp.png";
    
    /** 
     * Convenience method for loading an image icon by name, assuming it is located within the 
     * classpath, which typically means within the .jar file containing the application.
     */
    private static ImageIcon loadIcon(String fileName)
    {
        return new ImageIcon(GUIDiffRunner.class.getClassLoader().getResource(fileName));
    }
    
    /** 
     * Setup the GUI.
     */
    public void buildInterface()
    {
        window = new JFrame("Diff");
        window.setPreferredSize(new Dimension(800, 800));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JToolBar controls = new JToolBar();
        fileName1Field = new JTextField("", 20);
        fileName2Field = new JTextField("", 20);
        JButton fileName1Selector = new JButton(loadIcon(OPEN_IMAGE));
        JButton fileName2Selector = new JButton(loadIcon(OPEN_IMAGE));
        JButton diffButton = new JButton("Compare Files", loadIcon(COMPARE_IMAGE));
        
        fileChooser = new JFileChooser();
        initFileSelector(fileName1Field, fileName1Selector);
        initFileSelector(fileName2Field, fileName2Selector);
        diffButton.addActionListener(event -> runDiff());
        
        controls.add(fileName1Field);
        controls.add(fileName1Selector);
        controls.add(fileName2Field);
        controls.add(fileName2Selector);
        controls.add(diffButton);
        
        display = new JEditorPane();
        display.setEditable(false);
        display.setContentType("text/html");
        display.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane displayArea = new JScrollPane(display);
        
        Container contentPane = window.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(controls, BorderLayout.NORTH);
        contentPane.add(displayArea, BorderLayout.CENTER);   
        window.pack();
        window.setVisible(true);
    }
    
    /** 
     * Set up a file-open dialog to be launched when a button is pressed, and where the filename
     * chosen by the user is put into a JTextField.
     */
    private void initFileSelector(JTextField field, JButton button)
    {
        button.addActionListener(event ->
        {
            int result = fileChooser.showOpenDialog(window);
            if(result == JFileChooser.APPROVE_OPTION)
            {
                field.setText(fileChooser.getSelectedFile().toString());
            }
        });
    }
    
    /** 
     * Launch the main Diff algorithm, and convert the results into HTML so they can be displayed 
     * inside a JEditorPane.
     */
    private void runDiff()
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            
            String prevFormat = "";
            DiffResult diffResult = new Diff().fileDiff(new File(fileName1Field.getText()),
                                                        new File(fileName2Field.getText()));
            for(DiffResult.Entry entry : diffResult)
            {
                char ch = entry.getChar();

                String format;
                if(entry.isCommon() || ch == '\n' || ch == '\r') // Don't highlight newlines
                {
                    format = "";
                }
                else if(entry.isFirstText())
                {
                    format = "background: #FF8080;";
                }
                else //if(entry.isSecondText())
                {
                    format = "background: #80FF80;";
                }
                
                if(!prevFormat.equals(format))
                {
                    if(!"".equals(prevFormat))
                    {
                        pw.print("</span>");
                    }
                    else if(!"".equals(format))
                    {
                        pw.printf("<span style=\"%s\">", format);
                    }
                    prevFormat = format;
                }
                
                switch(ch)
                {
                    case '\n': pw.println("<br/>"); break;
                    case '\r':                      break;
                    case ' ':  pw.print("&nbsp;");  break;
                    case '&':  pw.print("&amp;");   break;
                    case '\'': pw.print("&quot;");  break;
                    case '"':  pw.print("&#39;");   break;
                    case '<':  pw.print("&lt;");    break;
                    case '>':  pw.print("&gt;");    break;
                    default:   pw.write(ch);        break;
                }
            }
            if(!"".equals(prevFormat))
            {
                pw.printf("</%s>", prevFormat);
            }
            
            display.setText(String.format(
                "<div style=\"font-family: monospace;\">%s</div>",
                sw.toString()));
        }
        catch(IOException e)
        {
            System.out.println("Can't read file(s): " + e.getMessage());
        }
    }
}
