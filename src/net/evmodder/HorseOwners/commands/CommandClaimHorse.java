package net.evmodder.HorseOwners.commands;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import net.evmodder.EvLib.extras.TextUtils;

public class CommandClaimHorse extends HorseCommand{
	final boolean renameNametag, alphanumeric;
	final int minNameLength, maxNameLength;

	public CommandClaimHorse(){
		renameNametag = plugin.getConfig().getBoolean("nametag-required-to-rename");
		alphanumeric = plugin.getConfig().getBoolean("names-mostly-alphanumeric");

		minNameLength = plugin.getConfig().getInt("min-name-length");
		maxNameLength = plugin.getConfig().getInt("max-name-length");
		if(maxNameLength < minNameLength) plugin.getLogger().severe(
				"max-name-length("+maxNameLength+") is < than " +
				"min-name-length("+minNameLength+")!");
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a){return null;}

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm claim [name]
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			COMMAND_SUCCESS = false;
			return true;
		}

		Player p = (Player)sender;
		if(!p.isInsideVehicle() || !plugin.isClaimableHorseType(p.getVehicle())){
			p.sendMessage(ChatColor.RED+"You must be riding on a horse to use this command");
			COMMAND_SUCCESS = false;
			return true;
		}
		Entity h = p.getVehicle();

		if(args.length == 0 && h.getCustomName() == null){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+" (Please supply a name)");
			COMMAND_SUCCESS = false;
			return false;
		}

		if(h instanceof Tameable && ((Tameable)h).isTamed() == false){
			p.sendMessage(ChatColor.RED+"You must first tame this horse before you can claim it");
			COMMAND_SUCCESS = false;
			return true;
		}

		String oldName = h.getCustomName();
		boolean isOwner = plugin.isOwner(p.getUniqueId(), oldName);
		
		if(oldName != null && plugin.isClaimedHorse(oldName) && isOwner == false){
			p.sendMessage(ChatColor.RED+"This horse already belongs to someone!");
			COMMAND_SUCCESS = false;
			return true;
		}

		if(args.length > 0){
			String newName;
			if(args.length == 1) newName = args[0];
			else{
				StringBuilder builder = new StringBuilder(args[0]);
				for(int i=1; i<args.length; ++i) builder.append(' ').append(args[i]);
				newName = builder.toString();
			}
			if(alphanumeric) newName = newName.replaceAll("[:(),"+TextUtils.colorSymbol+"<>{}\\-\\[\\]\\.]'\"", "");

			if(p.hasPermission("evp.horseowners.coloredname")){
				if(p.hasPermission("evp.horseowners.fullformats") == false){
					newName = newName.replace("&k", "").replace("&m", "").replace("&n", "");
				}
				newName = TextUtils.translateAlternateColorCodes('&', newName);
			}
			newName.replaceAll("\\s{2,}", " ").trim();//remove leftover spaces

			if(oldName != null){
				if(newName.equals(oldName)){
					if(isOwner){
						p.sendMessage(ChatColor.RED+"This horse already has that name!");
						COMMAND_SUCCESS = false;
						return true;
					}
				}
				else if(plugin.isLockedHorse(oldName)){
					sender.sendMessage(ChatColor.RED+"You cannot rename "
								+ChatColor.GRAY+ChatColor.ITALIC+oldName+ChatColor.RED+'.');
					COMMAND_SUCCESS = false;
					return false;
				}
			}
			if(newName.length() < minNameLength){
				p.sendMessage(ChatColor.RED+"Too short of a name!");
				COMMAND_SUCCESS = false;
				return true;
			}
			else if(newName.length() > maxNameLength){
				p.sendMessage(ChatColor.RED+"Too long of a name!");
				COMMAND_SUCCESS = false;
				return true;
			}
			else if(plugin.horseExists(newName)){
				p.sendMessage(ChatColor.RED+"That name has already been taken!");
				COMMAND_SUCCESS = false;
				return true;
			}
			if((oldName == null || oldName.equals(newName) == false)
					&& renameNametag && p.getGameMode() != GameMode.CREATIVE){
				if(p.getInventory().contains(Material.NAME_TAG) == false){
					p.sendMessage(ChatColor.RED+"You need a nametag in order to "
						+(oldName == null ? "name/claim" : "rename")+" a horse!");
					COMMAND_SUCCESS = false;
					return true;
				}
				//charge 1 nametag
				ItemStack firstNametag = p.getInventory().getItem(p.getInventory().first(Material.NAME_TAG));
				firstNametag.setAmount(firstNametag.getAmount()-1);
				p.getInventory().setItem(p.getInventory().first(Material.NAME_TAG),
										((firstNametag.getAmount() > 0) ? firstNametag : new ItemStack(Material.AIR)));
			}
			h.setCustomName(newName);//Update name

			if(oldName != null && oldName.equals(newName) == false){//if had a name previously
				p.sendMessage(ChatColor.GREEN+"Successfully renamed " + ChatColor.GRAY + ChatColor.ITALIC + oldName
						+ ChatColor.GREEN + " to " + ChatColor.GRAY + ChatColor.ITALIC + newName + ChatColor.GREEN + "!");
				
				//if owner of the horse, update the name (but keep the stats)
				if(isOwner) plugin.renameHorse(oldName, newName);
				else plugin.addHorse(p.getUniqueId(), h);
				COMMAND_SUCCESS = true;
				return true;
			}
		}
		else if(oldName == null){
			p.sendMessage(ChatColor.RED+"Please supply a name for this horse\n"+ChatColor.GRAY+command.getUsage());
			COMMAND_SUCCESS = false;
			return false;
		}
		else if(isOwner){
			p.sendMessage(ChatColor.GRAY+"You already own this horse");
			COMMAND_SUCCESS = false;
			return true;
		}

		//My horsie!
		plugin.addHorse(p.getUniqueId(), h);
		sender.sendMessage(ChatColor.GREEN+"Successfully claimed " + ChatColor.GRAY + ChatColor.ITALIC
					+ h.getCustomName() + ChatColor.GREEN + " as your horse!");
		COMMAND_SUCCESS = true;
		return true;
	}
}