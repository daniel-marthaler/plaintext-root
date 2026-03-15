package ch.plaintext.cron;

import ch.plaintext.PlaintextCron;
import it.sauronsoftware.cron4j.Predictor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class SuperCron implements PlaintextCron, InitializingBean, ApplicationContextAware, BeanNameAware, Runnable {

    // @Autowired
    // private LinkLoginController linkLogin;

    // Temporary stub - replace with actual controller
    private void loginMandatDummy(String mandant) {
        // Stub implementation - would normally set up security context
    }

    private Object state;

    public void setState(Object obj) {
        this.state = obj;
    }

    public Object getState() {
        return state;
    }

    private boolean running = false;

    private Date last;

    private int seconds;

    private StopWatch watch = new StopWatch();

    private String myName;

    private ApplicationContext context;

    private boolean startup = false;

    private boolean enabled = false;

    private boolean global = false;
    @Getter
    @Setter
    private String mandant = "n/a";
    @Getter
    private int counter;
    private String cron = "0 0 * * *";

    @Getter
    @Setter
    private Class<? extends PlaintextCron> originalBeanClass;

    public String getCronString() {
        return cron;
    }

    public void setCronString(String cron) {
        this.cron = cron;
    }

    public Date getNextRun() {
        try {
            String cronString = getCronString();
            if (cronString == null || cronString.trim().isEmpty()) {
                log.warn("Cron '{}' has no cron string set", getName());
                return null;
            }
            Predictor pr = new Predictor(cronString);
            return pr.nextMatchingDate();
        } catch (Exception e) {
            log.error("Failed to calculate next run for cron '{}' (mandant: {}) with pattern '{}': {}",
                    getName(), getMandant(), getCronString(), e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    public int getPercente() {
        if (watch.isStarted()) {
            watch.split();
            int zeit = (int) watch.getSplitTime() / 1000;
            int sec = 1;
            if (seconds < 1) {
                sec = 1;
            }

            if (zeit < 1) {
                zeit = 1;
            }

            int p = (sec * 100) / zeit;

            log.debug("seconds: " + sec + " / " + zeit + " = " + p);

            return 100 - p;
        }
        return 0;
    }

    public Date getLastRun() {
        return last;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getDisplayName() {
        // Default implementation returns the simple class name
        // Subclasses can override this to provide a more descriptive name
        return getClass().getSimpleName();
    }

    @Override
    public String getDefaultCronExpression() {
        // Default implementation returns daily at midnight
        // Subclasses can override this to provide their own default schedule
        return "0 0 * * *";
    }

    public String getBeanName() {
        return myName;
    }

    public int getLastSeconds() {
        return seconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        watch = new StopWatch();
        watch.start();
        running = true;
    }

    public String getWann() {
        try {
            if (getCronString() == null || getCronString().trim().isEmpty()) {
                return "-";
            }

            PrettyTime t = new PrettyTime(new Date());
            t.setLocale(Locale.GERMAN);

            Predictor pr = new Predictor(getCronString());
            Date d = pr.nextMatchingDate();

            return t.format(d);
        } catch (Exception e) {
            log.error("Failed to calculate next run time for cron '{}' (mandant: {}) with pattern '{}': {}",
                    getName(), getMandant(), getCronString(), e.getMessage());
            return "ERROR: Invalid pattern";
        }
    }


    public void ende() {
        last = new Date();
        running = false;
        seconds = (int) watch.getTime(TimeUnit.SECONDS);
        watch.reset();
        counter++;

        // Sync execution statistics to the persistent entity
        syncToEntity();
    }

    /**
     * Synchronizes the execution statistics (counter, lastRun, lastSeconds)
     * from this SuperCron instance to the CronConfigEntity.
     */
    private void syncToEntity() {
        if (state instanceof CronConfigEntity) {
            CronConfigEntity entity = (CronConfigEntity) state;
            entity.setCounter(counter);
            entity.setLastRun(last);
            entity.setLastSeconds(seconds);
        }
    }

    /**
     * Loads the execution statistics from the CronConfigEntity into this SuperCron instance.
     * This should be called during initialization to restore the state from the database.
     */
    public void loadFromEntity() {
        if (state instanceof CronConfigEntity) {
            CronConfigEntity entity = (CronConfigEntity) state;
            if (entity.getCounter() != null) {
                this.counter = entity.getCounter();
            }
            if (entity.getLastRun() != null) {
                this.last = entity.getLastRun();
            }
            if (entity.getLastSeconds() != null) {
                this.seconds = entity.getLastSeconds();
            }
        }
    }


    public void run() {
        log.info(">>> Starting cron '{}' for mandant '{}'", getName(), getMandant());
        start();

        loginMandatDummy(getMandant());

        try {
            run(getMandant());
            log.info(">>> Cron '{}' for mandant '{}' completed successfully", getName(), getMandant());
        } catch (Exception e) {
            log.error(">>> ERROR in cron '{}' for mandant '{}'", getName(), getMandant(), e);
            throw e;
        } finally {
            ende();
            log.info(">>> Cron '{}' ended. Counter: {}, Last run: {}, Duration: {}s",
                    getName(), getCounter(), getLastRun(), getLastSeconds());
        }
    }

    @Override
    public abstract void run(String mandant);


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setBeanName(String s) {
        this.myName = s;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.context.isSingleton(this.myName)) {
            throw new RuntimeException("Bean CANNOT be singleton");
        }
    }

}
