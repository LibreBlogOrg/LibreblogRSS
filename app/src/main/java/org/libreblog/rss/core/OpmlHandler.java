package org.libreblog.rss.core;

import android.content.Context;
import android.net.Uri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class OpmlHandler {
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    public static List<SimpleOutline> getSimpleOutlines(Context ctx, Uri inputUri) {
        if (ctx == null || inputUri == null) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream in = ctx.getContentResolver().openInputStream(inputUri);
            Document srcDoc = db.parse(in);

            NodeList bodyList = srcDoc.getElementsByTagName("body");
            if (bodyList.getLength() == 0) {
                return null;
            }

            Element bodyIn = (Element) bodyList.item(0);
            List<SimpleOutline> result = new ArrayList<>();
            collectLeafTexts(bodyIn, result);

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static void collectLeafTexts(Element node, List<SimpleOutline> outList) {
        if (node == null) return;

        NodeList children = node.getChildNodes();
        boolean hasOutlineChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase("outline")) {
                hasOutlineChild = true;
                collectLeafTexts((Element) n, outList);
            }
        }

        if (node.getNodeName().equalsIgnoreCase("outline")) {
            String xmlUrl = node.getAttribute("xmlUrl");
            if (!hasOutlineChild && xmlUrl != null && !xmlUrl.isEmpty()) {
                String title = node.getAttribute("title");
                String text = node.getAttribute("text");
                String type = node.getAttribute("type");
                String imageUrl = node.getAttribute("libreblog:imageUrl");
                String preferredImageUrl = node.getAttribute("libreblog:preferredImageUrl");
                String score = node.getAttribute("libreblog:score");
                String description = node.getAttribute("libreblog:description");
                outList.add(new SimpleOutline(title, text, xmlUrl, imageUrl, preferredImageUrl, score, description, type));
            }
        }
    }

    public static File createOpmlFile(Context context, String fileName, List<DbHandler.Source> sources) throws Exception {
        if (context == null || fileName == null || sources == null) return null;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element opml = doc.createElement("opml");
        opml.setAttribute("version", "2.0");
        opml.setAttribute("xmlns:libreblog", "https://rss.libreblog.org");
        doc.appendChild(opml);

        Element head = doc.createElement("head");
        Element title = doc.createElement("title");
        title.setTextContent("Subscriptions");
        head.appendChild(title);
        opml.appendChild(head);

        Element body = doc.createElement("body");
        opml.appendChild(body);

        for (DbHandler.Source source : sources) {
            String sTitle = source.title == null ? "" : source.title;
            if (sTitle.length() > MAX_TITLE_LENGTH) {
                sTitle = sTitle.substring(0, MAX_TITLE_LENGTH) + "...";
            }
            String sDescription = source.description == null ? "" : source.description;
            if (sDescription.length() > MAX_DESCRIPTION_LENGTH) {
                sDescription = sDescription.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
            }

            Element outline = doc.createElement("outline");
            outline.setAttribute("xmlUrl", source.id);
            outline.setAttribute("text", source.name);
            outline.setAttribute("title", sTitle);
            outline.setAttribute("type", source.type == null ? DbHandler.SOURCE_TYPE_RSS : source.type);
            outline.setAttribute("libreblog:imageUrl", source.image == null ? "" : source.image);
            outline.setAttribute("libreblog:preferredImageUrl", source.preferredImage == null ? "" : source.preferredImage);
            outline.setAttribute("libreblog:score", "" + source.score);
            outline.setAttribute("libreblog:description", sDescription);
            body.appendChild(outline);
        }

        File outFile = new File(context.getFilesDir(), fileName);
        FileOutputStream fos = new FileOutputStream(outFile);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(fos));

        fos.flush();
        fos.close();
        return outFile;
    }

    public static class SimpleOutline {
        public final String title;
        public final String text;
        public final String xmlUrl;
        public final String imageUrl;
        public final String preferredImageUrl;
        public final double score;
        public final String description;
        public final String type;

        public SimpleOutline(String title, String text, String xmlUrl, String imageUrl,
                             String preferredImageUrl, String sourceStr, String description, String type) {
            this.title = title;
            this.text = text;
            this.xmlUrl = xmlUrl;
            this.imageUrl = imageUrl;
            this.preferredImageUrl = preferredImageUrl;
            this.description = description;
            this.type = type;
            double score = 2.5;
            try {
                score = Double.parseDouble(sourceStr);
            } finally {
                this.score = score;
            }
        }
    }
}
