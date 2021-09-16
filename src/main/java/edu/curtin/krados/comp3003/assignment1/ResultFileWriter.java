package edu.curtin.krados.comp3003.assignment1;

import javafx.application.Platform;

import java.io.*;

public class ResultFileWriter
{
    public static final String OUTPUT_FILENAME = "results.csv";

    private Thread thread;
    private FileFinder producer;
    private FileComparerUI ui;

    public ResultFileWriter(FileFinder producer, FileComparerUI ui)
    {
        this.producer = producer;
        this.ui = ui;
    }

    public void start()
    {
        thread = new Thread(this::writeResults, "writer-thread");
        thread.start();
    }

//    public void stop()
//    {
//        if (thread == null)
//        {
//            throw new IllegalArgumentException("Writer thread doesn't exist");
//        }
//        thread.interrupt();
//        thread = null;
//    }

    private void writeResults()
    {
//        FileOutputStream fileStrm = null;
//        PrintWriter pw;
        FileWriter fw = null;
        BufferedWriter bw;
        PrintWriter pw;
        try
        {
//            fileStrm = new FileOutputStream(OUTPUT_FILENAME);
//            pw = new PrintWriter(fileStrm);
            fw = new FileWriter(OUTPUT_FILENAME, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            try
            {
                while (true)
                {
                    ComparisonResult result = producer.getNextComparison();
                    if (result != null)
                    {
                        String newLine = result.getFile1() + "," + result.getFile2() + "," + result.getSimilarity();
                        System.out.println("WRITING result: " + newLine); ///
                        pw.println(newLine);
                    }
                    else
                    {
                        break;
                    }
                }
                pw.close();
            }
            catch(InterruptedException e)
            {
                System.out.println("===X=== INTERRUPT: Writer"); ///
                //TODO
            }
            //TODO: Need contingency for thread to end if POISON wasn't successfully placed in blocking queue
        }
        catch(IOException e)
        {
            showError(e.getMessage());
        }
        finally
        {
            if (fw != null)
            {
                try
                {
                    fw.close();
                }
                catch (IOException e2)
                {
                    showError(e2.getMessage());
                }
            }
        }
    }

    private void showError(String error)
    {
        Platform.runLater(() ->
        {
            ui.showError("An error occurred while writing a result to the output file.\n\n" + error);
        });
    }
}
