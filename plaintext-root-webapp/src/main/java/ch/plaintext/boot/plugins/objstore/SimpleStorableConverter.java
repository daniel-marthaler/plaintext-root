package ch.plaintext.boot.plugins.objstore;

import ch.plaintext.boot.plugins.jpa.XstreamBaseJPAConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SimpleStorableConverter extends XstreamBaseJPAConverter<SimpleStorable> {

}