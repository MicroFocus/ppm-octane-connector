package com.ppm.integration.agilesdk.connector.octane.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class XmlUtils {

    public static final String CHARSET = "UTF-8";

    public static void toXml(Object o, OutputStream output) {
        try {
            JAXBContext context = JAXBContext.newInstance(o.getClass());
            Marshaller marshaller = context.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_ENCODING, CHARSET);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(o, new OutputStreamWriter(output, CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static String toXml(Object o) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            toXml(o, output);
            output.flush();
            return output.toString(CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @SuppressWarnings("unchecked") public static <T> T fromXml(InputStream input, Class<T> clazz) {

        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return (T)unmarshaller.unmarshal(new InputStreamReader(input, CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T fromXml(String xmlStr, Class<T> clazz) {
        return fromXml(new ByteArrayInputStream(xmlStr.getBytes(Charset.forName(CHARSET))), clazz);
    }
}
