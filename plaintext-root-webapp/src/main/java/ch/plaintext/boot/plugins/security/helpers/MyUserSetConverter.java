package ch.plaintext.boot.plugins.security.helpers;

import jakarta.persistence.Converter;

import java.util.Set;

@Converter(autoApply = true)
public class MyUserSetConverter extends MyUserXstreamBaseJPAConverter<Set<String>> {

}