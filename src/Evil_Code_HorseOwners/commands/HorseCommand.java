package Evil_Code_HorseOwners.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import EvLib2.VaultHook;
import Evil_Code_HorseOwners.HorseManager;

abstract class HorseCommand implements CommandExecutor{
	HorseManager plugin;
	double price;

	HorseCommand(){
		plugin = HorseManager.getPlugin();
		String commandName = getClass().getSimpleName().substring(7).toLowerCase();
		plugin.getCommand(commandName).setExecutor(this);
		price = plugin.getConfig().getDouble(commandName);
	}

	@Override
	final public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(price > 0 && sender instanceof Player && !sender.hasPermission("evp.horseowners.commands.free")){
			if(VaultHook.hasAtLeast((Player)sender, price) == false){
				sender.sendMessage("§4You do not have sufficient funds (§c$"+price+"§4)");
				return true;
			}
			else if(onHorseCommand(sender, command, label, args)){
				VaultHook.chargeFee((Player)sender, price);
				sender.sendMessage("§7You were charged §c$"+price+"§7 for using this command.");
				return true;
			}
			else return false;
		}
		else return onHorseCommand(sender, command, label, args);
	}
	public abstract boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]);
}