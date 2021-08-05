@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import groovy.time.*
import groovy.transform.*
import com.ibm.dbb.build.VersionInfo
import groovy.json.JsonSlurper

/**
 * This script creates a release branch with the particular contents of a feature.
 *
 * usage: git-cherrypick-feature.groovy [options]
 *
 * options:
 *  -w,--workDir <dir>            Absolute path to the DBB build output directory
 *  -h,--help                     Prints this message
 *  -p,--preview                  Preview, not executing buztool.sh
 *  -b,--baselineBranch			  Name of the basline branch to fork off	
 *  -g,--gitBranch				  Name of the git branch
 *  -r,--repositoryPath           RepoPath
 *  -u,--gitUrl					  GitURL of origin
 *  -v,--verbose					  Verbose Script output
 *
 * Version 1 - 2021-07-20
 */

// start create version
@Field def props = new Properties()

parseInput(args)

def startTime = new Date()
props.startTime = startTime.format("yyyyMMdd.hhmmss.mmm")
println("** Cherry Pick Commits start at $props.startTime")
println("** Properties at startup:")
props.each{k,v->
	println "   $k -> $v"
}

// read build report data
println("** Read build report data from $props.workDir/BuildReport.json")
def jsonOutputFile = new File("${props.workDir}/BuildReport.json")

if(!jsonOutputFile.exists()){
	println("** Build report data at $props.workDir/BuildReport.json not found")
	System.exit(1)
}

def buildReport= BuildReport.parse(new FileInputStream(jsonOutputFile))

// parse build report to find the build result meta info
def buildResult = buildReport.getRecords().findAll{it.getType()==DefaultRecordFactory.TYPE_BUILD_RESULT}[0];

println("** Initialize Git Environment for release branch for feature")

// Setup Git Environment
// Git clone always writes into stderr https://github.com/cli/cli/issues/2984
rc = runGitCommand("git -C $props.repositoryPath clone --branch $props.baselineBranch $props.gitUrl","Cloning into","fatal:")

def repoFolder
if (rc==0) repoFolder = getRepositoryFolder(props.repositoryPath)

if (rc==0) println(" * Create branch $props.gitBranch and check it out. ")
if (rc==0) rc = runGitCommand("git -C $props.repositoryPath/$repoFolder branch $props.gitBranch","","")
if (rc==0) rc = runGitCommand("git -C $props.repositoryPath/$repoFolder checkout $props.gitBranch","Switched to branch","error:")


if (rc==0){ println("** Find DBB.FeatureBuildInfo in the build report to obtain the Merge Commits")

	def featureBuildInfo = buildReport.getRecords().find{
		try {
			it.getType()==DefaultRecordFactory.TYPE_PROPERTIES && it.getId()=="DBB.FeatureBuildInfo"
		} catch (Exception e){}
	}

	// Sort Feature merge commits based on time and perform cherry pick
	if (featureBuildInfo!=null){
		new TreeMap<>(featureBuildInfo.getProperties()).each{

			// println("   ${it.key} -> ${it.value} ")
			(mergeCommit, mergeCommitMessage, mergeCommitChangedFiles) = it.value.toString().split("\\+\\+\\+")
			rc = runCherryPickingProcess("$props.repositoryPath/$repoFolder", mergeCommit, mergeCommitMessage, mergeCommitChangedFiles)
		}
	}

}


if (rc==0){
	println("** Push release branch for feature back to central git provider.")
	rc = runGitCommand("git -C $props.repositoryPath/$repoFolder push --set-upstream origin $props.gitBranch","new branch","error:")
}


if (rc!=0){
	println("! Preparation of Cherry-Picking Process failed. See log for more details.")
	System.exit(rc)	
} else {
	println("* Cherry-Picking Process completed.")
	
}



/***
 * Run git process
 * 
 */
def runGitCommand(String gitCmd, String successMsg, String errorMsg){
	if(props.verbose) println " ** runGitCommand $gitCmd"
	int returnCode = 0
	def StringBuffer gitResponse = new StringBuffer()
	def StringBuffer gitError = new StringBuffer()

	Process process = gitCmd.execute()
	process.waitForProcessOutput(gitResponse, gitError)

	//while git often writes by default into stderr check for successMsg +
	if (gitError && gitError.contains(errorMsg) && !gitError.contains(successMsg)) {
		returnCode = 1 // set error returncode
		println("*! Error executing Git command: $gitCmd")
		println("   Stderr: $gitError")
	} else{
		if(props.verbose) {
			println("   . Command successfully completed. ")
			println("      gitStdOut : $gitResponse ")
			println("      gitStdErr : $gitError ")
		}
	}

	return returnCode

}

/***
 * Get repository path
 *
 */
def getRepositoryFolder(String path){
	if(props.verbose) println " ** getting repositoryFolder"
	String cmd="ls $path"
	StringBuffer cmdResponse = new StringBuffer()
	StringBuffer cmdError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(cmdResponse, cmdError)
	if (cmdError) {
		println("*! Error obtainting RepositoryFolder")
	} else
	{
		if(props.verbose) {println("*   . Command successfully completed.")}
	}
	return cmdResponse.toString().trim()
}

/***
 * Perform cherryPicking Process 
 */
def runCherryPickingProcess(String gitDir, String mergeCommit, String mergeCommitMessage, String mergeCommitChangedFiles) {

	println " * gitCherryPick: \t $mergeCommit \t $mergeCommitMessage \t $mergeCommitChangedFiles"

	int returnCode = 0
	String cmd = "git -C $gitDir cherry-pick $mergeCommit -m 1"
	StringBuffer gitResponse = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitResponse, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error: $gitError")
		returnCode = 1
	} else
	{
		if(props.verbose) println("   . Command successfully completed. ($gitResponse, $gitError)")
	}

	return returnCode
}
/***
 * read commandline and populate script props
 */
def parseInput(String[] cliArgs){
	def cli = new CliBuilder(usage: "git-cherrypick-feature.groovy [options]")
	cli.w(longOpt:'workDir', args:1, argName:'dir', 'Absolute path to the DBB build output directory')
	cli.b(longOpt:'baselineBranch', args:1,'Name of the basline branch to fork off')
	cli.g(longOpt:'gitBranch', args:1,'Name of the git branch')
	cli.u(longOpt:'gitUrl', args:1,'Git Url')
	cli.r(longOpt:'repositoryPath', args:1,'RepoPath')
	cli.v(longOpt:'verbose', 'Flag to turn on script trace')
	cli.h(longOpt:'help', 'Prints this message')
	def opts = cli.parse(cliArgs)
	if (opts.h) { // if help option used, print usage and exit
		cli.usage()
		System.exit(0)
	}

	// set command line arguments
	if (opts.w) props.workDir = opts.w
	if (opts.g) props.gitBranch = opts.g
	if (opts.b) props.baselineBranch = opts.b
	if (opts.r) props.repositoryPath = opts.r
	if (opts.u) props.gitUrl = opts.u
	if (opts.v) props.verbose = true


	// validate required props
	try {
		assert props.workDir: "Missing required property build work directory"
		assert props.gitBranch: "Missing required property gitBranch"
		assert props.baselineBranch : "Missing required property baselineBranch"
	} catch (AssertionError e) {
		cli.usage()
		throw e
	}
}