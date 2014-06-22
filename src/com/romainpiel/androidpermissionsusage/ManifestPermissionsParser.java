package com.romainpiel.androidpermissionsusage;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by rpiel on 21/06/2014.
 */
public class ManifestPermissionsParser {

    public static void findPermissions(InputStream stream, OnPermissionFoundListener listener) {

        if (stream == null || listener == null) {
            return;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("uses-permission");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    String nameAttr = ((Element) node).getAttribute("android:name");
                    if (nameAttr != null) {
                        listener.onPermissionFound(nameAttr);
                    }
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public static String getPermissionName(String path) throws IllegalStateException {
        if (path == null || !path.matches("android.permission\\.[A-Z_]+")) {
            throw new IllegalStateException("Permission is not Android specific");
        }
        String[] parts = StringUtils.split(path, '.');
        return parts[parts.length - 1];
    }

    public interface OnPermissionFoundListener {
        public void onPermissionFound(String path);
    }
}
