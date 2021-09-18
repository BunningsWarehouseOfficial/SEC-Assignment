package edu.curtin.krados.comp3003.assignment1;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class FileComparer
{
    private Thread thread;
    private ExecutorService comparisonService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private BlockingQueue<ComparisonResult> comparisons = new ArrayBlockingQueue<>(1000);
    private static final ComparisonResult POISON = new ComparisonResult();

    private String[] comparisonFiles;
    private FileComparerUI ui;

    public FileComparer(String[] comparisonFiles, FileComparerUI ui)
    {
        this.comparisonFiles = comparisonFiles;
        this.ui = ui;
    }

    public void start()
    {
        thread = new Thread(this::compareFiles, "file-comparer-thread");
        thread.start();
    }

    public void stop()
    {
        if (thread == null)
        {
            throw new IllegalArgumentException("Writer thread doesn't exist");
        }

        comparisonService.shutdown();
        try
        {
            Platform.runLater(() -> ui.displayDetail("Waiting for the currently running comparisons to terminate..."));
            //Force shutdown if natural shutdown takes too long
            if (!comparisonService.awaitTermination(3, TimeUnit.SECONDS))
            {
                comparisonService.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            Platform.runLater(() -> ui.displayDetail("File finding/reading/comparing shutdown process was interrupted"));
        }

        thread.interrupt();
        thread = null;
    }

    private void compareFiles()
    {
        List<Future<String>> futures = new LinkedList<>();
        int numFiles = comparisonFiles.length;
        int numMaxComparisons = (numFiles * numFiles - numFiles) / 2;

        for (int ii = 0; ii < comparisonFiles.length - 1; ii++)
        {
            String comparisonFile = comparisonFiles[ii];
            int startIndex = ii;
            //Submitting comparison callables to thread pool
            Future<String> future = comparisonService.submit(() ->
            {
                try
                {
                    String primaryFile = Files.readString(Paths.get(comparisonFile));

                    //Compare the target file to every other file for which a comparison hasn't been made already
                    for (int jj = startIndex + 1; jj < comparisonFiles.length; jj++)
                    {
                        try
                        {
                            String targetFile = comparisonFiles[jj];
                            if (!comparisonFile.equals(targetFile) && !Thread.currentThread().isInterrupted())
                            {
                                String secondaryFile = Files.readString(Paths.get(targetFile));

                                double similarity = calcSimilarity(primaryFile, secondaryFile);
                                ComparisonResult newComparison = new ComparisonResult(
                                        comparisonFile, targetFile, similarity);

                                if (similarity > FileComparerUI.MIN_SIMILARITY)
                                {
                                    comparisons.put(newComparison);
                                }
                                Platform.runLater(() ->
                                {
                                    ui.addComparison(newComparison);
                                    ui.incrementProgress(numMaxComparisons);
                                });
                            } else
                            {
                                Platform.runLater(() -> ui.displayDetail("Cancelled a comparison thread"));
                                break;
                            }
                        }
                        catch (OutOfMemoryError e)
                        {
                            Platform.runLater(() -> ui.addMissedComparison(comparisonFile, numMaxComparisons));
                        }
                    }
                }
                catch(InterruptedException e)
                {
                    Platform.runLater(() -> ui.displayDetail("A comparison task was interrupted"));
                }
                catch (IOException e)
                {
                    Platform.runLater(() -> ui.showError("An error occurred while making comparisons for "
                            + comparisonFile + "\n\n" + e.getMessage()));
                }
                return comparisonFile;
            });
            futures.add(future);
        }

        //Block and wait for each comparison task to finish
        for (Future<String> future : futures)
        {
            try
            {
                future.get();
            }
            catch (InterruptedException ignored) { }
            catch (ExecutionException e)
            {
                Platform.runLater(() ->
                {
                    if (e.getMessage() != null)
                    {
                        ui.showError("An error occurred with comparisons for a file.\n\n" + e.getMessage());
                    }
                    else
                    {
                        ui.showError("An error occurred with comparisons for a file.");
                    }
                });
            }
        }
        comparisonService.shutdown();

        try
        {
            Platform.runLater(() -> ui.displayDetail("Finished comparing files"));
            comparisons.put(POISON);
        }
        catch (InterruptedException e)
        {
            //Try again to ensure that the end of the producer is signalled to any consumers
            try
            {
                comparisons.put(POISON);
            }
            catch (InterruptedException e2)
            {
                Platform.runLater(() ->
                {
                    ui.showError("An error occurred signaling the end of the file comparer thread\n\n"
                            + e2.getMessage());
                });
            }
        }
    }

    public ComparisonResult getNextComparison() throws InterruptedException
    {
        ComparisonResult comparison = comparisons.take();
        if (comparison == POISON)
        {
            comparison = null;
        }
        return comparison;
    }

    /**
     * Implementation of the LCS Dynamic Programming Algorithm for determining the similarity between two files.
     *
     * Source: assignment specification.
     */
    private double calcSimilarity(String file1, String file2)
    {
        int rows    = file1.length() + 1;
        int columns = file2.length() + 1;
        int[][] subsolutions = new int[rows][columns];
        boolean[][] directionLeft = new boolean[rows][columns];

        //Fill first row and first column of subsolutions with zeros
        for (int mm = 0; mm < columns; mm++)
        {
            subsolutions[0][mm] = 0;
        }
        for (int nn = 0; nn < rows; nn++)
        {
            subsolutions[nn][0] = 0;
        }

        for (int ii = 1; ii <= file1.length(); ii++)
        {
            for (int jj = 1; jj <= file2.length(); jj++)
            {
                if (file1.charAt(ii - 1) == file2.charAt(jj - 1))
                {
                    subsolutions[ii][jj] = subsolutions[ii - 1][jj - 1] + 1;
                }
                else if (subsolutions[ii - 1][jj] > subsolutions[ii][jj - 1])
                {
                    subsolutions[ii][jj] = subsolutions[ii - 1][jj];
                    directionLeft[ii][jj] = true;
                }
                else
                {
                    subsolutions[ii][jj] = subsolutions[ii][jj - 1];
                    directionLeft[ii][jj] = false;
                }
            }
        }

        int matches = 0;
        int ii = file1.length();
        int jj = file2.length();

        while (ii > 0 && jj > 0)
        {
            if (file1.charAt(ii - 1) == file2.charAt(jj - 1))
            {
                matches += 1;
                ii -= 1;
                jj -= 1;
            }
            else if (directionLeft[ii][jj])
            {
                ii -= 1;
            }
            else
            {
                jj -= 1;
            }
        }
        return (double)(matches * 2) / (double)(file1.length() + file2.length());
    }
}
