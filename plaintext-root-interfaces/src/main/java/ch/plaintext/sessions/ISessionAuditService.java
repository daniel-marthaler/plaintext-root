/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.sessions;

import org.springframework.security.core.Authentication;

public interface ISessionAuditService {

    void updateOrCreate(Long userId, String sessionId, Authentication authentication, String userAgent);

    void commit(Long userId);
}
