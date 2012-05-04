<#-- To render the third-party file.
 Available context :
 - dependencyMap a collection of Map.Entry with
   key are dependencies (as a MavenProject) (from the maven project)
   values are licenses of each dependency (array of string)

 - licenseMap a collection of Map.Entry with
   key are licenses of each dependency (array of string)
   values are all dependencies using this license
-->
<#function artifactFormat p>
  <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
</#function>

<#if licenseMap?size == 0>
The project has no dependencies.
<#else>
List of third-party dependencies grouped by their license type.
  <#list licenseMap as e>
    <#assign license = e.getKey()/>
    <#assign projects = e.getValue()/>

${license}:

    <#list projects as project>
 * ${artifactFormat(project)}
    </#list>
  </#list>
</#if>
