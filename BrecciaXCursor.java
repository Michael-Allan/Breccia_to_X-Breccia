package Breccia.XML.translator;

import Breccia.parser.*;
import Java.IntArrayExtensor;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import static Breccia.parser.ParseState.Symmetry.*;
import static Breccia.parser.Typestamp.*;
import static Breccia.XML.translator.BrecciaXCursor.TranslationProcess.*;


/** A reusable, pull translator of Breccia to X-Breccia that operates as a unidirectional cursor
  * over a series of discrete parse states.  This translator suffices (as is) to support any extension
  * of Breccia that models its extended fractal states as instances of `Fractum` and `Fractum.End`,
  * and extended file-fractal states (if any) as instances of `FileFractum` and `FileFractum.End`. *//*
  *
  * For the XML event types actually emitted by this translator (at present), see the `assert` statement
  * and comment at the foot of method `next`.
  */
public final class BrecciaXCursor implements AutoCloseable, XStreamConstants, XMLStreamReader {


    public BrecciaXCursor() { halt(); }



    /** Begins translating a new source of markup comprising a single file fractum.  Sets the translation
      * state either to `{@linkplain #EMPTY EMPTY}` or to `{@linkplain #START_DOCUMENT START_DOCUMENT}`.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not an {@linkplain ParseState#isInitial() initial state}.
      */
    public void markupSource( final Cursor source ) {
        this.source = source;
        final ParseState initialParseState = source.state();
        if( !initialParseState.isInitial() ) {
            halt();
            throw new IllegalStateException( "Markup source in non-initial state" ); }
        namespaceCount = 0;
        if( initialParseState.isFinal() ) {
            assert initialParseState.typestamp() == empty;
            eventType = EMPTY;
            location = locationUnknown;
            hasNext = false; }
        else {
            eventType = START_DOCUMENT; /* This starts the document.  The first call to `next` will
              start the document element, which for X-Breccia is always that of the file fractum. */
            location = locationUnknown;
            assert initialParseState instanceof FileFractum;
            translationProcess = interstate_traversal;
            hasNext = true; }}



    /** Translates the markup of the given source, feeding each state of the translation to `sink`
      * till all are exhausted.  Calling this method will abort any translation already in progress.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not {@linkplain ParseState#isInitial() initial}.
      */
    public void perState( final Cursor source, final IntConsumer sink ) throws ParseError {
        markupSource( source );
        for( ;; ) {
            sink.accept( eventType );
            if( !hasNext() ) break;
            try { next(); }
            catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}



    /** Translates the markup of the given source, feeding each state of the translation to `sink`
      * till either all are exhausted or `sink` returns false.  Calling this method will abort
      * any translation already in progress.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not {@linkplain ParseState#isInitial() initial}.
      */
    public void perStateConditionally( final Cursor source, final IntPredicate sink ) throws ParseError {
        markupSource( source );
        while( sink.test(eventType) && hasNext() ) {
            try { next(); }
            catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}



   // ━━━  A u t o   C l o s e a b l e  ━━━  X M L   S t r e a m   R e a d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void close() {}



   // ━━━  X M L   S t r e a m   R e a d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override int getAttributeCount() {
     // if( eventType != START_ELEMENT ) throw wrongEventType(); // As per contract.
        return 0; }



    public @Override String getAttributeLocalName( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override QName getAttributeName( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributePrefix( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override String getAttributeNamespace( int index ) {
        throw new UnsupportedOperationException(); }



    public @Override String getAttributeType( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributeValue( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributeValue( String namespace, String localName ) {
        throw new UnsupportedOperationException(); }



    public @Override String getCharacterEncodingScheme() { throw new UnsupportedOperationException(); }



    public @Override String getElementText() { throw new UnsupportedOperationException(); }



    public @Override String getEncoding() { throw new UnsupportedOperationException(); }



    /** The present translation state, aka ‘event type’.  {@inheritDoc}
      */
    public @Override int getEventType() { return eventType; }



    public @Override String getLocalName() {
        if( eventType != START_ELEMENT && eventType != END_ELEMENT ) throw wrongEventType();
          // As per contract.
        return localName; }



    public @Override Location getLocation() { return location; }



    public @Override QName getName() {
        if( eventType != START_ELEMENT && eventType != END_ELEMENT ) throw wrongEventType();
          // As per contract.
        return new QName( namespace, localName ); }



    public @Override NamespaceContext getNamespaceContext() {
        throw new UnsupportedOperationException(); }



    public @Override int getNamespaceCount() {
     // if( eventType != START_ELEMENT && eventType != END_ELEMENT ) throw wrongEventType();
     //   // As per contract.
        return namespaceCount; }



    public @Override String getNamespacePrefix( final int n ) {
     // if( eventType != START_ELEMENT && eventType != END_ELEMENT ) throw wrongEventType();
     //   // As per contract.
        if( n < 0 || n >= namespaceCount ) throw new IndexOutOfBoundsException( n );
        return null; } // No prefix, the namespace declared here is the default namespace.



    public @Override String getNamespaceURI() { return namespace; }



    public @Override String getNamespaceURI( final int n ) {
     // if( eventType != START_ELEMENT && eventType != END_ELEMENT ) throw wrongEventType();
     //   // As per contract.
        if( n < 0 || n >= namespaceCount ) throw new IndexOutOfBoundsException( n );
        return namespace; }


    public @Override String getNamespaceURI( String prefix ) {
        throw new UnsupportedOperationException(); }



    public @Override String getPIData() { throw new UnsupportedOperationException(); }



    public @Override String getPITarget() { throw new UnsupportedOperationException(); }



    public @Override String getPrefix() { throw new UnsupportedOperationException(); }



    public @Override Object getProperty( String name) { throw new UnsupportedOperationException(); }



    public @Override String getText() { throw new UnsupportedOperationException(); }



    public @Override char[] getTextCharacters() { throw new UnsupportedOperationException(); }



    public @Override int getTextCharacters( final int sourceStart, final char[] target,
          final int targetStart, int length ) {
        if( eventType != CHARACTERS ) throw wrongEventType();
        if( sourceStart < 0 ) throw new IndexOutOfBoundsException( sourceStart );
        final CharSequence characters = markup.text();
        final int lengthAvailable = characters.length() - sourceStart;
        if( length > lengthAvailable ) length = lengthAvailable;
        final int tEnd = targetStart + length;
        if( targetStart < 0 || tEnd > target.length ) throw new IndexOutOfBoundsException( targetStart );
        for( int s = sourceStart, t = targetStart; t < tEnd; ++s, ++t ) target[t] = characters.charAt(s);
        return length; } /* The abpve might be speeded if the character sequences of the source cursor
          exposed (optionally) their backing buffer.  But then the more flexible and potentially faster
          methods `getTextStart`, getTextLength` and `getTextCharacters` should be supported *instead*
          of the present method. */



    public @Override int getTextLength() {
        if( eventType != CHARACTERS ) throw wrongEventType();
        return markup.text().length(); }



    public @Override int getTextStart() { throw new UnsupportedOperationException(); }



    public @Override String getVersion() { return null; }



    public @Override boolean hasName() { throw new UnsupportedOperationException(); }



    public @Override boolean hasNext() { return hasNext; }



    public @Override boolean hasText() { throw new UnsupportedOperationException(); }



    /** @throws XMLStreamException Always with a {@linkplain XMLStreamException#getCause() cause}
      *   of type {@linkplain ParseError ParseError} against the Breccian source.
      */
    public @Override int next() throws XMLStreamException {
        if( !hasNext ) throw new java.util.NoSuchElementException();
        switch( translationProcess ) {
            case interstate_traversal -> { /* Traversing the parse states of the Breccian source.
                   Normally each of the parse states reflects a fractal head.  After emitting its
                   start tag, the translation process typically switches to `head_encapsulation`. */
                ParseState state = source.state();
                if( state.isFinal() ) { // Then it remains to end the translated document.
                    assert state instanceof FileFractum.End; /* The alternatives are `empty` and `error`,
                      both of which are impossible unless the `hasNext` of the guard above is wrong. */
                    eventType = END_DOCUMENT;
                    namespaceCount = 0;
                    location = locationUnknown;
                    hasNext = false;
                    return eventType; }
                if( /*old*/eventType == START_DOCUMENT ) { // Then already `source` is at the next state.
                    assert state instanceof FileFractum;
                    namespaceCount = 1; }
                else {
                    if( /*old*/state instanceof FileFractum ) namespaceCount/*at next state*/ = 0;
                    try { state = source.next(); } catch( final ParseError x ) { throw halt( x ); }}
                switch( state.symmetry() ) {
                    case asymmetric -> throw new IllegalStateException(); /* A state of `halt`
                      or `empty`, neither of which could have come from the `source.next` above. */
                    case fractalStart -> {
                        eventType = START_ELEMENT;
                        final Fractum fractum = source.asFractum();
                        localNameStack.push( localName = fractum.tagName() );
                        markup = fractum;
                        location = locationFromMarkup;

                      // clean up, preparing for subsequent events
                      // ┈┈┈┈┈┈┈┈
                        final List<? extends Markup> components;
                        try { components = fractum.components(); }
                        catch( final ParseError x ) { throw halt( x ); }
                        if( components.isEmpty() ) {
                            if( fractum.text().isEmpty() ) {            // a) The fractum is headless;
                                assert fractum instanceof FileFractum; //    it must be a file fractum.
                                break; } // Continuing with `interstate_traversal`.
                            this.components = null; }                // b) The fractal head is flat.
                        else {                                      // c) The fractal head is composite.
                            this.components = components;
                            this.componentIndex = 0;
                            assert componentsStack.isEmpty() && componentIndexStack.isEmpty(); }
                        translationProcess = head_encapsulation;
                        eventTypeNext = START_ELEMENT; }
                    case fractalEnd -> {
                        eventType = END_ELEMENT;
                        localName = localNameStack.pop();
                        if( state.isFinal() ) {
                            assert state instanceof FileFractum.End; /* End of document element.
                              The next call will end the document. */
                            namespaceCount = 1; }
                        location = locationUnknown; }}}
            case head_encapsulation -> { /* Encapsulating a fractal head.  This process emits either:
                  a) an opening `Head` tag, then switches to `head_content_traversal'; or
                  b) a closing `Head` tag, then switches back to `interstate_traversal`. */
                eventType = eventTypeNext;
                if( eventType == START_ELEMENT ) {
                    localNameStack.push( localName = "Head" );
                    assert markup instanceof Fractum && location == locationFromMarkup;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    translationProcess = head_content_traversal;
                    if( components == null ) eventTypeNext = CHARACTERS;
                    else assert eventTypeNext == START_ELEMENT; }
                else {
                    assert eventType == END_ELEMENT;
                    localName = localNameStack.pop();
                    assert "Head".equals( localName );
                    location = locationUnknown;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    translationProcess = /*back to*/interstate_traversal; }}
            case head_content_traversal -> { /* Traversing the markup content of a fractal head.
                  This process traverses in depth the content of the head, emitting XML events
                  to reflect in turn each instance of a component, subcomponent or flat markup,
                  then switches the process back to one of `head_encapsulation`. */
                eventType = eventTypeNext;
                if( eventType == START_ELEMENT ) {
                    final Markup component = components.get( componentIndex );
                    localNameStack.push( localName = component.tagName() );
                    markup = component;
                    location = locationFromMarkup;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    final List<? extends Markup> subcomponents;
                    try { subcomponents = component.components(); }
                    catch( final ParseError x ) { throw halt( x ); }
                    if( subcomponents.size() > 0 ) { // Then descend the component hierarchy.
                        componentsStack.add( components );
                        components = subcomponents;
                        componentIndexStack.add( componentIndex );
                        componentIndex = 0;
                        assert eventTypeNext == START_ELEMENT; }
                    else eventTypeNext = CHARACTERS; } // No next subcomponent, only flat markup.
                else if( eventType == CHARACTERS ) {
                    assert location == locationFromMarkup;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    eventTypeNext = END_ELEMENT; // This ends either the present component,
                    if( components == null ) {  // or (with the code herein) the fractal head.
                        assert markup instanceof Fractum;
                        translationProcess = /*back to*/head_encapsulation; }} // To end it.
                else {
                    assert eventType == END_ELEMENT;
                    localName = localNameStack.pop();
                    location = locationUnknown;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    if( ++componentIndex/*to the next sibling*/ < components.size() ) {
                        eventTypeNext = START_ELEMENT;
                        break; }
                    // No sibling remains at this level of the hierarchy.  Ascend to the next level:
                    assert eventTypeNext == END_ELEMENT; // To close the parent.
                    int depth = componentsStack.size();
                    assert depth == componentIndexStack.length; // Both stacks are kept in sync.
                    if( depth > 0 ) { // Then the parent to close is itself a head component.
                        componentIndexStack.length = --depth; // Ascending to the higher parent.
                        components = /*those of the higher parent*/componentsStack.remove( depth );
                        componentIndex = /*recall it*/componentIndexStack.array[depth]; }
                    else translationProcess = /*back to*/head_encapsulation; }}} /* The parent to close
                      is not itself a head component, rather it is the composite head. */
        assert eventType == START_ELEMENT || eventType == CHARACTERS || eventType == END_ELEMENT;
          // These plus `EMPTY`, `START_DOCUMENT`, `END_DOCUMENT` and `HALT` alone are emitted.
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



    public @Override void require( int type, String namespace, String localName ) {
        throw new UnsupportedOperationException(); }



    public @Override boolean standaloneSet() { throw new UnsupportedOperationException(); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Index of a component within {@linkplain #components components}.
      */
    private int componentIndex;



    private final IntArrayExtensor componentIndexStack = new IntArrayExtensor( new int[0x100] );



    private List<? extends Markup> components;



    private final ArrayList<List<? extends Markup>> componentsStack = new ArrayList<>( 0x100 );



    private int eventType;



    private int eventTypeNext; // Used only for `translationProcess` ≠ `interstate_traversal`.



    private void halt() {
        eventType = HALT;
        namespaceCount = 0;
        location = locationUnknown;
        hasNext = false; }



    private XMLStreamException halt( final ParseError x ) {
        halt();
        return new XMLStreamException( x ); }



    private boolean hasNext = false;



    private String localName;



    private final ArrayDeque<String> localNameStack = new ArrayDeque<>( 0x100 );



    private Location location;



    private final Location locationFromMarkup = new Location() {
        public @Override int getCharacterOffset() { return -1; }
        public @Override int getColumnNumber()    { return markup.column(); }
        public @Override int getLineNumber()      { return markup.lineNumber(); }
        public @Override String getPublicId()     { return null; }
        public @Override String getSystemId()     { return null; }};



    private final Location locationUnknown = new Location() {
        public @Override int getCharacterOffset() { return -1; }
        public @Override int getColumnNumber()    { return -1; }
        public @Override int getLineNumber()      { return -1; }
        public @Override String getPublicId()     { return null; }
        public @Override String getSystemId()     { return null; }};



    private Markup markup;



    private static final String namespace = "data:,Breccia/XML";



    private int namespaceCount;



    private Cursor source;



    static enum TranslationProcess { // Access is non-private only to allow a static `import` at top.
        interstate_traversal,
        head_encapsulation,
        head_content_traversal }



    private TranslationProcess translationProcess;



    private static IllegalStateException wrongEventType() {
        return new IllegalStateException( "Wrong type of parse event" ); }}



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
