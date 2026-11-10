@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%." -cp "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
