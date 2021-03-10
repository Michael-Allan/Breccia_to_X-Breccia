package Breccia.XML.translator;


/** Extended XML ‘stream constants’ for the translator.  This extension reserves constants in the range
  * 174,350,000 to 174,358,467; user defined constants should be outside that range.
  *
  *     @see javax.xml.stream.XMLStreamConstants
  */
public interface XStreamContants {


    /** Nothing, no markup to translate.  Occurs on attempting to translate an empty source of markup.
      * This is both an initial and final state.
      *
      *     @see Breccia.parser.Empty
      */
    public static final int EMPTY = 174_358_466;


    /** A formal state of error.  This is a final state, rendering the translator unusable
      * for the present markup source.  It results from any occurence of a parse error
      * in the markup source, or detection there of an invalid state.
      *
      *     @see ParseError
      *     @see Breccia.parser.Error
      */
    public static final int ERROR = 174_358_467; }



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
