package net.evmodder.HorseOwners.commands;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.evmodder.HorseOwners.HorseManager;

public class CommandShadowfax implements CommandExecutor{
	public CommandShadowfax(){
		HorseManager.getPlugin().getCommand("summonshadowfax").setExecutor(this);
	}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm shadowfax
		if(sender instanceof Player == false){
			sender.sendMessage("§cThis command can only be run by in-game players");
		}
		else if(sender.getName().equals("EvDoc")){
			Player p = (Player)sender;
			Horse h = (Horse) p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
			h.setStyle(Style.WHITE_DOTS);
			h.setAdult();
			h.setColor(Color.WHITE);
			h.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(34.0);
			h.setHealth(34.0);
			h.setJumpStrength(1.15);
			h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.302);

			//h.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 1));
			h.addPassenger(p);
			//h.setAgeLock(true);
			h.setTamed(true);
			h.setRemoveWhenFarAway(false);
			h.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100000, 0));
			h.setCustomName("Shadowfax");
			h.setMaximumAir(h.getMaximumAir()+10);
			h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
			h.getInventory().setArmor(new ItemStack(Material.DIAMOND_HORSE_ARMOR));
			h.setMaxDomestication(10);
			h.setDomestication(10);
			p.sendMessage("§7Shadowfax has been summoned!");
		}
		return true;
	}
}