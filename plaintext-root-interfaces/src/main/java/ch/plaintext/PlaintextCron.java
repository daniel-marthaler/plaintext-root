/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext;

/**
 * Interface for scheduled cron jobs in the Plaintext application.
 * Implementations are auto-discovered and can be managed (enabled/disabled,
 * schedule changed) via the admin cron UI. Each cron job runs per mandate
 * unless {@link #isGlobal()} returns true.
 */
public interface PlaintextCron {

    /**
     * Indicates whether this cron job runs globally (once) or per mandate.
     *
     * @return true if the job should run once globally, false if it should run per mandate
     */
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

    /**
     * Executes the cron job logic for the given mandate.
     *
     * @param mandant the mandate/tenant identifier for which the job runs
     */
    void run(String mandant);

}