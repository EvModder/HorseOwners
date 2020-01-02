package net.evmodder.HorseOwners.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event created by the HorseOwners plugin when a horse is renamed by a player.
 * Note: does not get called when claiming a new horse.
 * @since X.X.X-SNAPSHOT
 * @author evmodder (_ at gmail.com)
 */
public class HorseRenameEvent extends Event implements Cancellable, HorseOwnerEvent{
	private static final HandlerList HANDLERS = new HandlerList();
	private boolean canceled = false;

	final String oldName, newName;
	final String fullOldName, fullNewName;

	public HorseRenameEvent(final String oldName, final String newName, final String oldNameRaw, final String newNameRaw){
		this.oldName = oldName;
		this.newName = newName;
		this.fullOldName = oldNameRaw;
		this.fullNewName = newNameRaw;
	}

	public String getOldFullName(){return fullOldName;}
	public String getNewFullName(){return fullNewName;}
	public String getOldConfigName(){return oldName;}
	public String getNewConfigName(){return newName;}

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