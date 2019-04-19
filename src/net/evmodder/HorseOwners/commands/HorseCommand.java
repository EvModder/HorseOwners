package net.evmodder.HorseOwners.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import net.evmodder.EvLib.VaultHook;
import net.evmodder.HorseOwners.HorseManager;

abstract class HorseCommand implements TabExecutor{
	HorseManager plugin;
	double price;

	HorseCommand(){
		plugin = HorseManager.getPlugin();
		String commandName = getClass().getSimpleName().substring(7).toLowerCase();
		plugin.getCommand(commandName).setExecutor(this);
		plugin.getCommand(commandName).setTabCompleter(this);
		price = plugin.getConfig().getDouble(commandName);
	}

	@SuppressWarnings("deprecation") @Override
	final public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(price > 0 && sender instanceof Player && !sender.hasPermission("evp.horseowners.commands.free")){
			if(VaultHook.hasAtLeast((Player)sender, price) == false){
				sender.sendMessage("§4You do not have sufficient funds (§c$"+price+"§4)");
				return true;
			}
			else if(onHorseCommand(sender, command, label, args)){
				if(plugin.getServer().getPluginManager().isPluginEnabled("Eventials")){
					plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
							"serverbal charge "+sender.getName()+" "+price);
				}
				else{
					VaultHook.chargeFee((Player)sender, price);
				}
				sender.sendMessage("§7You were charged §c$"+price+"§7 for using this command.");
				return true;
			}
			else return false;
		}
		else return onHorseCommand(sender, command, label, args);
	}
	public abstract boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]);
}