package ch.plaintext.framework;

import com.thoughtworks.xstream.XStream;
import jakarta.persistence.AttributeConverter;

import java.util.Arrays;

public class XstreamBaseJPAConverter<T> implements AttributeConverter<T, String> {

    private XStream xstream = new XStream();

    @Override
    public String convertToDatabaseColumn(T object) {
        return xstream.toXML(object);
    }

    @Override
    public T convertToEntityAttribute(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return (T) xstream.fromXML(text);
        } catch (Exception e) {
            // todo kann weg im 2021
            if (text != null && !text.isEmpty()) {
                return (T) Arrays.asList(text.split(","));
            }
        }
        return null;
    }

}