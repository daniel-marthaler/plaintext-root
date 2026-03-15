/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.boot.plugins.jpa;

import jakarta.persistence.Converter;

import java.util.Set;

@Converter(autoApply = true)
public class SetConverter extends XstreamBaseJPAConverter<Set<String>> {

}