/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.framework;/*
 * Copyright (C) eMad, 2017.
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author info@emad.ch
 * @since 2017
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Text2 extends SuperModel {

    @NotNull
    @Column(length = 200000)
    private String value = null;

    @NotNull
    private String key = null;

    private String index;

    private String index1;

    private String index2;

    private String index3;

    private String index4;

    private String type;


}