/*
 *    TextProcessor.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util.text;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * This abstract class provides a framework of what is expected from a TextProcessor; a class that 
 * performs modifications on strings to facilitate matching between them. Conservative modifications 
 * makes few changes to the string, sacrificing recall for precision.
 * Aggressive modifications makes significant changes to the string, sacrificing precision for recall.
 * 
 * @author David Milne
 */
public abstract class TextProcessor {
	 /**
     * Maximum length for a TextProcessor name.
     */
    protected static final int MAX_ID_LENGTH=40;
	
	/**
	 * Returns a string that identifies this TextProcessor. It should be human-readable and describe what this
	 * processor does, with enough detail to distinguish it from other TextProcessors. The default is to return the
	 * name of the class.
	 * 
	 * @return	the name of this TextProcessor. 
	 */
	public String getName() {
		return this.getClass().getSimpleName() ;
	}
	
	/**
	 * Returns the modified copy of the argument text
	 * 
	 * @param text	the text to be processed.
	 * @return	the processed version of this text.
	 */
	public abstract String processText(final String text) ;
	
	
	/**
     * Ancillary function to encode parameters.
     * This should be used in toString() methods.
     *
     * @param aText the string to encode.
     * @return the encoded string.
     */
    protected static String encodeParam(String aText) {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(aText);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                result.append("&lt;");
            } else if (character == '>') {
                result.append("&gt;");
            } else if (character == '&') {
                result.append("&amp;");
            } else {
                //the char is not a special one
                //add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    /**
     * Ancillary function to decode parameters.
     * This should be used to decode parameters encoded by encodeParam().
     *
     * @param aText the string to decode.
     * @return the decoded string.
     */
    protected static String decodeParam(String aText) {
        return aText.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    /**
     * Create a TextProcessor from a textual description.
     * The generic TextProcessor description is of the form "TextProcessor.className&lt;paramType&lt;encodedParam".
     * Currently supported paramType are "file" and "string". When paramType is file a constructor with a single File
     * argument is used. When paramType is a string a constructor with two parameters (a File and a String) is used,
     * if available.
     *
     * @param base_path relative paths are assumed to be relative to this directory.
     * @param description the textual description of the text processor.
     * @return a new TextProcessor.
     *
     * @throws java.lang.InstantiationException
     */
    public static TextProcessor fromString(File base_path, String description) throws InstantiationException {
        TextProcessor my_tp = null;
        String tokens[] = description.split("<", 3);
        if (tokens.length > 0) {
            try {
                Class textProcessorClass = Class.forName(tokens[0]);
                if (tokens.length == 3) {
                    tokens[2] = TextProcessor.decodeParam(tokens[2]);
                    if (tokens[1].equals("file")) {
                        Constructor textProcessorConstructor = textProcessorClass.getConstructor(File.class);
                        File my_file = new File(tokens[2]);
                        if (!my_file.isAbsolute()) {
                            my_file = new File(base_path, tokens[2]);
                        }
                        my_tp = (TextProcessor) textProcessorConstructor.newInstance(new Object[]{my_file});
                    } else {
                        try {
                            Constructor textProcessorConstructor = textProcessorClass.getConstructor(File.class, String.class);
                            my_tp = (TextProcessor) textProcessorConstructor.newInstance(new Object[]{base_path, tokens[2]});
                        } catch (NoSuchMethodException ex) {
                            Constructor textProcessorConstructor = textProcessorClass.getConstructor(String.class);
                            my_tp = (TextProcessor) textProcessorConstructor.newInstance(new Object[]{tokens[2]});
                        }
                    }
                } else {
                    my_tp = (TextProcessor) textProcessorClass.newInstance();
                }
            } catch (Exception ex) {
                throw new InstantiationException("TextProcessor instantiation failed: " + ex.toString());
            }
        }

        return my_tp;
    }
	
}