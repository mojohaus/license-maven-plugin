<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Output method set to html for HTML table -->
    <xsl:output method="html" encoding="UTF-8" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
                doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>

    <xsl:template match="/*//*[1]">
        <link rel="stylesheet" type="text/css" href="../aggregate-download-licenses-shared/sortBy.css"/>
    </xsl:template>

    <xsl:template match="/">
        <html>
            <body>
                <h2>Dependency Infos</h2>
                <table border="1">
                    <tr>
                        <th>Name</th>
                        <th>Group ID</th>
                        <th>Artifact ID</th>
                        <th>Version</th>
                        <th>Licenses</th>
                    </tr>
                    <xsl:apply-templates select="/dependencyInfos/dependencyInfos"/>
                </table>
                <script src="../aggregate-download-licenses-shared/sortBy.js"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="dependencyInfos">
        <tr>
            <xsl:call-template name="create-cell">
                <xsl:with-param name="value" select="@name"/>
            </xsl:call-template>

            <xsl:call-template name="create-cell">
                <xsl:with-param name="value" select="@groupId"/>
            </xsl:call-template>

            <xsl:call-template name="create-cell">
                <xsl:with-param name="value" select="@artifactId"/>
            </xsl:call-template>

            <xsl:call-template name="create-cell">
                <xsl:with-param name="value" select="@version"/>
            </xsl:call-template>

            <td>
                <table border="1">
                    <xsl:for-each select="licenses">
                        <tr>
                            <td>
                                <xsl:value-of select="."/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </td>
        </tr>
    </xsl:template>

    <xsl:template name="create-cell">
        <xsl:param name="value"/>
        <td>
            <xsl:choose>
                <xsl:when test="$value != ''">
                    <xsl:value-of select="$value"/>
                </xsl:when>
                <xsl:otherwise>
                    N/A
                </xsl:otherwise>
            </xsl:choose>
        </td>
    </xsl:template>

</xsl:stylesheet>
