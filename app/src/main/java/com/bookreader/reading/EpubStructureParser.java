package com.bookreader.reading;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the spine (reading order) from an EPUB that's already been unzipped
 * to disk by EpubExtractor. Works on the extracted files directly rather
 * than the zip, since by this point in the flow we always have both an
 * extraction and a reason to read the spine (opening the reader).
 */
public class EpubStructureParser {

    public static class EpubStructure {
        /** Directory (relative to extractionRoot) that the OPF file lives in — chapter hrefs are relative to this. */
        public String opfBaseDir;
        /** Ordered list of chapter file names (relative to opfBaseDir), one per spine entry. */
        public List<String> spineFiles = new ArrayList<>();
    }

    public static EpubStructure parse(File extractionRoot) throws IOException {
        File containerFile = new File(extractionRoot, "META-INF/container.xml");
        if (!containerFile.exists()) {
            throw new IOException("Invalid EPUB extraction: missing META-INF/container.xml");
        }

        Document containerDoc = parseXml(containerFile);
        NodeList rootFiles = containerDoc.getElementsByTagName("rootfile");
        if (rootFiles.getLength() == 0) {
            throw new IOException("Invalid EPUB: no rootfile in container.xml");
        }
        String opfRelativePath = ((Element) rootFiles.item(0)).getAttribute("full-path");
        File opfFile = new File(extractionRoot, opfRelativePath);
        if (!opfFile.exists()) {
            throw new IOException("Invalid EPUB: OPF file not found at " + opfRelativePath);
        }

        Document opfDoc = parseXml(opfFile);

        // manifest: id -> href, so we can resolve spine itemrefs (which reference ids) to actual files
        Map<String, String> manifestIdToHref = new HashMap<>();
        NodeList items = opfDoc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            manifestIdToHref.put(item.getAttribute("id"), item.getAttribute("href"));
        }

        EpubStructure structure = new EpubStructure();
        File opfParent = opfFile.getParentFile();
        structure.opfBaseDir = opfParent == null ? "" : relativePath(extractionRoot, opfParent);

        NodeList spines = opfDoc.getElementsByTagName("spine");
        if (spines.getLength() == 0) {
            throw new IOException("Invalid EPUB: no spine in OPF");
        }
        NodeList itemRefs = ((Element) spines.item(0)).getElementsByTagName("itemref");
        for (int i = 0; i < itemRefs.getLength(); i++) {
            String idref = ((Element) itemRefs.item(i)).getAttribute("idref");
            String href = manifestIdToHref.get(idref);
            if (href != null) {
                structure.spineFiles.add(href);
            }
            // Silently skip itemrefs with no manifest match rather than failing the whole
            // book open — malformed spine entries shouldn't block reading the rest.
        }

        return structure;
    }

    private static String relativePath(File root, File target) {
        String rootPath = root.getAbsolutePath();
        String targetPath = target.getAbsolutePath();
        if (targetPath.equals(rootPath)) return "";
        return targetPath.substring(rootPath.length() + 1);
    }

    private static Document parseXml(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Same fix as EpubMetadataExtractor: the Apache-specific
            // "disallow-doctype-decl" feature isn't recognized by Android's
            // built-in parser and throws instead of being ignored, which was
            // silently breaking every EPUB open. Try it, but fall back to the
            // cross-platform-supported settings below rather than aborting.
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file);
        } catch (Exception e) {
            throw new IOException("Failed to parse " + file.getName(), e);
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            // Not recognized by this platform's parser (seen on Android) — skip it.
        }
    }
}
