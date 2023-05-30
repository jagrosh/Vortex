package com.jagrosh.vortex.database.managers;

import com.jagrosh.vortex.database.Database;

import java.util.List;

/**
 * An interface used by all data managers that are in charge of dealing with modlogs
 */
public interface ModlogManager {
    /**
     * Gets the maximum case ID in the case
     * @param guildId Guild ID
     * @return The maximum id
     */
    int getMaxId(long guildId);

    /**
     * Updates a reason of a specific modlog
     * @param guildId Guild ID
     * @param caseId Case ID
     * @param reason The new reason
     * @return The old reason, or null if there was no old reason
     */
    String updateReason(long guildId, int caseId, String reason);

    /**
     * Deletes a case
     * @param guildId Guild ID
     * @param caseId Case ID
     * @return A modlog object of the deleted modlog if the code was successfull, null if it couldn't be deleted or was not found
     * @see Database.Modlog
     */
    Database.Modlog deleteCase(long guildId, int caseId);

    /**
     * Returns a list of modlogs for a specific user
     * @param guildId Guild ID
     * @param userId User Id
     * @return A list of all modlogs for the user that the manager is in charge of.
     */
    List<Database.Modlog> getModlogs(long guildId, long userId);
}