package ch.plaintext.framework;

import jakarta.persistence.Converter;

import java.util.List;

@Converter(autoApply = true)
public class StringArrayJPAConverter extends XstreamBaseJPAConverter<List<String>> {

}