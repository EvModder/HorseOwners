package Evil_Code_HorseOwners.commands;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

import Evil_Code_HorseOwners.HorseLibrary;

public class CommandInspectHorse extends HorseCommand{
	private boolean doesRanking, rankUnclaimed, doesLineage;

	public CommandInspectHorse(){
		doesRanking = plugin.getConfig().getBoolean("rank-claimed-horses");
		rankUnclaimed = plugin.getConfig().getBoolean("rank-unclaimed-horses");
		doesLineage = plugin.getConfig().getBoolean("save-horse-lineage");
	}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm inspect [horse]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		AbstractHorse h;
		if(p != null && p.isInsideVehicle() && p.getVehicle() instanceof AbstractHorse){
			h = (AbstractHorse) p.getVehicle();
		}
		else{
			if(args.length == 0){
				sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+"\n"+command.getUsage());
				return false;
			}
			String targetName = StringUtils.join(args, ' ');
			if((h = plugin.findClaimedHorse(targetName, null)) == null
					&& (h = HorseLibrary.findAnyHorse(targetName)) == null){
				sender.sendMessage(ChatColor.RED+"Unable to find your horse! Perhaps the chunk it is in was unloaded?");
				return false;
			}
			else if(plugin.isPrivateHorse(h.getCustomName()) == false){
				sender.sendMessage(ChatColor.RED+"Unknown horse (check name spelling)"
						+ChatColor.GRAY+'\n'+command.getUsage());
				return false;
			}
			else if(p != null && !p.hasPermission("evp.horseowners.inspect.others")
					&& !plugin.canAccess(p, h.getCustomName())){
				sender.sendMessage(ChatColor.RED+"You cannot inspect horses which you do not own");
				return false;
			}
		}

		//Yay got to here! Now what's it worth?
		String name = h.getCustomName();

		//Build info message
		StringBuilder builder = new StringBuilder();
		if(p.hasPermission("evp.horseowners.inspect.name"))
			builder.append("§7Name: §f").append(name == null ? "§cN/A" : name);

		if(name != null && doesRanking && p.hasPermission("evp.horseowners.inspect.rankings")
				&& (rankUnclaimed || plugin.isPrivateHorse(name))){
			plugin.updateData(h);
			int[] rank = plugin.getRankings(name);

			if(p.hasPermission("evp.horseowners.inspect.speed")){
				builder.append("\n§7Speed: ").append(plugin.getHorseSpeed(name)).append("§cm/s")
					.append("§7 [§e#").append(rank[2]);
				if(rank[3] != rank[2]) builder.append('-').append(rank[3]);
				builder.append("§7]");
			}
			if(p.hasPermission("evp.horseowners.inspect.jump")){
				builder.append("\n§7Jump: ").append(plugin.getHorseJump(name)).append("§cm")
					.append("§7 [§e#").append(rank[0]);
				if(rank[1] != rank[0]) builder.append('-').append(rank[1]);
				builder.append("§7]");
			}
			if(p.hasPermission("evp.horseowners.inspect.health")){
				builder.append("\n§7Health: ").append(plugin.getHorseHealth(name)).append("§ch")
					.append("§7 [§e#").append(rank[4]);
				if(rank[5] != rank[4]) builder.append('-').append(rank[5]);
				builder.append("§7]");
			}
		}
		else{
			if(p.hasPermission("evp.horseowners.inspect.speed")){
				builder.append("\n§7Speed: ").append(HorseLibrary.getNormalSpeed(h)).append("§cm/s");
			}
			if(p.hasPermission("evp.horseowners.inspect.jump")){
				builder.append("\n§7Jump: ").append(HorseLibrary.getNormalJump(h)).append("§cm");
			}
			if(p.hasPermission("evp.horseowners.inspect.health")){
				builder.append("\n§7Health: ").append(Math.rint(
						h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue())).append("§ch");
			}
		}
		if(p.hasPermission("evp.horseowners.inspect.tamer")){
			builder.append("\n§7Tamer: ").append(h.getOwner() == null ? "§cN/A" : h.getOwner().getName());
		}
		if(p.hasPermission("evp.horseowners.inspect.owner")){
			UUID uuid = (name == null ? null : plugin.getOwner(name));
			OfflinePlayer owner = (uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid));
			builder.append("\n§7Official Owner: ").append(owner == null ? "§cN/A" : owner.getName());
		}
		if(doesLineage && p.hasPermission("evp.horseowners.inspect.lineage")){
			List<String> parents = plugin.getHorseLineage(h);
			if(parents != null && !parents.isEmpty()) builder.append("\n§7Parents: ").append(parents);
		}

		sender.sendMessage(builder.toString());
		return true;
	}
}
