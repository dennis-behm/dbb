# dbb-modeller (prototype)

A  utility that is leveraging IBM DBB to dispatch claimed source code and classifies unowned copybooks.

## purpose

This utility is helping users that have no information about the owner for perceived shared copybooks. It provides a preview about the layouts of applications and dispatches shared copybooks that are only referenced by one application into the application folder.

## scripts

A mixture of shell and groovy scripts that cover the 4 phases:

1. Migrate codebase from PDS libraries to a work directory in Unix System Services 
1. Analyze the provided application descriptors which claim ownership of source code and move it to a work directory
1. Scanning the work directories
1. Analyze the perceived shared copybooks and dispatch them to the application directories if they are only used by this application.

Review reach script before executing it. Find more details in the below table.

script | description
------ | -------
[1_migrateCodebase.sh](1_migrateCodebase.sh) | script to migrate existing codebase using the DBB Migration utilities to a temporary directory `../work_codebase` in USS.
[2_populateTempWorkDirectory.sh](2_populateTempWorkDirectory.sh) | script parsing the application descriptors that reside in a `applicationConfigurations` sitting next to the dbb-modeller. A sample of the application configuration yaml is provided in the [samples directory](samples/). It document the source codes that is owned by an application. The script moves the code that is claimed through the application configurations to the `../work_applications` directory. A directory per application is created. Unowned copybooks are copied into the directory `work_applications/Unclassified-Copybooks`
[3_scanContexts.sh](3_scanContexts.sh) | script that inspects the directories in `work_applications`. The contents of each directory is scanned with the DBB scanners and stored within a dedicated collection in the DBB Metadatastore (scripts leverage the DBB File Metadatastore), that is created at `work_applications/.dbb`.
[4_classifySharedCopybooks](4_classifySharedCopybooks.sh) | script to loop through the unowned copybooks in `Unclassified-Copybooks` folder in `work_applications` and queries the DBB Metadatastore for files referencing it. if an application is exclusively using the copybook, the copybook is moved into the application folder and is classified as a **"private"** copybook. 

## setup

Scripts contain some hard-coded naming conventions. 
Pull the scripts into a working directory and provide your application configurations in the `applicationConfigurations` directory.
The component-modeller is a copy of the scripts provided by this repo

### before execution

```
workdirectory
|_applicationConfigurations # containing the application configurations
|_component-modeller # the above scripts
```

### after execution

```
workdirectory
|_applicationConfigurations # containing the application configurations
|_component-modeller # the above scripts
|_work_codebase
|_work_applications
```



