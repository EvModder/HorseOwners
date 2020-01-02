package net.evmodder.HorseOwners.api.events;

import org.bukkit.entity.Entity;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

/**
 * Event created by the HorseOwners plugin when a horse is claimed by a player.
 * Note: does not occur when renaming an already claimed horse.
 * @since X.X.X-SNAPSHOT
 * @author evmodder (_ at gmail.com)
 */
public class HorseClaimEvent extends EntityEvent implements Cancellable, HorseOwnerEvent{
	private static final HandlerList HANDLERS = new HandlerList();
	private boolean canceled = false;

//	final Entity horse;
	final UUID playerUUID;
	final String claimName;

	public HorseClaimEvent(final Entity horse, final UUID playerUUID, final String claimName) {
		super(horse);
//		this.horse = horse;
		this.playerUUID = playerUUID;
		this.claimName = claimName;
	}

//	public Entity getHorse(){return horse;} // Use getEntity() instead
	public UUID getClaimer(){return playerUUID;}
	public String getClaimName(){return claimName;}

	/**
	 * Whether the event has been cancelled.
	 * @return Whether the event has been cancelled.
	 */
	@Override public boolean isCancelled(){return canceled;}
	/**
	 * Sets whether the event should be cancelled.
	 * @param cancel whether the event should be cancelled.
	 */
	@Override public void setCancelled(final boolean cancel){canceled = cancel;}

	/**
	 * Get a list of handlers for the event.
	 * @return a list of handlers for the event
	 */
	@Override public HandlerList getHandlers(){return HANDLERS;}
}