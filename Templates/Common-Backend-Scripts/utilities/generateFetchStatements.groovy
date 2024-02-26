@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.lang.GroovyShell
import groovy.cli.commons.*
import groovy.util.*
import java.nio.file.*


// script properties
@Field Properties props = new Properties()
// Parse arguments from command-line
parseArgs(args)


// Print parms
println("** Script configuration:")
props.each { k,v->
	println "   $k -> $v"
}

File descriptorFile = new File(props.applicationDescriptor)
File applicationConfigurationFile = new File(props.applicationConfigurations)

if (descriptorFile.exists() && applicationConfigurationFile.exists()) {

	def yamlSlurper = new groovy.yaml.YamlSlurper()

	// Parse the application descriptor and application configurations
	applicationDescriptor = yamlSlurper.parse(descriptorFile)
	applicationConfigurations = yamlSlurper.parse(applicationConfigurationFile)

	// Create the output file
	File fetchScript = new File(props.fetchScript)

	fetchScript.withWriter() { writer ->

		applicationDescriptor.dependencies.each { dependency ->

			println("** Generating instructions to fetch dependencies of application component ${dependency.name}")



			applicationDependency = applicationConfigurations.find { it ->
				it.application == dependency.name
			}

			version = dependency.version

			// assess application dependency
			if (applicationDependency) {

				if (dependency.type == "git") {
					// retrieve current prod state
					if (version.equals("latest")) {
						println("*  Retrieving production state of application ${applicationDependency.application}")
						version = applicationDependency.productionLevel
					}

					// retrieve git repoInfo
					repoInfo = applicationDependency.repositories.find {it ->
						it.name == "git"
					}

					url = repoInfo.path
					println("*  Instruction to fetch ${applicationDependency.application} from git")
					cmd = "gitClone.sh -w ${props.workspace} -r ${url} -b ${version}"
					println(cmd)
					writer.write("$cmd\n")
				}
				else {
					println("* Dependency Types other than git are not yet implemented.")
				}

			} else {
				println("!* Specified application dependency not documented in Application Configurations File (${props.applicationConfigurations})")
				System.exit(1)
			}

		}

		println("** Instructions written to ${props.fetchScript}")

	}

} else {
	println("The application descriptor file ${props.applicationDescriptor} could not be found. Process exits.")
	System.exit(1)
}


/**
 * Parse CLI config
 */
def parseArgs(String[] args) {

	String usage = 'generateFetchStatements.groovy [options]'

	def cli = new CliBuilder(usage:usage)
	// required sandbox options
	cli.a(longOpt:'applicationDescriptor', args:1, 'Absolute path to the application descriptor')
	cli.c(longOpt:'applicationConfigurations', args:1, 'Absolute path to the application configuration file')
	cli.w(longOpt:'workspace', args:1, 'Absolute path to the build workspace')
	cli.o(longOpt:'fetchScript', args:1, 'Absolute path to the script to generate')

	def opts = cli.parse(args)
	if (!opts) {
		System.exit(1)
	}

	if (opts.a) props.applicationDescriptor = opts.a
	if (opts.o) props.fetchScript = opts.o
	if (opts.c) props.applicationConfigurations = opts.c
	if (opts.w) props.workspace = opts.w

}