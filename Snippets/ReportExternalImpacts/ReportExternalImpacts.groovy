@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.build.*
import com.ibm.dbb.dependency.*
import groovy.transform.*
import java.util.regex.*
import java.nio.file.PathMatcher
import java.nio.file.Path
import java.nio.file.FileSystems
import groovy.json.JsonSlurper

/************************************************************************************
 * This script queries the DBB WebApp for impactedFiles
 *
 * usage: ReportExternalImpacts.groovy [options]
 * 
 *  -o,--outDir <outDir> 			Directory where to store the output records
 *  -p,--reportExternalImpactsConfigFile <configFile> Absolute path to a properties file containing the necessary configuration.
 *   
 *  -f,--filesList <filesList>		Absolute path to a file containing references to the files
 *  -l,--logicalNames <logicalName> Comma-separated list of logicalFiles
 *  -n,--fileNames <fileNames> 		Comma-separated list of file names
 *  
 *  -url,--url <arg> 				DBB repository URL
 *  -id,--id <arg>					DBB repository id
 *  -pw,--pw <arg> 					DBB repository password
 *  -pf,--pwFile <arg>  				Absolute path to file containing DBB password
 *  
 *  -h,--help						Prints this message
 */

@Field BuildProperties props = BuildProperties.getInstance()
@Field RepositoryClient repositoryClient

props = parseInput(args)

def startTime = new Date()

props.startTime = startTime.format("yyyyMMdd.hhmmss.mmm")
println("** ReportExternalImpacts start at $props.startTime")
println("** Properties at startup:")
props.each{k,v->
	println "   $k -> $v"
}

// Logon to DBB WebApp
repositoryClient = new RepositoryClient().forceSSLTrusted(true)
println "** Repository client created for ${props.getProperty('dbb.RepositoryClient.url')}"

Set<String> files = new HashSet<String>()

// add fileNames
if(props.fileNames){
	props.fileNames.split(',').each{ files.add(it) }
}

// add logicalNames
if(props.logicalNames){
	props.logicalNames.split(',').each{ files.add(it) }
}

// add files from filesList
if (props.filesList){
	File fileList = new File(props.filesList)
	fileList.readLines().each{ files.add(it) }
}
// add fileList
println "** About to inspect collections for logical files with below logical dependencies"
files.each{ println "   $it" }

// create the work directory (build output)
new File(props.outDir).mkdirs()
println("** Start to run queries. Reports located at ${props.outDir}")

reportExternalImpacts(repositoryClient, files)


def reportExternalImpacts(RepositoryClient repositoryClient, Set<String> changedFiles){
	// query collections to produce externalImpactList
	List<Pattern> collectionMatcherPatterns = createMatcherPatterns(props.reportExternalImpactsCollectionPatterns)

	// caluclated and collect external impacts

	repositoryClient.getAllCollections().each{ collection ->
		String cName = collection.getName()

		if(matchesPattern(cName,collectionMatcherPatterns)){ // find matching collection names
			if (cName != props.applicationCollectionName && cName != props.applicationOutputsCollectionName){

				def Set<String> externalImpactList = new HashSet<String>()

				changedFiles.each{ changedFile->

					List<PathMatcher> fileMatchers = createPathMatcherPattern(props.reportExternalImpactsAnalysisFileFilter)

					if(matches(changedFile, fileMatchers) || matchesPattern(changedFile, [Pattern.compile('^[a-zA-Z0-9]{0,8}$')])){

						String memberName = CopyToPDS.createMemberName(changedFile)
						def ldepFile = new LogicalDependency(memberName, null, null);
						// Simple resolution without recursive resolution because we don't deal with phyisical files
						def logicalFiles = repositoryClient.getAllLogicalFiles(cName, ldepFile);
						logicalFiles.each{ logicalFile ->
							def impactRecord = "${logicalFile.getLname()} \t ${logicalFile.getFile()} \t ${cName} \t impacted by \t $changedFile"
							//println("*** $impactRecord")
							externalImpactList.add(impactRecord)
						}
					}
				}
				// write impactedFiles per application to build workspace
				if (externalImpactList.size()!=0){
					String impactListFileLoc = "${props.outDir}/externalImpacts_${cName}.${props.listFileExt}"
					println("  Writing report of external impacts to file $impactListFileLoc")
					File impactListFile = new File(impactListFileLoc)
					String enc = props.logEncoding ?: 'IBM-1047'
					impactListFile.withWriter(enc) { writer ->
						externalImpactList.each { file ->
							// if (props.verbose) println file
							writer.write("$file\n")
						}
					}
				}
			}
		}
		else {
			//if (props.verbose) println("*** Analysis and reporting has been skipped due to script configuration (see configuration of property reportExternalImpactsAnalysisFileFilter)")
		}
	}
}

/**
 * create List of Regex Patterns
 */

def createMatcherPatterns(String property) {
	List<Pattern> patterns = new ArrayList<Pattern>()
	if (property) {
		property.split(',').each{ patternString ->
			Pattern pattern = Pattern.compile(patternString);
			patterns.add(pattern)
		}
	}
	return patterns
}

/**
 * createPathMatcherPattern
 * Generic method to build PathMatcher from a build property
 */

def createPathMatcherPattern(String property) {
	List<PathMatcher> pathMatchers = new ArrayList<PathMatcher>()
	if (property) {
		property.split(',').each{ filePattern ->
			if (!filePattern.startsWith('glob:') || !filePattern.startsWith('regex:'))
				filePattern = "glob:$filePattern"
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher(filePattern)
			pathMatchers.add(matcher)
		}
	}
	return pathMatchers
}

/**
 * Match a file against a list of pathMatchers
 */
def matches(String file, List<PathMatcher> pathMatchers) {
	def result = pathMatchers.any { matcher ->
		Path path = FileSystems.getDefault().getPath(file);
		if ( matcher.matches(path) )
		{
			return true
		}
	}
	return result
}

/**
 * match a String against a list of patterns
 */
def matchesPattern(String name, List<Pattern> patterns) {
	def result = patterns.any { pattern ->
		if (pattern.matcher(name).matches())
		{
			return true
		}
	}
	return result
}


/**
 * read cliArgs
 */
def parseInput(String[] cliArgs){
	def cli = new CliBuilder(usage: "ReportExternalImpacts.groovy [options]")

	cli.o(longOpt:'outDir', args:1, argName:'outDir','Directory where to store the output records')
	cli.p(longOpt:'reportExternalImpactsConfigFile', args:1, argName:'reportExternalImpactsConfigFile', 'Absolute path to a properties file containing the necessary script configuration.')

	cli.l(longOpt:'logicalNames', args:1, argName:'logicalName','Comma-separated list of logicalFiles')
	cli.n(longOpt:'fileNames', args:1, argName:'fileNames','Comma-separated list of file names')
	cli.f(longOpt:'filesList', args:1, argName:'filesList', 'Absolute path to a file containing references to the files')

	// web application credentials (overrides properties in dependencyReport.properties)
	cli.url(longOpt:'url', args:1, 'DBB repository URL')
	cli.id(longOpt:'id', args:1, 'DBB repository id')
	cli.pw(longOpt:'pw', args:1,  'DBB repository password')
	cli.pf(longOpt:'pwFile', args:1, 'Absolute path to file containing DBB password')

	cli.h(longOpt:'help', 'Prints this message')
	def opts = cli.parse(cliArgs)
	if (opts.h) { // if help option used, print usage and exit
		cli.usage()
		System.exit(0)
	}

	def props = new Properties()

	// set command line arguments
	if (opts.l) props.logicalNames = opts.l
	if (opts.n) props.fileNames = opts.n
	if (opts.f) props.filesList = opts.f
	if (opts.o) props.outDir = opts.o as String


	// Optional Artifactory to publish
	if (opts.reportExternalImpactsConfigFile){
		def propertyFile = new File(opts.reportExternalImpactsConfigFile)
		if (propertyFile.exists()){
			propertyFile.withInputStream { props.load(it) }
		}
	}

	// set DBB configuration properties
	if (opts.url) props.'dbb.RepositoryClient.url' = opts.url
	if (opts.id) props.'dbb.RepositoryClient.userId' = opts.id
	if (opts.pw) props.'dbb.RepositoryClient.password' = opts.pw
	if (opts.pf) props.'dbb.RepositoryClient.passwordFile' = opts.pf

	return props
}