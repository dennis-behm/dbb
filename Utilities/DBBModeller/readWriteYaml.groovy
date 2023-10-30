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
 * takes multiple yaml files as arguments and parses them and copies files to work directory
 *
 */

class ApplicationDescriptor {
	String application
	String description
	String owner
	ArrayList<SourceGroup> source
}

class SourceGroup {
	String name
	String languageProcessor
	ArrayList<FileDef> files
}

class FileDef {
	String name
	String type
	String usage
}

// Internal objects
def yamlSlurper = new groovy.yaml.YamlSlurper()

// Parse arguments
applicationDefinitions = parseArgs(args)


ybuilder = new YamlBuilder()

// application <> applicationConfiguration
Map<String, Map> applicationConfigurationsMap = new HashMap<String, Map>()

applicationDefinitions.each{ applicationDefinition ->
	println "* Parsing file \t: ${applicationDefinition}"

	File userProvidedAppConfiguration = new File("/u/dbehm/componentization/applicationConfigurations/${applicationDefinition}.yaml")
	File yamlUpdate = new File("/u/dbehm/componentization/work/${applicationDefinition}/${applicationDefinition}.yaml")
	
	def appConfiguration = new java.lang.Object()
	
	if (yamlUpdate.exists()) {
		appConfiguration = yamlSlurper.parse(yamlUpdate)
	}else {
		appConfiguration = yamlSlurper.parse(userProvidedAppConfiguration)
	}
			
	applicationDescriptor = new ApplicationDescriptor()
	applicationDescriptor.application = appConfiguration.application
	applicationDescriptor.description = appConfiguration.description
	applicationDescriptor.owner = appConfiguration.owner
	applicationDescriptor.source = new ArrayList<SourceGroup>()
	
	println "* Application \t: ${appConfiguration.application}"
	// add the existing SourceGroups
	appConfiguration.source.each { sourceGroup ->
		targetDir = "/u/dbehm/componentization/work/${appConfiguration.application}/${sourceGroup.name}/"
		
		println " ** Moving Source Code Type \"${sourceGroup.name}\" to target directory $targetDir"
		
		def ySourceGroup = new SourceGroup()
		ySourceGroup.name = sourceGroup.name
		ySourceGroup.languageProcessor = sourceGroup.languageProcessor
		ySourceGroup.files = new ArrayList<FileDef>()
		FileDef fileDef = new FileDef() 
		sourceGroup.files.each { member ->
			
			println "$member.name | $member.type"
			
			fileDef.name = member.name
			fileDef.type = member.type
			if (member.usage) fileDef.usage = member.usage
			ySourceGroup.files.add(fileDef)
			
			
			
			Path source = Paths.get("/u/dbehm/componentization/codeBase/${sourceGroup.name}/", "${member.name}.cbl");
			Path target = Paths.get("${targetDir}","${member.name}.cbl")
			if (!(new File(targetDir).exists())) new File(targetDir).mkdirs()

			if (source.toFile().exists()) {
				println "\t Moving ${source.toString()}"
				//Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		applicationDescriptor.source.add(ySourceGroup)
				
	}
	
	//applicationConfigurationsMap.put(appConfiguration.application, applicationDescriptor)
	
	
	// Append a new/update sourceGroup
	
	sourceGroupCopybooks = new SourceGroup()
	sourceGroupCopybooks.name = "copy"
	
	existingSourceGroup = applicationDescriptor.source.find(){ sourceGroup ->   
	 sourceGroup.name == "copy"	
	}
	if (existingSourceGroup) sourceGroupCopybooks = existingSourceGroup
	
	
	
	//sourceGroupCopybooks.languageProcessor = "none"
	sourceGroupCopybooks.files = new ArrayList<FileDef>()
	
	def copyFile = new FileDef()
	copyFile.name = "copy1"
	copyFile.type = "COPYBOOK"
	copyFile.usage = "private"
	
	sourceGroupCopybooks.files.add(copyFile)
	
	applicationDescriptor.source.add(sourceGroupCopybooks)
	
	// build updated application descriptor
	ybuilder(applicationDescriptor)
	println ybuilder.toString()
	
	// update file
	yamlUpdate.withWriter() { writer ->
		writer.write(ybuilder.toString())
	}
	
}



def parseArgs(String[] args){
	// add validation etc.
	return args
}

def polulateWorkingDirs() {
	
}


