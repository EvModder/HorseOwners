package net.evmodder.HorseOwners.api.events;

import org.bukkit.entity.AbstractHorse;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event created by the HorseOwners plugin when a claimable horse dies
 * TODO: does not occur for non-living entities
 * @since X.X.X-SNAPSHOT
 * @author evmodder (_ at gmail.com)
 */
public class HorseDeathEvent extends EntityDeathEvent implements HorseOwnerEvent{
	private static final HandlerList HANDLERS = new HandlerList();

	public HorseDeathEvent(final AbstractHorse horse, List<ItemStack> drops, int droppedExp){
		super(horse, drops, droppedExp);
//		this.horse = horse;
	}

//	public Entity getHorse(){return horse;} // Use getEntity() instead

	/**
	 * Get a list of handlers for the event.
	 * @return a list of handlers for the event
	 */
	@Override public HandlerList getHandlers(){return HANDLERS;}

	/**
	 * Get a list of handlers for the event.
	 * @return a list of handlers for the event
	 */
	public static HandlerList getHandlerList(){return HANDLERS;}
}