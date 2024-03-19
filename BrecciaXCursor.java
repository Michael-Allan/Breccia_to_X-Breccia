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
import static Java.StringBuilding.clear;


/** A reusable, pull translator of Breccia to X-Breccia that operates as a unidirectional cursor over
  * a series of discrete parse states.  This translator suffices (as is) to support any extension of
  * Breccia Parser that models its extended fractal states as instances of `Fractum` and `Fractum.End`,
  * and file-fractal states, if any, as instances of `FileFractum` and `FileFractum.End`.
  *
  * <p>The {@linkplain #source(Cursor) initial translation state} is either
  * `{@linkplain #EMPTY EMPTY}` or `{@linkplain #START_DOCUMENT START_DOCUMENT}`.
  * If `EMPTY`, then this translator emits no elements.  Otherwise, for each granum `g`,
  * this translator emits an element named `g.{@linkplain Breccia.parser.Granum#tagName() tagName}`.
  * Further it emits an element named `Head` to encapsulate the content of each fractal head.
  * (All body fracta have heads.  The file fractum alone is potentially headless.)
  * The namespace for all emitted elements is `{@value #namespace}`.</p>
  *
  * <p>The element for each fractum is given the following attributes.</p><ul>
  *
  *     <li>`{@linkplain Breccia.parser.Granum#lineNumber() lineNumber}`</li>
  *     <li>`{@linkplain Breccia.parser.ParseState#typestamp() typestamp}`</li>
  *     <li>`{@linkplain Breccia.parser.Granum#xunc() xunc}`</li></ul>
  *
  * <p>Its `Head` element, if any, is given these attributes:</p><ul>
  *
  *     <li>`{@linkplain Breccia.parser.Granum#xunc() xunc}`</li>
  *     <li>`xuncLineEnds`, the value being a space delimited list of each
  *         `{@linkplain Breccia.parser.Granum#xuncLineEnd() xuncLineEnd}`</li></ul>
  *
  * <p>The other granal elements (descendants all of the `Head` element) are given:</p><ul>
  *
  *     <li>`{@linkplain Breccia.parser.Granum#xunc() xunc}`</li></ul>
  *
  * <p>Moreover each of the following attributes is given to any fractum to which it is proper.</p><ul>
  *
  *     <li>`{@linkplain Breccia.parser.CommandPoint#modifiers() modifiers}`</li>
  *     <li>`{@linkplain Breccia.parser.FileLocant#qualifiers() qualifiers}`</li></ul>
  *
  * <p>This translator emits no ignorable whitespace.</p>
  *
  *     @see <a href='http://reluk.ca/project/Breccia/'>Breccia Parser</a>
  *     @see Breccia.parser.Fractum
  *     @see Breccia.parser.Fractum.End
  *     @see Breccia.parser.FileFractum
  *     @see Breccia.parser.FileFractum.End *//*
  *
  * For the XML event types emitted by this translator (at present),
  * see the `assert` statement and comment at the foot of method `next`.
  */
public final class BrecciaXCursor implements AutoCloseable, XStreamConstants, XMLStreamReader {


    public BrecciaXCursor() { halt(); }



    /** Translates the text of the given source, feeding each state of the translation to `sink`
      * till all are exhausted.  Calling this method will abort any translation already in progress.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not {@linkplain ParseState#isInitial() initial}.
      */
    public void perState( final Cursor source, final IntConsumer sink ) throws ParseError {
        source( source );
        for( ;; ) {
            sink.accept( eventType );
            if( !hasNext() ) break;
            try { next(); }
            catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}



    /** Translates the text of the given source, feeding each state of the translation to `sink`
      * till either all are exhausted or `sink` returns false.  Calling this method will abort
      * any translation already in progress.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not {@linkplain ParseState#isInitial() initial}.
      */
    public void perStateConditionally( final Cursor source, final IntPredicate sink ) throws ParseError {
        source( source );
        while( sink.test(eventType) && hasNext() ) {
            try { next(); }
            catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}



    /** Begins translating a new source of text comprising a single file fractum.  Sets the translation
      * state either to `{@linkplain #EMPTY EMPTY}` or to `{@linkplain #START_DOCUMENT START_DOCUMENT}`.
      *
      *     @throws IllegalStateException If `source.{@linkplain Cursor#state() state}`
      *       is not an {@linkplain ParseState#isInitial() initial state}.
      */
    public void source( final Cursor source ) {
        this.source = source;
        final ParseState initialParseState = source.state();
        if( !initialParseState.isInitial() ) {
            halt();
            throw new IllegalStateException( "Source in non-initial state" ); }
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



   // ━━━  A u t o   C l o s e a b l e  ━━━  X M L   S t r e a m   R e a d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void close() {}



   // ━━━  X M L   S t r e a m   R e a d e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override int getAttributeCount() {
        if( eventType != START_ELEMENT ) throw wrongEventType(); // As per contract.
        return attributes.length; }



    public @Override String getAttributeLocalName( final int a ) { return attributes[a].localName; }



    public @Override QName getAttributeName( int index ) { throw new UnsupportedOperationException(); }



    public @Override String getAttributePrefix( final int a ) { return attributes[a].prefix; }



    public @Override String getAttributeNamespace( final int a ) { return attributes[a].namespace; }



    public @Override String getAttributeType( final int a ) { return attributes[a].type; }



    public @Override String getAttributeValue( final int a ) { return attributes[a].value(); }



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
        final CharSequence characters = granum.text();
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
        return granum.text().length(); }



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
                        granum = fractum;
                        location = locationFromGranum;
                        if( source.asCommandPoint() != null ) attributes = attributesCommandPoint;
                        else attributes = attributesFractum;

                      // clean up, preparing for subsequent events
                      // ┈┈┈┈┈┈┈┈
                        final List<? extends Granum> components;
                        try { components = fractum.components(); }
                        catch( final ParseError x ) { throw halt( x ); }
                        if( components.isEmpty() ) {
                            if( fractum.text().isEmpty() ) {            // a) The fractum is headless;
                                assert fractum instanceof FileFractum; //    it must be a file fractum.
                                break; } // Continuing with `interstate_traversal`.
                            this.components = null;                  // b) The fractal head is flat.
                            assert false: "Live code"; } // [FH]
                        else {                                     // c) The fractal head is composite.
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
                    assert granum instanceof Fractum && location == locationFromGranum;
                    attributes = attributesHead;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    translationProcess = head_content_traversal;
                    if( components == null ) {
                        eventTypeNext = CHARACTERS;
                        assert false: "Live code"; } // [FH]
                    else assert eventTypeNext == START_ELEMENT; }
                else {
                    assert eventType == END_ELEMENT;
                    localName = localNameStack.pop();
                    assert "Head".equals( localName );
                    location = locationUnknown;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    translationProcess = /*back to*/interstate_traversal; }}
            case head_content_traversal -> { /* Traversing the content of a fractal head.
                  This process traverses the content in depth, emitting XML events
                  to reflect in turn each instance of a component, subcomponent or flat text,
                  then switches the process back to one of `head_encapsulation`. */
                eventType = eventTypeNext;
                if( eventType == START_ELEMENT ) {
                    final Granum component = components.get( componentIndex );
                    localNameStack.push( localName = component.tagName() );
                    granum = component;
                    location = locationFromGranum;
                    if( component instanceof FileLocant ) attributes = attributesFileLocant;
                    else attributes = attributesOther;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    final List<? extends Granum> subcomponents;
                    try { subcomponents = component.components(); }
                    catch( final ParseError x ) { throw halt( x ); }
                    if( subcomponents.size() > 0 ) { // Then descend the component hierarchy.
                        componentsStack.add( components );
                        components = subcomponents;
                        componentIndexStack.add( componentIndex );
                        componentIndex = 0;
                        assert eventTypeNext == START_ELEMENT; }
                    else eventTypeNext = CHARACTERS; } // No next subcomponent, only flat text.
                else if( eventType == CHARACTERS ) {
                    assert location == locationFromGranum;

                  // clean up, preparing for the next event
                  // ┈┈┈┈┈┈┈┈
                    eventTypeNext = END_ELEMENT; // This ends either the present component,
                    if( components == null ) {  // or (with the code herein) the fractal head.
                        assert granum instanceof Fractum;
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



    public @Override boolean isAttributeSpecified( final int a ) { return attributes[a].isSpecified; }



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


    private Attribute[] attributes;



    /** Index of a component within {@linkplain #components components}.
      */
    private int componentIndex;



    private final IntArrayExtensor componentIndexStack = new IntArrayExtensor( new int[0x100] );



    private List<? extends Granum> components;



    private final ArrayList<List<? extends Granum>> componentsStack = new ArrayList<>( 0x100 );



    private int eventType;



    private int eventTypeNext; // Used only for `translationProcess` ≠ `interstate_traversal`.



    private Granum granum;



    private void halt() {
        eventType = HALT;
        namespaceCount = 0;
        location = locationUnknown;
        hasNext = false; }



    private XMLStreamException halt( final ParseError x ) {
        halt();
        return new XMLStreamException( x ); }



    private boolean hasNext = false;



    private final Attribute lineNumber = new Attribute( "lineNumber" ) {
        @Override String value() { return Integer.toString( granum.lineNumber() ); }};



    private String localName;



    private final ArrayDeque<String> localNameStack = new ArrayDeque<>( 0x100 );



    private Location location;



    private final Location locationFromGranum = new Location() {
        public @Override int getCharacterOffset() { return -1; }
        public @Override int getColumnNumber()    { return granum.column(); }
        public @Override int getLineNumber()      { return granum.lineNumber(); }
        public @Override String getPublicId()     { return null; }
        public @Override String getSystemId()     { return null; }};



    private final Location locationUnknown = new Location() {
        public @Override int getCharacterOffset() { return -1; }
        public @Override int getColumnNumber()    { return -1; }
        public @Override int getLineNumber()      { return -1; }
        public @Override String getPublicId()     { return null; }
        public @Override String getSystemId()     { return null; }};



    private final Attribute modifiers = new Attribute( "modifiers" ) {
        @Override String value() { return spaceDelimited( source.asCommandPoint().modifiers() ); }};



    private static final String namespace = "data:,Breccia/XML";



    private int namespaceCount;



    private final Attribute qualifiers = new Attribute( "qualifiers" ) {
        @Override String value() { return spaceDelimited( ((FileLocant)granum).qualifiers() ); }};



    private Cursor source;



    /* @paramImplied #stringBuilder
     */
    private final String spaceDelimited( final List<String> strings ) {
        final int sN = strings.size();
        if( sN == 0 ) return "";
        final StringBuilder b = clear( stringBuilder );
        for( int s = 0;; ) {
            b.append( strings.get( s ));
            if( ++s == sN ) break;
            b.append( ' ' ); } // Separator.
        return b.toString(); }



    private final StringBuilder stringBuilder = new StringBuilder( /*initial capacity*/0x300/*or 768*/ );



    private TranslationProcess translationProcess;



    private final Attribute typestamp = new Attribute( "typestamp" ) {
        @Override String value() { return Integer.toString( source.state().typestamp() ); }};



    private static IllegalStateException wrongEventType() {
        return new IllegalStateException( "Wrong type of parse event" ); }



    private final Attribute xunc = new Attribute( "xunc" ) {
        @Override String value() { return Integer.toString( granum.xunc() ); }};



    private final Attribute xuncLineEnds = new Attribute( "xuncLineEnds" ) {
        @Override String value() {
            final Fractum fractum = source.asFractum();
            final int iN = fractum.lineCount();
            if( iN <= 0 ) throw new IllegalStateException();
            final StringBuilder b = clear( stringBuilder );
            for( int i = 0;; ) {
                b.append( fractum.xuncLineEnd( i ));
                if( ++i == iN ) break;
                b.append( ' ' ); } // Separator.
            return b.toString(); }};



   // ┈┈┈  l a t e   d e c l a r a t i o n s  ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈


    private final Attribute[] attributesCommandPoint = { xunc, lineNumber, typestamp, modifiers };


    private final Attribute[] attributesFileLocant =  { xunc, qualifiers };


    private final Attribute[] attributesFractum =    { xunc, lineNumber, typestamp }; // [LN]


    private final Attribute[] attributesHead =      { xunc, xuncLineEnds }; /* Each proper
          to the fractal head (as opposed to the whole fractum), or to all grana. */


    private final Attribute[] attributesOther =    { xunc };



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    private static abstract class Attribute {


        Attribute( String localName ) { this.localName = localName; }



        final boolean isSpecified = false;



        final String localName;



        final String namespace = null;



        final String prefix = null;



        final String type = "CDATA"; /* Presumeably correct for the present (non-validating) parser.
          https://developer.android.com/reference/org/xmlpull/v1/XmlPullParser#getAttributeType(int) */



        abstract String value(); }



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    static enum TranslationProcess { // Access is non-private only to allow a static `import` at top.
        interstate_traversal,
        head_encapsulation,
        head_content_traversal }}



// NOTES
// ─────
//   FH · Flat head: marking an instance of code that deals with flat, non-composite fractal heads.
//        At the time of writing, no flat head actually occurs in any `Fractum` implementation.
//        Rather all head content is composite, being modelled by one or more `Granum` components.
//        Therefore the marked code is dead and untested.
//
//   LN · Line number attribution on the fractal element.  While the parser considers line numbers to be
//        ‘adjunct state’, requests for which ‘may be slow’, here they are much wanted (in tandem with
//        `xunc` and `xuncLineEnds`) to anchor the resolution of line numbers more generally.



                                             // Copyright © 2020-2022, 2024  Michael Allan.  Licence MIT.
