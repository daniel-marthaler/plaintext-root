package ch.plaintext;

public interface PlaintextCron {

    default boolean isGlobal() {
        return false;
    }

    /**
     * Returns a human-readable display name for this cron job.
     * This name will be shown in the cron jobs table.
     *
     * @return the display name (defaults to simple class name if not overridden)
     */
    default String getDisplayName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the default cron expression for this job.
     * This expression will be used when creating a new database entry for this cron.
     *
     * @return the default cron expression (defaults to "0 0 * * *" - every day at midnight)
     */
    default String getDefaultCronExpression() {
        return "0 0 * * *";
    }

    void run(String mandant);

}