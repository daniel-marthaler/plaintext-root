/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings;

public interface ISetupConfigService {

    boolean isAutologinEnabled(String mandat);

    boolean isOidcAutoRedirectEnabled(String mandat);

    Long getOidcAutoRedirectConfigId(String mandat);
}
