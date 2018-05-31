package runner;

import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.shell.ShellEvaluatorFactory;
import org.rascalmpl.uri.URIUtil;

import io.usethesource.vallang.ISourceLocation;

public class Runner {

	private static final String MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE = "rjtl::MutableMembersUsage";

	public static void main(String[] args) throws URISyntaxException {
		Evaluator evaluator = ShellEvaluatorFactory.getDefaultEvaluator(new PrintWriter(System.out),
				new PrintWriter(System.err));

		evaluator.doImport(evaluator.getMonitor(), MUTABLE_MEMBERS_USAGE_REFACTORER_MODULE);

		Path fileLocation = Paths.get(args[0]);
		ISourceLocation fileLoc = URIUtil.createFileLocation(fileLocation.toString());
		evaluator.call("refactorMutableGettersAndSettersViolations", fileLoc);

	}

}
