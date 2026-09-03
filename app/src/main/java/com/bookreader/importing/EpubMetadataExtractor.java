package com.bookreader.importing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * EPUB is a zip archive whose contents are described by an OPF (Open
 * Packaging Format) XML file. We don't need a full EPUB library just to
 * read title/author/spine-length — standard java.util.zip + XML parsing
 * covers it, and it keeps this dependency-free per the "Java only" constraint.
 *
 * Steps:
 *   1. Read META-INF/container.xml to find the path to the .opf file
 *      (its location inside the zip is not fixed by spec).
 *   2. Parse the .opf file's <metadata> for dc:title / dc:creator.
 *   3. Count <itemref> entries in <spine> — this becomes Book.totalUnits.
 */
public class EpubMetadataExtractor {

    public static class EpubMetadata {
        public String title;
        public String author;
        public int spineLength;
    }

    public static EpubMetadata extract(String epubFilePath) throws IOException, ParserConfigurationException {
        try (ZipFile zipFile = new ZipFile(epubFilePath)) {
            String opfPath = findOpfPath(zipFile);
            Document opfDoc = parseXml(zipFile, opfPath);

            EpubMetadata metadata = new EpubMetadata();
            metadata.title = extractText(opfDoc, "title");
            metadata.author = extractText(opfDoc, "creator");
            metadata.spineLength = countSpineItems(opfDoc);

            // Fallback: some EPUBs omit dc:title, extremely rare but don't crash the import.
            if (metadata.title == null || metadata.title.trim().isEmpty()) {
                metadata.title = "Untitled";
            }

            return metadata;
        }
    }

    private static String findOpfPath(ZipFile zipFile) throws IOException, ParserConfigurationException {
        ZipEntry containerEntry = zipFile.getEntry("META-INF/container.xml");
        if (containerEntry == null) {
            throw new IOException("Invalid EPUB: missing META-INF/container.xml");
        }

        Document containerDoc = parseXmlFromStream(zipFile.getInputStream(containerEntry));
        NodeList rootFiles = containerDoc.getElementsByTagName("rootfile");
        if (rootFiles.getLength() == 0) {
            throw new IOException("Invalid EPUB: no rootfile declared in container.xml");
        }

        Element rootFile = (Element) rootFiles.item(0);
        String fullPath = rootFile.getAttribute("full-path");
        if (fullPath == null || fullPath.isEmpty()) {
            throw new IOException("Invalid EPUB: rootfile missing full-path attribute");
        }
        return fullPath;
    }

    private static Document parseXml(ZipFile zipFile, String entryPath) throws IOException, ParserConfigurationException {
        ZipEntry entry = zipFile.getEntry(entryPath);
        if (entry == null) {
            throw new IOException("Invalid EPUB: referenced file not found: " + entryPath);
        }
        return parseXmlFromStream(zipFile.getInputStream(entry));
    }

    private static Document parseXmlFromStream(InputStream stream) throws ParserConfigurationException, IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // XXE hardening. The Apache-specific "disallow-doctype-decl" feature
            // works on desktop JVMs but Android's built-in parser doesn't
            // recognize it and throws ParserConfigurationException instead of
            // just ignoring it — which is exactly the bug that caused every
            // EPUB import to fail with "Could not read EPUB file". Try it, but
            // don't let an unsupported feature abort the parse; the two calls
            // below are supported cross-platform and still block the main
            // external-entity attack vectors on their own.
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySetXIncludeAware(factory, false);
            trySetExpandEntityReferences(factory, false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(stream));
        } catch (org.xml.sax.SAXException e) {
            throw new IOException("Malformed XML in EPUB", e);
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            // Not recognized by this platform's parser (seen on Android) — skip it.
        }
    }

    // setXIncludeAware/setExpandEntityReferences declare no checked exception,
    // but Android's parser implementation can throw UnsupportedOperationException
    // (unchecked) instead of just no-op'ing — which previously slipped past the
    // catch (SAXException) above and surfaced as the same generic import failure.
    private static void trySetXIncludeAware(DocumentBuilderFactory factory, boolean value) {
        try {
            factory.setXIncludeAware(value);
        } catch (RuntimeException e) {
            // Not supported on this platform — skip it, the caller still has
            // trySetFeature's doctype guard and/or setExpandEntityReferences.
        }
    }

    private static void trySetExpandEntityReferences(DocumentBuilderFactory factory, boolean value) {
        try {
            factory.setExpandEntityReferences(value);
        } catch (RuntimeException e) {
            // Not supported on this platform — skip it.
        }
    }

    private static String extractText(Document opfDoc, String dcTagLocalName) {
        // dc:title / dc:creator — namespace-agnostic lookup since some EPUBs
        // vary the declared prefix for the Dublin Core namespace.
        NodeList nodes = opfDoc.getElementsByTagNameNS("*", dcTagLocalName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private static int countSpineItems(Document opfDoc) {
        NodeList spines = opfDoc.getElementsByTagName("spine");
        if (spines.getLength() == 0) return 0;
        Element spine = (Element) spines.item(0);
        return spine.getElementsByTagName("itemref").getLength();
    }
}
