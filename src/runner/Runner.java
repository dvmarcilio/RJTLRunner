package runner;

import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

import io.usethesource.vallang.ISourceLocation;

public class Runner {

	private static final String MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE = "rjtl::MutableMembersUsage";

	public static void main(String[] args) throws URISyntaxException {
		ArgsValidator.validate(args);

		Evaluator evaluator = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out),
				new PrintWriter(System.err));

		evaluator.doImport(evaluator.getMonitor(), MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE);

		for (String path : args) {
			refactorForMutableMembersUsage(evaluator, path);
		}
	}

	private static void refactorForMutableMembersUsage(Evaluator evaluator, String path) throws URISyntaxException {
		Path fileLocation = Paths.get(path);
		ISourceLocation fileLoc = URIUtil.createFileLocation(fileLocation.toString());
		evaluator.call("refactorMutableGettersAndSettersViolations", fileLoc);
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
			Predicate<Path> exists = Files::exists;
			Predicate<Path> isDirectory = Files::isDirectory;
			Predicate<Path> isNotDirectory = isDirectory.negate();
			Predicate<Path> isReadable = Files::isReadable;
			Predicate<Path> isWritable = Files::isWritable;
			Predicate<Path> validFile = exists.and(isNotDirectory).and(isReadable).and(isWritable);
			return Stream.of(args).map(Paths::get).allMatch(validFile);
		}
	}
}
