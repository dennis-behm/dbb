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
import groovy.util.*
import java.nio.file.*

/**
 * Utilities to read, update or export existing ApplicationDescriptor from/to YAML 
 */

class ApplicationDescriptor {
	String application
	String description
	String owner
	ArrayList<Source> sources
}

class Source {
	String name
	String languageProcessor
	String fileExtension
	ArrayList<FileDef> files
}

class FileDef {
	String name
	String type
	String usage
}

/////// Test
//ApplicationDescriptor applicationDescriptor = readApplicationDescriptor(new File("/u/dbehm/componentization/applicationConfigurations/retirementCalculator.yaml"))
//println applicationDescriptor.application
//println applicationDescriptor.description
//println applicationDescriptor.sources.size()
//
//applicationDescriptor = appendFileDefinition(applicationDescriptor, "copy", "none", "COPY", "COPYBOOK", "PRIVATE")
//
//println applicationDescriptor.application
//println applicationDescriptor.description
//println applicationDescriptor.sources.size()
//
//writeApplicationDescriptor(new File("/u/dbehm/componentization/work/retirementCalculator.yaml"), applicationDescriptor)

/**
 * 
 * Reads an existing application descriptor YAML
 * returns an ApplicationDescriptor Object
 * 
 */
def readApplicationDescriptor(File yamlFile){
	// Internal objects
	def yamlSlurper = new groovy.yaml.YamlSlurper()
	def appConfiguration = new ApplicationDescriptor()
	ApplicationDescriptor appDescriptor = yamlSlurper.parse(yamlFile)
	return appDescriptor
}

/**
 * Write an ApplicationDescriptor Object into a YAML file
 */
def writeApplicationDescriptor(File yamlFile, ApplicationDescriptor appConfiguration){

	// if file exists?

	def yamlBuilder = new YamlBuilder()
	// build updated application descriptor

	yamlBuilder {
		application appConfiguration.application
		description appConfiguration.description
		owner appConfiguration.owner
		sources (appConfiguration.sources)
	}

	// debug
	// println yamlBuilder.toString()

	// write file
	yamlFile.withWriter() { writer ->
		writer.write(yamlBuilder.toString())
	}
}

/**
 * Method to update the Application Descriptor
 * 
 * Appends to an existing source sourceGroupName, if it exists.
 * If the sourceGroupName cannot be found, it creates a new sourceGroup
 * 
 */

def appendFileDefinition(ApplicationDescriptor appConfiguration, String sourceGroupName, String languageProcessor, String fileExtension, String name, String type, String usage){

	def sourceGroupRecord

	def fileRecord = new FileDef()
	fileRecord.name = name
	fileRecord.type = type
	fileRecord.usage = usage

	existingSourceGroup = appConfiguration.sources.find(){ source ->
		source.name == sourceGroupName
	}

	if (existingSourceGroup) { // append file record definition to existing sourceGroup
		sourceGroupRecord = existingSourceGroup
		sourceGroupRecord.files.add(fileRecord)

	}
	else {

		// create a new source group entry
		sourceGroupRecord = new Source()
		sourceGroupRecord.name = sourceGroupName
		sourceGroupRecord.languageProcessor = languageProcessor
		sourceGroupRecord.fileExtension = fileExtension

		sourceGroupRecord.files = new ArrayList<FileDef>()
		// append file record
		sourceGroupRecord.files.add(fileRecord)
		appConfiguration.sources.add(sourceGroupRecord)
	}

	return appConfiguration
}
