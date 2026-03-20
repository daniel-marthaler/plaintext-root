/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.cron;

import ch.plaintext.PlaintextSecurity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CronBackingBeanTest {

    @Mock
    private CronController cronController;

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @InjectMocks
    private CronBackingBean backingBean;

    private CronConfigEntity createEntity(String name, String mandat) {
        CronConfigEntity entity = new CronConfigEntity();
        entity.setCronName(name);
        entity.setMandat(mandat);
        entity.setEnabled(true);
        entity.setStartup(false);
        entity.setCronExpression("0 6 * * *");
        return entity;
    }

    private Map<String, List<CronConfigEntity>> buildCronsMap() {
        Map<String, List<CronConfigEntity>> cronsMap = new HashMap<>();
        cronsMap.put("mandatA", new ArrayList<>(List.of(
                createEntity("Cron1", "mandatA"),
                createEntity("Cron2", "mandatA")
        )));
        cronsMap.put("mandatB", new ArrayList<>(List.of(
                createEntity("Cron1", "mandatB")
        )));
        cronsMap.put("global", new ArrayList<>(List.of(
                createEntity("GlobalCron", "global")
        )));
        return cronsMap;
    }

    private void setupCronsMapAndRefresh(String mandat, boolean isRoot) {
        when(cronController.getCronsMap()).thenReturn(buildCronsMap());
        when(plaintextSecurity.getMandat()).thenReturn(mandat);
        when(plaintextSecurity.ifGranted("root")).thenReturn(isRoot);
        backingBean.refreshCrons();
    }

    // --- refreshCrons / init ---

    @Test
    void refreshCrons_nonRootUserSeesOwnMandantOnly() {
        setupCronsMapAndRefresh("mandatA", false);

        assertThat(backingBean.getCrons()).hasSize(2);
        assertThat(backingBean.getCrons()).allMatch(e -> "mandatA".equals(e.getMandat()));
    }

    @Test
    void refreshCrons_rootUserSeesAllMandanten() {
        setupCronsMapAndRefresh("mandatA", true);

        // Should see all 4 entities across all mandanten
        assertThat(backingBean.getCrons()).hasSize(4);
    }

    @Test
    void refreshCrons_nonRootUserWithUnknownMandantGetsEmptyList() {
        setupCronsMapAndRefresh("unknownMandat", false);

        assertThat(backingBean.getCrons()).isEmpty();
    }

    @Test
    void refreshCrons_clearsOldCronsBeforeReloading() {
        setupCronsMapAndRefresh("mandatA", false);
        assertThat(backingBean.getCrons()).hasSize(2);

        // Refresh again - should still be 2, not 4
        backingBean.refreshCrons();
        assertThat(backingBean.getCrons()).hasSize(2);
    }

    // --- select ---

    @Test
    void select_nonExistentCronDoesNotSetCrone() {
        setupCronsMapAndRefresh("mandatA", false);

        backingBean.setCrone("oldValue");
        backingBean.select("NonExistent", "mandatA");

        // crone should remain unchanged since entity was not found
        assertThat(backingBean.getCrone()).isEqualTo("oldValue");
    }

    @Test
    void select_setssCroneFieldWhenEntityFoundWithNullExpression() {
        setupCronsMapAndRefresh("mandatA", false);

        // Set null expression so validate() is NOT called (avoids FacesContext dependency)
        backingBean.getCrons().get(0).setCronExpression(null);

        backingBean.select("Cron1", "mandatA");

        assertThat(backingBean.getCrone()).isNull();
    }

    @Test
    void select_doesNotCallValidateWhenCroneIsEmpty() {
        setupCronsMapAndRefresh("mandatA", false);

        // Create entity with empty expression
        backingBean.getCrons().get(0).setCronExpression("");

        // Should not throw, since validate() is skipped for empty crone
        backingBean.select("Cron1", "mandatA");

        assertThat(backingBean.getCrone()).isEmpty();
    }

    @Test
    void select_doesNotCallValidateWhenCroneIsNull() {
        setupCronsMapAndRefresh("mandatA", false);

        // Create entity with null expression
        backingBean.getCrons().get(0).setCronExpression(null);

        // Should not throw, since validate() is skipped for null crone
        backingBean.select("Cron1", "mandatA");

        assertThat(backingBean.getCrone()).isNull();
    }

    // --- isEnabled / isStartup ---

    @Test
    void isEnabled_returnsTrueForEnabledCron() {
        setupCronsMapAndRefresh("mandatA", false);

        assertThat(backingBean.isEnabled("Cron1", "mandatA")).isTrue();
    }

    @Test
    void isEnabled_returnsFalseForNonExistentCron() {
        setupCronsMapAndRefresh("mandatA", false);

        assertThat(backingBean.isEnabled("NotExist", "mandatA")).isFalse();
    }

    @Test
    void isEnabled_returnsFalseForDisabledCron() {
        setupCronsMapAndRefresh("mandatA", false);
        backingBean.getCrons().get(0).setEnabled(false);

        assertThat(backingBean.isEnabled("Cron1", "mandatA")).isFalse();
    }

    @Test
    void isStartup_returnsFalseForDefaultCron() {
        setupCronsMapAndRefresh("mandatA", false);

        assertThat(backingBean.isStartup("Cron1", "mandatA")).isFalse();
    }

    @Test
    void isStartup_returnsFalseForNonExistentCron() {
        setupCronsMapAndRefresh("mandatA", false);

        assertThat(backingBean.isStartup("NotExist", "mandatA")).isFalse();
    }

    @Test
    void isStartup_returnsTrueWhenStartupEnabled() {
        setupCronsMapAndRefresh("mandatA", false);
        backingBean.getCrons().get(0).setStartup(true);

        assertThat(backingBean.isStartup("Cron1", "mandatA")).isTrue();
    }

    // --- toggleEnabled ---

    @Test
    void toggleEnabled_togglesFromEnabledToDisabledAndSaves() {
        setupCronsMapAndRefresh("mandatA", false);

        CronConfigEntity savedEntity = createEntity("Cron1", "mandatA");
        savedEntity.setEnabled(false);
        when(cronController.save(any(CronConfigEntity.class))).thenReturn(savedEntity);

        backingBean.toggleEnabled("Cron1", "mandatA");

        verify(cronController).save(any(CronConfigEntity.class));
        // The entity's enabled was toggled from true -> false, so unschedule should be called
        verify(cronController).unschedule(any(CronConfigEntity.class));
    }

    @Test
    void toggleEnabled_nonExistentCronDoesNothing() {
        setupCronsMapAndRefresh("mandatA", false);

        backingBean.toggleEnabled("NotExist", "mandatA");

        verify(cronController, never()).save(any());
    }

    @Test
    void toggleEnabled_toEnabledSchedulesCron() {
        setupCronsMapAndRefresh("mandatA", false);

        // Disable first
        CronConfigEntity entity = backingBean.getCrons().get(0);
        entity.setEnabled(false);

        CronConfigEntity savedEntity = createEntity("Cron1", "mandatA");
        savedEntity.setEnabled(true);
        when(cronController.save(any(CronConfigEntity.class))).thenReturn(savedEntity);

        backingBean.toggleEnabled("Cron1", "mandatA");

        verify(cronController).schedule(any(CronConfigEntity.class));
    }

    // --- toggleStartup ---

    @Test
    void toggleStartup_togglesAndSaves() {
        setupCronsMapAndRefresh("mandatA", false);

        backingBean.toggleStartup("Cron1", "mandatA");

        verify(cronController).save(any(CronConfigEntity.class));
    }

    @Test
    void toggleStartup_nonExistentCronDoesNothing() {
        setupCronsMapAndRefresh("mandatA", false);

        backingBean.toggleStartup("NotExist", "mandatA");

        verify(cronController, never()).save(any());
    }

    // --- getNextRun ---

    @Test
    void getNextRun_returnsDateForValidExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("0 6 * * *");

        Date result = backingBean.getNextRun(entity);

        assertThat(result).isNotNull();
        assertThat(result).isAfter(new Date());
    }

    @Test
    void getNextRun_returnsNullForNullEntity() {
        assertThat(backingBean.getNextRun(null)).isNull();
    }

    @Test
    void getNextRun_returnsNullForNullExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression(null);

        assertThat(backingBean.getNextRun(entity)).isNull();
    }

    @Test
    void getNextRun_returnsNullForEmptyExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("");

        assertThat(backingBean.getNextRun(entity)).isNull();
    }

    @Test
    void getNextRun_returnsNullForInvalidExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("invalid expression");

        assertThat(backingBean.getNextRun(entity)).isNull();
    }

    // --- getWann ---

    @Test
    void getWann_returnsStringForValidExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("0 6 * * *");

        String result = backingBean.getWann(entity);

        assertThat(result).isNotNull();
        assertThat(result).isNotEqualTo("-");
    }

    @Test
    void getWann_returnsDashForNullEntity() {
        assertThat(backingBean.getWann(null)).isEqualTo("-");
    }

    @Test
    void getWann_returnsDashForNullExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression(null);

        assertThat(backingBean.getWann(entity)).isEqualTo("-");
    }

    @Test
    void getWann_returnsDashForEmptyExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("");

        assertThat(backingBean.getWann(entity)).isEqualTo("-");
    }

    @Test
    void getWann_returnsDashForInvalidExpression() {
        CronConfigEntity entity = createEntity("Test", "mandatA");
        entity.setCronExpression("invalid expression");

        assertThat(backingBean.getWann(entity)).isEqualTo("-");
    }

    // --- isRoot ---

    @Test
    void isRoot_returnsTrueWhenUserHasRootRole() {
        when(plaintextSecurity.ifGranted("root")).thenReturn(true);

        assertThat(backingBean.isRoot()).isTrue();
    }

    @Test
    void isRoot_returnsFalseWhenUserDoesNotHaveRootRole() {
        when(plaintextSecurity.ifGranted("root")).thenReturn(false);

        assertThat(backingBean.isRoot()).isFalse();
    }

    // --- findCron (by name only, private) ---

    @Test
    void findCronByName_prefersCurrentMandant() throws Exception {
        setupCronsMapAndRefresh("mandatA", true);

        java.lang.reflect.Method method = CronBackingBean.class.getDeclaredMethod("findCron", String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(backingBean, "Cron1");
        assertThat(result).isNotNull();
        assertThat(result.getMandat()).isEqualTo("mandatA");
    }

    @Test
    void findCronByName_fallsBackToFirstMatch() throws Exception {
        setupCronsMapAndRefresh("unknownMandat", true);

        java.lang.reflect.Method method = CronBackingBean.class.getDeclaredMethod("findCron", String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(backingBean, "Cron1");
        assertThat(result).isNotNull();
        assertThat(result.getCronName()).isEqualTo("Cron1");
    }

    @Test
    void findCronByName_returnsNullForUnknownCron() throws Exception {
        setupCronsMapAndRefresh("mandatA", false);

        java.lang.reflect.Method method = CronBackingBean.class.getDeclaredMethod("findCron", String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(backingBean, "Unknown");
        assertThat(result).isNull();
    }

    // --- findCron (by name and mandat, private) ---

    @Test
    void findCronByNameAndMandat_findsExact() throws Exception {
        setupCronsMapAndRefresh("mandatA", false);

        java.lang.reflect.Method method = CronBackingBean.class.getDeclaredMethod("findCron", String.class, String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(backingBean, "Cron1", "mandatA");
        assertThat(result).isNotNull();
        assertThat(result.getCronName()).isEqualTo("Cron1");
        assertThat(result.getMandat()).isEqualTo("mandatA");
    }

    @Test
    void findCronByNameAndMandat_returnsNullForWrongMandat() throws Exception {
        setupCronsMapAndRefresh("mandatA", false);

        java.lang.reflect.Method method = CronBackingBean.class.getDeclaredMethod("findCron", String.class, String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(backingBean, "Cron1", "mandatB");
        assertThat(result).isNull();
    }

    // --- crone getter/setter ---

    @Test
    void croneGetterSetter() {
        backingBean.setCrone("*/5 * * * *");
        assertThat(backingBean.getCrone()).isEqualTo("*/5 * * * *");
    }

    // --- getCrons ---

    @Test
    void getCrons_returnsEmptyListByDefault() {
        assertThat(backingBean.getCrons()).isNotNull();
        assertThat(backingBean.getCrons()).isEmpty();
    }
}
