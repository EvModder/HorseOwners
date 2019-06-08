package net.evmodder.HorseOwners.listeners;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
import net.evmodder.HorseOwners.HorseLibrary;
import net.evmodder.HorseOwners.HorseManager;
import net.evmodder.HorseOwners.commands.CommandClaimHorse;

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

	static boolean isUsableNametag(ItemStack item){
		return item != null && item.getType() == Material.NAME_TAG
				&& item.hasItemMeta() && item.getItemMeta().hasDisplayName();
	}

	void delayedEject(final AbstractHorse horse, final UUID uuid){
		new BukkitRunnable(){@Override public void run(){
			if(horse.getPassengers() != null && !horse.getPassengers().isEmpty()){
				for(Entity e : horse.getPassengers()) if(e.getUniqueId().equals(uuid)){
					horse.removePassenger(e);
				}
			}
		}}.runTaskLater(plugin, 1);
	}

	private void onHorseClick(PlayerInteractEntityEvent evt){
		Player p = evt.getPlayer();
		AbstractHorse horse = (AbstractHorse) evt.getRightClicked();
		ItemStack clickItem = p.getInventory().getItemInMainHand();

		if(isUsableNametag(clickItem)){
//			p.sendMessage(ChatColor.RED+"Use /namehorse to rename your horses.");
			if(!plugin.canAccess(p, horse.getCustomName())){
				p.sendMessage(ChatColor.RED+"You do not have permission to rename this horse");
				evt.setCancelled(true);
			}
			else{
				if(evt.getPlayer().getGameMode() == GameMode.CREATIVE) return;//TODO
				String newName = clickItem.getItemMeta().getDisplayName();
				Location loc = p.getLocation();
				horse.addPassenger(p);
				plugin.getCommand("claimhorse").execute(p, "claimhorse", new String[]{newName});
				horse.removePassenger(p);
				p.teleport(loc);
				if(CommandClaimHorse.COMMAND_SUCCESS == false) evt.setCancelled(true);
			}
			return;
		}

		if(plugin.canAccess(p, horse.getCustomName())) return;

		// Plugin-handled leashing/unleashing
		if(horse.isLeashed()){
			if(horse.getLeashHolder() instanceof Player && horse.getLeashHolder().getUniqueId().equals(p.getUniqueId())){
				evt.setCancelled(true);
				horse.setLeashHolder(null);
				if(p.getGameMode() != GameMode.CREATIVE){
					horse.getWorld().dropItemNaturally(horse.getLocation(), new ItemStack(Material.LEAD));
				}
				return;
			}
		}
		else if(clickItem.getType() == Material.LEAD){
			evt.setCancelled(true);
			horse.setLeashHolder(p);
			if(p.getGameMode() != GameMode.CREATIVE){
				if(clickItem.getAmount() == 1) p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
				else clickItem.setAmount(clickItem.getAmount()-1);
			}
			return;
		}

		//if they do NOT own the horse and are NOT performing a leashing action
		if(plugin.isClaimedHorse(horse.getCustomName())){
			if(breedPrivateHorse && HorseLibrary.isBreedingFood(clickItem.getType()) && horse.canBreed()){
				//don't cancel, but make sure a new rider isn't put on the horse
				//if(horse.getPassengers() == null || horse.getPassengers().isEmpty())
				delayedEject(horse, p.getUniqueId());
			}
			else if(feedPrivateHorse && HorseLibrary.isHorseFood(clickItem.getType())
					&& horse.getHealth() < HorseLibrary.getNormalHealth(horse))
			{
				//don't cancel, but make sure a new rider isn't put on the horse
				//if(horse.getPassengers() == null || horse.getPassengers().isEmpty())
				delayedEject(horse, p.getUniqueId());
			}
			else if(plugin.useOneTimeAccess(p.getUniqueId(), horse.getCustomName())){
				p.sendMessage(ChatColor.GREEN+"You have used a one-time-pass to mount this horse, "+
						ChatColor.GRAY+horse.getCustomName()+ChatColor.GREEN+".");
				//allow
			}
			else if(p.isSneaking() && snoopPrivateHorse == false){
				p.sendMessage(ChatColor.RED+"You do not have permission to view this horse's inventory.");
				evt.setCancelled(true);
			}
			else if(ridePrivateHorse == false){
				p.sendMessage(ChatColor.RED+"You do not have permission to mount this horse.");
				evt.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onClick(PlayerInteractEntityEvent evt){
		if(evt.isCancelled())return;
		if(evt.getRightClicked() instanceof AbstractHorse){ onHorseClick(evt); return; }
		Player p = evt.getPlayer();

		//leash any clicked LivingEntity, handy to have
		if(leashLivingEntities && evt.getRightClicked() instanceof LivingEntity){
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