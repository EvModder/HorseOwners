package net.evmodder.HorseOwners.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import net.evmodder.HorseOwners.HorseUtils;
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
			String arg = HorseUtils.cleanName(String.join(" ", args));
			return (sender.hasPermission("horseowners.inspect.others")
					? (sender.hasPermission("horseowners.inspect.unclaimed")
					? plugin.getAPI().getAllHorses() : plugin.getAPI().getAllClaimedHorses())
					: plugin.getAPI().getHorses(((Player)sender).getUniqueId())
					).stream().filter(name -> name.startsWith(arg)).limit(20).collect(Collectors.toList());
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
		String dna = HorseUtils.getDNA(horse, null);
		if(dna == null) return null;
		char[] dnaChars = dna.toLowerCase().toCharArray();
		for(int i=0; i<dnaChars.length; ++i) dnaChars[i] = dnaMap[(int)(dnaChars[i] - 'a')];// Assumes all DNA chars are [a-z]
		return TextUtils.pxSubstring(new String(dnaChars), 320-6*4, false).str;
	}

	private Entity findPhysicalHorse(Player player){
		if(player == null) return null;
		if(player.isInsideVehicle() && plugin.getAPI().isClaimableHorseType(player.getVehicle())){
			return player.getVehicle();
		}
		if(player.getSpectatorTarget() != null && plugin.getAPI().isClaimableHorseType(player.getSpectatorTarget())){
			return player.getSpectatorTarget();
		}
		if(player.getSpectatorTarget() != null && player.getSpectatorTarget().isInsideVehicle()
			&& plugin.getAPI().isClaimableHorseType(player.getSpectatorTarget().getVehicle())){
			return player.getSpectatorTarget().getVehicle();
		}
		return null;
	}

	@Override public boolean onHorseCommand(CommandSender sender, Command command, String label, String args[]){
		//cmd:	/hm inspect [horse]
		Player p = (sender instanceof Player) ? (Player)sender : null;
		Entity horse = null;
		String cleanHorseName = null;
		if((args.length > 0 && plugin.getAPI().horseExists(cleanHorseName = HorseUtils.cleanName(String.join(" ", args))))){
			if(p != null && !p.hasPermission("horseowners.inspect.others") && !plugin.getAPI().canAccess(p.getUniqueId(), cleanHorseName)){
				sender.sendMessage(ChatColor.RED+"You cannot inspect horses which you do not own");
				COMMAND_SUCCESS = false;
				return true;
			}
			if(!INSPECT_UNTAMED && plugin.getAPI().getHorseTamer(cleanHorseName) == null
					&& !sender.hasPermission("horseowners.inspect.untamed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on untamed horses");
				COMMAND_SUCCESS = false;
				return true;
			}
			if(!INSPECT_UNCLAIMED && plugin.getAPI().getHorseOwner(cleanHorseName) == null
					&& !sender.hasPermission("horseowners.inspect.unclaimed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on unclaimed horses");
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else if((horse = findPhysicalHorse(p)) != null){
			if(!INSPECT_UNTAMED && 
					(horse instanceof Tameable == false || !((Tameable)horse).isTamed()) &&
					!sender.hasPermission("horseowners.inspect.untamed")){
				sender.sendMessage(ChatColor.RED+"You must tame this steed before you can use that command");
				COMMAND_SUCCESS = false;
				return true;
			}
			cleanHorseName = HorseUtils.cleanName(horse.getCustomName());
			if(!INSPECT_UNCLAIMED && (cleanHorseName == null || plugin.getAPI().getHorseOwner(cleanHorseName) == null)
					&& !sender.hasPermission("horseowners.inspect.unclaimed")){
				sender.sendMessage(ChatColor.RED+"You cannot use this command on unclaimed horses");
				sender.sendMessage(ChatColor.GRAY+"To claim this horse, use "
								+ChatColor.DARK_GREEN+"/hm claim"+(cleanHorseName == null ? " <name>" : ""));
				COMMAND_SUCCESS = false;
				return true;
			}
		}
		else if(args.length == 0){
			sender.sendMessage(ChatColor.RED+"Too few arguments!"+ChatColor.GRAY+'\n'+command.getUsage());
			COMMAND_SUCCESS = false;
			return true;
		}
		else if(!plugin.getAPI().horseExists(cleanHorseName)){
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
		List<String> passengers;
		if(cleanHorseName != null){
			if(horse != null){
				plugin.getAPI().updateDatabase(horse);
				locX = horse.getLocation().getBlockX();
				locZ = horse.getLocation().getBlockZ();
				DNA = getCondensedDNA(horse);
			}
			// Added permission check because getHorseLocation() is an expensive call
			else if(sender.hasPermission("horseowners.inspect.coords")){
				Location loc = plugin.getAPI().getHorseLocation(cleanHorseName);
				locX = loc.getBlockX();
				locZ = loc.getBlockZ();
			}
			else locX = locZ = null;
			passengers = plugin.getAPI().getPassengers(cleanHorseName).stream().map(uuid -> {
				Entity e = plugin.getServer().getEntity(uuid);
				return e == null || e.getName() == null ? uuid.toString() : e.getName();
			}).collect(Collectors.toList());
			displayName = plugin.getAPI().getHorseName(cleanHorseName);
			if(displayName == null) displayName = cleanHorseName;
			ownerName = plugin.getAPI().getHorseOwnerName(cleanHorseName);
			if(ownerName == null) ownerName = "§cN/A";
			tamerName = plugin.getAPI().getHorseTamerName(cleanHorseName);
			if(tamerName == null && horse != null && horse instanceof Tameable)
				tamerName = ((Tameable)horse).isTamed() ? "§cUnknown" : "§cN/A";
			EntityType type = plugin.getAPI().getHorseType(cleanHorseName);
			typeName = type == null ? null : TextUtils.capitalizeAndSpacify(type.name(), '_');
			speed = plugin.getAPI().getHorseSpeed(cleanHorseName);
			jump = plugin.getAPI().getHorseJump(cleanHorseName);
			health = plugin.getAPI().getHorseHealth(cleanHorseName);
			strength = plugin.getAPI().getLlamaStrength(cleanHorseName);
			parents = plugin.getAPI().getHorseParents(cleanHorseName);
			age = plugin.getAPI().getHorseAge(cleanHorseName);
			claim_timestamp = plugin.getAPI().getHorseClaimTime(cleanHorseName);
			if(sender.hasPermission("horseowners.inspect.rankings")) rank = plugin.getAPI().getHorseRankings(cleanHorseName);
		}
		else{
			displayName = horse.getCustomName() == null ? "§cN/A" : horse.getCustomName();
			ownerName = "§cN/A";
			tamerName = horse instanceof Tameable && ((Tameable)horse).isTamed() ?
					(((Tameable)horse).getOwner() == null ? "§cUnknown" : ((Tameable)horse).getOwner().getName()) : "§cN/A";
			if(horse instanceof Attributable){
				speed = HorseUtils.getNormalSpeed((Attributable)horse);
				health = HorseUtils.getNormalMaxHealth((Attributable)horse);
				if(horse instanceof AbstractHorse) jump = HorseUtils.getNormalJump((AbstractHorse)horse);
				if(horse instanceof Llama) strength = ((Llama)horse).getStrength();
			}
			parents = plugin.getAPI().getHorseParents(horse);
			DNA = getCondensedDNA(horse);
			locX = horse.getLocation().getBlockX();
			locZ = horse.getLocation().getBlockZ();
			passengers = horse.getPassengers().stream()
					.map(e -> e == null || e.getName() == null ? e.getUniqueId().toString() : e.getName()).collect(Collectors.toList());
			typeName = TextUtils.capitalizeAndSpacify(horse.getType().name(), '_');
			Long status_or_claim_ts = HorseUtils.getTimeClaimed(horse);
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
			builder.append("\n§7Age: §f").append(TextUtils.formatTime(age, /*show0s=*/false, ChatColor.WHITE, ChatColor.RED, ChatColor.GRAY));
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
			parents.replaceAll(cleanName -> plugin.getAPI().getHorseName(cleanName));
			builder.append("\n§7Parents: §f").append(String.join("§7, §f", parents));
		}
		if(sender.hasPermission("horseowners.inspect.dna") && DNA != null){
			builder.append("\n§7DNA: §f").append(DNA);
		}
		if(sender.hasPermission("horseowners.inspect.coords") && locX != null){
			builder.append("\n§7Location: §f").append(locX).append("§cx§7, §f").append(locZ).append("§cz");
		}
		if(sender.hasPermission("horseowners.inspect.passengers") && passengers != null && !passengers.isEmpty()){
			if(passengers.size() == 1) builder.append("\n§7Passenger: §f").append(passengers.get(0));
			else builder.append("\n§7Passengers: §f").append(passengers);
		}

		sender.sendMessage(builder.toString());
		COMMAND_SUCCESS = true;
		return true;
	}
}