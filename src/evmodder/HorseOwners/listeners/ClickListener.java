package evmodder.HorseOwners.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import evmodder.HorseOwners.HorseLibrary;
import evmodder.HorseOwners.HorseManager;

public class ClickListener implements Listener{
	private HorseManager plugin;
	boolean preventUnleashOther = true;
	final boolean feedPrivateHorse, breedPrivateHorse, snoopPrivateHorse, ridePrivateHorse, nametagRequired;
	boolean leashLivingEntities;

	public ClickListener(){
		plugin = HorseManager.getPlugin();
		preventUnleashOther = plugin.getConfig().getBoolean("prevent-lead-breaking", true);
		feedPrivateHorse = plugin.getConfig().getBoolean("feed-private-horse", false);
		breedPrivateHorse = plugin.getConfig().getBoolean("breed-private-horse", false);
		ridePrivateHorse = plugin.getConfig().getBoolean("ride-private-horse", false);
		snoopPrivateHorse = plugin.getConfig().getBoolean("snoop-private-horse", false);
		leashLivingEntities = plugin.getConfig().getBoolean("leash-other-mobs", false);
		nametagRequired = plugin.getConfig().getBoolean("nametag-required-to-rename", true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onClick(PlayerInteractEntityEvent evt){
		if(evt.isCancelled())return;

		Player p = evt.getPlayer();

		if(evt.getRightClicked() instanceof AbstractHorse){
			AbstractHorse horse = (AbstractHorse) evt.getRightClicked();
			if(horse.isTamed() == false)return;

			//  -=-=-=-=-=-=-=-=-=-=-=-=| Begin Horse Responses |=-=-=-=-=-=-=-=-=-=-=-=-
			if(p.getInventory().getItemInMainHand().getType() == Material.LEAD && horse.isLeashed() == false){
				evt.setCancelled(true);
				horse.setLeashHolder(p);

				if(p.getGameMode() != GameMode.CREATIVE){
					if(p.getInventory().getItemInMainHand().getAmount() == 1){
						p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					}
					p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount()-1);
				}
			}
			else if(horse.isLeashed() && horse.getLeashHolder() instanceof Player /*&& p.isSneaking()*/){
				if(horse.getLeashHolder().getUniqueId().equals(p.getUniqueId())){
					evt.setCancelled(true);
					horse.setLeashHolder(null);
					if(p.getGameMode() != GameMode.CREATIVE){
						horse.getWorld().dropItemNaturally(horse.getLocation(), new ItemStack(Material.LEAD));
					}
				}
			}
			//Block unleashing of horses that are hitched to fenceposts
			//p.sendMessage("�cYou do not have permission to unleash this horse.");

			//if they do NOT own the horse and are NOT holding a leash
			else if(horse.getCustomName() != null && plugin.isClaimedHorse(horse.getCustomName())){
				if(plugin.canAccess(p, horse.getCustomName()) == false){
					if(breedPrivateHorse && HorseLibrary.isBreedingFood(p.getInventory().getItemInMainHand())
							&& horse.canBreed()){
						
						//don't cancel, but make sure a new rider isn't put on the horse
						if(horse.getPassengers() == null || horse.getPassengers().isEmpty()){
							final AbstractHorse finalH = horse;
							new BukkitRunnable(){@Override public void run(){
								if(finalH.getPassengers() != null || !finalH.getPassengers().isEmpty()){
									finalH.eject();
									if(finalH.getPassengers() != null) finalH.getPassengers().clear();
								}
							}}.runTaskLater(plugin, 1);
						}
					}
					else if(feedPrivateHorse && horse.getHealth() < horse.getAttribute(
							Attribute.GENERIC_MAX_HEALTH).getValue()
							&& HorseLibrary.isHorseFood(p.getInventory().getItemInMainHand()))
					{
						//don't cancel, but make sure a new rider isn't put on the horse
						if(horse.getPassengers() == null || horse.getPassengers().isEmpty()){
							final AbstractHorse finalH = horse;
							new BukkitRunnable(){@Override public void run(){
								if(finalH.getPassengers() != null || !finalH.getPassengers().isEmpty()){
									finalH.eject();
									if(finalH.getPassengers() != null) finalH.getPassengers().clear();
								}
							}}.runTaskLater(plugin, 1);
						}
					}
					else if(p.getInventory().getItemInMainHand().getType() == Material.NAME_TAG){
						p.sendMessage(ChatColor.RED+"You do not have permission to rename this horse");
						evt.setCancelled(true);
					}
					else if(plugin.useOneTimeAccess(p.getUniqueId(), horse.getCustomName())){
						p.sendMessage(ChatColor.GREEN+"You have used a one-time-pass to mount this horse, "+
								ChatColor.GRAY+horse.getCustomName()+ChatColor.GREEN+".");
						//allow mount
					}
					else if(p.isSneaking() && snoopPrivateHorse == false){
						p.sendMessage(ChatColor.RED+"You do not have permission to view this horse's inventory.");
						evt.setCancelled(true);
					}
					else if(ridePrivateHorse == false){
						p.sendMessage(ChatColor.RED+"You do not have permission to mount this horse.");
						evt.setCancelled(true);
					}
				}//if(canAccess == false)
				else if(p.getInventory().getItemInMainHand().getType() == Material.NAME_TAG){
//					p.sendMessage(ChatColor.RED+"Use /namehorse to rename your horses.");
					evt.setCancelled(true);

					if(p.getInventory().getItemInMainHand().hasItemMeta() &&
							p.getInventory().getItemInMainHand().getItemMeta().hasDisplayName())
					{
						String newName = p.getInventory().getItemInMainHand().getItemMeta().getDisplayName();

						horse.addPassenger(p);
						plugin.getCommand("claimhorse").execute(p, "claimhorse", new String[]{newName});
						horse.eject();
					}
				}
			}
		}
		//leash any clicked LivingEntity, handy to have
		else if(leashLivingEntities && evt.getRightClicked() instanceof LivingEntity){
			LivingEntity le = (LivingEntity) evt.getRightClicked();

			if(le.isLeashed() && le.getLeashHolder().getUniqueId().equals(p.getUniqueId())){
				evt.setCancelled(true);
				le.setLeashHolder(null);
				if(p.getGameMode() != GameMode.CREATIVE){
					le.getWorld().dropItemNaturally(le.getLocation(), new ItemStack(Material.LEAD));
				}
			}
			else if(p.getInventory().getItemInMainHand().getType() == Material.LEAD && !le.isLeashed()
					&& le instanceof Player == false){//Errors happen when trying to leash other players
				evt.setCancelled(true);
				le.setLeashHolder(p);
				if(p.getGameMode() != GameMode.CREATIVE){
					if(p.getInventory().getItemInMainHand().getAmount() == 1){
						p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					}
					p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount()-1);
				}
			}
		}
		else if(preventUnleashOther && evt.getRightClicked() instanceof LeashHitch){
			LeashHitch leash = (LeashHitch) evt.getRightClicked();

			for(Entity e : leash.getNearbyEntities(15, 15, 15)){
				if(e instanceof AbstractHorse && e.getCustomName() != null){
					AbstractHorse h = (AbstractHorse) e;
					if(h.isLeashed() && h.getLeashHolder().getUniqueId().equals(leash.getUniqueId())){
						if(plugin.canAccess(p, h.getCustomName()) == false){
							evt.setCancelled(true);
							p.sendMessage(ChatColor.RED+"You do not have permission to unleash this horse.");
						}
					}
				}
			}
			//would be nice if this existed "leash.getPassenger()"
			//but since it doesn't, just use the above
//			if(leash.getPassenger() instanceof Horse){
//				Horse horse = (Horse) leash.getPassenger();
//				getLogger().info("lala2");
//				if(isPrivateHorse(horse) && isOwner(p, horse) == false){
//					getLogger().info("lala3");
//					evt.setCancelled(true);
//					p.sendMessage("�cYou do not have permission to unleash this horse.");
//				}
//			}
		}
	}
}