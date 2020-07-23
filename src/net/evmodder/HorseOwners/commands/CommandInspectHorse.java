package net.evmodder.HorseOwners.commands;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attributable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.HorseOwners.HorseLibrary;
import net.evmodder.HorseOwners.HorseManager;

public class CommandInspectHorse extends HorseCommand{
	final boolean INSPECT_UNTAMED, INSPECT_UNCLAIMED, ONLY_ONE_HORSE_TYPE;
	public CommandInspectHorse(){
		HorseManager pl = HorseManager.getPlugin();
		INSPECT_UNTAMED = pl.getConfig().getBoolean("inspect-untamed", true);
		INSPECT_UNCLAIMED = pl.getConfig().getBoolean("inspect-unclaimed", true);
		ONLY_ONE_HORSE_TYPE = pl.getConfig().getStringList("valid-horses").size() < 2;// 0 or 1
	}

	@Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length > 0){
			String arg = String.join(" ", args).toLowerCase();
			final List<String> tabCompletes = new ArrayList<String>();
			byte shown = 0;
			for(String horseName : sender.hasPermission("horseowners.inspect.others")
					? (sender.hasPermission("horseowners.inspect.unclaimed")
					? plugin.getAllHorses() : plugin.getAllClaimedHorses())
					: plugin.getHorseOwners().getOrDefault(((Player)sender).getUniqueId(), new HashSet<>())){
				if(horseName.startsWith(arg)){
					tabCompletes.add(horseName);
					if(++shown == 20) break;
				}
			}
			return tabCompletes;
		}
		return null;
	}

	private char[] dnaMap = new char[]{
			'.', ',', ':', ';', 'i', '!', '|', '\'', '¡', '¦', '·', //'´', '¸', (11+2,w=1)
			'`', 'l', '‚', '‘', '’', '•', 'ì', 'í', //'ˆ', '¨', (8+2,w=2)
			'[', ']', '(', ')', '{', '}', '‹', '›', '°', '¹', //'˜', (10+1,w=3)
			//'I', 't', 'Ì', 'Í', 'Î', 'Ï', 'î', 'ï', // (8)
	};
	private String getCondensedDNA(Entity horse){
		String dna = HorseLibrary.getDNA(horse, null);
		if(dna == null) return null;
		char[] dnaChars = dna.toLowerCase().toCharArray();
		for(int i=0; i<dnaChars.length; ++i) dnaChars[i] = dnaMap[(int)(dnaChars[i] - 'a')];// Assumes all DNA chars are [a-z]
		return TextUtils.pxSubstring(new String(dnaChars), 320-6*4, false).str;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm inspect [horse]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		Entity horse = null;
		String horseName = null;
		if((args.length > 0 && plugin.horseExists(horseName = HorseLibrary.cleanName(String.join(" ", args))))){
			if(p != null && !p.hasPermission("horseowners.inspect.others") && !plugin.canAccess(p, horseName)){
				sender.sendMessage(ChatColor.RED+"You cannot inspect horses which you do not own");
				COMMAND_SUCCESS = false;
				return true;
			}
			if(!INSPECT_UNTAMED && plugin.getHorseTamer(horseName) == null
					&& !sender.hasPermission("horseowners.inspect.untamed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on untamed horses");
				COMMAND_SUCCESS = false;
				return true;
			}
			if(!INSPECT_UNCLAIMED && plugin.getHorseOwner(horseName) == null
					&& !sender.hasPermission("horseowners.inspect.unclaimed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on unclaimed horses");
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else if(p != null && p.isInsideVehicle()){
			horse = p.getVehicle();
			if(!INSPECT_UNTAMED && 
					(horse instanceof Tameable == false || !((Tameable)horse).isTamed()) &&
					!sender.hasPermission("horseowners.inspect.untamed")){
				sender.sendMessage(ChatColor.RED+"You must tame this steed before you can use that command");
				COMMAND_SUCCESS = false;
				return true;
			}
			horseName = horse.getCustomName();
			if(!INSPECT_UNCLAIMED && (horseName == null || plugin.getHorseOwner(horseName) == null)
					&& !sender.hasPermission("horseowners.inspect.unclaimed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on unclaimed horses");
				sender.sendMessage(ChatColor.GRAY+"To claim this horse, use "
								+ChatColor.DARK_GREEN+"/hm claim"+(horseName == null ? " <name>" : ""));
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else if(args.length == 0){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}
		else if(!plugin.horseExists(horseName)){
			sender.sendMessage(ChatColor.RED+"Unknown horse! (check spelling)"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}

		//Yay got to here! Now what's it worth?
		String displayName, ownerName, tamerName, typeName;
		double speed = -1, jump = -1; int health = -1, strength = -1;
		long age = -1, claim_timestamp = -1;
		int[] rank = null;
		List<String> parents;
		String DNA = null;
		Integer locX, locZ;
		if(horseName != null){
			if(horse != null){
				plugin.updateData(horse);
				locX = horse.getLocation().getBlockX();
				locZ = horse.getLocation().getBlockZ();
				DNA = getCondensedDNA(horse);
			}
			// Added permission check because getHorseBlockX,Z is an expensive call
			else if(sender.hasPermission("horseowners.inspect.coords")){
				locX = plugin.getHorseBlockX(horseName);
				locZ = plugin.getHorseBlockZ(horseName);
			}
			else locX = locZ = null;
			displayName = plugin.getHorseName(horseName);
			if(displayName == null) displayName = horseName;
			ownerName = plugin.getHorseOwnerName(horseName);
			if(ownerName == null) ownerName = "§cN/A";
			tamerName = plugin.getHorseTamerName(horseName);
			if(tamerName == null && horse != null && horse instanceof Tameable)
				tamerName = ((Tameable)horse).isTamed() ? "§cUnknown" : "§cN/A";
			EntityType type = plugin.getHorseType(horseName);
			typeName = type == null ? null : TextUtils.capitalizeAndSpacify(type.name(), '_');
			speed = plugin.getHorseSpeed(horseName);
			jump = plugin.getHorseJump(horseName);
			health = plugin.getHorseHealth(horseName);
			strength = plugin.getLlamaStrength(horseName);
			parents = plugin.getHorseParents(horseName);
			age = plugin.getHorseAge(horseName);
			claim_timestamp = plugin.getHorseClaimTime(horseName);
			if(sender.hasPermission("horseowners.inspect.rankings")) rank = plugin.getRankings(horseName);
		}
		else{
			displayName = horse.getCustomName() == null ? "§cN/A" : horse.getCustomName();
			ownerName = "§cN/A";
			tamerName = horse instanceof Tameable && ((Tameable)horse).isTamed() ?
					(((Tameable)horse).getOwner() == null ? "§cUnknown" : ((Tameable)horse).getOwner().getName()) : "§cN/A";
			if(horse instanceof Attributable){
				speed = HorseLibrary.getNormalSpeed((Attributable)horse);
				health = HorseLibrary.getNormalMaxHealth((Attributable)horse);
				if(horse instanceof AbstractHorse) jump = HorseLibrary.getNormalJump((AbstractHorse)horse);
				if(horse instanceof Llama) strength = ((Llama)horse).getStrength();
			}
			parents = plugin.getHorseParents(horse);
			DNA = getCondensedDNA(horse);
			locX = horse.getLocation().getBlockX();
			locZ = horse.getLocation().getBlockZ();
			typeName = TextUtils.capitalizeAndSpacify(horse.getType().name(), '_');
			Long status_or_claim_ts = HorseLibrary.getTimeClaimed(horse);
			if(status_or_claim_ts != null) claim_timestamp = status_or_claim_ts;
			age = horse.getTicksLived()*50L;
		}

		//Build info message
		StringBuilder builder = new StringBuilder("\n");
		if(sender.hasPermission("horseowners.inspect.name")) builder.append("§7Name: §f").append(displayName);
		if(!ONLY_ONE_HORSE_TYPE && typeName != null && sender.hasPermission("horseowners.inspect.type"))
			builder.append("\n§7Species: §6").append(typeName);

		if(speed > 0 && sender.hasPermission("horseowners.inspect.speed")){
			builder.append("\n§7Speed: §f").append(speed).append("§cm/s");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[0]);
				if(rank[0] != rank[1]) builder.append('-').append(rank[1]);
				builder.append("§7]");
			}
		}
		if(jump > 0 && sender.hasPermission("horseowners.inspect.jump")){
			builder.append("\n§7Jump: §f").append(jump).append("§cm");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[2]);
				if(rank[2] != rank[3]) builder.append('-').append(rank[3]);
				builder.append("§7]");
			}
		}
		if(health > 0 && sender.hasPermission("horseowners.inspect.health")){
			builder.append("\n§7Health: §f").append(health).append("§ch");
			if(rank != null){
				builder.append("§7 [§6#").append(rank[4]);
				if(rank[4] != rank[5]) builder.append('-').append(rank[5]);
				builder.append("§7]");
			}
		}
		if(strength > 0 && sender.hasPermission("horseowners.inspect.strength")){
			builder.append("\n§7Strength: §f").append(strength).append("§ch");
		}
		if(age > 0 && sender.hasPermission("horseowners.inspect.age")){
			builder.append("\n§7Age: §f").append(TextUtils.formatTime(age, false, ChatColor.WHITE, ChatColor.RED, ChatColor.GRAY));
		}
		if(claim_timestamp > 0 && sender.hasPermission("horseowners.inspect.claimtime")){
			String formatted_date = new SimpleDateFormat("MMM.dd, YYYY").format(new Date(claim_timestamp));
			builder.append("\n§7Claimed: §f").append(formatted_date);
		}
		if(sender.hasPermission("horseowners.inspect.tamer")){
			builder.append("\n§7Tamer: §f").append(tamerName);
		}
		if(sender.hasPermission("horseowners.inspect.owner")){
			builder.append("\n§7Owner: §f").append(ownerName);
		}
		if(sender.hasPermission("horseowners.inspect.lineage") && parents != null && !parents.isEmpty()){
			builder.append("\n§7Parents: §f").append(String.join("§7, §f", parents));
		}
		if(sender.hasPermission("horseowners.inspect.dna") && DNA != null){
			builder.append("\n§7DNA: §f").append(DNA);
		}
		if(sender.hasPermission("horseowners.inspect.coords") && locX != null){
			builder.append("\n§7Location: §f").append(locX).append("§cx§7, §f").append(locZ).append("§cz");
		}

		sender.sendMessage(builder.toString());
		COMMAND_SUCCESS = true;
		return true;
	}
}