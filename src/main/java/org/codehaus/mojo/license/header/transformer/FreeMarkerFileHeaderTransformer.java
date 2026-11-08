package org.codehaus.mojo.license.header.transformer;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link FileHeaderTransformer} for freemarker format.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Named("ftl")
@Singleton
public class FreeMarkerFileHeaderTransformer extends AbstractFileHeaderTransformer {

    /**
     * The {@code ftl} directive in angle bracket or square bracket syntax, which FreeMarker requires to be the first
     * element of a template.
     */
    private static final Pattern FTL_DIRECTIVE =
            Pattern.compile("\\A\\s*(?:<#ftl(?:\\s[^>]*)?>|\\[#ftl(?:\\s[^\\]]*)?\\])");

    private static final String SQUARE_COMMENT_START_TAG = "[#--";

    private static final String SQUARE_COMMENT_END_TAG = "--]";

    /**
     * Default constructor.
     */
    public FreeMarkerFileHeaderTransformer() {
        super("ftl", "header transformer with free marker comment style", "<#--", "-->", " ");
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDefaultAcceptedExtensions() {
        return new String[] {"ftl"};
    }

    /**
     * Adds the header after the {@code ftl} directive when the template starts with one, as FreeMarker does not
     * accept anything before it. In a template using the square bracket syntax the header is written with square
     * bracket comment delimiters.
     */
    @Override
    public String addHeader(String header, String content) {
        final Matcher matcher = FTL_DIRECTIVE.matcher(content);
        if (!matcher.find()) {
            return super.addHeader(header, content);
        }
        final String directive = matcher.group();
        if (directive.trim().startsWith("[")) {
            header = toSquareBracketComment(header);
        }
        return super.addHeader(directive + getLineSeparator() + header, content.substring(directive.length()));
    }

    /**
     * Reads a header written with square bracket comment delimiters.
     */
    @Override
    public String unboxComent(String header) {
        return super.unboxComent(header.replace(SQUARE_COMMENT_START_TAG, getCommentStartTag())
                .replace(SQUARE_COMMENT_END_TAG, getCommentEndTag()));
    }

    private String toSquareBracketComment(String header) {
        final int start = header.indexOf(getCommentStartTag());
        final int end = header.lastIndexOf(getCommentEndTag());
        if (start < 0 || end < start) {
            return header;
        }
        return header.substring(0, start)
                + SQUARE_COMMENT_START_TAG
                + header.substring(start + getCommentStartTag().length(), end)
                + SQUARE_COMMENT_END_TAG
                + header.substring(end + getCommentEndTag().length());
    }
}
