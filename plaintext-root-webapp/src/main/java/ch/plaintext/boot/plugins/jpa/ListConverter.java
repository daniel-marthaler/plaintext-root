/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.boot.plugins.jpa;

import jakarta.persistence.Converter;

import java.util.List;

@Converter(autoApply = true)
public class ListConverter extends XstreamBaseJPAConverter<List<String>> {

}