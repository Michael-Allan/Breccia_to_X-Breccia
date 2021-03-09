package Breccia.XML.translator;

import Breccia.parser.*;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import Java.Unhandled;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import static Breccia.parser.Project.newSourceReader;
import static Breccia.parser.ParseState.Symmetry.*;
import static Breccia.parser.Typestamp.*;


/** A reusable translator of Breccia to X-Breccia.  This translator supports extensions of Breccia
  * that model their extended fractal states as instances of `Fractum` and `FractumEnd`,
  * and extended terminal states (if any) as instances of `FileFractum` and `FileFractumEnd`.
  */
public class BrecciaXCursor implements ReusableCursor, XMLStreamReader, XStreamContants {


    /** @see #sourceCursor()
      */
    public BrecciaXCursor( BrecciaCursor sourceCursor ) { this.sourceCursor = sourceCursor; }



    /** Translates the given source file, feeding each state of the translation to `sink` till all
      * are exhausted.  Calling this method will abort any translation already in progress.
      */
    public void perState( final Path sourceFile, final IntConsumer sink ) throws ParseError {
        try( final Reader source = newSourceReader​( sourceFile )) {
            markupSource( source );
            for( ;; ) {
                sink.accept( eventType );
                if( !hasNext() ) break;
                try { next(); }
                catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}
        catch( IOException x ) { throw new Unhandled( x ); }}



    /** Translates the given source file, feeding each state of the translation to `sink` till either
      * all are exhausted or `sink` returns false.  Calling this method will abort any translation
      * already in progress.
      */
    public void perStateConditionally( final Path sourceFile, final IntPredicate sink )
          throws ParseError {
        try( final Reader source = newSourceReader​( sourceFile )) {
            markupSource( source );
            while( sink.test(eventType) && hasNext() ) {
                try { next(); }
                catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}
        catch( IOException x ) { throw new Unhandled( x ); }}



    /** The source cursor to use during translations.
      * Between translations, it may be used for other purposes.
      */
    public BrecciaCursor sourceCursor() { return sourceCursor; }



   // ━━━  R e u s a b l e   C u r s o r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** {@inheritDoc}  Sets the translation state either to `{@linkplain #START_DOCUMENT START_DOCUMENT}`
      * or to `{@linkplain #EMPTY EMPTY}`.
      *
      *     @param r {@inheritDoc}  It is taken to comprise a single file at most.
      */
    public @Override void markupSource( final Reader r ) throws ParseError {
        sourceCursor.markupSource( r );
        final ParseState initialParseState = sourceCursor.state();
        if( initialParseState.typestamp() == empty ) {
            eventType = EMPTY;
            hasNext = false; }
        else { // TODO here: If this file fractum has a head, then translate it as for those at `next`.
            assert initialParseState instanceof FileFractum;
            eventType = START_DOCUMENT;
            hasNext = true; }}



   // ━━━  X M L   S t r e a m   R e a d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** Does nothing, this cursor maintains no resource that needs freeing.
      */
    public @Override void close() {}



    public @Override int getAttributeCount() { throw new UnsupportedOperationException(); }



    public @Override String getAttributeLocalName( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override QName getAttributeName( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributePrefix( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override String getAttributeNamespace( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override String getAttributeType( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributeValue( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributeValue( String namespaceURI, String localName ) {
        throw new UnsupportedOperationException(); }



    public @Override String getCharacterEncodingScheme() { throw new UnsupportedOperationException(); }



    public @Override String getElementText() { throw new UnsupportedOperationException(); }



    public @Override String getEncoding() { throw new UnsupportedOperationException(); }



    /** The present translation state, aka ‘event type’.  {@inheritDoc}
      */
    public @Override int getEventType() { return eventType; }



    public @Override String getLocalName() { throw new UnsupportedOperationException(); }



    public @Override Location getLocation() { throw new UnsupportedOperationException(); }



    public @Override QName getName() { throw new UnsupportedOperationException(); }



    public @Override NamespaceContext getNamespaceContext() {
        throw new UnsupportedOperationException(); }



    public @Override int getNamespaceCount() { throw new UnsupportedOperationException(); }



    public @Override String getNamespacePrefix( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override String getNamespaceURI() { throw new UnsupportedOperationException(); }



    public @Override String getNamespaceURI( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getNamespaceURI( String prefix ) {
        throw new UnsupportedOperationException(); }



    public @Override String getPIData() { throw new UnsupportedOperationException(); }



    public @Override String getPITarget() { throw new UnsupportedOperationException(); }



    public @Override String getPrefix() { throw new UnsupportedOperationException(); }



    public @Override Object getProperty( String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException(); }



    public @Override String getText() { throw new UnsupportedOperationException(); }



    public @Override char[] getTextCharacters() { throw new UnsupportedOperationException(); }



    public @Override int getTextCharacters( int sourceStart, char[] target, int targetStart,
          int length ) {
        throw new UnsupportedOperationException(); }



    public @Override int getTextLength() { throw new UnsupportedOperationException(); }



    public @Override int getTextStart() { throw new UnsupportedOperationException(); }



    public @Override String getVersion() { throw new UnsupportedOperationException(); }



    public @Override boolean hasName() { throw new UnsupportedOperationException(); }



    public @Override boolean hasNext() { return hasNext; }



    public @Override boolean hasText() { throw new UnsupportedOperationException(); }



    /** @throws XMLStreamException Always with a {@linkplain XMLStreamException#getCause() cause}
      *   of type {@linkplain ParseError ParseError} against the Breccian source.
      */
    public @Override int next() throws XMLStreamException {
        if( !hasNext ) throw new java.util.NoSuchElementException();
        if( sourceCursor.state().isFinal() ) { // Then it remains to end the translated document.
            assert sourceCursor.state() instanceof FileFractumEnd; /* The only alternatives are `empty`
              and `error`, both impossible unless the `hasNext` of the guard above is incorrect. */
            eventType = END_DOCUMENT;
            hasNext = false;
            return eventType; }
        final ParseState newParseState;
        try {
            newParseState = sourceCursor.next();
            eventType = switch( newParseState.symmetry() ) {
                case asymmetric -> throw new IllegalStateException(); /* A state of `error` or `empty`,
                  both impossible to get from the `sourceCursor.next` above. */
                case fractalStart -> START_ELEMENT; /* TODO here, and at `markupSource`:
                  For any of these fractal states `f` that has a head, translate it as follows.
                  A) Emit a `Head` start tag.
                  B) Recursively translate each parsed head component `c`, as follows.
                     1) Emit a start tag of `c.tagName`.
                     2) Translate `c` as follows.
                        b) If `c.isComposite`, then  recursively translate each parsed component of `c`.
                        a) Else emit `c.text`.
                     3) Emit the corresponding end tag.
                  C) Emit a `Head` end tag. */
                case fractalEnd -> END_ELEMENT; };} /* If the parse state here is a `FileFractumEnd`,
                  then this ends the document element and the next call will end the document itself. */
        catch( final ParseError x ) {
            eventType = ERROR;
            hasNext = false;
            throw new XMLStreamException( x ); }
        assert !newParseState.isFinal() || newParseState instanceof FileFractumEnd; // Wherefore:
        hasNext = true;
        return eventType; }



    public @Override boolean isAttributeSpecified( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override boolean isCharacters() { throw new UnsupportedOperationException(); }



    public @Override boolean isEndElement() { throw new UnsupportedOperationException(); }



    public @Override boolean isStandalone() { throw new UnsupportedOperationException(); }



    public @Override boolean isStartElement() { throw new UnsupportedOperationException(); }



    public @Override boolean isWhiteSpace() { throw new UnsupportedOperationException(); }



    public @Override int nextTag() {
        throw new UnsupportedOperationException(); }



    public @Override void require( int type, String namespaceURI, String localName ) {
        throw new UnsupportedOperationException(); }



    public @Override boolean standaloneSet() { throw new UnsupportedOperationException(); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private int eventType = ERROR;



    private boolean hasNext = false;



    private final BrecciaCursor sourceCursor; }



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
