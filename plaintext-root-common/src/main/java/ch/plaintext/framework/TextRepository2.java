/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.framework;/*
 * Copyright (C) eMad, 2016.
 */

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for a JPA2 Entity instances. Provides basic CRUD
 * operations due to the extension of <br/>
 * </>{@link JpaRepository}. Includes
 * custom implemented <br/>
 * </>functionality by extending
 * {@link JpaRepository}.
 *
 * @author $Author: daniel.marthaler@plaintext.ch $
 * @since 0.1.1
 */
public interface TextRepository2 extends JpaRepository<Text2, Long>, PlaintextRepository<Text2> {

    Text2 findByKey(String key);

    Text2 findByKeyAndMandat(String key, String mandant);

    List<Text2> findByTypeAndMandat(String type, String mandat);

    List<Text2> findByType(String type);

    List<Text2> findByIndexAndMandat(String index, String mandat);

    List<Text2> findByIndex1AndMandat(String index1, String mandat);

    List<Text2> findByIndex2AndMandat(String index2, String mandat);

    List<Text2> findByIndex3AndMandat(String index3, String mandat);

    List<Text2> findByIndex4AndMandat(String index4, String mandat);

}
