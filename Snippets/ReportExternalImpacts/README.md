# Report External Impacts

This sample queries a configured set of collections in the DBB WebApp for files which have a dependency to a provided list of files or logical names.

It can be used in scenarios to understand cross application dependencies where a given file is referenced, while the generated report can be shared with the application consuming a shared component.

You can for example leverage this script, when an application publishes a shared copybook / submodule at a given time in the development lifecycle (e.q. during the deployment into a stage). 

Similar functionality is available through the build framework implemented via https://github.com/IBM/dbb-zappbuild/pull/128   

## Prerequisites
No concrete prerequisites. `ReportExternalImpacts.groovy` is a stand-alone script querying the DBB WebApp collections for logical files. Only assumption is that the consuming applications also store their dependency metadata in the DBB WebApp.  

## Report External Impacts Process

### Inputs

The script has different options to take input via the CLI to query the DBB WebApp for Logical Files

1. A comma-separated list of logical file names via `--logicalNames`, e.q. `EPSMTCOM,EPSMLIST`
2. A comma-separated list of relative or absolute file names via `--fileNames`, e.q. `MortgageApplication/copybook/epsmtcom.cpy`
3. A file containing a list of relative or absolute file names . `--filesList`, e.q. `changedFiles.txt` containing
```
App-EPSM/copybooks-public/epsmtcom.cpy
App-EPSM/cobol/epsmlist.cbl
App-EPSM/link/epsmlist.lnk
```

### Script configuration

The sample script allows to scope the query by providing
* a filter of collection names via property `reportExternalImpactsCollectionPatterns` to query only collections with a provided name pattern.
* a filter for the file extensions of input files `reportExternalImpactsAnalysisFileFilter`

### Outputs

For each collection in which logical files where found with a logical dependency, the script produces an output file.
The output file follows the naming convention `externalImpacts_<collectionName>.txt`.

The schema of the file contents is logicalName - logicalFile - collectionName - impacted by file provided as an input file.  

```
EPSCMORT 	 App-EPSC/cobol/epscmort.cbl 	 App-EPSC-Scenario4 	 impacted by 	 App-EPSM/copybooks-public/epsmtcom.cpy 
``

## Invocation 

```
groovyz ReportExternalImpacts.groovy --fileNames App-EPSM/copybooks-public/epsmtcom.cpy --outDir /var/dbb/extensions/ReportExternalImpacts/test1 --reportExternalImpactsConfigFile /var/dbb/extensions/ReportExternalImpacts/dependencyReport.properties --url https://10.3.20.96:10443/dbb --id ADMIN --pw ADMIN
** ReportExternalImpacts start at 20210811.094850.048
** Properties at startup:
   dbb.RepositoryClient.password -> ADMIN
   listFileExt -> txt
   reportExternalImpactsCollectionPatterns -> .*App.*C-Scenario4.*
   dbb.RepositoryClient.passwordFile ->
   dbb.RepositoryClient.userId -> ADMIN
   startTime -> 20210811.094850.048
   reportExternalImpactsAnalysisFileFilter -> **/*
   fileNames -> App-EPSM/copybooks-public/epsmtcom.cpy
   dbb.RepositoryClient.url -> https://xxxxx:10443/dbb
   outDir -> /var/dbb/extensions/ReportExternalImpacts/test1
** Repository client created for https://xxxxx:10443/dbb
   App-EPSM/copybooks-public/epsmtcom.cpy
** Reports located at /var/dbb/extensions/ReportExternalImpacts/test1
*** Writing report of external impacts to file /var/dbb/extensions/ReportExternalImpacts/test1/externalImpacts_App-EPSC-Scenario4.txt
*** Writing report of external impacts to file /var/dbb/extensions/ReportExternalImpacts/test1/externalImpacts_App-EPSC-Scenario4-outputs.txt
```

## Command Line Options Summary

```
 This script queries the DBB WebApp for impactedFiles

 usage: ReportExternalImpacts.groovy [options]
 
  -o,--outDir <outDir> 			Directory where to store the output records
  -p,--reportExternalImpactsConfigFile <configFile> Absolute path to a properties file containing the necessary configuration.
   
  -f,--filesList <filesList>		Absolute path to a file containing references to the files
  -l,--logicalNames <logicalName> Comma-separated list of logicalFiles
  -n,--fileNames <fileNames> 		Comma-separated list of file names
  
  -url,--url <arg> 				DBB repository URL
  -id,--id <arg>					DBB repository id
  -pw,--pw <arg> 					DBB repository password
  -pf,--pwFile <arg>  				Absolute path to file containing DBB password
  
  -h,--help						Prints this message

```
