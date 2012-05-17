<#-- To render the description of a license file header.
 Available context :
 - project the maven project
 - addSvnKeyWords
 - organizationName
 - projectName
 - inceptionYear
 - file current file to treat
-->
${file.name} - ${projectName} - ${organizationName} - ${inceptionYear}
${project.groupId}-${project.artifactId}-${project.version}
<#if addSvnKeyWords>
<#--Add svn Keywords-->
${"|Id:|"?replace("|", "$")}
${"|HeadURL:|"?replace("|", "$")}</#if>
