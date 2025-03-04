<h1 align="center">
  <a href="https://github.com/orangebeard-io/burp-extension">
    <img src="https://raw.githubusercontent.com/orangebeard-io/burp-extension/main/.github/logo.svg" alt="Orangebeard.io Burp Suite Pro Extension" height="200">
  </a>
  <br>Orangebeard.io Burp Suite Pro Extension<br>
</h1>

<h4 align="center">Burp Suite Pro Extension to report Audit Issues to Orangebeard</h4>

<p align="center">
  <a href="https://repo.maven.apache.org/maven2/io/orangebeard/burp-extension/">
    <img src="https://img.shields.io/maven-central/v/io.orangebeard/burp-extension?style=flat-square"
      alt="MVN Version" />
  </a>
  <a href="https://github.com/orangebeard-io/burp-extension/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/orangebeard-io/burp-extension/main.yml?branch=main&style=flat-square"
      alt="Build Status" />
  </a>
  <a href="https://github.com/orangebeard-io/burp-extension/blob/main/LICENSE.txt">
    <img src="https://img.shields.io/github/license/orangebeard-io/burp-extension?style=flat-square"
      alt="License" />
  </a>
</p>

<div align="center">
  <h4>
    <a href="https://orangebeard.io">Orangebeard</a> |
    <a href="#installation">Installation</a> |
    <a href="#usage">Usage</a>
  </h4>
</div>

## Installation
Build the jar-with-dependencies (`mvn clean package`) or download it from the releases page.  
Open your burp suite project, navigate to the extensions page and add the Jar as an extension. Make sure it
automatically activates on startup.

## Usage
This extension is meant to report scans from a CI pipeline, thus through CLI execution.  
The extension will only register itself when a system property `orangebeard` is set with value `true`.  
  
By default, the extension will report a scan started as soon as the extension registers it's handlers. This occurs on startup.
The extension will mark the scan finished when the extension unloads (usually on exit of Burp Suite).
  
### Usage example:  
  
```shell
java  -Dorangebeard=true \
-jar burpsuite_pro.jar -Xmx4g \ 
--project-file 
```
  
The Orangebeard configuration is read from orangebeard.json, environment variables or system properties. For
more information, see the Java client documentation.  

### Orangebeard.json example: 
```json
{
	"endpoint": "https://my.orangebeard.app",
	"token": "listener-api-token-uuid",
	"project": "my-project",
	"testset": "Burp Pro Scan",
	"description": "A run from Burp Suite",
	"attributes": [
		{
			"key": "Key",
			"value": "Value"
		},
		{
			"value": "Tag value"
		}
	]
}

```
### Orangebeard system properties example:
```shell
  -Dorangebeard.endpoint=https://my.orangebeard.app
  -Dorangebeard.project=my-project
  -Dorangebeard.testset="Burp Pro Scan"
  -Dorangebeard.token=listener-api-token
  -Dorangebeard.description="description"
  -Dorangebeard.attributes=key:val;key2:val2;tag
```
  
Make sure to add any system properties *before* the `-jar` argument  
When a property is present in both json and system property, the system property value will overwrite the json file's value.