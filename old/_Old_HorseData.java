package Evil_Code_HorseManager;

import java.util.UUID;

public class _Old_HorseData {
	String name;
	UUID owner;
	boolean locked;
	int x_coord, z_coord;
	double jump, speed, health;
	
	public _Old_HorseData(String n, UUID o, boolean l, int x, int z, double j, double s, double h){
		name = n; owner = o; locked = l;
		x_coord = x; z_coord = z;
		jump = j; speed = s; health = h;
	}
	
	@Override public String toString(){
		return new StringBuilder().append('[')
				.append(name).append(',')
				.append(owner).append(',')
				.append(locked).append(',')
				.append(x_coord).append(',')
				.append(z_coord).append(',')
				.append(jump).append(',')
				.append(speed).append(',')
				.append(health).append(']')
				.toString();
	}
}
