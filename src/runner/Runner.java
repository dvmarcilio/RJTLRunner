package runner;

import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.impl.reference.ValueFactory;

public class Runner {

	private static final String SONAR_FIXES_MODULE = "lang::java::refactoring::sonar::SonarFixes";

	private static final String FIX_DIRECTORY_METHOD = "allSonarFixesForDirectory";
	private static final String FIX_FILE_METHOD = "allSonarFixesForFile";

	private static final String FIX_OPTIONS_DIRECTORY_INCLUDES = "sonarFixesForDirectoryIncludes";
	private static final String FIX_OPTIONS_FILE_INCLUDES = "sonarFixesForFileIncludes";

	private static final String FIX_OPTIONS_DIRECTORY_EXCLUDES = "sonarFixesForDirectoryExcludes";
	private static final String FIX_OPTIONS_FILE_EXCLUDES = "sonarFixesForFileExcludes";

	private static final Evaluator evaluator;

	static {
		evaluator = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out),
				new PrintWriter(System.err));
	}

	public static void main(String[] args) throws URISyntaxException {
		ArgsValidator argsValidator = new ArgsValidator(args);
		fix(argsValidator);
	}

	private static void fix(ArgsValidator argsValidator) throws URISyntaxException {
		evaluator.doImport(evaluator.getMonitor(), SONAR_FIXES_MODULE);

		if (!argsValidator.rulesToExclude.isEmpty()) {
			fixForExcludes(argsValidator);
		} else if (!argsValidator.rulesToInclude.isEmpty()) {
			fixForIncludes(argsValidator);
		} else {
			fixForAllRules(argsValidator);
		}
	}

	private static void fixForExcludes(ArgsValidator argsValidator) throws URISyntaxException {
		IList excludes = listOfStringAsIList(argsValidator.rulesToExclude);

		for (String dir : argsValidator.directoryArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(dir);
			IBool ignoreTestFile = ValueFactory.getInstance().bool(argsValidator.ignoreTestFiles);
			evaluator.call(FIX_OPTIONS_DIRECTORY_EXCLUDES, fileLoc, excludes, ignoreTestFile);
		}

		for (String file : argsValidator.fileArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(file);
			evaluator.call(FIX_OPTIONS_FILE_EXCLUDES, fileLoc, excludes);
		}
	}

	private static void fixForIncludes(ArgsValidator argsValidator) throws URISyntaxException {
		IList rules = listOfStringAsIList(argsValidator.rulesToInclude);

		for (String dir : argsValidator.directoryArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(dir);
			IBool ignoreTestFile = ValueFactory.getInstance().bool(argsValidator.ignoreTestFiles);
			evaluator.call(FIX_OPTIONS_DIRECTORY_INCLUDES, fileLoc, rules, ignoreTestFile);
		}

		for (String file : argsValidator.fileArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(file);
			evaluator.call(FIX_OPTIONS_FILE_INCLUDES, fileLoc, rules);
		}
	}

	private static ISourceLocation stringPathToISourceLocation(String dir)
			throws URISyntaxException {
		Path fileLocation = Paths.get(dir);
		ISourceLocation fileLoc = URIUtil.createFileLocation(fileLocation.toString());
		return fileLoc;
	}

	private static IList listOfStringAsIList(List<String> strings) {
		List<IString> listOfIStrings = new ArrayList<>(strings.size());

		strings.forEach(s -> listOfIStrings.add(ValueFactory.getInstance().string(s)));

		return ValueFactory.getInstance()
				.list(listOfIStrings.toArray(new IString[listOfIStrings.size()]));
	}

	private static void fixForAllRules(ArgsValidator argsValidator) throws URISyntaxException {
		for (String dir : argsValidator.directoryArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(dir);
			IBool ignoreTestFile = ValueFactory.getInstance().bool(argsValidator.ignoreTestFiles);
			evaluator.call(FIX_DIRECTORY_METHOD, fileLoc, ignoreTestFile);
		}

		for (String file : argsValidator.fileArguments) {
			ISourceLocation fileLoc = stringPathToISourceLocation(file);
			evaluator.call(FIX_FILE_METHOD);
		}
	}

	private static class ArgsValidator {

		private static final String OPTION_PREFIX = "--";
		private static final String INCLUDES_OPTION = "rules";
		private static final String EXCLUDES_OPTION = "excludeRules";
		private static final String IGNORE_TEST_FILES = "ignoreTestFiles";

		private final List<String> pathArguments;

		private final List<String> options;
		private final List<String> rulesToInclude;
		private final List<String> rulesToExclude;
		private final boolean ignoreTestFiles;

		private List<String> directoryArguments;
		private List<String> fileArguments;

		ArgsValidator(String[] args) {
			pathArguments = Stream.of(args) //
					.filter(arg -> !arg.startsWith(OPTION_PREFIX)) //
					.distinct() //
					.collect(Collectors.toList());

			options = getOptionsAsList(args);

			rulesToInclude = valuesFromListOption(INCLUDES_OPTION);
			rulesToExclude = valuesFromListOption(EXCLUDES_OPTION);
			ignoreTestFiles = valueFromBoolean(IGNORE_TEST_FILES, true);

			validateOptions();

			validatePaths();
		}

		private List<String> getOptionsAsList(String[] args) {
			List<String> options = new ArrayList<String>(Arrays.asList(args));
			options.removeAll(pathArguments);
			return options;
		}

		private List<String> valuesFromListOption(String option) {
			Optional<String> possible = possibleOption(option);
			if (possible.isPresent()) {
				String valuesSeparatedByComma = valuesSubStringFromOption(option, possible);
				return Stream.of(valuesSeparatedByComma.split(",")) //
						.distinct() //
						.collect(Collectors.toList());
			}
			return Collections.emptyList();
		}

		private Optional<String> possibleOption(String option) {
			return options.stream() //
					.filter(opt -> opt.startsWith(OPTION_PREFIX + option)) //
					.findFirst();
		}

		private String valuesSubStringFromOption(String option, Optional<String> possible) {
			String beforeValues = OPTION_PREFIX + option + "=";
			return possible.get().substring(beforeValues.length());
		}

		private boolean valueFromBoolean(String option, boolean defaultValue) {
			Optional<String> possible = possibleOption(option);
			if (possible.isPresent()) {
				return new Boolean(valuesSubStringFromOption(option, possible));
			}
			return defaultValue;
		}

		private void validateOptions() {
			if (!rulesToInclude.isEmpty() && !rulesToExclude.isEmpty()) {
				throw new IllegalArgumentException(
						"You should not pass --excludeRules and --rules at the same time.");
			}
		}

		private void validatePaths() {
			if (pathArguments.size() == 0)
				throw new IllegalArgumentException("At least one path should be given as argument");

			if (!areAllPathsValid())
				throw new IllegalArgumentException("All paths should exist."
						+ "\nFiles should end with '.java' and be readable and writable");
		}

		private boolean areAllPathsValid() {
			return allPathsExist() && allNonDirectoriesAreValid();
		}

		private boolean allPathsExist() {
			return pathArguments.stream().map(Paths::get).allMatch(Files::exists);
		}

		private boolean allNonDirectoriesAreValid() {
			collectFilesAndDirectories();
			return fileArguments.stream() //
					.map(Paths::get) //
					.allMatch(allFilesAreJavaAndAreReadableAndWritable());
		}

		private void collectFilesAndDirectories() {
			Predicate<Path> isDirectory = Files::isDirectory;
			fileArguments = pathArguments.stream() //
					.map(Paths::get) //
					.filter(isDirectory.negate()) //
					.map(Path::toString) //
					.collect(Collectors.toList());

			// Just making sure the separator is normalized according to the OS ('/' vs '\')
			directoryArguments = pathArguments.stream() //
					.map(Paths::get) //
					.map(Path::toString) //
					.collect(Collectors.toList());
			directoryArguments.removeAll(fileArguments);
		}

		private Predicate<Path> allFilesAreJavaAndAreReadableAndWritable() {
			Predicate<Path> endsWithJava = p -> p.toFile().getName().endsWith(".java");
			Predicate<Path> isReadable = Files::isReadable;
			Predicate<Path> isWritable = Files::isWritable;
			return endsWithJava.and(isReadable).and(isWritable);
		}

		@Override
		public String toString() {
			return "ArgsValidator [pathArguments=" + pathArguments + ", options=" + options
					+ ", rulesToInclude=" + rulesToInclude + ", rulesToExclude=" + rulesToExclude
					+ ", ignoreTestFiles=" + ignoreTestFiles + ", directoryArguments="
					+ directoryArguments + ", fileArguments=" + fileArguments + "]";
		}

	}

}
