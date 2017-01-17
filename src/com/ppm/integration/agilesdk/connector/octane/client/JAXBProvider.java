package com.ppm.integration.agilesdk.connector.octane.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlRootElement;

@Provider @Produces(MediaType.APPLICATION_XML) @Consumes(MediaType.APPLICATION_XML) public class JAXBProvider
        implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    @Override public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {

        return -1;
    }

    @Override public boolean isWriteable(Class<?> cls, Type arg1, Annotation[] arg2, MediaType arg3) {

        XmlRootElement xmlRootEl = cls.getAnnotation(XmlRootElement.class);
        return xmlRootEl != null;
    }

    @Override public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType mediaType,
            MultivaluedMap<String, Object> header, OutputStream output) throws IOException, WebApplicationException
    {

        XmlUtils.toXml(arg0, output);
    }

    @Override public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return isWriteable(arg0, arg1, arg2, arg3);
    }

    @Override public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> arg4, InputStream input) throws IOException, WebApplicationException
    {

        return XmlUtils.fromXml(input, arg0);
    }

}
