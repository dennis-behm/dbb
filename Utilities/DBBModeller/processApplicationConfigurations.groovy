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

/**
 * takes multiple yaml files as arguments and parses them and copies files to work directory
 * 
 */



// Internal objects
def yamlSlurper = new groovy.yaml.YamlSlurper()
def applicationDefinition=""
def sourceDir=""
def targetDir=""


sourceDir = args[0]
targetDir = args[1]
applicationDefinition = args[2]

println "* Parsing file \t: ${applicationDefinition}"
println "* Dispatching files from ${sourceDir} to ${targetDir}"
appConfiguration = yamlSlurper.parse(new File(applicationDefinition))
println "* Application \t: ${appConfiguration.application}"
appConfiguration.source.each { sourceGroup ->
	targetApplicationDir = "${targetDir}/${appConfiguration.application}/${sourceGroup.name}/"
	
	println " ** Moving Source Code Type \"${sourceGroup.name}\" to target directory $targetApplicationDir"
	sourceGroup.files.each { member ->
		
		Path source = Paths.get("${sourceDir}/${sourceGroup.name}/", "${member.name}.cbl");
		Path target = Paths.get("${targetApplicationDir}","${member.name}.cbl")
		if (!(new File(targetApplicationDir).exists())) new File(targetApplicationDir).mkdirs()
			if (source.toFile().exists()) {
			println "\t Moving ${source.toString()} to ${targetApplicationDir}"
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}