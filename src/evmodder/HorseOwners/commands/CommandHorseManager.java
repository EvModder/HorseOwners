package evmodder.HorseOwners.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import evmodder.HorseOwners.HorseManager;

public class CommandHorseManager implements TabExecutor{//Cannot extend HorseCommand or money stuff gets weird
	final HorseManager plugin;
	final List<String> shortNames;
	final Map<String, PluginCommand> executorLookup;

	public CommandHorseManager(){
		plugin = HorseManager.getPlugin();

		// Load a list of this plugin's commands
		shortNames = new ArrayList<String>();
		executorLookup = new HashMap<String, PluginCommand>();
		for(String cmdName : plugin.getDescription().getCommands().keySet()){
			PluginCommand cmd = plugin.getCommand(cmdName);
			if(!cmdName.equals("horsemanager")) shortNames.add(getShortName(cmdName));
			executorLookup.put(cmdName, cmd);
			executorLookup.put(getShortName(cmdName), cmd);
			for(String cmdAlias : cmd.getAliases()){
				executorLookup.put(cmdAlias, cmd);
				executorLookup.put(getShortName(cmdAlias), cmd);
			}
		}

		plugin.getCommand("horsemanager").setExecutor(this);
	}

	String getShortName(String cmdName){
		return cmdName.toLowerCase().replace("horse", "");
	}

	boolean isHelpCommand(String cmd){
		return cmd.isEmpty() || cmd.equals("?") || cmd.equals("help") || cmd.equals("info");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		String actualCmd = "";
		if(args.length != 0){
			actualCmd = getShortName(args[0]);
			args = Arrays.copyOfRange(args, 1, args.length);
		}

		if(isHelpCommand(actualCmd)){
			sender.sendMessage("§6+ §7§m-------------------------------------§6 +");
			// Send help/info/commands
			for(String name : plugin.getDescription().getCommands().keySet()){
				Command cmd = plugin.getCommand(name);
				if(sender.hasPermission(cmd.getPermission())){
					sender.sendMessage("§8 ● §e"+cmd.getUsage()+"§7 -  §f"+cmd.getDescription());
				}
			}
//			sender.sendMessage("§6+ §7§m-------------------------§6 +");
			return true;
		}
		PluginCommand cmd = executorLookup.get(actualCmd);
		if(cmd == null) return false;

		if(sender.hasPermission(cmd.getPermission()) == false){
			sender.sendMessage(cmd.getPermissionMessage());
			return true;
		}
		return cmd.getExecutor().onCommand(sender, cmd, cmd.getName(), args);
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){
		if(args.length == 1){
			args[0] = args[0].toLowerCase();
			List<String> completions = new ArrayList<String>();
			for(String cmdName : shortNames){
				if(cmdName.startsWith(args[0]) && sender.hasPermission(executorLookup.get(cmdName).getPermission())){
					completions.add(cmdName);
				}
			}
			return completions;
		}
		else if(args.length > 1){
			PluginCommand cmd = executorLookup.get(args[0]);
			if(cmd != null && sender.hasPermission(cmd.getPermission())){
				return cmd.tabComplete(sender, alias, Arrays.copyOfRange(args, 1, args.length));
			}
		}
		return null;
	}
}