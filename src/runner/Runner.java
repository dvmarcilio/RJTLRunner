package runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.control_exceptions.ControlException;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.RemoveUnusedImports;

import io.usethesource.vallang.ISourceLocation;

public class Runner {

	private static final String MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE = "rjtl::MutableMembersUsage";

	private static final Evaluator evaluator;

	static {
		evaluator = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out), new PrintWriter(System.err));

		evaluator.doImport(evaluator.getMonitor(), MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE);
	}

	public static void main(String[] args) throws URISyntaxException {
		ArgsValidator.validate(args);
		runTransformations(args);
	}

	private static void runTransformations(String[] paths) {
		try {
			doRunTransformations(evaluator, paths);
		} catch (ControlException | URISyntaxException e) {
			// ignoring exceptions, just proceed to next file
			System.out.println();
		}
	}

	private static void doRunTransformations(Evaluator evaluator, String[] paths) throws URISyntaxException {
		for (String path : paths) {
			refactorForMutableMembersUsage(path);
			organizeImports(path);
		}
	}

	private static void refactorForMutableMembersUsage(String path) throws URISyntaxException {
		Path fileLocation = Paths.get(path);
		ISourceLocation fileLoc = URIUtil.createFileLocation(fileLocation.toString());
		evaluator.call("refactorMutableGettersAndSettersViolations", fileLoc);
	}

	private static void organizeImports(String path) {
		try {
			doOrganizeImports(path);
		} catch (IOException | FormatterException e) {
			System.out.println();
			// ignore
		}
	}

	private static void doOrganizeImports(String pathStr) throws IOException, FormatterException {
		Path path = Paths.get(pathStr);
		String contents = new String(Files.readAllBytes(path));
		String classWithoutUnusedImports = RemoveUnusedImports.removeUnusedImports(contents);
		Files.write(path, classWithoutUnusedImports.getBytes());
	}

	private static class ArgsValidator {
		private static void validate(String[] args) {
			if (args.length == 0)
				throw new IllegalArgumentException("At least one path should be given as argument");

			if (!areAllFilesValid(args))
				throw new IllegalArgumentException(
						"All paths should exist, be a file (not a directory) and be readable/writable");
		}

		private static boolean areAllFilesValid(String[] args) {
			final Predicate<Path> isDirectory = Files::isDirectory;
			final Predicate<Path> isNotDirectory = isDirectory.negate();
			final List<Predicate<Path>> predicates = Arrays.asList(Files::exists, isNotDirectory, Files::isReadable, Files::isWritable);

			return Stream.of(args).map(Paths::get).allMatch(predicateByPath(predicates));
		}

	    	private static Predicate<Path> predicateByPath(final List<Predicate<Path>> predicates) {
			return path -> allMatch(path, predicates);
	    	}

	    	private static Boolean allMatch(final Path path, final List<Predicate<Path>> predicates) {
			return predicates.stream().allMatch(predicate -> predicate.test(path));
	    	}
	}
}
