/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.PlaintextSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronControllerTest {

    @Mock
    private CronConfigRepository cronConfigRepository;

    @Mock
    private ApplicationContext ctx;

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @InjectMocks
    private CronController cronController;

    private SuperCron createTestSuperCron(String name, boolean global) {
        SuperCron sc = new SuperCron() {
            @Override
            public void run(String mandant) {
                // no-op
            }

            @Override
            public boolean isGlobal() {
                return global;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDefaultCronExpression() {
                return "0 0 * * *";
            }
        };
        sc.setBeanName(name + "Bean");
        sc.setApplicationContext(ctx);
        return sc;
    }

    @BeforeEach
    void setUp() throws Exception {
        // Set the crons list field via reflection (normally @Autowired)
        Field cronsField = CronController.class.getDeclaredField("crons");
        cronsField.setAccessible(true);
        cronsField.set(cronController, new ArrayList<PlaintextCron>());
    }

    // --- validateAndFixCronExpression (private, tested via schedule indirectly) ---

    @Test
    void validateAndFixCronExpression_nullExpression() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "validateAndFixCronExpression", String.class, String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, null, "test", "mandatA");
        assertThat(result).isEqualTo("0 0 * * *");
    }

    @Test
    void validateAndFixCronExpression_emptyExpression() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "validateAndFixCronExpression", String.class, String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, "", "test", "mandatA");
        assertThat(result).isEqualTo("0 0 * * *");
    }

    @Test
    void validateAndFixCronExpression_sixFieldConvertedToFive() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "validateAndFixCronExpression", String.class, String.class, String.class);
        method.setAccessible(true);

        // 6-field (Spring/Quartz) pattern: seconds minutes hours dayOfMonth month dayOfWeek
        String result = (String) method.invoke(cronController, "0 30 6 * * *", "test", "mandatA");
        assertThat(result).isEqualTo("30 6 * * *");
    }

    @Test
    void validateAndFixCronExpression_fiveFieldPassedThrough() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "validateAndFixCronExpression", String.class, String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, "30 6 * * *", "test", "mandatA");
        assertThat(result).isEqualTo("30 6 * * *");
    }

    @Test
    void validateAndFixCronExpression_invalidFieldCountPassedThrough() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "validateAndFixCronExpression", String.class, String.class, String.class);
        method.setAccessible(true);

        // 3 fields - invalid, but returned as-is (will fail at scheduling)
        String result = (String) method.invoke(cronController, "30 6 *", "test", "mandatA");
        assertThat(result).isEqualTo("30 6 *");
    }

    // --- applyTimeOffset (private) ---

    @Test
    void applyTimeOffset_nullExpression() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, null, 1);
        assertThat(result).isNull();
    }

    @Test
    void applyTimeOffset_emptyExpression() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, "", 1);
        assertThat(result).isEmpty();
    }

    @Test
    void applyTimeOffset_zeroOffset() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        String result = (String) method.invoke(cronController, "0 6 * * *", 0);
        assertThat(result).isEqualTo("0 6 * * *");
    }

    @Test
    void applyTimeOffset_appliesOffset() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        // Offset 1 -> 2 minutes added to minute field
        String result = (String) method.invoke(cronController, "0 6 * * *", 1);
        assertThat(result).isEqualTo("2 6 * * *");
    }

    @Test
    void applyTimeOffset_wrapsMinutesAt60() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        // Offset 30 -> 60 minutes, 55 + 60 = 115 % 60 = 55
        String result = (String) method.invoke(cronController, "55 6 * * *", 30);
        assertThat(result).isEqualTo("55 6 * * *");  // (55 + 60) % 60 = 55
    }

    @Test
    void applyTimeOffset_skipsNonNumericMinuteField() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        // */15 is not a simple numeric, so expression returned as-is
        String result = (String) method.invoke(cronController, "*/15 6 * * *", 1);
        assertThat(result).isEqualTo("*/15 6 * * *");
    }

    @Test
    void applyTimeOffset_nonStandardFieldCount() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "applyTimeOffset", String.class, int.class);
        method.setAccessible(true);

        // 6 fields - can't modify, returned as-is
        String result = (String) method.invoke(cronController, "0 0 6 * * *", 1);
        assertThat(result).isEqualTo("0 0 6 * * *");
    }

    // --- save ---

    @Test
    void savePreservesTransientCronReference() {
        CronConfigEntity entity = new CronConfigEntity();
        SuperCron sc = createTestSuperCron("TestCron", false);
        entity.setCron(sc);

        CronConfigEntity savedEntity = new CronConfigEntity();
        when(cronConfigRepository.save(any(CronConfigEntity.class))).thenReturn(savedEntity);

        CronConfigEntity result = cronController.save(entity);

        assertThat(result.getCron()).isSameAs(sc);
        verify(cronConfigRepository).save(entity);
    }

    // --- findCronEntity (private) ---

    @Test
    void findCronEntity_findsMatchingEntity() throws Exception {
        // Set up cronsMap with an entity
        CronConfigEntity entity = new CronConfigEntity();
        entity.setCronName("TestCron");
        entity.setMandat("mandatA");

        Map<String, List<CronConfigEntity>> map = new HashMap<>();
        map.put("mandatA", new ArrayList<>(List.of(entity)));
        cronController.getCronsMap().putAll(map);

        Method method = CronController.class.getDeclaredMethod(
                "findCronEntity", String.class, String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(cronController, "TestCron", "mandatA");
        assertThat(result).isSameAs(entity);
    }

    @Test
    void findCronEntity_returnsNullWhenNotFound() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "findCronEntity", String.class, String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(cronController, "NotExist", "mandatX");
        assertThat(result).isNull();
    }

    @Test
    void findCronEntity_returnsNullWhenMandatNotInMap() throws Exception {
        Method method = CronController.class.getDeclaredMethod(
                "findCronEntity", String.class, String.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(cronController, "TestCron", "unknownMandat");
        assertThat(result).isNull();
    }

    // --- createCronConfigEntity (private) ---

    @Test
    void createCronConfigEntity_setsDefaults() throws Exception {
        SuperCron sc = createTestSuperCron("TestCron", false);

        Method method = CronController.class.getDeclaredMethod(
                "createCronConfigEntity", SuperCron.class);
        method.setAccessible(true);

        CronConfigEntity result = (CronConfigEntity) method.invoke(cronController, sc);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isStartup()).isTrue();
        assertThat(result.getCronExpression()).isEqualTo("0 0 * * *");
    }

    // --- cronsMap getter ---

    @Test
    void getCronsMapReturnsEmptyMapByDefault() {
        assertThat(cronController.getCronsMap()).isNotNull();
        assertThat(cronController.getCronsMap()).isEmpty();
    }
}
