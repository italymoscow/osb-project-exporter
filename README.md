# OSB Project Exporter

## Overview

The OSB Project Exporter is a utility that exports the given OSB project sources and their dependencies from the specified OSB environment into a given folder.
This tool is designed to facilitate the extraction and management of OSB project files.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Apache Maven 3.6.0 or higher

## Installation

1. Clone the repository:
   
        git clone https://github.com/yourusername/osb-project-exporter.git
        cd osb-project-exporter

2. Build the project using Maven:
   
        mvn clean install

## Usage
To run the OSB Project Exporter, use the following command:

    java -jar OsbPr.\OsbProjectExporter-jar-with-dependencies.jar [url userName password projectName [exportDir]]

## Parameters

- `url`: Required. WLS Admin host and port to connect to over t3 protocol. E.g. t3://localhost:7001.
- `userName`: Required. User name to connect to WLS Admin server.
- `password`: Required. User password to connect to WLS Admin server.
- `projectName`: Required. An OSB project name to be exported.
- `exportDir`: Optional. Path on the local machine to export to. Default: current directory.

### Example

    java -jar .\OsbProjectExporter-jar-with-dependencies.jar t3://localhost:7001 adminuser password MyProject /path/to/export

## Key Classes and Methods

`osbProjectExporter.OsbProjectExporter`
* **Description**: The main class that handles the export process.
* **Methods**:
  * `main(String[] args)`: Entry point of the application.
  * `parseArgs(String[] args)`: Parses the command-line arguments.

`osbProjectExporter.FileUtil`
* **Description**: Utility class for file operations.
* **Methods**:
  * `getXmlDocFromFile(File file)`: Retrieves an XML document from a file.
  * `processFilesInFolder(String folder)`: Processes files in a given folder.
  * `parseFile(File file)`: Parses a file as XML and handles CDATA content.

`osbProjectExporter.OsbUtil`
* **Description**: Handles the logic for exporting the OSB project.
* **Methods**:
  * `exportProject(String projectName, String exportDir)`: Exports the specified project to the given directory.
  * `connectToServer(String url, String userName, String password)`: Establishes a connection to the WLS Admin server.
  * `fetchProjectDependencies(String projectName)`: Retrieves the dependencies for the specified project.