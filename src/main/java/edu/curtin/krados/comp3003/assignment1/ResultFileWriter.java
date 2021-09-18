package edu.curtin.krados.comp3003.assignment1;

import javafx.application.Platform;

import java.io.*;

/**
 * Responsible for writing the results of file similarity comparisons to an output file under its own thread.
 * */
public class ResultFileWriter
{
    public static final String OUTPUT_FILENAME = "results.csv";

    private Thread thread;
    private FileComparer producer;
    private FileComparerUI ui;

    public ResultFileWriter(FileComparer producer, FileComparerUI ui)
    {
        this.producer = producer;
        this.ui = ui;
    }

    public void start()
    {
        thread = new Thread(this::writeResults, "writer-thread");
        thread.start();
    }

    /**
     * A task for writing comparison results to an output file.
     */
    private void writeResults()
    {
        FileWriter fw = null;
        BufferedWriter bw;
        PrintWriter pw;
        try
        {
            fw = new FileWriter(OUTPUT_FILENAME, false);
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
                Platform.runLater(() -> ui.displayDetail("Writing results to " + OUTPUT_FILENAME + " was interrupted"));
            }
        }
        catch(IOException e)
        {
            showError(e.getMessage());
        }
        finally
        {
            //Make sure that the FileWriter closes, ending the thread
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
        Platform.runLater(() -> ui.showError("An error occurred while writing a result to the output file.\n\n" + error));
    }
}
