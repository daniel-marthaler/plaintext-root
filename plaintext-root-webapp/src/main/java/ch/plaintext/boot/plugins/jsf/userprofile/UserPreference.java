/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.jsf.userprofile;
import ch.plaintext.boot.plugins.objstore.SimpleStorable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@Data
public class UserPreference implements SimpleStorable<UserPreference>, Serializable {

    private String menuMode = "layout-sidebar";

    private String darkMode = "light";

    private String componentTheme = "green";

    private String topbarTheme = "light";

    private String menuTheme = "light";

    private String inputStyle = "outlined";

    private boolean lightLogo = false;

    private boolean menuStatic = true;  // Sidebar is expanded/pinned by default

    private String user = "";

    @Override
    public String getUniqueId() {
        return user;
    }

    @Override
    public void setUniqueId(String id) {
        user = id;
    }
}
