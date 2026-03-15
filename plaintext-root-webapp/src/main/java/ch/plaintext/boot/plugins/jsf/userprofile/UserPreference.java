/*
   Copyright 2009-2022 PrimeTek.

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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
