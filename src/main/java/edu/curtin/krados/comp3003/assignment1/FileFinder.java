package edu.curtin.krados.comp3003.assignment1;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import javafx.application.Platform;

public class FileFinder
{
    public static final String[] TEXT_EXTENSIONS = { ".txt", ".md", ".java", ".cs" };

    private Thread thread;
    private ExecutorService comparisonService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()); //TODO: Change number of threads
    private BlockingQueue<ComparisonResult> comparisons = new ArrayBlockingQueue<>(1000);
    private static final ComparisonResult POISON = new ComparisonResult();

    private String searchPath;
    private FileComparerUI ui;
    private List<String> textFiles = new LinkedList<>();

    public FileFinder(String searchPath, FileComparerUI ui)
    {
        this.searchPath = searchPath;
        this.ui = ui;
    }

    public void start()
    {
        thread = new Thread(this::findFiles, "file-finder-thread");
        thread.start();
    }

    public void stop()
    {
        if (thread == null)
        {
            throw new IllegalArgumentException("Writer thread doesn't exist");
        }

        comparisonService.shutdownNow(); ///
//        comparisonService.shutdown();
//        try
//        {
//            System.out.println("Waiting for the service to terminate..."); ///
//            //Force shutdown if natural shutdown takes too long
//            if (!comparisonService.awaitTermination(3, TimeUnit.SECONDS))
//            {
//                comparisonService.shutdownNow();
//            }
//        }
//        catch (InterruptedException e)
//        {
//            System.out.println("---------FileFinder stop() interrupt"); ///
//            //TODO
//        }

        thread.interrupt(); //TODO: Test if we need InterruptedException catch block in findFiles()
        thread = null;
    }

    public void findFiles()
    {
        //FIXME: App execution doesn't finish if UI is crossed off if FileFinder has begun
        try
        {
            // Recurse through the directory tree
            Files.walkFileTree(Paths.get(searchPath), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    String fileStr = file.toString();
                    if (isTextFile(fileStr))
                    {
                        try
                        {
                            //Check that the file is not empty
                            if (Files.size(file) > 0)
                            {
                                textFiles.add(fileStr);
                                Platform.runLater(() ->
                                {
                                    ui.displayDetail("Found text file to compare: " + fileStr);
                                });
                            }
                        }
                        catch (IOException e)
                        {
                            //Ignore a file whose size couldn't be checked
                            Platform.runLater(() ->
                            {
                                ui.displayDetail("Couldn't determine file size for " + fileStr);
                            });
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            int numFiles = textFiles.size();
            int numMaxComparisons = (numFiles * numFiles - numFiles) / 2;
            //System.out.println("files: " + numFiles + ", maxComparisons = " + numMaxComparisons); ///

            //Start creating threads to compare all the found text files
            List<Future<String>> futures = new LinkedList<>();
            String[] comparisonFiles = textFiles.toArray(new String[0]);
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
                                    Platform.runLater(() ->
                                    {
                                        ui.displayDetail("Cancelled a comparison thread");
                                    });
                                    break;
                                }
                            }
                            catch (OutOfMemoryError e)
                            {
                                Platform.runLater(() ->
                                {
                                    ui.addMissedComparison(comparisonFile, numMaxComparisons);
                                });
                            }
                        }
                    }
                    catch(InterruptedException e)
                    {
                        Platform.runLater(() ->
                        {
                            ui.displayDetail("A comparison task was interrupted");
                        });
                    }
                    catch (IOException e)
                    {
                        Platform.runLater(() ->
                        {
                            ui.showError("An error occurred while making comparisons for " + comparisonFile +
                                    "\n\n" + e.getMessage());
                        });
                    }
                    return comparisonFile;
                }); //TODO: Perhaps move actual callable code elsewhere?
                futures.add(future);
            }

            //Block and wait for each comparison task to finish
            for (Future<String> future : futures)
            {
                try
                {
                    future.get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    Platform.runLater(() ->
                    {
                        ui.showError("An error occurred with comparisons for a file.\n\n" + e.getMessage());
                    });
                }
            }
            comparisonService.shutdown();

            try
            {
                System.out.println("Finished comparing files"); ///
                comparisons.put(POISON);
            }
            catch (InterruptedException e)
            {
                System.out.println("??? ??? FAILED TO PUT POISON"); ///
                //TODO: Signal UI to end any threads dependent on poison (i.e. FileWriter)?
            }
        }
        catch(IOException e)
        {
            Platform.runLater(() ->
            {
                ui.showError("An error occurred while finding files to compare.\n\n" + e.getMessage());
            });
            stop(); //TODO: Test/check this doesn't break weirdly
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

    //Adapted from code by EboMike, https://stackoverflow.com/a/3571239/12350950 (accessed 15 September 2021)
    private boolean isTextFile(String fileStr)
    {
        boolean isTextFile = false;

        int iiDot = fileStr.lastIndexOf('.');
        int iiDirectory = Math.max(fileStr.lastIndexOf('/'), fileStr.lastIndexOf('\\'));
        if (iiDot > iiDirectory)
        {
            String fileExtension = fileStr.substring(iiDot);
            if (Arrays.asList(TEXT_EXTENSIONS).contains(fileExtension))
            {
                isTextFile = true;
            }
        }
        return isTextFile;
    }

    /**
     * Implementation of the LCS Dynamic Programming Algorithm for determining the similarity between two files.
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
