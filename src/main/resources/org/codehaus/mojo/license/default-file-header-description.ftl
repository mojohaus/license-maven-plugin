<#--
  #%L
  License Maven Plugin
  %%
  Copyright (C) 2012 Codehaus, Tony Chemit
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Lesser Public License for more details.

  You should have received a copy of the GNU General Lesser Public
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/lgpl-3.0.html>.
  #L%
  -->
<#-- To render the description of a license file header.
 Available context :
 - project the maven project
 - addSvnKeyWords
 - organizationName
 - projectName
 - inceptionYear
 - file current file to treat
-->
${projectName}
<#if addSvnKeyWords>
<#--Add svn Keywords-->
${"|Id:|"?replace("|", "$")}
${"|HeadURL:|"?replace("|", "$")}</#if>