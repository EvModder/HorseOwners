package Evil_Code_HorseOwners.commands;

import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Player;
import Evil_Code_HorseOwners.HorseLibrary;

@SuppressWarnings("deprecation")
public class CommandSpawnHorse extends HorseCommand{

	@Override
	public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	usage: /hm spawn [n:name] [v:varient] [c:color] [t:style] [j:jump] [s:speed] [h:health] [tamed]
		//hm spawn n:fat c:white t:white s:50 j:20 h:60
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players");
			return true;
		}

		String name = null;
		EntityType variant = EntityType.HORSE;
		Color color = null;
		Style style = null;
		double jump=0, speed=0, health=0;
		boolean tamed = false;

		for(String input : args){
			String arg = input.toUpperCase();
			if(!arg.contains(":")){
				if(!(tamed = arg.replace("-", "").toLowerCase().equals("tamed"))){
					sender.sendMessage(ChatColor.RED+"Invalid arguments (perhaps missing ':'?)");
					return false;
				}
			}
			if(arg.startsWith("N")) name = input.split(":")[1];
			else if(arg.startsWith("V") || arg.startsWith("TYPE:")){
				try{variant = EntityType.valueOf(arg.split(":")[1]);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid variant '"+ChatColor.GRAY+arg.split(":")[1]
						+ChatColor.RED+"'\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Variant.values()));
					return true;
				}
			}
			else if(arg.startsWith("C")){
				try{color = Color.valueOf(arg.split(":")[1]);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid color '"+ChatColor.GRAY+arg.split(":")[1]
							+ChatColor.RED+"'\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Color.values()));
					return true;
				}
			}
			else if(arg.startsWith("T:") || arg.startsWith("STYLE:")){
				try{style = Style.valueOf(arg.split(":")[1]);}
				catch(IllegalArgumentException ex){
					sender.sendMessage(ChatColor.RED+"Invalid style '"+ChatColor.GRAY+arg.split(":")[1]
							+ChatColor.RED+"'\n"+ChatColor.GRAY+"Possible values: "+Arrays.asList(Style.values()));
					return true;
				}
			}
			else if(arg.startsWith("J")){
				try{jump = Double.parseDouble(arg.split(":")[1]);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid jump, only accepts number values");
					return true;
				}
			}
			else if(arg.startsWith("S")){
				try{speed = Double.parseDouble(arg.split(":")[1]);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid speed, only accepts number values");
					return true;
				}
			}
			else if(arg.startsWith("H")){
				try{health = Double.parseDouble(arg.split(":")[1]);}
				catch(NumberFormatException ex){
					sender.sendMessage(ChatColor.RED+"Invalid health, only accepts number values");
					return true;
				}
			}
		}

		Player p = (Player) sender;
		AbstractHorse horse = (AbstractHorse) p.getWorld().spawnEntity(p.getLocation(), variant);

		if(name != null) horse.setCustomName(name);
		if(variant == EntityType.HORSE){
			if(color != null) ((Horse)horse).setColor(color);
			if(style != null) ((Horse)horse).setStyle(style);
		}
		if(jump != 0) horse.setJumpStrength(HorseLibrary.denormalizeJump(jump));
		if(speed != 0) HorseLibrary.speedCalc.setHorseSpeed(horse, HorseLibrary.denormalizeSpeed(speed));
		if(health != 0) HorseLibrary.setMaxHealth(horse, health);
		if(tamed) horse.setTamed(true);

		sender.sendMessage(ChatColor.GREEN+"Successfully spawned your horse!");
		return true;
	}
}