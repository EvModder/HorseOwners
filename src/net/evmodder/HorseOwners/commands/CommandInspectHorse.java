package net.evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import com.google.common.collect.Sets;
import net.evmodder.HorseOwners.HorseLibrary;

public class CommandInspectHorse extends HorseCommand{
	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender.hasPermission("evp.horseowners.inspect.others")
					? (sender.hasPermission("evp.horseowners.inspect.unowned")
					? plugin.getAllHorses() : plugin.getAllClaimedHorses())
					: plugin.getHorseOwners().getOrDefault(((Player)sender).getUniqueId(), Sets.newHashSet())){
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
		//cmd:	/hm inspect [horse]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		AbstractHorse horse = null;
		String horseName = null;
		if(p != null && p.isInsideVehicle() && p.getVehicle() instanceof AbstractHorse){
			horse = (AbstractHorse) p.getVehicle();
		}
		else{
			if(args.length == 0){
				sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+"\n"+command.getUsage());
				COMMAND_SUCCESS = false;
				return false;
			}
			horseName = HorseLibrary.cleanName(StringUtils.join(args, ' '));
			if(!plugin.horseExists(horseName)){
				sender.sendMessage(ChatColor.RED+"Unknown horse (check name spelling)"
						+ChatColor.GRAY+'\n'+command.getUsage());
				COMMAND_SUCCESS = false;
				return true;
			}
			else if(p != null && !p.hasPermission("evp.horseowners.inspect.others") && !plugin.canAccess(p, horseName)){
				sender.sendMessage(ChatColor.RED+"You cannot inspect horses which you do not own");
				COMMAND_SUCCESS = false;
				return true;
			}
			/*Entity entity = plugin.findClaimedHorse(horseName, null);
			if(entity == null) entity = HorseLibrary.findAnyHorse(horseName);
			if(entity == null || !(entity instanceof AbstractHorse)){
				if(plugin.isClaimedHorse(horseName)){
					sender.sendMessage(ChatColor.RED
							+"Unable to find your horse! Perhaps the chunk it is in was unloaded?");
				}
				else{
					sender.sendMessage(ChatColor.RED+"Unknown horse (check name spelling)"
							+ChatColor.GRAY+'\n'+command.getUsage());
				}
				return true;
			}
			h = (AbstractHorse)entity;*/
		}

		//Yay got to here! Now what's it worth?
		if(horse != null && horse.getCustomName() != null/* && plugin.horseExists(horse.getCustomName())*/)
			horseName = horse.getCustomName();

		String displayName, ownerName, tamerName;
		double speed, jump;
		int health;
		int[] rank = null;
		List<String> parents;
		if(horseName != null){
			if(horse != null) plugin.updateData(horse);
			displayName = plugin.getHorseName(horseName);
			if(displayName == null) displayName = horseName;
			ownerName = plugin.getHorseOwnerName(horseName);
			if(ownerName == null) ownerName = "§cN/A";
			tamerName = plugin.getHorseTamerName(horseName);
			if(tamerName == null) tamerName = (horse == null || horse.isTamed()) ? "§cUnknown" : "§cN/A";
			speed = plugin.getHorseSpeed(horseName);
			jump = plugin.getHorseJump(horseName);
			health = plugin.getHorseHealth(horseName);
			parents = plugin.getHorseParents(horseName);
			if(p.hasPermission("evp.horseowners.inspect.rankings")) rank = plugin.getRankings(horseName);
		}
		else{
			displayName = horse.getCustomName() == null ? "§cN/A" : horse.getCustomName();
			ownerName = "§cN/A";
			tamerName = horse.isTamed() ? (horse.getOwner() == null ? "§cUnknown" : horse.getOwner().getName()) : "§cN/A";
			speed = HorseLibrary.getNormalSpeed(horse);
			jump = HorseLibrary.getNormalJump(horse);
			health = HorseLibrary.getNormalMaxHealth(horse);
			parents = plugin.getHorseParents(horse);
		}

		//Build info message
		StringBuilder builder = new StringBuilder();
		if(p.hasPermission("evp.horseowners.inspect.name")) builder.append("§7Name: §f").append(displayName);

		if(p.hasPermission("evp.horseowners.inspect.speed")){
			builder.append("\n§7Speed: §f").append(speed).append("§cm/s");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[0]);
				if(rank[0] != rank[1]) builder.append('-').append(rank[1]);
				builder.append("§7]");
			}
		}
		if(p.hasPermission("evp.horseowners.inspect.jump")){
			builder.append("\n§7Jump: §f").append(jump).append("§cm");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[2]);
				if(rank[2] != rank[3]) builder.append('-').append(rank[3]);
				builder.append("§7]");
			}
		}
		if(p.hasPermission("evp.horseowners.inspect.health")){
			builder.append("\n§7Health: §f").append(health).append("§ch");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[4]);
				if(rank[4] != rank[5]) builder.append('-').append(rank[5]);
				builder.append("§7]");
			}
		}
		if(p.hasPermission("evp.horseowners.inspect.tamer")){
			builder.append("\n§7Tamer: §f").append(tamerName);
		}
		if(p.hasPermission("evp.horseowners.inspect.owner")){
			builder.append("\n§7Owner: §f").append(ownerName);
		}
		if(p.hasPermission("evp.horseowners.inspect.lineage") && parents != null && !parents.isEmpty()){
			builder.append("\n§7Parents: §f").append(String.join("§7, §f", parents));
		}

		sender.sendMessage(builder.toString());
		COMMAND_SUCCESS = true;
		return true;
	}
}
