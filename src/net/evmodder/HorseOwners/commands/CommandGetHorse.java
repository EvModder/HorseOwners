package net.evmodder.HorseOwners.commands;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.hooks.MultiverseHook;
import net.evmodder.HorseOwners.HorseLibrary;

public class CommandGetHorse extends HorseCommand{
	boolean saveCoords, safeTeleports, allowTransworld;

	public CommandGetHorse(){
		saveCoords = plugin.getConfig().getBoolean("save-horse-coordinates");
		safeTeleports = plugin.getConfig().getBoolean("teleport-only-if-safe");
		allowTransworld = plugin.getConfig().getBoolean("teleport-across-worlds");
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0 && sender instanceof Player){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			//TODO: possible (but laggy): only list horses in same world if player lacks cross-world permission
			byte shown = 0;
			for(String horseName : plugin.getHorseOwners().get(((Player)sender).getUniqueId())){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm get [horse]
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			return false;
		}
		if(args.length < 1){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+'\n'+command.getUsage());
			return true;
		}
		Player p = (Player) sender;
		String target = StringUtils.join(args, ' ');
		Entity horse;
		Set<Entity> horses = new HashSet<Entity>();

		if(safeTeleports && HorseLibrary.safeForHorses(p.getLocation()) == false){
			p.sendMessage(ChatColor.RED
					+"Unable to teleport horse - Please move to a more open area to prevent risk of horse suffocation");
			return true;
		}

		if(target.toLowerCase().equals("all")  || target.toLowerCase().equals("@a")){
			int lost = 0;
			World[] worlds = new World[]{p.getWorld()};//restrict to just this world

			if(allowTransworld){
				if(p.hasPermission("evp.horseowners.tpansworld.*"))/* worlds = null*/;//already null
				else if(p.hasPermission("evp.horseowners.tpansworld.samegamemode")){
					List<World> worldsList = new ArrayList<World>();
					for(World w : plugin.getServer().getWorlds()){
						GameMode gm = MultiverseHook.getWorldGameMode(w);
						if(gm == null || p.getGameMode() == gm) worldsList.add(w);
					}
					worlds = worldsList.toArray(worlds);
				}
			}

			for(String horseName : plugin.getHorseOwners().get(p.getUniqueId())){
				p.sendMessage("Fetching: "+horseName);
				horse = plugin.findClaimedHorse(horseName, worlds);
				if(horse != null) horses.add(horse);
				else ++lost;
			}
			//Sometimes some of the horses won't make it
			if(lost > 0) p.sendMessage(ChatColor.YELLOW+"Unable to teleport "
						+ChatColor.GRAY+lost+ChatColor.YELLOW+" of your horses");
			if(horses.size() == 0) return false;
		}
		else{
			if(plugin.isClaimedHorse(target) == false){
				sender.sendMessage(ChatColor.RED+"Unknown horse '"+ChatColor.GRAY+target+ChatColor.RED+'\'');
//				sender.sendMessage("�cUnclaimed horses cannot be teleported via command, you must first use /claimhorse");
				return true;
			}
			else if(plugin.canAccess(p, target) == false){
				p.sendMessage(ChatColor.RED+"You may not teleport horses which you do not own");
				return true;
			}
			horse = plugin.findClaimedHorse(target, null);
//			horse = HorseLibrary.findAnyHorse(target);
			if(horse == null){
				p.sendMessage(ChatColor.RED+"Unable to find your horse! Perhaps its location was unloaded?");
				return true;
			}
			if(horse.getWorld().getUID().equals(p.getWorld().getUID()) == false){
				GameMode gm = MultiverseHook.getWorldGameMode(horse.getWorld());
				if(!allowTransworld || (!p.hasPermission("evp.horseowners.crossworld.anywhere") &&
						(!p.hasPermission("evp.horseowners.crossworld.samegamemode") ||
								(gm != null && p.getGameMode() != gm))))
				{
					p.sendMessage(ChatColor.RED+"Unable to teleport the horse, "
								+ChatColor.GRAY+horse.getCustomName()+ChatColor.RED+"--");
					p.sendMessage(ChatColor.RED+"You do not have permission to use trans-world horse teleportation");
					return true;
				}
				else p.sendMessage(ChatColor.GRAY+"Attempting to fetch horse from world: "
								+ChatColor.GREEN+horse.getWorld().getName());
			}
			horses.add(horse);
		}

		//Yay got to here! Hi horsie!
		for(Entity h : horses){
			HorseLibrary.teleportEntityWithPassengers(h, p.getLocation());
			if(saveCoords && h instanceof AbstractHorse) plugin.updateData((AbstractHorse)h);
		}
		p.sendMessage(ChatColor.GREEN+"Fetched your horse"+(horses.size() > 1 ? "s!" : "!"));
		return true;
	}
}