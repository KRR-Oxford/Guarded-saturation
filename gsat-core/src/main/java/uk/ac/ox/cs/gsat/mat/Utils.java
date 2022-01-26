package uk.ac.ox.cs.gsat.mat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import uk.ac.ox.cs.gsat.Configuration;
import uk.ac.ox.cs.gsat.Log;

public class Utils {
     
	/**
	 * From EmbASP code, slightly modified
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static SolverOutput invokeSolver(String exe_path, String options, List<String> files)
        throws InterruptedException, IOException {
		String files_paths = "";

		String final_program = "";

		for (final String program_file : files) {
			File f = new File(program_file);
			if (f.exists() && !f.isDirectory()) {
				files_paths += program_file;
				files_paths += " ";
			} else {
				Log.GLOBAL.warning("The file " + f.getAbsolutePath() + " does not exist.");
            }

		}

		final StringBuffer solverOutput = new StringBuffer();

		final StringBuffer solverError = new StringBuffer();

		final long startTime = System.nanoTime();

		final StringBuffer stringBuffer = new StringBuffer();

		if (exe_path == null)
			return new SolverOutput("", "Error: executable not found");

		stringBuffer.append(exe_path).append(" ").append(options).append(" ").append(files_paths);

		Log.GLOBAL.fine(stringBuffer.toString());

		final Process solver_process = Runtime.getRuntime().exec(stringBuffer.toString());

		Thread threadOutput = new Thread() {
                @Override
                public void run() {
                    InputStreamReader in = new InputStreamReader(solver_process.getInputStream(), StandardCharsets.UTF_8);
                    final BufferedReader bufferedReaderOutput = new BufferedReader(in);

                    // Read output of the solver and store in solverOutput
                    String currentLine;
                    try {
                        while ((currentLine = bufferedReaderOutput.readLine()) != null)
                            solverOutput.append(currentLine + "\n");
                    } catch (IOException e) {
                        System.err.println("Error while reading the output of the solver.");
                    } finally {
                        if (bufferedReaderOutput != null)
                            try {
                                bufferedReaderOutput.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

		threadOutput.start();
		threadOutput.join();

		Thread threadError = new Thread() {
                @Override
                public void run() {
                    InputStreamReader in = new InputStreamReader(solver_process.getErrorStream(), StandardCharsets.UTF_8);
                    final BufferedReader bufferedReaderError = new BufferedReader(in);
                    String currentErrLine;
                    try {
                        while ((currentErrLine = bufferedReaderError.readLine()) != null)
                            solverError.append(currentErrLine + "\n");
                    } catch (IOException e) {
                        System.err.println("Error while reading the output of the solver.");
                    } finally {
                        if (bufferedReaderError != null)
                            try {
                                bufferedReaderError.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

		threadError.start();
		threadError.join();

		final PrintWriter writer = new PrintWriter(
                                                   new OutputStreamWriter(solver_process.getOutputStream(), StandardCharsets.UTF_8), true);
		writer.println(final_program);

		if (writer != null)
			writer.close();

		solver_process.waitFor();

		final long stopTime = System.nanoTime();

		long totalTime = stopTime - startTime;

		Log.GLOBAL.info("Solver total time : " + String.format(Locale.UK, "%.0f", totalTime / 1E6) + " ms = "
                        + String.format(Locale.UK, "%.2f", totalTime / 1E9) + " s");

		return new SolverOutput(solverOutput.toString(), solverError.toString());

	}

    public static void writeSolverOutput(SolverOutput solverOutput, String path) throws IOException {

        if (!
            Configuration.isSolverOutputToFile())
            return;

        Files.write(Paths.get(path), Arrays.asList(solverOutput.getOutput(), solverOutput.getErrors()),
                    StandardCharsets.UTF_8);

    }

}
