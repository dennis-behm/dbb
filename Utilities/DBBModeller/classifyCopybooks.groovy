@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import groovy.yaml.YamlSlurper
import groovy.lang.GroovyShell
import groovy.util.*
import java.nio.file.*
import groovy.cli.commons.*


@Field BuildProperties props = BuildProperties.getInstance()
@Field MetadataStore metadataStore
@Field def appDescriptorUtils = loadScript(new File("utils/applicationDescriptorUtils.groovy"))

///**
// * Internal Classes to write objects back to a new application - YAML
// */
//class ApplicationDescriptor {
//	String application
//	String description
//	String owner
//	ArrayList<Source> sources
//}
//
//class SourceGroup {
//	String name
//	String languageProcessor
//	ArrayList<FileDef> files
//}
//
//class FileDef {
//	String name
//	String type
//	String usage
//}


/**
 * Internal variables
 */

// application <> applicationConfiguration
Map<String, Map> applicationConfigurationsMap = new HashMap<String, Map>()

/**
 * Processing logic
 */

println ("** Classification Process started. ")

// Initialization
parseArgs(args)
initScriptParameters()

// Print parms
println("** Script configuration:")
props.each { k,v->
	println "   $k -> $v"
}

// create metadatstore
metadataStore = MetadataStoreFactory.createFileMetadataStore("${props.workspace}/.dbb")
println "** File MetadataStore initialized at ${props.workspace}/.dbb"

println ("** Get list of files. ")
Set<String> appFiles = getFileList()

println ("** Analyze impacted applications. ")
assessImpactedFiles(appFiles)


/** Methods **/

/**
 * Get list relative files in the application directory
 */
def getFileList() {
	Set<String> fileSet = new HashSet<String>()

	Files.walk(Paths.get(props.applicationDir)).forEach { filePath ->
		if (Files.isRegularFile(filePath)) {
			relFile = relativizePath(filePath.toString())
			fileSet.add(relFile)
		}
	}

	return fileSet
}

/**
 * 
 */
def assessImpactedFiles(appFiles) {

	appFiles.each { file ->
		Set<String> referencingCollections = new HashSet<String>()

		// Obtain impacts
		println ("** Analyze impacted applications for file $file. ")
		def impactedFiles = findImpactedFiles(props.copybookImpactSearch, file)
		println "  Files depending on $file :"
		impactedFiles.each { impact ->
			println "    ${impact.getFile()} \t in application context ${impact.getCollection().getName()}"
			referencingCollections.add(impact.getCollection().getName())
		}

		// Asses Unique application usage
		if (referencingCollections.size() == 1) {
			println "    ==> $file is owned by application ${referencingCollections[0]} "
			// append to YAML

			def movedCopybook
			
			// TODO: determine the segment in target directory
			if (props.copySharedCopybooks) 
				movedCopybook = copyMemberToApplicationFolder(file, "${referencingCollections[0]}/${props.defaultCopybookFolderName}/")

			// If update flag is set
			if (props.generateUpdatedApplicationConfiguration) {

				File userAppConfigurationYaml = new File("${props.inputConfigurations}/${referencingCollections[0]}.yaml")
				File updateAppConfigurationYaml = new File("${props.workspace}/${referencingCollections[0]}/${referencingCollections[0]}.yaml")
				
				// determine which YAML file to use
				def appConfiguration

				if (updateAppConfigurationYaml.exists()) { // update
					appConfiguration = appDescriptorUtils.readApplicationDescriptor(updateAppConfigurationYaml)
				}else { // use application yaml provided by user
					appConfiguration = appDescriptorUtils.readApplicationDescriptor(userAppConfigurationYaml)
				}
				
				// append definition
				Path pFile = Paths.get(file)
				memberName = pFile.getFileName().toString().substring(0, pFile.getFileName().toString().indexOf("."))
				
				appConfiguration = appDescriptorUtils.appendFileDefinition(appConfiguration, props.defaultCopybookFolderName, "none", props.defaultCopybookFileExtension, memberName, "COPYBOOK", "PRIVATE")

				// update YAML file
				println "        Adding private copybook $movedCopybook to application descriptor " + updateAppConfigurationYaml.getPath()
				appDescriptorUtils.writeApplicationDescriptor(updateAppConfigurationYaml, appConfiguration)
				
			}

		} else if (referencingCollections.size() > 1) {
			println "    ==> $file referenced by multiple applications. $referencingCollections "
			
			// document as a external dependency, if update flag is set
			if (props.generateUpdatedApplicationConfiguration && props.application != "Unclassified-Copybooks") {

				File userAppConfigurationYaml = new File("${props.inputConfigurations}/${props.application}.yaml")
				File updateAppConfigurationYaml = new File("${props.workspace}/${props.application}/${props.application}.yaml")
				
				// determine which YAML file to use
				def appConfiguration

				if (updateAppConfigurationYaml.exists()) { // update
					appConfiguration = appDescriptorUtils.readApplicationDescriptor(updateAppConfigurationYaml)
				}else { // use application yaml provided by user
					appConfiguration = appDescriptorUtils.readApplicationDescriptor(userAppConfigurationYaml)
				}
				
				// append definition
				Path pFile = Paths.get(file)
				memberName = pFile.getFileName().toString().substring(0, pFile.getFileName().toString().indexOf("."))
				// update YAML file
				println "        Adding public copybook $file to application descriptor " + updateAppConfigurationYaml.getPath()
				appDescriptorUtils.appendFileDefinition(appConfiguration, props.defaultCopybookFolderName, "none", props.defaultCopybookFileExtension, memberName, "COPYBOOK", "PUBLIC")			
				appDescriptorUtils.writeApplicationDescriptor(updateAppConfigurationYaml, appConfiguration)
				
			}

		} else {
			println "\t No references found for include file $file."
		}
	}

}


/**
 * Parse CLI config
 */
def parseArgs(String[] args) {

	String usage = 'scanApplication.groovy [options]'

	def cli = new CliBuilder(usage:usage)
	// required sandbox options
	cli.w(longOpt:'workspace', args:1, 'Absolute path to workspace (root) directory containing all required source directories')
	cli.a(longOpt:'application', args:1, required:true, 'Application  name.')
	cli.c(longOpt:'configurations', args:1, required:true, 'Path of application configuration Yaml files.')
	cli.cf(longOpt:'copySharedCopybooks', args:0, 'Flag to indicate if shared copybooks should be moved.')
	cli.g(longOpt:'generateUpdatedApplicationConfiguration', args:0, 'Flag to generate updated application configuration file.')


	def opts = cli.parse(args)
	if (!opts) {
		System.exit(1)
	}

	if (opts.w) props.workspace = opts.w
	if (opts.a) props.application = opts.a
	if (opts.c) props.inputConfigurations = opts.c
	if (opts.cf) props.copySharedCopybooks = "true"
	if (opts.g) props.generateUpdatedApplicationConfiguration = "true"

}

/*
 * findImpactedFiles -
 *  method to configure and invoke SearchPathImpactFinder
 *
 *  @return list of impacted files
 *
 */
def findImpactedFiles(String impactSearch, String file) {

	List<String> collections = new ArrayList<String>()
	metadataStore.getCollections().each{ collection ->
		collections.add(collection.getName())
	}

	println ("*** Creating SearchPathImpactFinder with collections " + collections + " and impactSearch configuration " + impactSearch)

	def finder = new SearchPathImpactFinder(impactSearch, collections)

	// Find all files impacted by the changed file
	impacts = finder.findImpactedFiles(file, props.workspace)
	return impacts
}

/**
 * Copyies a relateive source member to the relative target directory.
 *  
 */
def copyMemberToApplicationFolder(String src, String trgtDir) {

	Path source = Paths.get("${props.workspace}", src);
	targetFile = source.getFileName().toString()
	targetDir = "${props.workspace}/${trgtDir}"
	if (!(new File(targetDir).exists())) new File(targetDir).mkdirs()

	Path target = Paths.get(targetDir, targetFile);

	if (source.toFile().exists() && source.toString() != target.toString()) {
		println "        Moving ${source.toString()} to ${target.toString()}"
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		return target.toString()
	}
	
	return null
}

/* 
 * init additional parameters
 */
def initScriptParameters() {
	// Settings

	String applicationFolder = "${props.workspace}/${props.application}"

	// application folder
	if (new File(applicationFolder).exists()){
		props.applicationDir = applicationFolder
	} else {
		println ("!* Application Diretory $applicationFolder does not exists.")
		System.exit(1)
	}

	// TODO: Customize here
	props.defaultCopybookFileExtension = "cpy"
	props.defaultCopybookFolderName = "copy"
	
	// searchpath
	props.copybookImpactSearch = "search:${props.workspace}/?path=${props.application}/*.cpy;*/copy/*.cpy" as String
}

/*
 * relativizePath - converts an absolute path to a relative path from the workspace directory
 */
def relativizePath(String path) {
	if (!path.startsWith('/'))
		return path
	String relPath = new File(props.workspace).toURI().relativize(new File(path.trim()).toURI()).getPath()
	// Directories have '/' added to the end.  Lets remove it.
	if (relPath.endsWith('/'))
		relPath = relPath.take(relPath.length()-1)
	return relPath
}

